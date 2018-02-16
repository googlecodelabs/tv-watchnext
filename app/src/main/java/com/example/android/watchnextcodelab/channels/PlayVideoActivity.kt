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

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.example.android.watchnextcodelab.PlaybackActivity
import com.example.android.watchnextcodelab.R
import com.example.android.watchnextcodelab.database.FindMovieByIdTask
import com.example.android.watchnextcodelab.database.MockDatabase

/**
 * Receives an intent to play the selected program from the home screen.
 */
class PlayVideoActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val movieId = ChannelTvProviderFacade.parseVideoId(intent.data)

        val task = FindMovieByIdTask(MockDatabase.get()) { movie ->
            if (movie != null) {
                val playbackIntent = PlaybackActivity.newIntent(context = this, movie = movie)
                startActivity(playbackIntent)
            } else {
                Toast.makeText(this, getString(R.string.movie_not_available), Toast.LENGTH_LONG)
                        .show()
            }
            finish()
        }

        task.execute(movieId)
    }
}