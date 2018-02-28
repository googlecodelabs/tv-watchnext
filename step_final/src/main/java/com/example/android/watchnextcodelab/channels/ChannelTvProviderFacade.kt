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

import android.content.ComponentName
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.annotation.DrawableRes
import android.support.annotation.VisibleForTesting
import android.support.annotation.WorkerThread
import android.support.media.tv.Channel
import android.support.media.tv.ChannelLogoUtils
import android.support.media.tv.PreviewProgram
import android.support.media.tv.TvContractCompat
import android.util.Log
import com.example.android.watchnextcodelab.MainActivity
import com.example.android.watchnextcodelab.R
import com.example.android.watchnextcodelab.model.Category
import com.example.android.watchnextcodelab.model.Movie
import com.example.android.watchnextcodelab.model.MovieProgramId
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import java.util.*

private const val TAG = "ChannelTvProviderFacade"

private const val SCHEME = "watchnextcodelab"
private const val APPS_LAUNCH_HOST = "com.example.android.watchnextcodelab"
val BASE_URI = Uri.parse(SCHEME + "://" + APPS_LAUNCH_HOST)
const private val START_APP_ACTION_PATH = "startapp"
const val PLAY_VIDEO_ACTION_PATH = "playvideo"

private const val CHANNELS_COLUMN_ID_INDEX = 0
private const val CHANNELS_COLUMN_INTERNAL_PROVIDER_ID_INDEX = 1

private val CHANNELS_MAP_PROJECTION = arrayOf(
        TvContractCompat.Channels._ID,
        TvContractCompat.Channels.COLUMN_INTERNAL_PROVIDER_ID,
        TvContractCompat.Channels.COLUMN_BROWSABLE)

private const val PROGRAMS_COLUMN_ID_INDEX = 0
private const val PROGRAMS_COLUMN_INTERNAL_PROVIDER_ID_INDEX = 1
private const val PROGRAMS_COLUMN_TITLE_INDEX = 2
private val PROGRAMS_MAP_PROJECTION = arrayOf(
        TvContractCompat.PreviewPrograms._ID,
        TvContractCompat.PreviewPrograms.COLUMN_INTERNAL_PROVIDER_ID,
        TvContractCompat.PreviewPrograms.COLUMN_TITLE)

/**
 * Wraps the TV Provider's content provider API for channels and programs.
 */
object ChannelTvProviderFacade {

    @WorkerThread
    fun deleteChannel(context: Context, channelId: Long) {
        val rowsDeleted = context.contentResolver
                .delete(TvContractCompat.buildChannelUri(channelId),
                        null, null)
        if (rowsDeleted < 1) {
            Log.e(TAG, "Failed to delete channel " + channelId)
        }
    }

    @WorkerThread
    @VisibleForTesting
    internal fun deleteChannels(context: Context) {
        val rowsDeleted = context.contentResolver
                .delete(TvContractCompat.Channels.CONTENT_URI, null, null)
        Log.e(TAG, "Deleted $rowsDeleted channels")
    }

    @WorkerThread
    internal fun addChannel(context: Context, category: Category): Long {

        val channelInputId = createInputId(context)
        val channel = Channel.Builder()
                .setDisplayName(category.name)
                .setDescription(category.description)
                .setType(TvContractCompat.Channels.TYPE_PREVIEW)
                .setInputId(channelInputId)
                .setAppLinkIntentUri(Uri.withAppendedPath(BASE_URI, START_APP_ACTION_PATH))
                .setInternalProviderId(category.id)
                .build()

        val channelUri = context.contentResolver
                .insert(TvContractCompat.Channels.CONTENT_URI, channel.toContentValues())
        if (channelUri == null || channelUri == Uri.EMPTY) {
            Log.e(TAG, "Failed to insert channel.")
            return -1
        }
        val channelId = ContentUris.parseId(channelUri)

        writeChannelLogo(context, channelId, R.drawable.watchnext_channel_banner)

        Log.d(TAG, "Added channel $channelId")

        return channelId
    }

    private fun createInputId(context: Context): String {
        val componentName = ComponentName(context, MainActivity::class.java.name)
        return TvContractCompat.buildInputId(componentName)
    }

