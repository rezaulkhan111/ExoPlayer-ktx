/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.util

import android.view.*
import com.google.android.exoplayer2.util.Assertions.checkArgument

/** Immutable value class for a [Surface] and supporting information.  */
class SurfaceInfo {

    /** The [Surface].  */
    var surface: Surface? = null

    /** The width of frames rendered to the [.surface], in pixels.  */
    var width = 0

    /** The height of frames rendered to the [.surface], in pixels.  */
    var height = 0

    /**
     * A counter-clockwise rotation to apply to frames before rendering them to the [.surface].
     *
     *
     * Must be 0, 90, 180, or 270 degrees. Default is 0.
     */
    var orientationDegrees = 0

    /** Creates a new instance.  */
    constructor(surface: Surface?, width: Int, height: Int) {
        SurfaceInfo(surface, width, height,  /* orientationDegrees= */0)
    }

    /** Creates a new instance.  */
    constructor(surface: Surface?, width: Int, height: Int, orientationDegrees: Int) {
        checkArgument(orientationDegrees == 0 || orientationDegrees == 90 || orientationDegrees == 180 || orientationDegrees == 270, "orientationDegrees must be 0, 90, 180, or 270")
        this.surface = surface
        this.width = width
        this.height = height
        this.orientationDegrees = orientationDegrees
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is SurfaceInfo) {
            return false
        }
        val that = o
        return width == that.width && height == that.height && orientationDegrees == that.orientationDegrees && surface == that.surface
    }

    override fun hashCode(): Int {
        var result = surface.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + orientationDegrees
        return result
    }
}