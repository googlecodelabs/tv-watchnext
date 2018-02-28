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

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.media.Rating
import android.media.tv.TvContentRating
import android.support.media.tv.TvContractCompat
import android.text.TextUtils
import com.example.android.watchnextcodelab.OpenForMocking
import com.example.android.watchnextcodelab.model.Category
import com.example.android.watchnextcodelab.model.Movie
import com.example.android.watchnextcodelab.model.MovieProgramId
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Manages persistence of categories. Provides a live list of categories that can be observed by the
 * UI.
 *
 * Note that all data is mocked locally within this class.
 */
@OpenForMocking
class MockDatabase(private val sharedPrefDatabase: SharedPreferencesDatabase) {

    private var categories: List<Category>? = null
    private val liveCategories = MutableLiveData<List<Category>>()

    /**
     * Returns a list of all of the categories of movies.
     *
     * @param context Used to access shared preferences.
     * @return A list of all categories.
     */
    fun findAllCategories(context: Context): MutableList<Category> {
        return getCategories(context)
    }

    /**
     * A live list of categories that can observe updates to the list.
     *
     * @param context Used to access shared preferences.
     * @return an observable list of categories.
     */
    fun getLiveCategories(context: Context): LiveData<List<Category>> {
        getCategories(context) // Ensure categories are initialized.
        return liveCategories
    }

    private fun getCategories(context: Context): MutableList<Category> {
        if (categories == null) {
            categories = sharedPrefDatabase.findCategories(context)
            if (categories?.isEmpty() == true) {
                categories = createCategories()
            }
            liveCategories.postValue(categories)
        }
        return categories as MutableList<Category>
    }

    fun findCategoryById(context: Context, id: String): Category? {
        val categories = getCategories(context)
        return categories.find { (currentId) -> TextUtils.equals(id, currentId) }
    }

    fun findMovieById(movieId: Long): Movie? {
        return findAllMovies().find { movie -> movie.movieId == movieId }
    }

    fun saveCategories(context: Context, categories: List<Category>) {
        this.categories = categories
        notifyCategoriesChanged(context, categories)
    }

    fun removeFromCategory(context: Context, category: Category, movie: Movie) {
        category.movies.removeIf { m -> m.movieId == movie.movieId }

        notifyCategoriesChanged(context, getCategories(context))
    }

    fun removeCategory(context: Context, category: Category) {

        val categories = getCategories(context)
        categories.removeIf { (id) -> TextUtils.equals(category.id, id) }

        notifyCategoriesChanged(context, categories)
    }

    private fun notifyCategoriesChanged(context: Context, categories: List<Category>) {
        liveCategories.postValue(categories)

        sharedPrefDatabase.saveCategories(context, categories)
    }

    @OpenForMocking
    fun saveMovieProgramIds(context: Context, ids: List<MovieProgramId>) {
        ids.map { (movieId, programIds, watchNextProgramId) ->

            // Mapping to a new copy of the MovieProgramId with the old ids merged with the new.
            val movieProgramId = findMovieProgramId(context = context, movieId = movieId) ?:
                    MovieProgramId(movieId, programIds, watchNextProgramId)

            val updatedProgramIds = movieProgramId.programIds.union(programIds).toList()

            movieProgramId
                    .copy(programIds = updatedProgramIds, watchNextProgramId = watchNextProgramId)
        }.forEach {
            saveMovieProgramId(context, it)
        }
    }

    fun removeProgramId(context: Context, movieId: Long, programId: Long) {
        val ids = findAllMovieProgramIds(context).toMutableList()
        val movieProgramId = ids.find { it.movieId == movieId }
        movieProgramId?.apply {
            val programIds = this.programIds.toMutableList()
            programIds.removeIf { it == programId }
            val updatedIds = this.copy(programIds = programIds)

            ids.removeIf { it == this }
            ids.add(updatedIds)

            saveMovieProgramIds(context, ids)
        }
    }

    fun updateMovieProgramId(
            context: Context,
            movieId: Long,
            programIds: List<Long> = MIN_PROGRAM_IDS,
            watchNextProgramId: Long = Long.MIN_VALUE) {

        // If the id does not exists, it has not been added to the home screen yet. Create a new id.
        val movieProgramId = findMovieProgramId(context = context, movieId = movieId) ?:
                MovieProgramId(movieId, programIds, watchNextProgramId)

        val updatedProgramIds =
                if (programIds == MIN_PROGRAM_IDS) movieProgramId.programIds else programIds

        val updatedWatchNextId =
                if (watchNextProgramId == Long.MIN_VALUE) movieProgramId.watchNextProgramId
                else watchNextProgramId

        val updatedIds = movieProgramId
                .copy(programIds = updatedProgramIds, watchNextProgramId = updatedWatchNextId)

        saveMovieProgramId(context, updatedIds)
    }

