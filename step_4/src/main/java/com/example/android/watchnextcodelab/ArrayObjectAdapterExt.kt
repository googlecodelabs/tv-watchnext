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

@file:JvmName("ArrayObjectAdapterUtil")

package com.example.android.watchnextcodelab

import android.support.v17.leanback.widget.ArrayObjectAdapter
import android.support.v17.leanback.widget.Presenter

operator fun <T> ArrayObjectAdapter.plusAssign(element: T) {
    this.add(element)
}

inline fun <reified T> Collection<T>.toArrayObjectAdapter(): ArrayObjectAdapter {
    val adapter = ArrayObjectAdapter()
    return copyIntoArrayObjectAdapter(adapter)
}

inline fun <reified T> Collection<T>.toArrayObjectAdapter(presenter: Presenter)
        : ArrayObjectAdapter {
    val adapter = ArrayObjectAdapter(presenter)
    return copyIntoArrayObjectAdapter(adapter)
}

inline fun <T> Collection<T>.copyIntoArrayObjectAdapter(adapter: ArrayObjectAdapter)
        : ArrayObjectAdapter {
    this.forEach { item -> adapter += item }
    return adapter
}