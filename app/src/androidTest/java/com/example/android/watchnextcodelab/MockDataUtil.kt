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

@file:JvmName("MockDataUtil")

package com.example.android.watchnextcodelab

import android.media.tv.TvContentRating
import android.support.media.tv.TvContractCompat
import com.example.android.watchnextcodelab.model.Movie

/**
 * Contains helper method for generating mock data to test with.
 */
fun createMovieWithTitle(id: Long, title: String): Movie =
        Movie(
            movieId = id,
            title = title,
            description = "Description...",
            duration = 1,
            previewVideoUrl = "preview url",
            videoUrl = "video url",
            posterArtAspectRatio = TvContractCompat.PreviewPrograms.ASPECT_RATIO_1_1,
            aspectRatio = TvContractCompat.PreviewPrograms.ASPECT_RATIO_1_1,
            thumbnailUrl = "thumb url",
            cardImageUrl = "card image url",
            contentRating = TvContentRating.UNRATED,
            genre = "action",
            isLive = false,
            releaseDate = "2001",
            rating = "3",
            ratingStyle = TvContractCompat.WatchNextPrograms.REVIEW_RATING_STYLE_STARS,
            startingPrice = "$1.99",
            offerPrice = "$0.99",
            width = 1,
            height = 1,
            weight = 1)
