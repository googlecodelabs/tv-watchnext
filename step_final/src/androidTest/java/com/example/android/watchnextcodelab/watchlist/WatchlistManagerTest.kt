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

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import com.example.android.watchnextcodelab.createMovieWithTitle
import com.example.android.watchnextcodelab.database.MockDatabase
import com.example.android.watchnextcodelab.database.SharedPreferencesDatabase
import com.example.android.watchnextcodelab.model.Category
import com.example.android.watchnextcodelab.model.Movie
import org.hamcrest.collection.IsCollectionWithSize.hasSize
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.lang.reflect.Array as JavaArray

@RunWith(AndroidJUnit4::class)
@SmallTest
class WatchlistManagerTest {

    private lateinit var manager: WatchlistManager

    @Mock
    private lateinit var mockSharedPrefDatabase: SharedPreferencesDatabase

    private lateinit var mockDatabase: MockDatabase
    private lateinit var context: Context

    private lateinit var one: Movie

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        one = createMovieWithTitle(id =1, title = "one")

        context = InstrumentationRegistry.getTargetContext()

        `when`(mockSharedPrefDatabase.findCategories(context)).thenReturn(emptyList())

        mockDatabase = MockDatabase(mockSharedPrefDatabase)

        manager = WatchlistManager(mockDatabase)

        // Clear the watchlist before the first run test.
        teardown()
    }

    @After
    fun teardown() {
        manager.removeMovieFromWatchlist(context, one.movieId)
    }

    @Test
    fun addToWatchlistCategory() {

        // Assert that the Watchlist category does not exist before we add the first movie.
        assertWatchlistDoesNotExist()

        manager.addToWatchlist(context, one.movieId)

        // Verify the category has been persisted.
        assertWatchlistDoesExist()

        val category = mockDatabase.findCategoryById(context, WATCHLIST_CATEGORY_ID)
        if (category == null ) {
            fail("There should be one category with the Watchlist id.")
        } else {
            val (_, _, _, movies) = category
            assertThat(
                    "There should be one movie in the Watchlist category.",
                    movies,
                    hasSize(1))
        }
    }

    @Test
    fun addToWatchlistCategory_prevent_adding_duplicate_movies() {

        // Assert that the Watchlist category does not exist before we add the first movie.
        assertWatchlistDoesNotExist()

        manager.addToWatchlist(context, one.movieId)

        // Assert that movie has been added.
        mockDatabase.findCategoryById(context, WATCHLIST_CATEGORY_ID)?.let { (_, _, _, movies) ->
            assertThat("There should be one movie persisted", movies, hasSize(1))
        }
        // Add the same movie again and check that it did not add the duplicate.
        manager.addToWatchlist(context, one.movieId)

        // Test that the duplicate was not added.
        val category = mockDatabase.findCategoryById(context, WATCHLIST_CATEGORY_ID)
        if (category == null ) {
            fail("There should be one movie added to the Watchlist category.")
        } else {
            val (_, _, _, movies) = category
            assertThat(
                    "There should only be one movie in the Watchlist category even though a duplicate was added.",
                    movies,
                    hasSize(1))
        }
    }

    @Test
    fun removeMovieFromWatchlist_removed_category_if_empty() {

        // Prime the list with a movie to be removed.
        manager.addToWatchlist(context, one.movieId)
        assertWatchlistDoesExist()

        manager.removeMovieFromWatchlist(context, one.movieId)
        assertWatchlistDoesNotExist()
    }

    @Test
    fun removeMovieFromWatchlist_updates_hotlist() {

        // Prime the list with a movie to be removed.
        manager.addToWatchlist(context, one.movieId)
        assertThat("There should be 3 movies in the watch next list",
                mockDatabase.findAllCategories(context), hasSize(3))

        val categories = mockDatabase.getLiveCategories(context)

        manager.removeMovieFromWatchlist(context, one.movieId)

        categories.observeForever { updated -> assertThat<List<Category>>(updated, hasSize(2)) }
    }

    private fun assertWatchlistDoesNotExist() {
        assertThatWatchlist(exists = false)
    }

    private fun assertWatchlistDoesExist() {
        assertThatWatchlist(exists = true)
    }

    private fun assertThatWatchlist(exists: Boolean) {
        val category = mockDatabase.findCategoryById(context, WATCHLIST_CATEGORY_ID)
        assertThat(
                "The watchlist category should ${if (exists) "" else "not "}exist.",
                category != null,
                `is`(exists))
    }
}
