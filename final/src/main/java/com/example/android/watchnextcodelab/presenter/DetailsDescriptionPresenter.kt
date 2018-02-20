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

package com.example.android.watchnextcodelab.presenter

import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter

import com.example.android.watchnextcodelab.model.Movie

import java.util.Locale
import java.util.concurrent.TimeUnit

class DetailsDescriptionPresenter : AbstractDetailsDescriptionPresenter() {

    override fun onBindDescription(viewHolder: AbstractDetailsDescriptionPresenter.ViewHolder,
                                   item: Any) {
        val (_, title, description, duration) = item as Movie

        viewHolder.title.text = title
        viewHolder.subtitle.text = getDurationLabel(duration)
        viewHolder.body.text = description
    }

    private fun getDurationLabel(duration: Long): String {
        val seconds = TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS) % 60
        val minutes = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS) % 60
        val hours = TimeUnit.HOURS.convert(duration, TimeUnit.MILLISECONDS)

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d hours %d min", hours, minutes)
        } else {
            String.format(Locale.getDefault(), "%d min %d sec", minutes, seconds)
        }
    }
}