    /**
     * Writes a drawable as the channel logo.
     *
     * @param channelId  identifies the channel to write the logo.
     * @param drawableId resource to write as the channel logo. This must be a bitmap and not, say
     * a vector drawable.
     */
    @WorkerThread
    private fun writeChannelLogo(context: Context,
                                 channelId: Long,
                                 @DrawableRes drawableId: Int) {
        val bitmap = BitmapFactory.decodeResource(context.resources, drawableId)
        ChannelLogoUtils.storeChannelLogo(context, channelId, bitmap)
    }

    @WorkerThread
    fun addPrograms(context: Context, channelId: Long, category: Category): List<MovieProgramId> {
        val movies = category.movies

        // Maps movie ids to lists of program ids.
        val movieToProgramIds = movies.associateBy({ it.movieId }, { mutableListOf<Long>() })

        movies.forEachIndexed { index, movie ->
            val weight = movies.size - index
            val programId = addProgram(context, channelId, movie, weight)
            movieToProgramIds[movie.movieId]?.apply { add(programId) }
        }

        return movieToProgramIds.map { entry -> MovieProgramId(entry.key, entry.value) }
    }

    @WorkerThread
    fun addProgram(context: Context, channelId: Long, movie: Movie, weight: Int): Long {
        val builder = PreviewProgram.Builder()
                .setChannelId(channelId)
                .setWeight(weight)
        addMovieToBuilder(builder, movie)
        val program = builder.build()

        val previewProgramUri = TvContractCompat.buildPreviewProgramsUriForChannel(channelId)
        val programUri = context.contentResolver.insert(previewProgramUri, program.toContentValues())
        if (programUri == null || programUri == Uri.EMPTY) {
            Log.e(TAG, "Failed to insert program  ${movie.title}")
        } else {
            val programId = ContentUris.parseId(programUri)
            Log.d(TAG, "Added program $programId")
            return programId
        }
        return -1L
    }

    private fun addMovieToBuilder(builder: PreviewProgram.Builder, movie: Movie) {

        val movieId = java.lang.Long.toString(movie.movieId)
        builder.setTitle(movie.title)
                .setDescription(movie.description)
                .setDurationMillis(java.lang.Long.valueOf(movie.duration)!!.toInt())
                .setType(TvContractCompat.PreviewPrograms.TYPE_MOVIE)
                .setIntentUri(Uri
                        .withAppendedPath(BASE_URI, PLAY_VIDEO_ACTION_PATH)
                        .buildUpon()
                        .appendPath(movieId)
                        .build())
                .setInternalProviderId(movieId)
                .setContentId(movieId)
                .setPreviewVideoUri(Uri.parse(movie.previewVideoUrl))
                .setPosterArtUri(Uri.parse(movie.thumbnailUrl))
                .setPosterArtAspectRatio(movie.posterArtAspectRatio)
                .setContentRatings(arrayOf(movie.contentRating))
                .setGenre(movie.genre)
                .setLive(movie.isLive)
                .setReleaseDate(movie.releaseDate)
                .setReviewRating(movie.rating)
                .setReviewRatingStyle(movie.ratingStyle)
                .setStartingPrice(movie.startingPrice)
                .setOfferPrice(movie.offerPrice)
                .setVideoWidth(movie.width)
                .setVideoHeight(movie.height)
    }

    @WorkerThread
    internal fun loadProgramsForChannel(context: Context, channelId: Long): List<ProgramMetadata> {
        val programs = ArrayList<ProgramMetadata>()
        // Iterate "cursor" through all the programs assigned to "channelId".
        val programUri = TvContractCompat.buildPreviewProgramsUriForChannel(channelId)
        context.contentResolver.query(programUri, PROGRAMS_MAP_PROJECTION, null, null, null)
                .use { cursor ->
                    while (cursor.moveToNext()) {
                        if (!cursor.isNull(PROGRAMS_COLUMN_INTERNAL_PROVIDER_ID_INDEX)) {
                            // Found a row that contains a non-null COLUMN_INTERNAL_PROVIDER_ID.
                            val id = cursor.getString(PROGRAMS_COLUMN_INTERNAL_PROVIDER_ID_INDEX)
                            val programId = cursor.getLong(PROGRAMS_COLUMN_ID_INDEX)
                            val title = cursor.getString(PROGRAMS_COLUMN_TITLE_INDEX)

                            programs.add(ProgramMetadata(id, programId, title))
                        }
                    }
                }
        return programs
    }

