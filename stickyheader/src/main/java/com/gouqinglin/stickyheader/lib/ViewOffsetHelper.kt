package com.gouqinglin.stickyheader.lib

import android.view.View

class ViewOffsetHelper(private val view: View) {

    private var offsetTop: Int = 0
    private var offsetBottom: Int = 0

    var topAndBottomOffset: Int
        get() = offsetTop
        set(offset) {
            if (offsetTop != offset) {
                offsetTop = offset
                offsetBottom = offset
                updateOffsets()
            }
        }

    private fun updateOffsets() {
        view.translationY = offsetTop.toFloat()
    }
}