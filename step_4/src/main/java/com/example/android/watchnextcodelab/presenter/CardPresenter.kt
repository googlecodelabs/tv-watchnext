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

import android.graphics.drawable.Drawable
import android.support.media.tv.TvContractCompat
import android.support.v17.leanback.widget.ImageCardView
import android.support.v17.leanback.widget.Presenter
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.view.ViewGroup

import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.android.watchnextcodelab.R
import com.example.android.watchnextcodelab.model.Movie

private const val TAG = "CardPresenter"

/*
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an Image CardView
 */
class CardPresenter : Presenter() {
    private var sSelectedBackgroundColor: Int = 0
    private var sDefaultBackgroundColor: Int = 0

    private var mDefaultCardImage: Drawable? = null

    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        Log.d(TAG, "onCreateViewHolder")

        sDefaultBackgroundColor =
                ContextCompat.getColor(parent.context, R.color.default_background)
        sSelectedBackgroundColor =
                ContextCompat.getColor(parent.context, R.color.selected_background)
        /*
         * This template uses a default image in res/drawable, but the general case for Android TV
         * will require your resources in xhdpi. For more information, see
         * https://developer.android.com/training/tv/start/layouts.html#density-resources
         */
        mDefaultCardImage = ContextCompat.getDrawable(parent.context, R.drawable.poster_placeholder)

        val cardView = object : ImageCardView(parent.context) {
            override fun setSelected(selected: Boolean) {
                updateCardBackgroundColor(this, selected)
                super.setSelected(selected)
            }
        }

        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        updateCardBackgroundColor(cardView, false)
        return Presenter.ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
        Log.d(TAG, "onBindViewHolder")

        val (_, title, description, _, _, _, posterArtAspectRatio, _, thumbnailUrl) = item as Movie
        val cardView = viewHolder.view as ImageCardView
        val resources = cardView.resources

        cardView.titleText = title
        cardView.contentText = description

        val widthMultiplier = getWidthMultiplier(posterArtAspectRatio)
        val cardWidth = Math.round(
                resources.getDimensionPixelSize(R.dimen.card_width) * widthMultiplier)
        val cardHeight = resources.getDimensionPixelSize(R.dimen.card_height)
        cardView.setMainImageDimensions(cardWidth, cardHeight)

        val options = RequestOptions()
                .centerCrop()
                .error(mDefaultCardImage)
        Glide.with(viewHolder.view.context)
                .load(thumbnailUrl)
                .apply(options)
                .into(cardView.mainImageView)
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        Log.d(TAG, "onUnbindViewHolder")
        val cardView = viewHolder.view as ImageCardView
        // Remove references to images so that the garbage collector can free up memory.
        cardView.badgeImage = null
        cardView.mainImage = null
    }

    private fun getWidthMultiplier(aspectRatio: Int): Float {
        return when (aspectRatio) {
            TvContractCompat.PreviewProgramColumns.ASPECT_RATIO_16_9 -> 16.0f / 9.0f
            TvContractCompat.PreviewProgramColumns.ASPECT_RATIO_3_2 -> 3.0f / 2.0f
            TvContractCompat.PreviewProgramColumns.ASPECT_RATIO_2_3 -> 2.0f / 3.0f
            TvContractCompat.PreviewProgramColumns.ASPECT_RATIO_4_3 -> 4.0f / 3.0f
            TvContractCompat.PreviewProgramColumns.ASPECT_RATIO_1_1 -> 1.0f
            TvContractCompat.PreviewProgramColumns.ASPECT_RATIO_MOVIE_POSTER -> 1.0f / 1.441f
            else -> 1.0f
        }
    }

    private fun updateCardBackgroundColor(view: ImageCardView, selected: Boolean) {
        val color = if (selected) sSelectedBackgroundColor else sDefaultBackgroundColor
        // Both background colors should be set because the view's background is temporarily visible
        // during animations.
        view.setBackgroundColor(color)
        view.findViewById<View>(R.id.info_field).setBackgroundColor(color)
    }
}
