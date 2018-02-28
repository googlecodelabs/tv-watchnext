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

import android.content.Context
import android.content.SharedPreferences
import com.example.android.watchnextcodelab.OpenForMocking
import com.example.android.watchnextcodelab.model.Category
import com.example.android.watchnextcodelab.model.Movie
import com.example.android.watchnextcodelab.model.MovieProgramId
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private const val CATEGORY_PREFS_DB = "CATEGORY_PREFS_db"
private const val PLAYBACK_PREFS_DB = "PLAYBACK_PREFS_db"
private const val MOVIE_PROGRAM_ID_DB = "MOVIE_PROGRAM_ID_db"
private const val PREFS_CATEGORIES = "PREFS_CATEGORIES"
private const val PREFS_MOVIE_PROGRAM_ID = "PREFS_MOVIE_PROGRAM_ID"

/**
 * Persists data into shared preferences.
 */
@OpenForMocking
class SharedPreferencesDatabase {

    private val gson: Gson = Gson()

    fun saveCategories(context: Context, categories: List<Category>) {
        // Ideally you would not save a json serialization of a list in SharedPreferences and should
        // use a better more efficient data storage solution such as Sqlite.
        val json = gson.toJson(categories)

        getCategorySharedPreferences(context).edit()
                .putString(PREFS_CATEGORIES, json)
                .apply()
    }

    fun findCategories(context: Context): List<Category> {
        val json = getCategorySharedPreferences(context).getString(PREFS_CATEGORIES, null)
                ?: return emptyList()

        val listType = object : TypeToken<List<Category>>() {}.type
        return gson.fromJson(json, listType)
    }

    fun savePlaybackPosition(context: Context, movie: Movie, playbackPosition: Int) {
        getPlaybackSharedPreferences(context)
                .edit()
                .putInt(movie.movieId.toString(), playbackPosition)
                .apply()
    }

    fun findPlaybackPositionForMovie(context: Context, movie: Movie): Int {
        return getPlaybackSharedPreferences(context).getInt(movie.movieId.toString(), -1)
    }

    fun deletePlaybackPosition(context: Context, movieId: String) {
        getPlaybackSharedPreferences(context).edit().remove(movieId).apply()
    }

    fun findMovieProgramIds(context: Context): List<MovieProgramId> {
        val json = getMovieProgramSharedPreferences(context).getString(PREFS_MOVIE_PROGRAM_ID, null)
                ?: return emptyList()

        val listType = object : TypeToken<List<MovieProgramId>>() {}.type
        return gson.fromJson(json, listType)
    }

    fun saveMovieProgramIds(context: Context, ids: List<MovieProgramId>) {
        if( ids.isEmpty() ) {
            deleteMovieProgramIds(context)
            return
        }

        val json = gson.toJson(ids)

        getMovieProgramSharedPreferences(context).edit()
                .putString(PREFS_MOVIE_PROGRAM_ID, json)
                .apply()
    }

    fun deleteMovieProgramIds(context: Context) =
        getMovieProgramSharedPreferences(context).edit().clear().apply()


    private fun getCategorySharedPreferences(context: Context): SharedPreferences =
            getSharedPreferences(context, CATEGORY_PREFS_DB)

    private fun getPlaybackSharedPreferences(context: Context): SharedPreferences =
            getSharedPreferences(context, PLAYBACK_PREFS_DB)

    private fun getMovieProgramSharedPreferences(context: Context): SharedPreferences =
            getSharedPreferences(context, MOVIE_PROGRAM_ID_DB)

    private fun getSharedPreferences(context: Context, preferenceKey: String): SharedPreferences {
        return context.getSharedPreferences(preferenceKey, Context.MODE_PRIVATE)
    }
}
