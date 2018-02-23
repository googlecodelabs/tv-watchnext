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
import android.os.AsyncTask

/**
 * Used in conjunction with [BackgroundJobService], this is a fire-and-forget task that will not
 * return anything. This class will update the JobService to signal once the work has been completed
 * and the job is finished.
 *
 * # Usage
 * ``
 * public class MyJobServiceAsyncTask extends JobServiceTask {
 *
 *     protected void doWork() {
 *         // Perform work to be down in the background.
 *     }
 * }
 *``
 *
 * Performs work in the background. Notifies the encapsulating JobService when the work completes.
 */
abstract class JobServiceTask protected constructor(
        private val jobService: BackgroundJobService<*>,
        private val jobParameters: JobParameters) : AsyncTask<Void?, Void, Void?>() {

    protected abstract fun doWork()

    override fun doInBackground(vararg voids: Void?): Void? {
        doWork()
        return null
    }

    override fun onPostExecute(aVoid: Void?) {
        super.onPostExecute(aVoid)
        jobService.jobFinished(jobParameters)
    }
}