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

import com.example.android.watchnextcodelab.background.BackgroundJobService
import com.example.android.watchnextcodelab.background.JobServiceTask
import com.example.android.watchnextcodelab.database.MockDatabase

/**
 * Schedules and runs a background job to synchronize channels on the home screen.
 */
class SynchronizeChannelsJobService
    : BackgroundJobService<SynchronizeChannelsJobService.SynchronizeChannelsTask>() {

    override fun createTask(jobParameters: JobParameters)
            : SynchronizeChannelsJobService.SynchronizeChannelsTask {
        return SynchronizeChannelsTask(this, jobParameters)
    }

    /**
     * Wraps [SynchronizeChannelService] in an AsyncTask to move processing to the background.
     */
    @SuppressLint(value = ["StaticFieldLeak"])
    inner class SynchronizeChannelsTask constructor(
            jobService: SynchronizeChannelsJobService,
            jobParameters: JobParameters) : JobServiceTask(jobService, jobParameters) {

        override fun doWork() {
            SynchronizeChannelService(MockDatabase.get())
                    .synchronizeChannels(applicationContext)
        }
    }

    companion object {

        fun schedule(context: Context) {
            val scheduler = context.getSystemService(JobScheduler::class.java)
            scheduler?.schedule(JobInfo.Builder(0,
                    ComponentName(context, SynchronizeChannelsJobService::class.java))
                    .setMinimumLatency(0)
                    .build())
        }
    }
}
