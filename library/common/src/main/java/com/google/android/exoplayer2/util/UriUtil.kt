/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.net.Uri
import android.text.TextUtils

/** Utility methods for manipulating URIs.  */
object UriUtil {
    /** The length of arrays returned by [.getUriIndices].  */
    private const val INDEX_COUNT: Int = 4

    /**
     * An index into an array returned by [.getUriIndices].
     *
     *
     * The value at this position in the array is the index of the ':' after the scheme. Equals -1
     * if the URI is a relative reference (no scheme). The hier-part starts at (schemeColon + 1),
     * including when the URI has no scheme.
     */
    private const val SCHEME_COLON: Int = 0

    /**
     * An index into an array returned by [.getUriIndices].
     *
     *
     * The value at this position in the array is the index of the path part. Equals (schemeColon +
     * 1) if no authority part, (schemeColon + 3) if the authority part consists of just "//", and
     * (query) if no path part. The characters starting at this index can be "//" only if the
     * authority part is non-empty (in this case the double-slash means the first segment is empty).
     */
    private const val PATH: Int = 1

    /**
     * An index into an array returned by [.getUriIndices].
     *
     *
     * The value at this position in the array is the index of the query part, including the '?'
     * before the query. Equals fragment if no query part, and (fragment - 1) if the query part is a
     * single '?' with no data.
     */
    private const val QUERY: Int = 2

    /**
     * An index into an array returned by [.getUriIndices].
     *
     *
     * The value at this position in the array is the index of the fragment part, including the '#'
     * before the fragment. Equal to the length of the URI if no fragment part, and (length - 1) if
     * the fragment part is a single '#' with no data.
     */
    private const val FRAGMENT: Int = 3

    /**
     * Like [.resolve], but returns a [Uri] instead of a [String].
     *
     * @param baseUri The base URI.
     * @param referenceUri The reference URI to resolve.
     */
    fun resolveToUri(baseUri: String?, referenceUri: String?): Uri {
        return Uri.parse(resolve(baseUri, referenceUri))
    }

    /**
     * Performs relative resolution of a `referenceUri` with respect to a `baseUri`.
     *
     *
     * The resolution is performed as specified by RFC-3986.
     *
     * @param baseUri The base URI.
     * @param referenceUri The reference URI to resolve.
     */
    fun resolve(baseUri: String?, referenceUri: String?): String {
        var baseUri: String? = baseUri
        var referenceUri: String? = referenceUri
        val uri: StringBuilder = StringBuilder()

        // Map null onto empty string, to make the following logic simpler.
        baseUri = if (baseUri == null) "" else baseUri
        referenceUri = if (referenceUri == null) "" else referenceUri
        val refIndices: IntArray = getUriIndices(referenceUri)
        if (refIndices[SCHEME_COLON] != -1) {
            // The reference is absolute. The target Uri is the reference.
            uri.append(referenceUri)
            removeDotSegments(uri, refIndices[PATH], refIndices[QUERY])
            return uri.toString()
        }
        val baseIndices: IntArray = getUriIndices(baseUri)
        if (refIndices[FRAGMENT] == 0) {
            // The reference is empty or contains just the fragment part, then the target Uri is the
            // concatenation of the base Uri without its fragment, and the reference.
            return uri.append(baseUri, 0, baseIndices[FRAGMENT]).append(referenceUri).toString()
        }
        if (refIndices[QUERY] == 0) {
            // The reference starts with the query part. The target is the base up to (but excluding) the
            // query, plus the reference.
            return uri.append(baseUri, 0, baseIndices[QUERY]).append(referenceUri).toString()
        }
        if (refIndices[PATH] != 0) {
            // The reference has authority. The target is the base scheme plus the reference.
            val baseLimit: Int = baseIndices[SCHEME_COLON] + 1
            uri.append(baseUri, 0, baseLimit).append(referenceUri)
            return removeDotSegments(uri, baseLimit + refIndices[PATH], baseLimit + refIndices[QUERY])
        }
        if (referenceUri[refIndices[PATH]] == '/') {
            // The reference path is rooted. The target is the base scheme and authority (if any), plus
            // the reference.
            uri.append(baseUri, 0, baseIndices[PATH]).append(referenceUri)
            return removeDotSegments(uri, baseIndices[PATH], baseIndices[PATH] + refIndices[QUERY])
        }

        // The target Uri is the concatenation of the base Uri up to (but excluding) the last segment,
        // and the reference. This can be split into 2 cases:
        return if ((baseIndices[SCHEME_COLON] + 2 < baseIndices[PATH] && baseIndices[PATH] == baseIndices[QUERY])) {
            // Case 1: The base hier-part is just the authority, with an empty path. An additional '/' is
            // needed after the authority, before appending the reference.
            uri.append(baseUri, 0, baseIndices[PATH]).append('/').append(referenceUri)
            removeDotSegments(uri, baseIndices[PATH], baseIndices[PATH] + refIndices[QUERY] + 1)
        } else {
            // Case 2: Otherwise, find the last '/' in the base hier-part and append the reference after
            // it. If base hier-part has no '/', it could only mean that it is completely empty or
            // contains only one segment, in which case the whole hier-part is excluded and the reference
            // is appended right after the base scheme colon without an added '/'.
            val lastSlashIndex: Int = baseUri.lastIndexOf('/', baseIndices[QUERY] - 1)
            val baseLimit: Int = if (lastSlashIndex == -1) baseIndices[PATH] else lastSlashIndex + 1
            uri.append(baseUri, 0, baseLimit).append(referenceUri)
            removeDotSegments(uri, baseIndices[PATH], baseLimit + refIndices[QUERY])
        }
    }

