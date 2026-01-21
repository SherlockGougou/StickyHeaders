package com.gouqinglin.stickyheader.lib

import android.view.View

/**
 * Callback interface for monitoring sticky state changes in [StickyLinearLayout].
 *
 * Implement this interface and set it via [StickyLinearLayout.setOnStickyStateChangedListener]
 * to receive notifications when sticky views change their pinned state.
 *
 * ## Example
 *
 * ```kotlin
 * stickyLinearLayout.setOnStickyStateChangedListener(object : OnStickyStateChangedListener {
 *     override fun onViewPinned(view: View, index: Int) {
 *         // View has been pinned, e.g., change its background color
 *         view.setBackgroundColor(Color.RED)
 *     }
 *
 *     override fun onViewUnpinned(view: View, index: Int) {
 *         // View has been unpinned, restore its original state
 *         view.setBackgroundColor(Color.TRANSPARENT)
 *     }
 * })
 * ```
 */
interface OnStickyStateChangedListener {

    /**
     * Called when a view becomes pinned (stuck to the top).
     *
     * @param view The view that has been pinned
     * @param index The index of the view within [StickyLinearLayout]
     */
    fun onViewPinned(view: View, index: Int)

    /**
     * Called when a view becomes unpinned (no longer stuck).
     *
     * @param view The view that has been unpinned
     * @param index The index of the view within [StickyLinearLayout]
     */
    fun onViewUnpinned(view: View, index: Int)

    /**
     * Called when the offset of a pinned view changes during scrolling.
     *
     * This is useful for creating parallax effects or progressive animations
     * based on how far the view has scrolled.
     *
     * @param view The pinned view
     * @param index The index of the view within [StickyLinearLayout]
     * @param offset The current vertical offset applied to the view
     */
    fun onPinnedViewOffsetChanged(view: View, index: Int, offset: Int) {}

    /**
     * Called when the list of all currently pinned views changes.
     *
     * This provides a snapshot of all views that are currently in the pinned state.
     *
     * @param pinnedViews List of views that are currently pinned, in order from top to bottom
     */
    fun onPinnedViewsChanged(pinnedViews: List<View>) {}

    /**
     * Called when the total height of all pinned views changes.
     *
     * This is useful for adjusting other UI elements based on the sticky header area size.
     *
     * @param totalPinnedHeight The total height in pixels of all currently pinned views
     */
    fun onPinnedHeightChanged(totalPinnedHeight: Int) {}
}
