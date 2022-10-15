/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub.extensions

import java.text.NumberFormat

fun Number.format(maximumFractionDigits: Int): String {
    val format = NumberFormat.getNumberInstance()
    format.maximumFractionDigits = maximumFractionDigits
    return format.format(this)
}