    private fun saveMovieProgramId(context: Context, movieProgramId: MovieProgramId) {
        val ids = sharedPrefDatabase.findMovieProgramIds(context).toMutableList()
        ids.removeIf { movieProgramId.movieId == it.movieId }
        // If there is still a valid mapping, keep the composite id.
        if (movieProgramId.programIds.isNotEmpty() || movieProgramId.watchNextProgramId > -1L) {
            ids += movieProgramId
        }
        sharedPrefDatabase.saveMovieProgramIds(context, ids)
    }

    fun deleteMovieProgramIds(context: Context) = sharedPrefDatabase.deleteMovieProgramIds(context)

    fun findAllMovieProgramIds(context: Context) =
            sharedPrefDatabase.findMovieProgramIds(context)

    fun findMovieProgramId(context: Context, movieId: Long): MovieProgramId? {
        val ids = findAllMovieProgramIds(context)
        return ids.find { movieId == it.movieId }
    }

    companion object {

        private val INSTANCE = MockDatabase(SharedPreferencesDatabase())

        fun get(): MockDatabase {
            return INSTANCE
        }

    }
}

private fun findAllMovies(): MutableList<Movie> {
    return mutableListOf(rushmore, treasureMode, elephantsDream, bigBuckBunny)
}

private fun createCategories(): MutableList<Category> {
    val recommendations = Category(
            id = "1",
            name = "Recommendations",
            description = "Recommended videos for you.",
            movies = findAllMovies())

    val dramaMovies = findAllMovies()
    Collections.shuffle(dramaMovies)
    val dramas = Category(
            id = "2",
            name = "Dramas",
            description = "Drama movies based on your watching preferences.",
            movies = dramaMovies)

    return mutableListOf(recommendations, dramas)
}

val MIN_PROGRAM_IDS = listOf(Long.MIN_VALUE)

private val TV_RATING_PG = TvContentRating.createRating(
        "com.android.tv",
        "US_TV",
        "US_TV_PG",
        "US_TV_D", "US_TV_L", "US_TV_S", "US_TV_V")

private val bigBuckBunny: Movie
    get() = Movie(
            movieId = 1L,
            title = "Big Buck Bunny",
            description = "Big Buck Bunny tells the story of a giant rabbit with a heart bigger than himself. When one sunny day three rodents rudely harass him, something snaps... and the rabbit ain't no bunny anymore! In the typical cartoon tradition he prepares the nasty rodents a comical revenge.",
            duration = TimeUnit.MINUTES.toMillis(9) + TimeUnit.SECONDS.toMillis(56),
            // Using the elephants dream movie for Big Buck Bunny since the quality is better.
            previewVideoUrl = "https://archive.org/download/ElephantsDream/ed_hd_512kb.mp4",
            videoUrl = "https://archive.org/download/ElephantsDream/ed_hd_512kb.mp4",
            posterArtAspectRatio = TvContractCompat.PreviewPrograms.ASPECT_RATIO_2_3,
            aspectRatio = TvContractCompat.PreviewPrograms.ASPECT_RATIO_16_9,
            thumbnailUrl = "https://peach.blender.org/wp-content/uploads/poster_bunny_small.jpg?x11217",
            cardImageUrl = "https://peach.blender.org/wp-content/uploads/title_anouncement.jpg?x11217",
            contentRating = TV_RATING_PG,
            genre = TvContractCompat.Programs.Genres.DRAMA,
            isLive = false,
            releaseDate = "2008",
            rating = "4",
            ratingStyle = Rating.RATING_5_STARS,
            startingPrice = "$12.99",
            offerPrice = "$9.99",
            width = 3840,
            height = 2160,
            weight = 1
    )

private val elephantsDream: Movie
    get() = Movie(
            movieId = 2L,
            title = "Elephant's Dream",
            description = "The story of two strange characters exploring a capricious and seemingly infinite machine. The elder, Proog, acts as a tour-guide and protector, happily showing off the sights and dangers of the machine to his initially curious but increasingly skeptical protege Emo. As their journey unfolds we discover signs that the machine is not all Proog thinks it is, and his guiding takes on a more desperate aspect.",
            duration = (TimeUnit.MINUTES.toMillis(10) + TimeUnit.SECONDS.toMillis(53)),
            previewVideoUrl = "https://archive.org/download/ElephantsDream/ed_hd_512kb.mp4",
            videoUrl = "https://archive.org/download/ElephantsDream/ed_hd_512kb.mp4",
            posterArtAspectRatio = TvContractCompat.PreviewPrograms.ASPECT_RATIO_16_9,
            aspectRatio = TvContractCompat.PreviewPrograms.ASPECT_RATIO_16_9,
            thumbnailUrl = "https://orange.blender.org/wp-content/themes/orange/images/common/ed_header.jpg?x53801",
            cardImageUrl = "https://orange.blender.org/wp-content/themes/orange/images/common/ed_header.jpg?x53801",
            contentRating = TV_RATING_PG,
            genre = TvContractCompat.Programs.Genres.DRAMA,
            isLive = false,
            releaseDate = "2006",
            rating = "3",
            ratingStyle = Rating.RATING_5_STARS,
            startingPrice = "$2.99",
            offerPrice = "$1.99",
            width = 426,
            height = 240,
            weight = 2
    )

