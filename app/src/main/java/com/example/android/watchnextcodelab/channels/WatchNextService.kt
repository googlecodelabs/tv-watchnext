package com.example.android.watchnextcodelab.channels

import android.content.Context
import android.support.annotation.WorkerThread
import com.example.android.watchnextcodelab.database.MockDatabase
import com.example.android.watchnextcodelab.database.SharedPreferencesDatabase
import com.example.android.watchnextcodelab.watchlist.WatchlistService

/**
 * Functions for managing the watch next row.
 */
object WatchNextService {

    private val database = MockDatabase.getInstance()

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
        WatchlistService.get().removeMovieFromWatchlist(context, movieId)
    }

    @WorkerThread
    fun addToWatchlist(context: Context, movieId: Long) {
        WatchlistService.get().addToWatchlist(context, movieId)
    }

    @WorkerThread
    fun removeFromWatchlist(context: Context, movieId: Long) {
        WatchlistService.get().removeMovieFromWatchlist(context, movieId)
    }
}