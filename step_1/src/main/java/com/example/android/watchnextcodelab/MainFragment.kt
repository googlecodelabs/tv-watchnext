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
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.support.v17.leanback.app.BackgroundManager
import android.support.v17.leanback.app.BrowseFragment
import android.support.v17.leanback.widget.ArrayObjectAdapter
import android.support.v17.leanback.widget.DiffCallback
import android.support.v17.leanback.widget.HeaderItem
import android.support.v17.leanback.widget.ImageCardView
import android.support.v17.leanback.widget.ListRow
import android.support.v17.leanback.widget.ListRowPresenter
import android.support.v17.leanback.widget.OnItemViewClickedListener
import android.support.v17.leanback.widget.OnItemViewSelectedListener
import android.support.v17.leanback.widget.Presenter
import android.support.v17.leanback.widget.Row
import android.support.v17.leanback.widget.RowPresenter
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast

import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.example.android.watchnextcodelab.database.MockDatabase
import com.example.android.watchnextcodelab.model.Category
import com.example.android.watchnextcodelab.model.Movie
import com.example.android.watchnextcodelab.presenter.CardPresenter

import java.util.Timer
import java.util.TimerTask

private const val TAG = "MainFragment"

private const val BACKGROUND_UPDATE_DELAY = 300L

class MainFragment : BrowseFragment() {

    private val mHandler = Handler()
    private lateinit var mRowsAdapter: ArrayObjectAdapter
    private var mDefaultBackground: Drawable? = null
    private lateinit var mMetrics: DisplayMetrics
    private var mBackgroundTimer: Timer? = null
    private var mBackgroundUri: String? = null
    private lateinit var mBackgroundManager: BackgroundManager

    private val database: MockDatabase = MockDatabase.get()

    private val categoryObserver = Observer<List<Category>> { _ -> loadRows() }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onActivityCreated(savedInstanceState)

        prepareBackgroundManager()

        title = getString(R.string.browse_title)
        headersState = BrowseFragment.HEADERS_DISABLED

        onItemViewClickedListener = ItemViewClickedListener()
        onItemViewSelectedListener = ItemViewSelectedListener()

        mRowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        adapter = mRowsAdapter
        loadRows()
        database.getLiveCategories(context).observeForever(categoryObserver)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: " + mBackgroundTimer?.toString())
        mBackgroundTimer?.cancel()
        database.getLiveCategories(context).removeObserver(categoryObserver)
    }

    private fun loadRows() {
        mRowsAdapter.clear()
        val cardPresenter = CardPresenter()

        val categories = database.findAllCategories(context)

        val rows = mutableListOf<ListRow>()

        categories.map { (_, name, _, movies) ->
            val listRowAdapter = movies.toArrayObjectAdapter(cardPresenter)
            val header = HeaderItem(name)
            ListRow(header, listRowAdapter)
        }.forEach { row -> rows += row }

        val diffCallback = object : DiffCallback<ListRow>() {
            override fun areItemsTheSame(oldRow: ListRow, newRow: ListRow): Boolean {
                return TextUtils.equals(oldRow.contentDescription, newRow.contentDescription)
            }

            override fun areContentsTheSame(oldItem: ListRow, newItem: ListRow): Boolean {
                val oldAdapter = oldItem.adapter
                val newAdapter = newItem.adapter
                val sameSize = oldAdapter.size() == newAdapter.size()
                if (!sameSize) {
                    return false
                }

                for (i in 0 until oldAdapter.size()) {
                    val oldMovie = oldAdapter.get(i) as Movie
                    val newMovie = newAdapter.get(i) as Movie

                    if (oldMovie.movieId != newMovie.movieId) {
                        return false
                    }
                }

                return true
            }
        }
        mRowsAdapter.setItems(rows, diffCallback)
    }

    private fun prepareBackgroundManager() {

        mBackgroundManager = BackgroundManager.getInstance(activity)
        mBackgroundManager.attach(activity.window)
        mDefaultBackground = ContextCompat.getDrawable(context, R.drawable.default_background)
        mMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(mMetrics)
    }

    private fun updateBackground(uri: String?) {
        val width = mMetrics.widthPixels
        val height = mMetrics.heightPixels
        val options = RequestOptions()
                .centerCrop()
                .error(mDefaultBackground)
        Glide.with(activity)
                .asBitmap()
                .load(uri)
                .apply(options)
                .into(object : SimpleTarget<Bitmap>(width, height) {

                    override fun onResourceReady(
                            resource: Bitmap, transition: Transition<in Bitmap>) {
                        mBackgroundManager.setBitmap(resource)
                    }
                })
        mBackgroundTimer?.cancel()
    }

    private fun startBackgroundTimer() {
        mBackgroundTimer?.cancel()
        mBackgroundTimer = Timer()
        mBackgroundTimer?.schedule(UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY)
    }

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(itemViewHolder: Presenter.ViewHolder?, item: Any?,
                                   rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {

            if (item is Movie) {
                Log.d(TAG, "Item: $item")
                val intent = Intent(activity, DetailsActivity::class.java)
                intent.putExtra(MOVIE, item)

                val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        activity,
                        (itemViewHolder?.view as ImageCardView).mainImageView, SHARED_ELEMENT_NAME)
                        .toBundle()
                activity.startActivity(intent, bundle)
            } else if (item is String) {
                Toast.makeText(activity, item, Toast.LENGTH_SHORT)
                        .show()
            }
        }
    }

    private inner class ItemViewSelectedListener : OnItemViewSelectedListener {
        override fun onItemSelected(itemViewHolder: Presenter.ViewHolder?, item: Any?,
                                    rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
            if (item is Movie) {
                mBackgroundUri = item.cardImageUrl
                startBackgroundTimer()
            }
        }
    }

    private inner class UpdateBackgroundTask : TimerTask() {
        override fun run() {
            mHandler.post { updateBackground(mBackgroundUri) }
        }
    }
}
