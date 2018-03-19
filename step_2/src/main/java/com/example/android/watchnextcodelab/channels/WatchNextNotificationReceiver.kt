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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.android.watchnextcodelab.database.MockDatabase
import com.example.android.watchnextcodelab.watchlist.WatchlistManager

private const val TAG = "WatchNextNotificationReceiver"

class WatchNextNotificationReceiver : BroadcastReceiver() {

    private val watchlistManager = WatchlistManager.get()
    private val database = MockDatabase.get()

    override fun onReceive(context: Context, intent: Intent) {

        val extras = intent.extras
        // TODO: Step 10 extract the EXTRA_WATCH_NEXT_PROGRAM_ID

        when(intent.action) {
            // TODO: Step 11 remove the movie from the watchlist.


            // TODO: Step 12 add the movie to the watchlist.
        }
    }
}
