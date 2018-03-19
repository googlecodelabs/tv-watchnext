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

package com.example.android.watchnextcodelab

import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v17.leanback.app.DetailsFragment
import android.support.v17.leanback.app.DetailsFragmentBackgroundController
import android.support.v17.leanback.widget.Action
import android.support.v17.leanback.widget.ArrayObjectAdapter
import android.support.v17.leanback.widget.ClassPresenterSelector
import android.support.v17.leanback.widget.DetailsOverviewRow
import android.support.v17.leanback.widget.FullWidthDetailsOverviewRowPresenter
import android.support.v17.leanback.widget.FullWidthDetailsOverviewSharedElementHelper
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.Toast

import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.example.android.watchnextcodelab.channels.scheduleAddingToWatchlist
import com.example.android.watchnextcodelab.channels.scheduleRemoveFromWatchlist
import com.example.android.watchnextcodelab.model.Category
import com.example.android.watchnextcodelab.model.Movie
import com.example.android.watchnextcodelab.presenter.DetailsDescriptionPresenter
import com.example.android.watchnextcodelab.watchlist.WatchlistManager

private const val TAG = "VideoDetailsFragment"

private const val ACTION_WATCH_TRAILER_ID = 1L
private const val ACTION_ADD_TO_LIST_ID = 2L
private const val ACTION_BUY_ID = 3L
private const val ACTION_ADD_TO_LIST_INDEX = 1

private const val DETAIL_THUMB_WIDTH = 274
private const val DETAIL_THUMB_HEIGHT = 274

/*
 * LeanbackDetailsFragment extends DetailsFragment, a Wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its meta plus related videos.
 */
class VideoDetailsFragment : DetailsFragment() {

    private lateinit var selectedMovie: Movie

    private lateinit var rowAdapter: ArrayObjectAdapter
    private lateinit var presenterSelector: ClassPresenterSelector

    private val watchlistManager = WatchlistManager.get()
    private var isMovieInWatchList: Boolean = false

    private lateinit var detailsBackground: DetailsFragmentBackgroundController
    private lateinit var actionAdapter: ArrayObjectAdapter
    private lateinit var addToWatchlistAction: Action
    private val watchlistObserver = Observer<Category> {
        isMovieInWatchList = watchlistManager.isInWatchlist(context, selectedMovie)
        addToWatchlistAction.label1 = watchlistActionLabel
        actionAdapter.notifyArrayItemRangeChanged(ACTION_ADD_TO_LIST_INDEX, 1)
    }

    private val watchlistActionLabel: String
        get() = if (isMovieInWatchList)
            resources.getString(R.string.remove_from_list)
        else
            resources.getString(R.string.add_to_list)

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate DetailsFragment")
        super.onCreate(savedInstanceState)

        val param: Movie? = activity.intent.getParcelableExtra(MOVIE)
        if( param == null ) {
            Log.e(TAG, "Cannot show details for an invalid movie, returning to the MainActivity")
            val intent = Intent(activity, MainActivity::class.java)
            startActivity(intent)
            activity.finish()
            return
        }

        selectedMovie = param

        detailsBackground = DetailsFragmentBackgroundController(this)

        presenterSelector = ClassPresenterSelector()
        rowAdapter = ArrayObjectAdapter(presenterSelector)
        isMovieInWatchList = watchlistManager.isInWatchlist(context, selectedMovie)

        setupDetailsOverviewRow()
        setupDetailsOverviewRowPresenter()
        adapter = rowAdapter
        initializeBackground(selectedMovie)

        watchlistManager.getLiveWatchlist().observeForever(watchlistObserver)
    }

    override fun onDestroy() {
        super.onDestroy()
        watchlistManager.getLiveWatchlist().removeObserver(watchlistObserver)
    }

    private fun initializeBackground(data: Movie) {
        detailsBackground.enableParallax()
        val options = RequestOptions()
                .centerCrop()
                .error(R.drawable.default_background)
        Glide.with(activity)
                .asBitmap()
                .load(data.cardImageUrl)
                .apply(options)
                .into(object : SimpleTarget<Bitmap>() {

                    override fun onResourceReady(
                            resource: Bitmap, transition: Transition<in Bitmap>) {
                        detailsBackground.coverBitmap = resource
                        rowAdapter.notifyArrayItemRangeChanged(0, rowAdapter.size())
                    }
                })
    }

    private fun setupDetailsOverviewRow() {
        Log.d(TAG, "doInBackground: " + selectedMovie.toString())
        val row = DetailsOverviewRow(selectedMovie)
        row.imageDrawable = ContextCompat.getDrawable(context, R.drawable.default_background)
        val width = convertDpToPixel(activity.applicationContext, DETAIL_THUMB_WIDTH)
        val height = convertDpToPixel(activity.applicationContext, DETAIL_THUMB_HEIGHT)
        val options = RequestOptions()
                .centerCrop()
                .error(R.drawable.default_background)
        Glide.with(activity)
                .asDrawable()
                .load(selectedMovie.thumbnailUrl)
                .apply(options)
                .into(object : SimpleTarget<Drawable>(width, height) {

                    override fun onResourceReady(
                            resource: Drawable, transition: Transition<in Drawable>) {
                        Log.d(TAG, "details overview card image url ready: " + resource)
                        row.imageDrawable = resource
                        rowAdapter.notifyArrayItemRangeChanged(0, rowAdapter.size())
                    }
                })

        addToWatchlistAction = Action(ACTION_ADD_TO_LIST_ID, watchlistActionLabel)
        val watchAction = Action(
                ACTION_WATCH_TRAILER_ID,
                resources.getString(R.string.watch_trailer_1),
                resources.getString(R.string.watch_trailer_2))
        val buyAction = Action(
                ACTION_BUY_ID,
                resources.getString(R.string.buy_1),
                selectedMovie.offerPrice)

        actionAdapter = listOf(watchAction, addToWatchlistAction, buyAction).toArrayObjectAdapter()

        row.actionsAdapter = actionAdapter

        rowAdapter += row
    }

    private fun setupDetailsOverviewRowPresenter() {
        // Set detail background.
        val detailsPresenter = FullWidthDetailsOverviewRowPresenter(DetailsDescriptionPresenter())
        detailsPresenter.backgroundColor =
                ContextCompat.getColor(context, R.color.selected_background)

        // Hook up transition element.
        val sharedElementHelper = FullWidthDetailsOverviewSharedElementHelper()
        sharedElementHelper.setSharedElementEnterTransition(activity, SHARED_ELEMENT_NAME)
        detailsPresenter.setListener(sharedElementHelper)
        detailsPresenter.isParticipatingEntranceTransition = true

        detailsPresenter.setOnActionClickedListener { action ->
            when(action.id) {
                ACTION_WATCH_TRAILER_ID -> {
                    val intent = Intent(activity, PlaybackActivity::class.java)
                    intent.putExtra(MOVIE, selectedMovie)
                    startActivity(intent)
                }
                ACTION_ADD_TO_LIST_ID -> {
                    if (isMovieInWatchList) {
                        scheduleRemoveFromWatchlist(context, selectedMovie)
                    } else {
                        scheduleAddingToWatchlist(context, selectedMovie)
                    }
                }
                else -> Toast.makeText(activity, action.toString(), Toast.LENGTH_SHORT).show()

            }
        }
        presenterSelector.addClassPresenter(DetailsOverviewRow::class.java, detailsPresenter)
    }

    private fun convertDpToPixel(context: Context, dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return Math.round(dp.toFloat() * density)
    }
}
