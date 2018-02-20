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

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import com.example.android.watchnextcodelab.createMovieWithTitle
import com.example.android.watchnextcodelab.database.MockDatabase
import com.example.android.watchnextcodelab.model.Category
import com.example.android.watchnextcodelab.model.Movie
import com.google.common.collect.Lists
import com.nhaarman.mockito_kotlin.doNothing
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.collection.IsCollectionWithSize.hasSize
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
@SmallTest
class SynchronizeChannelServiceTest {

    private lateinit var service: SynchronizeChannelService

    private lateinit var context: Context

    private lateinit var mockDatabase: MockDatabase

    @Before
    fun setup() {
        mockDatabase = mock()

        doNothing().`when`(mockDatabase).saveMovieProgramIds(any(), any())

        context = InstrumentationRegistry.getTargetContext()
        service = SynchronizeChannelService(mockDatabase)

        // Start each test with a clean workspace.
        deleteChannels(context)
    }

    @After
    fun teardown() {
        // After the last test, leave the device in a clean state.
        deleteChannels(context)
    }

    @Test
    fun synchronizeChannels_adds_new_channels() {

        preloadTvProviderWithChannels()

        assertThat("A channel id has not been set on category A.",
                categoryA.channelId,
                `is`(greaterThan(0L)))

        // Confirm that programs have been added.
        val channels = getChannels(context)
        channels.forEach { channelId ->
            val programIds = loadProgramsForChannel(context, channelId)
            // There are three movies in every category. Verify consistent for each channel.
            assertThat("The channel should have the programs saved.", programIds, hasSize(3))
        }
    }

    @Test
    fun synchronizeChannels_add_new_channel_to_existing_channels() {

        // Load 2 channels to start the test.
        val categories = Lists.newArrayList(preloadTvProviderWithChannels())

        // Add a third channel to be added.
        categories.add(categoryC)

        whenever(mockDatabase.findAllCategories(eq(context))).thenReturn(categories)

        service.synchronizeChannels(context)

        // Test that the new channel also get's published.
        assertThatTvProviderHasChannels(expectedNumberOfChannels = 3)
    }

    @Test
    fun synchronizeChannels_remove_old_channel() {

        preloadTvProviderWithChannels()

        // Update the category list by removing a previously published category.
        val categories = mutableListOf(categoryB)
        whenever(mockDatabase.findAllCategories(eq(context))).thenReturn(categories)

        service.synchronizeChannels(context)

        // Test that the old channel is unpublished.
        assertThatTvProviderHasChannels(1)
    }

    @Test
    fun synchronizeChannels_remove_old_programs_from_channels() {

        // Load 2 channels to start the test.
        val categories = preloadTvProviderWithChannels()

        // Remove the first program from the first category.
        categoryA.movies.removeAt(0)
        whenever(mockDatabase.findAllCategories(eq(context))).thenReturn(categories)

        service.synchronizeChannels(context)

        assertThat("A channel id has not been set on category A.",
                categoryA.channelId,
                `is`(greaterThan(0L)))

        val programs = loadProgramsForChannel(context, categoryA.channelId)
        assertThat("Program has not been removed from category A.", programs, hasSize(2))
    }

    @Test
    fun synchronizeChannels_updates_old_programs_from_channels() {

        // Load 2 channels to start the test.
        val categories = preloadTvProviderWithChannels()

        // Change the title of Movie One in category A.
        categoryA.movies.removeAt(0)
        categoryA.movies.add(createMovieWithTitle(id = movieOne.movieId, title = "new title"))
        whenever(mockDatabase.findAllCategories(eq(context))).thenReturn(categories)

        service.synchronizeChannels(context)

        val programs = loadProgramsForChannel(context, categoryA.channelId)
        assertThat("Programs have been added or removed from category A when they should only be updated.",
                programs, hasSize(3))
        val program = programs.find { program -> program.title == "new title" }
        assertTrue("The program did not update it's title to 'new title'.", program != null)
    }

    @Test
    fun synchronizeChannels_add_new_programs_from_channels() {

        // Load 2 channels to start the test.
        val categories = preloadTvProviderWithChannels()

        // Add a new movie to category A.
        categoryA.movies.add(createMovieWithTitle(id = 4, title = "four"))
        whenever(mockDatabase.findAllCategories(eq(context))).thenReturn(categories)

        service.synchronizeChannels(context)

        val programs = loadProgramsForChannel(context, categoryA.channelId)
        assertThat("Program has not been added from category A.", programs, hasSize(4))
    }

    private fun preloadTvProviderWithChannels(): MutableList<Category> {
        // Load 2 channels into the Tv Provider.
        val categories = Arrays.asList<Category>(categoryA, categoryB)
        whenever(mockDatabase.findAllCategories(eq(context))).thenReturn(categories)
        assertThatTvProviderHasChannels(expectedNumberOfChannels = 0)
        service.synchronizeChannels(context)
        assertThatTvProviderHasChannels(expectedNumberOfChannels = 2)

        return categories
    }

    private fun assertThatTvProviderHasChannels(expectedNumberOfChannels: Int) {
        val channels = getChannels(context)
        assertThat("Channels do not have the expected size.",
                channels,
                hasSize(expectedNumberOfChannels))
    }

    private val movieOne: Movie = createMovieWithTitle(id = 1, title = "one")
    private val movieTwo: Movie = createMovieWithTitle(id = 2, title = "two")
    private val movieThree: Movie = createMovieWithTitle(id = 3, title = "three")
    private val movies: MutableList<Movie> = mutableListOf(movieOne, movieTwo, movieThree)
    private val categoryA: Category =
            Category(id = "1", name = "a", description = "a", movies = movies)
    private val categoryB: Category =
            Category(id = "2", name = "b", description = "b", movies = movies)
    private val categoryC: Category =
            Category(id = "3", name = "c", description = "c", movies = movies)

}