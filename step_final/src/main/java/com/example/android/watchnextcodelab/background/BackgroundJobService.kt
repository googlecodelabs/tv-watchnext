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

package com.example.android.watchnextcodelab.background

import android.app.job.JobParameters
import android.app.job.JobService

/**
 * Abstracts moving work to the background with an [android.os.AsyncTask].
 *
 *
 * Extend this class and supply an implementation of [JobServiceTask]
 * with [.createTask].
 * # Usage
 * ``
 * public class MyAsyncJobService extends BackgroundJobService<MyJobServiceAsyncTask> {
 *
 *     protected MyJobServiceAsyncTask createTask(JobParameters jobParameters) {
 *         return new MyJobServiceAsyncTask(this, jobParameters);
 *     }
 * }
 * ``
 */
abstract class BackgroundJobService<out T : JobServiceTask> : JobService() {

    private var task: T? = null

    protected abstract fun createTask(jobParameters: JobParameters): T

    override fun onStartJob(jobParameters: JobParameters): Boolean {
        task = createTask(jobParameters)
        task?.execute()
        return true
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean {
        task?.cancel(true)
        task = null
        return true
    }

    internal fun jobFinished(jobParameters: JobParameters) {
        task = null
        jobFinished(jobParameters, false)
    }
}
