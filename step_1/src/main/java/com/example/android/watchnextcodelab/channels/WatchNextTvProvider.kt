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

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.BaseColumns
import android.support.media.tv.TvContractCompat
import android.support.media.tv.WatchNextProgram
import android.text.TextUtils
import android.util.Log
import com.example.android.watchnextcodelab.model.Movie

private const val TAG = "WatchNextTvProviderFacade"

private const val COLUMN_WATCH_NEXT_INTERNAL_PROVIDER_ID_INDEX = 1
private val WATCH_NEXT_MAP_PROJECTION =
        arrayOf(
                BaseColumns._ID,
                TvContractCompat.WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID,
                TvContractCompat.WatchNextPrograms.COLUMN_BROWSABLE)

/**
 * When adding to the watch next row, we perform the following steps:
 *
 *   1. If movie exists in the watch next row:
 *      1. Verify it has ***not*** been removed by the user so we can **update** the program.
 *      1. If it has been removed by the user (browsable ==0) then **remove** the program from the
 *      Tv Provider and treat the movie as a new program to add.
 *  1. If it does not exists, then add the movie to the watch next row
 *
 */
object WatchNextTvProvider {

    fun addToWatchNextWatchlist(context: Context, movie: Movie): Long =
        addToWatchNextRow(
            context,
            movie,
            TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_WATCHLIST)

    fun addToWatchNextNext(context: Context, movie: Movie): Long =
        addToWatchNextRow(context, movie, TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_NEXT)

    fun addToWatchNextContinue(context: Context, movie: Movie, playbackPosition: Int): Long =
        addToWatchNextRow(
                context,
                movie,
                TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE,
                playbackPosition)

    private fun addToWatchNextRow(
        context: Context,
        movie: Movie,
        @TvContractCompat.WatchNextPrograms.WatchNextType watchNextType: Int,
        playbackPosition: Int? = null): Long {

        val movieId = movie.movieId.toString()

        // TODO: Step 3 - find the existing program, see if it has been removed, and check if we
        // should update the program.


        // TODO: Step 6 - Create the content values for the Content Provider.


        // TODO: Step 7 - Update or add the program to the content provider.
        return -1 // Replace this return statement with the code from the codelab
    }

    fun deleteFromWatchNext(context: Context, movieId: String) {
        val program = findProgramByMovieId(context = context, movieId = movieId)
        if (program != null) {
            removeProgram(context = context, watchNextProgramId = program.id)
        }
    }

    private fun findProgramByMovieId(context: Context, movieId: String): WatchNextProgram? {
        // TODO: Step 4 - Find the movie by our app's internal id.

        return null
    }

    private fun removeIfNotBrowsable(context: Context, program: WatchNextProgram?): Boolean {
        // TODO: Step 5 - Check is a program has been removed from the UI by the user. If so, then
        // remove the program from the content provider.
        if (program?.isBrowsable == false) {
            val watchNextProgramId = program.id
            removeProgram(context, watchNextProgramId)
            return true
        }
        return false
    }

    private fun removeProgram(context: Context, watchNextProgramId: Long): Int {
        val rowsDeleted = context.contentResolver.delete(
                TvContractCompat.buildWatchNextProgramUri(watchNextProgramId),
                null, null)
        if (rowsDeleted < 1) {
            Log.e(TAG, "Failed to delete program ($watchNextProgramId) from Watch Next row")
        }
        return rowsDeleted
    }

    private fun convertMovie(movie: Movie): WatchNextProgram.Builder {

        val movieId = java.lang.Long.toString(movie.movieId)
        val builder = WatchNextProgram.Builder()
        builder.setTitle(movie.title)
            .setDescription(movie.description)
            .setDurationMillis(movie.duration.toInt())
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
        return builder
    }
}