private val rushmore: Movie
    get() = Movie(
            movieId = 3L,
            title = "Rushmore",
            description = "Fusce id nisi turpis. Praesent viverra bibendum semper. Donec tristique, orci sed semper lacinia, quam erat rhoncus massa, non congue tellus est quis tellus. Sed mollis orci venenatis quam scelerisque accumsan. Curabitur a massa sit amet mi accumsan mollis sed et magna. Vivamus sed aliquam risus. Nulla eget dolor in elit facilisis mattis. Ut aliquet luctus lacus. Phasellus nec commodo erat. Praesent tempus id lectus ac scelerisque. Maecenas pretium cursus lectus id volutpat.",
            duration = TimeUnit.MINUTES.toMillis(1) + TimeUnit.SECONDS.toMillis(8),
            previewVideoUrl = "https://storage.googleapis.com/android-tv/Sample%20videos/Demo%20Slam/Google%20Demo%20Slam_%20Rushmore.mp4",
            videoUrl = "https://storage.googleapis.com/android-tv/Sample%20videos/Demo%20Slam/Google%20Demo%20Slam_%20Rushmore.mp4",
            posterArtAspectRatio = TvContractCompat.PreviewPrograms.ASPECT_RATIO_16_9,
            aspectRatio = TvContractCompat.PreviewPrograms.ASPECT_RATIO_16_9,
            thumbnailUrl = "https://storage.googleapis.com/android-tv/Sample%20videos/Demo%20Slam/Google%20Demo%20Slam_%20Rushmore/card.jpg",
            cardImageUrl = "https://storage.googleapis.com/android-tv/Sample%20videos/Demo%20Slam/Google%20Demo%20Slam_%20Rushmore/card.jpg",
            contentRating = TV_RATING_PG,
            genre = TvContractCompat.Programs.Genres.TECH_SCIENCE,
            isLive = false,
            releaseDate = "2010",
            rating = "4",
            ratingStyle = Rating.RATING_5_STARS,
            startingPrice = "Free",
            offerPrice = "Free",
            width = 426,
            height = 240,
            weight = 3,
            nextMovieIdInSeries = treasureMode.movieId
    )

private val treasureMode: Movie
    get() = Movie(
            movieId = 4L,
            title = "Explore Treasure Mode with Google Maps",
            description = "Fusce id nisi turpis. Praesent viverra bibendum semper. Donec tristique, orci sed semper lacinia, quam erat rhoncus massa, non congue tellus est quis tellus. Sed mollis orci venenatis quam scelerisque accumsan. Curabitur a massa sit amet mi accumsan mollis sed et magna. Vivamus sed aliquam risus. Nulla eget dolor in elit facilisis mattis. Ut aliquet luctus lacus. Phasellus nec commodo erat. Praesent tempus id lectus ac scelerisque. Maecenas pretium cursus lectus id volutpat.",
            duration = TimeUnit.MINUTES.toMillis(2) + TimeUnit.SECONDS.toMillis(17),
            previewVideoUrl = "https://storage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Explore%20Treasure%20Mode%20with%20Google%20Maps.mp4",
            videoUrl = "https://storage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Explore%20Treasure%20Mode%20with%20Google%20Maps.mp4",
            posterArtAspectRatio = TvContractCompat.PreviewPrograms.ASPECT_RATIO_16_9,
            aspectRatio = TvContractCompat.PreviewPrograms.ASPECT_RATIO_16_9,
            thumbnailUrl = "https://storage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Explore%20Treasure%20Mode%20with%20Google%20Maps/bg.jpg",
            cardImageUrl = "https://storage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Explore%20Treasure%20Mode%20with%20Google%20Maps/card.jpg",
            contentRating = TV_RATING_PG,
            genre = TvContractCompat.Programs.Genres.TECH_SCIENCE,
            isLive = false,
            releaseDate = "2013",
            rating = "2",
            ratingStyle = Rating.RATING_5_STARS,
            startingPrice = "Free",
            offerPrice = "Free",
            width = 426,
            height = 240,
            weight = 3
    )