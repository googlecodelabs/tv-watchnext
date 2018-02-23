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

package com.example.android.watchnextcodelab.database

import android.os.AsyncTask
import com.example.android.watchnextcodelab.model.Movie
import com.google.common.collect.Lists

/**
 * Queries for a movie by it's id in the background. Returns null for invalid input and when a movie
 * cannot be found for the given id.
 *
 * Returns the results of the query. The results will be returned in the main thread, albeit the
 * query is processed in the background.
 */
class FindMovieByIdTask(
        private val database: MockDatabase,
        private val onResult: (Movie?) -> Unit) : AsyncTask<Long, Void, Movie?>() {

    override fun doInBackground(vararg params: Long?): Movie? {
        val ids = Lists.newArrayList<Long>(*params)
        if (ids.size > 0) {
            val movieId = ids[0]
            return database.findMovieById(movieId)
        }
        return null
    }

    override fun onPostExecute(movie: Movie?) {
        super.onPostExecute(movie)
        onResult(movie)
    }
}