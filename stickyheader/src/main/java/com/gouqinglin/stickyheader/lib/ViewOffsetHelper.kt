package com.gouqinglin.stickyheader.lib

import android.view.View

/**
 * 辅助类，用于管理View的top和bottom偏移
 * 通过设置translationY来实现视觉上的偏移，而不影响真正的布局位置
 */
class ViewOffsetHelper(private val view: View) {

    private var offsetTop: Int = 0

    /**
     * 设置垂直方向的偏移量
     */
    var topAndBottomOffset: Int
        get() = offsetTop
        set(offset) {
            if (offsetTop != offset) {
                offsetTop = offset
                updateOffsets()
            }
        }

    private fun updateOffsets() {
        // 使用translationY来实现偏移，这不会影响View的实际布局位置
        view.translationY = offsetTop.toFloat()
    }
}
