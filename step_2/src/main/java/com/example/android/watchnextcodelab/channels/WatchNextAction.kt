/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.example.android.watchnextcodelab.channels

import android.content.Context
import android.support.annotation.WorkerThread
import com.example.android.watchnextcodelab.database.MockDatabase
import com.example.android.watchnextcodelab.database.SharedPreferencesDatabase
import com.example.android.watchnextcodelab.watchlist.WatchlistManager

/**
 * Functions for managing the watch next row.
 */
object WatchNextAction {

    private val database = MockDatabase.get()

    @WorkerThread
    fun watchNext(context: Context, movieId: Long) {
        val movie = database.findMovieById(movieId) ?: return

        val watchNextId = WatchNextTvProvider.addToWatchNextNext(context, movie)

        database.updateMovieProgramId(context, movieId, watchNextProgramId = watchNextId)
    }

    @WorkerThread
    fun addToContinueWatching(context: Context, movieId: Long, playbackPosition: Int) {
        val movie = database.findMovieById(movieId) ?: return

        val watchNextId = WatchNextTvProvider.addToWatchNextContinue(context, movie, playbackPosition)

        database.updateMovieProgramId(context, movieId, watchNextProgramId = watchNextId)

        SharedPreferencesDatabase().savePlaybackPosition(context, movie, playbackPosition)
    }

    @WorkerThread
    fun removeFromContinueWatching(context: Context, movieId: Long) {
        WatchNextTvProvider.deleteFromWatchNext(context, movieId.toString())

        SharedPreferencesDatabase().deletePlaybackPosition(context, movieId.toString())

        // If the user finishes watching a movie in the watch list, then remove it from the list.
        WatchlistManager.get().removeMovieFromWatchlist(context, movieId)
    }

    @WorkerThread
    fun addToWatchlist(context: Context, movieId: Long) {
        WatchlistManager.get().addToWatchlist(context, movieId)
    }

    @WorkerThread
    fun removeFromWatchlist(context: Context, movieId: Long) {
        WatchlistManager.get().removeMovieFromWatchlist(context, movieId)
    }
}
