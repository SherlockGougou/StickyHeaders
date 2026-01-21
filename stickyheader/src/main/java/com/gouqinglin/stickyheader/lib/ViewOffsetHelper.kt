package com.gouqinglin.stickyheader.lib

import android.view.View

/**
 * Helper class for offsetting a [View] vertically using [View.setTranslationY].
 *
 * This approach allows visual repositioning of views without affecting their
 * actual layout position, which is essential for the sticky header effect.
 *
 * Unlike [View.offsetTopAndBottom], using translation preserves the original
 * layout bounds and works better with the Android layout system.
 *
 * @property view The view to offset
 */
class ViewOffsetHelper(private val view: View) {

    private var offsetTop: Int = 0

    /**
     * The vertical offset applied to the view.
     *
     * Positive values move the view down, negative values move it up.
     * The offset is applied using [View.setTranslationY].
     */
    var topAndBottomOffset: Int
        get() = offsetTop
        set(offset) {
            if (offsetTop != offset) {
                offsetTop = offset
                view.translationY = offset.toFloat()
            }
        }
}