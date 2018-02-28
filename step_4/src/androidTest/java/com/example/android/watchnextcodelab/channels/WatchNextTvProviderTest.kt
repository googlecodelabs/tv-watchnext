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

package com.example.android.watchnextcodelab.channels

import android.content.ContentResolver
import android.content.Context
import android.media.tv.TvContract
import android.support.media.tv.TvContractCompat
import android.support.test.InstrumentationRegistry
import android.support.test.filters.MediumTest
import android.support.test.runner.AndroidJUnit4
import com.example.android.watchnextcodelab.createMovieWithTitle
import com.example.android.watchnextcodelab.model.Movie
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsEqual.equalTo
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith


private const val MOCK_MOVIE_TITLE = "Mock Movie Title"
/**
 * Instruments adding and removing programs to and from the Watch Next row. Verification will occur
 * against the Tv Provider. This test clears out the Watch Next (for this app only) row before and
 * after each test to test against a clean environment. If you have data (from this app) that you do
 * not want removed from the Watch Next row, you should [Ignore] this test suite.
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class WatchNextTvProviderTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver

    private val movie: Movie = createMovieWithTitle(id = 1, title = MOCK_MOVIE_TITLE)

    @Before
    fun setup() {
        context = InstrumentationRegistry.getTargetContext()
        contentResolver = context.contentResolver

        clearWatchNextRow()
    }

    @After
    fun teardown() {
        clearWatchNextRow()
    }

    @Test
    fun addToWatchNextWatchlist_base_case_adds_a_movie() {

        val programId = addToWatchNextWatchlist(context, movie)
        assertThat("A valid program id should have been returned",
                programId, `is`(greaterThan(-1L)))
        assertThatWatchNextHasSize(size = 1)
    }

    @Test
    fun addToWatchNextWatchlist_updates_existing_movie() {

        val programId = addToWatchNextWatchlist(context, movie)
        assertThat("A valid program id should have been returned",
                programId, `is`(greaterThan(-1L)))
        assertThatWatchNextHasSize(size = 1)

        // Add the same movie again and verify there is only one movie in the watch next row
        // implying that the movie was updated. This also verifies that we prevent adding
        // duplicates.
        val updateProgramId = addToWatchNextWatchlist(context, movie)
        assertThat("The program id should not have changed",
                updateProgramId, `is`(equalTo(programId)))
        assertThatWatchNextHasSize(size = 1)
    }

    private fun clearWatchNextRow() {
        contentResolver.delete(TvContractCompat.WatchNextPrograms.CONTENT_URI, null, null)

        assertThatWatchNextHasSize(size = 0)
    }

    private fun assertThatWatchNextHasSize(size: Int) {
        contentResolver.query(
                TvContract.WatchNextPrograms.CONTENT_URI,
                null, null, null, null).use {
            assertThat("Watch next row should have size $size", it.count, `is`(equalTo(size)))
        }
    }
}