    @WorkerThread
    internal fun updateProgram(context: Context, programId: Long, movie: Movie) {

        val programUri = TvContractCompat.buildPreviewProgramUri(programId)
        val contentResolver = context.contentResolver
        contentResolver.query(programUri, null, null, null, null)
                .use { cursor ->
                    if (!cursor.moveToFirst()) {
                        Log.w(TAG, "Could not update program $programId for movie ${movie.title}")
                    }
                    var program = PreviewProgram.fromCursor(cursor)
                    val builder = PreviewProgram.Builder(program)
                    addMovieToBuilder(builder, movie)
                    program = builder.build()

                    val rowsUpdated = contentResolver
                            .update(programUri, program.toContentValues(), null, null)
                    if (rowsUpdated < 1) {
                        Log.w(TAG,
                                "No programs were updated with id $programId for movie ${movie.title}")
                    }
                }
    }

    @WorkerThread
    internal fun deleteProgram(context: Context, programId: Long) {
        val rowsDeleted = context.contentResolver
                .delete(TvContractCompat.buildPreviewProgramUri(programId), null, null)
        if (rowsDeleted < 1) {
            Log.e(TAG, "Failed to delete program $programId")
        }
    }

    /**
     * Returns the id of the video to play. This will parse a program's intent Uri to retrieve the
     * id. If the Uri path is does not indicate that a video should be played, then -1 will be
     * returned.
     *
     * @param uri of the program's intent Uri.
     * @return the id of the video to play.
     */
    fun parseVideoId(uri: Uri): Long {
        val segments = uri.pathSegments
        return if (segments.size == 2 && PLAY_VIDEO_ACTION_PATH == segments[0])
            segments[1].toLong()
        else -1L
    }

    /**
     * Represents a program from the TV Provider. It contains the program's id assigned by the TV
     * Provider, the internal provided id provided by this app (in this case Movie::getMovieId), and
     * the title of the program.
     */
    class ProgramMetadata internal constructor(val id: String, val programId: Long, val title: String)

    /**
     * Queries the TV provider for all of the app's channels returning a bi-directional map of
     * channel ids to the internal provided id associated with the channel. In this app, the
     * internal provided id is the category id.
     *
     * A BiMap is returned so that a client may easily switch the direction of the map depending on
     * how they require the data. This prevents extra look ups queries to retrieve a map of category
     * ids to channel ids.
     *
     * @param context used to get a reference to a [ContentResolver].
     * @return a bi-directional map of channel ids to category ids.
     */
    @WorkerThread
    internal fun findChannelCategoryIds(context: Context): BiMap<Long, String> {
        val channelCategoryIds = HashBiMap.create<Long, String>()
        context.contentResolver.query(
                TvContractCompat.Channels.CONTENT_URI,
                CHANNELS_MAP_PROJECTION, null, null, null).use { cursor ->

            while (cursor.moveToNext()) {
                val categoryId = cursor.getString(CHANNELS_COLUMN_INTERNAL_PROVIDER_ID_INDEX)
                val channelId = cursor.getLong(CHANNELS_COLUMN_ID_INDEX)
                channelCategoryIds.put(channelId, categoryId)
            }
        }
        return channelCategoryIds
    }

    /**
     * Gets a list of channels from the TV Provider.
     *
     * @param context used to get a reference to a [android.content.ContentResolver]
     * @return a list of channel ids
     */
    @WorkerThread
    @VisibleForTesting
    internal fun getChannels(context: Context): List<Long> {
        val channelIds = mutableListOf<Long>()
        context.contentResolver.query(
                TvContractCompat.Channels.CONTENT_URI,
                CHANNELS_MAP_PROJECTION, null, null, null).use { cursor ->
            while (cursor.moveToNext()) {
                val channelId = cursor.getLong(CHANNELS_COLUMN_ID_INDEX)
                channelIds.add(channelId)
            }
        }
        return channelIds
    }
}