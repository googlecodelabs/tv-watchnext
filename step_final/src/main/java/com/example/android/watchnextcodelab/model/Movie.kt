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

package com.example.android.watchnextcodelab.model

import android.media.tv.TvContentRating
import android.os.Parcel
import android.os.Parcelable
import android.support.media.tv.TvContractCompat

/**
 * Represents a movie entity with detailed attributes describing the video.
 */
data class Movie @JvmOverloads constructor(
        var movieId: Long = 0,
        val title: String,
        val description: String,
        val duration: Long,
        val previewVideoUrl: String,
        val videoUrl: String,
        @field:TvContractCompat.PreviewProgramColumns.AspectRatio
        val posterArtAspectRatio: Int,
        @field:TvContractCompat.PreviewProgramColumns.AspectRatio
        val aspectRatio: Int,
        val thumbnailUrl: String,
        val cardImageUrl: String,
        val contentRating: TvContentRating,
        val genre: String,
        val isLive: Boolean,
        val releaseDate: String,
        val rating: String,
        val ratingStyle: Int,
        val startingPrice: String,
        val offerPrice: String,
        val width: Int,
        val height: Int,
        val weight: Int,
        val nextMovieIdInSeries: Long? = -1L) : Parcelable {

    constructor(parcel: Parcel) : this(
            movieId = parcel.readLong(),
            title = parcel.readString(),
            description = parcel.readString(),
            duration = parcel.readLong(),
            previewVideoUrl = parcel.readString(),
            videoUrl = parcel.readString(),
            posterArtAspectRatio = parcel.readInt(),
            aspectRatio= parcel.readInt(),
            thumbnailUrl = parcel.readString(),
            cardImageUrl = parcel.readString(),
            contentRating = TvContentRating.unflattenFromString(parcel.readString()),
            genre = parcel.readString(),
            isLive = parcel.readByte() != 0.toByte(),
            releaseDate = parcel.readString(),
            rating = parcel.readString(),
            ratingStyle = parcel.readInt(),
            startingPrice = parcel.readString(),
            offerPrice = parcel.readString(),
            width= parcel.readInt(),
            height = parcel.readInt(),
            weight = parcel.readInt(),
            nextMovieIdInSeries = parcel.readLong())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(movieId)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeLong(duration)
        parcel.writeString(previewVideoUrl)
        parcel.writeString(videoUrl)
        parcel.writeInt(posterArtAspectRatio)
        parcel.writeInt(aspectRatio)
        parcel.writeString(thumbnailUrl)
        parcel.writeString(cardImageUrl)
        parcel.writeString(contentRating.flattenToString())
        parcel.writeString(genre)
        parcel.writeByte(if (isLive) 1 else 0)
        parcel.writeString(releaseDate)
        parcel.writeString(rating)
        parcel.writeInt(ratingStyle)
        parcel.writeString(startingPrice)
        parcel.writeString(offerPrice)
        parcel.writeInt(width)
        parcel.writeInt(height)
        parcel.writeInt(weight)
        parcel.writeLong(nextMovieIdInSeries ?: -1L)
    }

    override fun describeContents() = 0

    companion object {

        @JvmField val CREATOR = object : Parcelable.Creator<Movie> {
            override fun createFromParcel(parcel: Parcel)= Movie(parcel)

            override fun newArray(size: Int) = arrayOfNulls<Movie>(size)
        }
    }
}