    /** Returns true if the URI is starting with a scheme component, false otherwise.  */
    fun isAbsolute(uri: String?): Boolean {
        return uri != null && getUriIndices(uri).get(SCHEME_COLON) != -1
    }

    /**
     * Removes query parameter from a URI, if present.
     *
     * @param uri The URI.
     * @param queryParameterName The name of the query parameter.
     * @return The URI without the query parameter.
     */
    fun removeQueryParameter(uri: Uri, queryParameterName: String): Uri {
        val builder: Uri.Builder = uri.buildUpon()
        builder.clearQuery()
        for (key: String in uri.queryParameterNames) {
            if (!(key == queryParameterName)) {
                for (value: String? in uri.getQueryParameters(key)) {
                    builder.appendQueryParameter(key, value)
                }
            }
        }
        return builder.build()
    }

    /**
     * Removes dot segments from the path of a URI.
     *
     * @param uri A [StringBuilder] containing the URI.
     * @param offset The index of the start of the path in `uri`.
     * @param limit The limit (exclusive) of the path in `uri`.
     */
    private fun removeDotSegments(uri: StringBuilder, offset: Int, limit: Int): String {
        var mOffset: Int = offset
        var mLimit: Int = limit
        if (mOffset >= mLimit) {
            // Nothing to do.
            return uri.toString()
        }
        if (uri[mOffset] == '/') {
            // If the path starts with a /, always retain it.
            mOffset++
        }
        // The first character of the current path segment.
        var segmentStart: Int = mOffset
        var i: Int = mOffset
        while (i <= mLimit) {
            val nextSegmentStart: Int = if (i == mLimit) {
                i
            } else if (uri[i] == '/') {
                i + 1
            } else {
                i++
                continue
            }
            // We've encountered the end of a segment or the end of the path. If the final segment was
            // "." or "..", remove the appropriate segments of the path.
            if (i == segmentStart + 1 && uri[segmentStart] == '.') {
                // Given "abc/def/./ghi", remove "./" to get "abc/def/ghi".
                uri.delete(segmentStart, nextSegmentStart)
                mLimit -= nextSegmentStart - segmentStart
                i = segmentStart
            } else if ((i == segmentStart + 2) && (uri[segmentStart] == '.') && (uri[segmentStart + 1] == '.')) {
                // Given "abc/def/../ghi", remove "def/../" to get "abc/ghi".
                val prevSegmentStart: Int = uri.lastIndexOf("/", segmentStart - 2) + 1
                val removeFrom: Int = if (prevSegmentStart > mOffset) prevSegmentStart else mOffset
                uri.delete(removeFrom, nextSegmentStart)
                mLimit -= nextSegmentStart - removeFrom
                segmentStart = prevSegmentStart
                i = prevSegmentStart
            } else {
                i++
                segmentStart = i
            }
        }
        return uri.toString()
    }

    /**
     * Calculates indices of the constituent components of a URI.
     *
     * @param uriString The URI as a string.
     * @return The corresponding indices.
     */
    private fun getUriIndices(uriString: String): IntArray {
        val indices = IntArray(INDEX_COUNT)
        if (TextUtils.isEmpty(uriString)) {
            indices[SCHEME_COLON] = -1
            return indices
        }

        // Determine outer structure from right to left.
        // Uri = scheme ":" hier-part [ "?" query ] [ "#" fragment ]
        val length: Int = uriString.length
        var fragmentIndex: Int = uriString.indexOf('#')
        if (fragmentIndex == -1) {
            fragmentIndex = length
        }
        var queryIndex: Int = uriString.indexOf('?')
        if (queryIndex == -1 || queryIndex > fragmentIndex) {
            // '#' before '?': '?' is within the fragment.
            queryIndex = fragmentIndex
        }
        // Slashes are allowed only in hier-part so any colon after the first slash is part of the
        // hier-part, not the scheme colon separator.
        var schemeIndexLimit: Int = uriString.indexOf('/')
        if (schemeIndexLimit == -1 || schemeIndexLimit > queryIndex) {
            schemeIndexLimit = queryIndex
        }
        var schemeIndex: Int = uriString.indexOf(':')
        if (schemeIndex > schemeIndexLimit) {
            // '/' before ':'
            schemeIndex = -1
        }

        // Determine hier-part structure: hier-part = "//" authority path / path
        // This block can also cope with schemeIndex == -1.
        val hasAuthority: Boolean = (schemeIndex + 2 < queryIndex) && (uriString[schemeIndex + 1] == '/') && (uriString[schemeIndex + 2] == '/')
        var pathIndex: Int
        if (hasAuthority) {
            pathIndex = uriString.indexOf('/', schemeIndex + 3) // find first '/' after "://"
            if (pathIndex == -1 || pathIndex > queryIndex) {
                pathIndex = queryIndex
            }
        } else {
            pathIndex = schemeIndex + 1
        }
        indices[SCHEME_COLON] = schemeIndex
        indices[PATH] = pathIndex
        indices[QUERY] = queryIndex
        indices[FRAGMENT] = fragmentIndex
        return indices
    }
}