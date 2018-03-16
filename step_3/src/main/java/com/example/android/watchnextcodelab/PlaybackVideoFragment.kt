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

package com.example.android.watchnextcodelab

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle
import android.support.v17.leanback.app.VideoSupportFragment
import android.support.v17.leanback.app.VideoSupportFragmentGlueHost
import android.support.v17.leanback.media.MediaPlayerAdapter
import android.support.v17.leanback.media.PlaybackGlue
import android.support.v17.leanback.media.PlaybackTransportControlGlue
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.example.android.watchnextcodelab.channels.scheduleAddToWatchNextContinue
import com.example.android.watchnextcodelab.channels.scheduleAddingToWatchNextNext
import com.example.android.watchnextcodelab.channels.scheduleRemoveFromWatchNextContinue
import com.example.android.watchnextcodelab.channels.scheduleRemoveFromWatchlist
import com.example.android.watchnextcodelab.database.SharedPreferencesDatabase
import com.example.android.watchnextcodelab.model.Movie

const private val TAG = "PlaybackVideoFragment"

/**
 * Handles video playback with media controls.
 */
class PlaybackVideoFragment : VideoSupportFragment() {

    private var playerGlue: PlaybackTransportControlGlue<MediaPlayerAdapter>? = null
    private lateinit var session: MediaSessionCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val param: Movie? = activity?.intent?.getParcelableExtra(MOVIE)
        if (param == null) {
            Log.e(TAG, "An invalid movie was asked to be played.")
            activity?.finish()
            return
        }

        val movie: Movie = param
        val (_, title, description, _, _, videoUrl) = movie

        val resumePosition = getResumePlaybackPosition(movie)

        val glueHost = VideoSupportFragmentGlueHost(this@PlaybackVideoFragment)

        playerGlue = PlaybackTransportControlGlue(context, MediaPlayerAdapter(context)).apply {
            host = glueHost
            this.title = title
            subtitle = description
            playerAdapter.setDataSource(Uri.parse(videoUrl))
            addPlayerCallback(SyncWatchNextCallback(context, movie))
            addPlayerCallback(object : PlaybackGlue.PlayerCallback() {
                override fun onPreparedStateChanged(glue: PlaybackGlue?) {
                    startPlaying(resumePosition)
                    updatePlaybackState()
                }
            })
            startPlaying(resumePosition)
        }

        session = MediaSessionCompat(context, TAG).apply {
            isActive = true
            setCallback(VideoMediaSessionCallBack(playerGlue))
        }
        updatePlaybackState()
        updateMetadata(movie)
    }

    override fun onPause() {
        super.onPause()

        playerGlue?.apply {
            if (isPlaying) {
                pause()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        session.release()
        playerGlue = null
    }

    private fun getResumePlaybackPosition(movie: Movie): Long {
        context?.let { context ->
            return SharedPreferencesDatabase()
                .findPlaybackPositionForMovie(context, movie)
                .toLong()
        }
        return -1
    }

    private fun updatePlaybackState() {
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(getAvailableActions())
        var state = PlaybackStateCompat.STATE_PAUSED
        if (playerGlue?.isPlaying == true) {
            state = PlaybackState.STATE_PLAYING
        }
        stateBuilder.setState(state, playerGlue?.currentPosition ?: 0L, 1.0f)
        session.setPlaybackState(stateBuilder.build())
    }

    @PlaybackStateCompat.Actions
    private fun getAvailableActions(): Long {
        var actions = (PlaybackStateCompat.ACTION_PLAY_PAUSE
                or PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                or PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH)
        val playOrPauseAction = if (playerGlue?.isPlaying == true) {
            PlaybackStateCompat.ACTION_PAUSE
        } else {
            PlaybackStateCompat.ACTION_PLAY
        }
        actions = actions or playOrPauseAction
        return actions
    }

    private fun updateMetadata(movie: Movie) {
        val (_, title, description, _, _, _, _, _, _, cardImageUrl) = movie

        val metadataBuilder = MediaMetadataCompat.Builder()

        metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, title)
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, description)
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI, cardImageUrl)

        // And at minimum the title and artist for legacy support
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, title)
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST, description)

        Glide.with(context)
            .asBitmap()
            .load(Uri.parse(cardImageUrl))
            .into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(
                    bitmap: Bitmap?,
                    transition: Transition<in Bitmap>?
                ) {
                    metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap)
                    session.setMetadata(metadataBuilder.build())
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    Log.e(TAG, "onLoadFailed: " + errorDrawable)
                    session.setMetadata(metadataBuilder.build())
                }
            })
    }
}

class VideoMediaSessionCallBack(private val glue: PlaybackTransportControlGlue<*>?) :
    MediaSessionCompat.Callback() {
    override fun onPlay() {
        glue?.play()
    }

    override fun onPause() {
        glue?.pause()
    }

    override fun onSeekTo(position: Long) {
        glue?.seekTo(position)
    }
}

private class SyncWatchNextCallback(private val context: Context, private val movie: Movie) :
    PlaybackGlue.PlayerCallback() {

    override fun onPlayStateChanged(glue: PlaybackGlue) {
        Log.d(TAG, "Player state changed: is ${if (glue.isPlaying) "playing" else "paused"}")

        // TODO: Step 1 - Update the Play Next row when the video is paused.
        if (!glue.isPlaying) {
            val controlGlue = glue as PlaybackTransportControlGlue<*>
            val playbackPosition = controlGlue.playerAdapter.currentPosition.toInt()
            scheduleAddToWatchNextContinue(context, movie, playbackPosition)
        }
    }

    override fun onPlayCompleted(glue: PlaybackGlue) {
        Log.d(TAG, "Playback completed, time to remove the program from the Play Next row.")

        // TODO: Step 2 - Schedule remove the program from the Play Next row.
        scheduleRemoveFromWatchNextContinue(context = context, movie = movie)

        // TODO: Step 8 - Schedule the next video to be added to the Play Next row.
        movie.nextMovieIdInSeries?.let { id ->
            if (id > -1L) {
                Log.d(TAG, "There is another video in the series, adding it to the Play Next row.")
                scheduleAddingToWatchNextNext(context = context, movieId = id)
            }
        }
    }
}

private fun PlaybackTransportControlGlue<MediaPlayerAdapter>.startPlaying(resumePosition: Long) {
    if (isPrepared) {
        if (resumePosition > 0) {
            seekTo(resumePosition)
        }
        play()
    }
}
