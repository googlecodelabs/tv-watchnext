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
import android.support.media.tv.TvContractCompat
import com.example.android.watchnextcodelab.channels.ChannelTvProviderFacade.ProgramMetadata
import com.example.android.watchnextcodelab.database.MockDatabase
import com.example.android.watchnextcodelab.model.Category
import com.example.android.watchnextcodelab.model.Movie
import com.example.android.watchnextcodelab.model.MovieProgramId
import com.google.common.collect.BiMap

/**
 * Synchronizes the backend with the TV Provider's channels. Compares the new categories from the
 * backend with the channels already in the TV Provider and performs the following.
 *
 *  1. Any channels that are not present in the current list of categories are unpublished.
 *  1. Any new categories are added as a new channel.
 *  1. Synchronize programs for any channels that have been publish and are still valid.
 *
 * A category's programs are synchronized in the following way:
 *
 *  1. Any programs no longer available in the category will be removed from the channel.
 *  1. Update all metadata on any program whose title has changed.
 *  1. Any remaining programs associated with the category are added to the channel.
 *
 * This service should run in the background as it performs multiple calls to the database and TV
 * Provider.
 */
class SynchronizeChannelService(private val database: MockDatabase) {

    fun synchronizeChannels(context: Context) {

        // Map of current channels in the TV Provider.
        val tvProviderChannelCategoryIds: BiMap<Long, String> =
                ChannelTvProviderFacade.findChannelCategoryIds(context)

        // Channels that should be on the home screen and loaded into the TV Provider.
        val categories = database.findAllCategories(context)
        val categoryIds: List<String> = categories.map { it.id }

        // Remove any channel that exist in the TV Provider but that channel has been removed from
        // the app.
        val channelsToUnpublish: List<Long> = tvProviderChannelCategoryIds.entries
                // Filter for categories that we want to remove.
                .filter { entry -> !categoryIds.contains(entry.value) }
                // Map to a set of channel ids.
                .map { it.key }

        for (channelId in channelsToUnpublish) {
            ChannelTvProviderFacade.deleteChannel(context, channelId)
            tvProviderChannelCategoryIds.remove(channelId)
        }

        // Load programs for already published channels
        val channelProgramMap: Map<Long, List<ProgramMetadata>> =
                tvProviderChannelCategoryIds.asSequence()
                        // Maps channelId -> programs
                         .associateBy(
                                 { it.key },
                                 { ChannelTvProviderFacade.loadProgramsForChannel(context, it.key) }
                         )

        // Add the remaining categories if they do not exist in the TV Provider or update their
        // programs.
        val publishedCategories = tvProviderChannelCategoryIds.values

        for (category in categories) {
            if (!publishedCategories.contains(category.id)) {
                val channelId = ChannelTvProviderFacade.addChannel(context, category)
                category.channelId = channelId
                TvContractCompat.requestChannelBrowsable(context, channelId)
                val ids = ChannelTvProviderFacade.addPrograms(context, channelId, category)
                database.saveMovieProgramIds(context, ids)
            } else {
                val channelId: Long = tvProviderChannelCategoryIds.inverse()[category.id] as Long
                val publishedPrograms = channelProgramMap[channelId]
                publishedPrograms?.let {
                    updateChannel(context = context,
                            channelId = channelId,
                            category = category,
                            publishedPrograms = it)
                }
            }
        }
    }

    private fun updateChannel(context: Context,
                              channelId: Long,
                              category: Category,
                              publishedPrograms: List<ProgramMetadata>) {
        val publishedMovieIds: List<String> = publishedPrograms.map { it.id }

        val serverPrograms: List<String> = category.movies.map { it.movieId.toString() }

        // Programs to remove.
        val programsMetadataToRemove: List<ProgramMetadata> = publishedPrograms
                .filter { program -> !serverPrograms.contains(program.id) }
        val programsToRemove: List<Long> = programsMetadataToRemove.map { it.programId }

        // Remove the programs from the TV Provider.
        programsToRemove
                .forEach { programId -> ChannelTvProviderFacade.deleteProgram(context, programId) }
        // Remove the program id from the composite key
        programsMetadataToRemove
                .forEach { program ->
                    database.removeProgramId(context, program.id.toLong(), program.programId)
                }

        // Programs to update.
        val programsToUpdate: List<ProgramMetadata> = publishedPrograms
                .filter { program -> !programsToRemove.contains(program.programId) }
                .filter { program ->
                    val movie = findMovieById(program.id, category.movies)
                    movie != null && program.title != movie.title
                }

        // Update programs in the TV Provider.
        programsToUpdate.forEach { program ->
            findMovieById(program.id, category.movies)?.apply {
                ChannelTvProviderFacade.updateProgram(
                        context = context, programId = program.programId, movie = this)
            }
        }

        // Programs to publish.
        val programsToPublish: List<Movie> = category.movies
                .filter { movie -> !publishedMovieIds.contains(movie.movieId.toString()) }

        // Add new programs to channel in the TV Provider.
        val movieProgramIds =
                programsToPublish.associateBy({ it.movieId }, { mutableListOf<Long>() })
        programsToPublish
                .forEach { movie ->
                    // Giving a weight of 0 so that the program will be added to the end of the
                    // channel.
                    val programId = ChannelTvProviderFacade.addProgram(
                            context = context, channelId = channelId, movie = movie, weight = 0)
                    movieProgramIds[movie.movieId]?.apply { this += programId }
                }
        val ids = movieProgramIds.map { MovieProgramId(it.key, it.value) }
        if (!ids.isEmpty()) {
            database.saveMovieProgramIds(context, ids)
        }
    }

    private fun findMovieById(id: String, movies: List<Movie>): Movie? {
        return movies.find { movie -> id.toLong() == movie.movieId }
    }
}
