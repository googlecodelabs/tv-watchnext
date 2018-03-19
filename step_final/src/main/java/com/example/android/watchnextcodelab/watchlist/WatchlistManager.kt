/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.example.android.watchnextcodelab.watchlist

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import com.example.android.watchnextcodelab.channels.WatchNextTvProvider
import com.example.android.watchnextcodelab.database.MockDatabase
import com.example.android.watchnextcodelab.model.Category
import com.example.android.watchnextcodelab.model.Movie

const val WATCHLIST_CATEGORY_ID = "3"

class WatchlistManager constructor(private val database: MockDatabase = MockDatabase.get()) {

    private val liveWatchlist = MutableLiveData<Category>()

    fun getLiveWatchlist(): LiveData<Category> {
        return liveWatchlist
    }

    fun addToWatchlist(context: Context, movieId: Long) {

        val movie = database.findMovieById(movieId) ?: return

        val categories = database.findAllCategories(context)

        // Check if the category has been added.
        val categoryOption = database.findCategoryById(context, WATCHLIST_CATEGORY_ID)
        val watchlistCategory: Category = categoryOption ?:
                // Add the watch list category if it does not exists.
                createWatchlistCategory().apply {
                    // Ensure the watchlist is the first category is the list.
                    categories.add(0, this)
                }

        // Only add the movie if it has not been added to the watch list before.
        val existingMovie = watchlistCategory.movies.find { m -> m.movieId == movie.movieId }
        if (existingMovie == null) {
            watchlistCategory.movies as MutableList += movie
            liveWatchlist.postValue(watchlistCategory)
        }

        database.saveCategories(context, categories)

        val watchNextId = WatchNextTvProvider.addToWatchNextWatchlist(context, movie)

        database.updateMovieProgramId(context, movieId, watchNextProgramId = watchNextId)
    }

    fun isInWatchlist(context: Context, movie: Movie): Boolean {
        val category = database.findCategoryById(context, WATCHLIST_CATEGORY_ID) ?: return false
        val (_, _, _, movies) = category

        return movies.any { m -> m.movieId == movie.movieId }
    }

    fun removeMovieFromWatchlist(context: Context, movieId: Long) {

        val movie = database.findMovieById(movieId) ?: return

        withWatchlistCategory(context) {
            database.removeFromCategory(context, it, movie)
            liveWatchlist.postValue(it)
            if (it.movies.isEmpty()) {
                removeWatchlist(context)
            }
        }

        WatchNextTvProvider.deleteFromWatchNext(context, movieId.toString())
        database.updateMovieProgramId(context, movieId, watchNextProgramId = -1L)
    }

    private fun removeWatchlist(context: Context) {
        withWatchlistCategory(context) {
            database.removeCategory(context, it)
            liveWatchlist.postValue(it)
        }
    }

    private fun withWatchlistCategory(context: Context, action: (category: Category) -> Unit) {
        database.findCategoryById(context, WATCHLIST_CATEGORY_ID)?.apply {
            action(this)
        }
    }

    companion object {

        private val INSTANCE = WatchlistManager()

        fun get(): WatchlistManager {
            return INSTANCE
        }
    }
}

private fun createWatchlistCategory(): Category {
    return Category(
            id = WATCHLIST_CATEGORY_ID,
            name = "Watchlist",
            description = "Movies that you wish to watch.")
}
