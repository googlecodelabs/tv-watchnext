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

import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import android.support.media.tv.TvContractCompat
import com.example.android.watchnextcodelab.MOVIE_ID
import com.example.android.watchnextcodelab.PLAYBACK_POSITION
import com.example.android.watchnextcodelab.background.BackgroundJobService
import com.example.android.watchnextcodelab.background.JobServiceTask
import com.example.android.watchnextcodelab.model.Movie

/**
 * Schedules and runs a background job to add a program to the watch next row on the home screen.
 * The program will have type [TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE].
 */
class AddToWatchNextContinueJobService
    : BackgroundJobService<PerformWatchNextWithPlaybackPositionTask>() {

    override fun createTask(jobParameters: JobParameters)
            : PerformWatchNextWithPlaybackPositionTask {
        return PerformWatchNextWithPlaybackPositionTask(jobService = this,
                jobParameters = jobParameters,
                action = WatchNextAction::addToContinueWatching)
    }
}

fun scheduleAddToWatchNextContinue(context: Context, movie: Movie, playbackPosition: Int) {
    scheduleWatchlistJob(context,
            AddToWatchNextContinueJobService::class.java,
            movie,
            playbackPosition)
}

/**
 * Schedules and runs a background job to remove a program from the watch next row on the home
 * screen and the watchlist category.
 */
class RemoveFromWatchNextContinueJobService :
        WatchListJobService(WatchNextAction::removeFromContinueWatching)


fun scheduleRemoveFromWatchNextContinue(context: Context, movie: Movie) {
    scheduleWatchlistJob(context, RemoveFromWatchNextContinueJobService::class.java, movie)
}

/**
 * Schedules and runs a background job to add a program to the watch next row on the home screen.
 * The program will have type [TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_NEXT].
 */
class AddToWatchNextNextJobService : WatchListJobService(WatchNextAction::watchNext)

fun scheduleAddingToWatchNextNext(context: Context, movieId: Long) {
    scheduleWatchlistJob(context, AddToWatchNextNextJobService::class.java, movieId)
}

/**
 * Schedules and runs a background job to add a program to the watch next row on the home screen and
 * the watchlist category. The program will have type
 * [TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_WATCHLIST].
 */
class AddToWatchListJobService : WatchListJobService(WatchNextAction::addToWatchlist)

fun scheduleAddingToWatchlist(context: Context, movie: Movie) {
    scheduleWatchlistJob(context, AddToWatchListJobService::class.java, movie)
}

/**
 * Schedules and runs a background job to remove a program from the watch next row on the home
 * screen and the watchlist category.
 */
class RemoveFromWatchListJobService : WatchListJobService(WatchNextAction::removeFromWatchlist)

fun scheduleRemoveFromWatchlist(context: Context, movie: Movie) {
    scheduleWatchlistJob(context, RemoveFromWatchListJobService::class.java, movie)
}

private fun <T> scheduleWatchlistJob(context: Context,
                                     clazz: Class<T>,
                                     movie: Movie,
                                     playbackPosition: Int = -1) {
    scheduleWatchlistJob(context, clazz, movie.movieId, playbackPosition)
}

private fun <T> scheduleWatchlistJob(context: Context,
                                     clazz: Class<T>,
                                     movieId: Long,
                                     playbackPosition: Int = -1) {
    val scheduler = context.getSystemService(JobScheduler::class.java)
    if (scheduler != null) {
        val bundle = PersistableBundle()
        bundle.putLong(MOVIE_ID, movieId)
        if (playbackPosition > -1) {
            bundle.putInt(PLAYBACK_POSITION, playbackPosition)
        }

        scheduler.schedule(JobInfo.Builder(0,
                ComponentName(context, clazz))
                .setMinimumLatency(0)
                .setExtras(bundle)
                .setPersisted(true)
                .build())
    }
}

abstract class WatchListJobService(private val action: (context: Context, movieId: Long) -> Unit)
    : BackgroundJobService<PerformWatchNextTask>() {

    override fun createTask(jobParameters: JobParameters): PerformWatchNextTask {
        return PerformWatchNextTask(jobService = this,
                jobParameters = jobParameters,
                action = action)
    }
}

class PerformWatchNextTask(
        jobService: BackgroundJobService<*>,
        jobParameters: JobParameters,
        private val action: (context: Context, movieId: Long) -> Unit)
    : JobServiceTask(jobService, jobParameters) {

    @SuppressLint(value = ["StaticFieldLeak"])
    private val context: Context = jobService.applicationContext
    private val movieId: Long = jobParameters.extras.getLong(MOVIE_ID)

    override fun doWork() {
        action(context, movieId)
    }
}

class PerformWatchNextWithPlaybackPositionTask(
        jobService: BackgroundJobService<*>,
        jobParameters: JobParameters,
        private val action: (context: Context, movieId: Long, playbackPosition: Int) -> Unit)
    : JobServiceTask(jobService, jobParameters) {

    @SuppressLint(value = ["StaticFieldLeak"])
    private val context: Context = jobService.applicationContext
    private val movieId: Long = jobParameters.extras.getLong(MOVIE_ID)
    private val playbackPosition: Int = jobParameters.extras.getInt(PLAYBACK_POSITION)

    override fun doWork() {
        action(context, movieId, playbackPosition)
    }
}
