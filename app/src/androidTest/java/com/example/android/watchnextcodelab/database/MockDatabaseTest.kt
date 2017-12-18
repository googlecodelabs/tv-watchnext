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

package com.example.android.watchnextcodelab.database

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import com.example.android.watchnextcodelab.model.MovieProgramId
import net.bytebuddy.matcher.CollectionItemMatcher
import org.hamcrest.collection.IsCollectionWithSize.hasSize
import org.hamcrest.collection.IsEmptyCollection.empty
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsCollectionContaining
import org.hamcrest.core.IsCollectionContaining.hasItems
import org.hamcrest.core.IsEqual.equalTo
import org.hamcrest.core.IsNot.not
import org.hamcrest.core.IsNull.nullValue
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class MockDatabaseTest {

    private lateinit var database: MockDatabase

    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getTargetContext()

        database = MockDatabase(SharedPreferencesDatabase())
    }

    @After
    fun teardown() {
        // Reset the data
        database.deleteMovieProgramIds(context)
    }

    @Test
    fun saveMovieProgramIds() {

        database.saveMovieProgramIds(context, ids)

        val savedIds = database.findAllMovieProgramIds(context)

        assertThat("${ids.size} movie/program ids should have been persisted",
                savedIds, hasSize(ids.size))

        val movieProgramId = savedIds[0]
        assertThat("Movie/Program id should be persisted",
                movieProgramId, `is`(not(nullValue())))
        assertThat("Movie/Program id program ids are not persisted correctly",
                movieProgramId.programIds, hasItems(1L, 2L))
        assertThat("Movie/Program id watch next is is not persisted correctly",
                movieProgramId.watchNextProgramId, `is`(equalTo(1L)))
    }

    @Test
    fun saveMovieProgramIds_updates_existing_ids() {
        // Save an initial set of ids.
        database.saveMovieProgramIds(context, ids)

        // Perform persisting update.
        database.saveMovieProgramIds(context, listOf(oneWithUpdate, two, three))

        val movieProgramId = database.findMovieProgramId(context, ONE_ID)
        assertThat("Movie/Program id should have been updated and should still exist",
                movieProgramId, `is`(not(nullValue())))
        movieProgramId?.let {
            assertThat("Program id was not updated",
                    movieProgramId.programIds, hasSize(2))
            assertThat("Program id was not updated",
                    movieProgramId.programIds[0], `is`(equalTo(10L)))
            assertThat("Watch Next id was updated",
                    movieProgramId.watchNextProgramId, `is`(equalTo(2L)))
        }
    }

    @Test
    fun saveMovieProgramIds_remove_if_ids_are_not_valid() {
        // Save an initial set of ids.
        database.saveMovieProgramIds(context, ids)

        database.updateMovieProgramId(context, ONE_ID, emptyList(), watchNextProgramId = -1L)

        val movieProgramId = database.findMovieProgramId(context, ONE_ID)
        assertThat("Movie/Program id should have been removed",
                movieProgramId, `is`(nullValue()))
    }

    @Test
    fun saveMovieProgramIds_remove_watch_next_id() {
        // Save an initial set of ids.
        database.saveMovieProgramIds(context, ids)

        val savedIds = database.findAllMovieProgramIds(context)
        // Simple assertion that the composite id was persisted.
        assertThat("${ids.size} movie/program id should have been persisted",
                savedIds, hasSize(ids.size))

        database.updateMovieProgramId(context, ONE_ID, watchNextProgramId = -1L)

        val movieProgramId = database.findMovieProgramId(context, ONE_ID)
        assertThat("Movie/Program id not be null", movieProgramId, `is`(not(nullValue())))
        movieProgramId?.let {
            assertThat("Movie/Program should not have a watch next id",
                    movieProgramId.watchNextProgramId, `is`(equalTo(-1L)))
            assertThat("Movie/Program should still have program ids associated with it",
                    movieProgramId.programIds, hasSize(2))
        }
    }

    @Test
    fun saveMovieProgramIds_remove_program_ids() {
        // Save an initial set of ids.
        database.saveMovieProgramIds(context, ids)

        val savedIds = database.findAllMovieProgramIds(context)
        // Simple assertion that the composite id was persisted.
        assertThat("${ids.size} movie/program id should have been persisted",
                savedIds, hasSize(ids.size))

        database.updateMovieProgramId(context, ONE_ID, programIds = emptyList())

        val movieProgramId = database.findMovieProgramId(context, ONE_ID)
        assertThat("Movie/Program id not be null", movieProgramId, `is`(not(nullValue())))
        movieProgramId?.let {
            assertThat("Movie/Program should have a watch next id",
                    movieProgramId.watchNextProgramId, `is`(equalTo(1L)))
            // IsNot.not() matcher is not working, this is a work around for `is`(not(empty()))
            assertThat("Movie/Program should not have any program ids",
                    movieProgramId.programIds, `is`(empty()))
        }
    }

    @Test
    fun removeProgramId_updated_list_Of_program_ids() {
        // Save an initial set of ids.
        database.saveMovieProgramIds(context, ids)

        database.removeProgramId(context, ONE_ID, programId = 2)

        val movieProgrmaId = database.findMovieProgramId(context, ONE_ID)

        assertThat("A movie / program id should still exist for movie $ONE_ID",
                movieProgrmaId, `is`(not(nullValue())))
        movieProgrmaId?.let {
            assertThat("A program id should have been removed from MovieProgram $ONE_ID",
                    movieProgrmaId.programIds, hasSize(1))
            assertThat("Program id 2 should have been removed",
                    movieProgrmaId.programIds[0], `is`(equalTo(1L)))
        }
    }

    private val ONE_ID = 1L
    private val one =
            MovieProgramId(movieId = ONE_ID, programIds = listOf(1, 2), watchNextProgramId = 1)
    private val two = MovieProgramId(movieId = 2, programIds = listOf(3), watchNextProgramId = 2)
    private val three = MovieProgramId(movieId = 3, programIds = listOf(4), watchNextProgramId = 3)
    private val oneWithUpdate = one.copy(programIds = listOf(10, 20), watchNextProgramId = 2L)

    private val ids = listOf(one, two, three)
}