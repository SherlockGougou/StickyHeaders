package com.gouqinglin.stickyheader.lib

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import com.google.android.material.appbar.AppBarLayout

/**
 * Defines the sticky behavior mode for [StickyLinearLayout].
 */
enum class StickyMode {
    /**
     * Multiple headers stack on top of each other.
     * When a new header becomes pinned, it appears below the previously pinned headers.
     */
    MULTI,

    /**
     * Only one header is visible at a time.
     * When a new header becomes pinned, it pushes the previous header out of view.
     */
    SINGLE
}

/**
 * A vertical LinearLayout that supports multi-level sticky headers.
 *
 * This layout must be placed inside an [AppBarLayout] and works by listening to
 * offset changes. When a child view marked with `app:layout_pin="true"` reaches
 * the top of the visible area, it will be pinned (stuck) in place while other
 * views continue to scroll.
 *
 * ## Sticky Modes
 *
 * - **MULTI**: Multiple headers stack on top of each other (default)
 * - **SINGLE**: Only one header is visible, new header pushes out the previous one
 *
 * ## Usage
 *
 * 1. Place `StickyLinearLayout` inside an `AppBarLayout`
 * 2. Set `app:layout_scrollFlags="scroll|exitUntilCollapsed"` on this layout
 * 3. Add `app:layout_pin="true"` to child views that should stick
 * 4. Optionally set `app:stickyMode="single"` for single header mode
 *
 * ## Example
 *
 * ```xml
 * <com.google.android.material.appbar.AppBarLayout
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content">
 *
 *     <com.gouqinglin.stickyheader.lib.StickyLinearLayout
 *         android:layout_width="match_parent"
 *         android:layout_height="wrap_content"
 *         app:stickyMode="multi"
 *         app:layout_scrollFlags="scroll|exitUntilCollapsed">
 *
 *         <TextView
 *             android:layout_width="match_parent"
 *             android:layout_height="50dp"
 *             app:layout_pin="true" />
 *
 *     </com.gouqinglin.stickyheader.lib.StickyLinearLayout>
 *
 * </com.google.android.material.appbar.AppBarLayout>
 * ```
 *
 * @see AppBarLayout
 * @see LayoutParams.pin
 * @see StickyMode
 */
class StickyLinearLayout @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(ctx, attrs, defStyle) {
    private var mOnOffsetChangedListener: AppBarLayout.OnOffsetChangedListener? = null

    private val TAG = "StickyLinearLayout"

    /**
     * The current sticky mode.
     *
     * - [StickyMode.MULTI]: Headers stack on top of each other (default)
     * - [StickyMode.SINGLE]: Only one header visible, new pushes out old
     */
    var stickyMode: StickyMode = StickyMode.MULTI
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    // 回调监听器
    private var stickyStateChangedListener: OnStickyStateChangedListener? = null

    // 记录每个pin View的当前吸顶状态，用于检测状态变化
    private val pinnedStateMap = mutableMapOf<Int, Boolean>()

    // 上一次的吸顶View列表，用于检测变化
    private var lastPinnedViews = listOf<View>()

    // 上一次的吸顶总高度
    private var lastPinnedHeight = 0

    init {
        // 启用自定义绘制顺序，让pin的View绘制在最上层
        isChildrenDrawingOrderEnabled = true

        // 解析XML属性
        attrs?.let {
            val ta = ctx.obtainStyledAttributes(it, R.styleable.StickyLinearLayout)
            val modeValue = ta.getInt(R.styleable.StickyLinearLayout_stickyMode, 0)
            stickyMode = if (modeValue == 1) StickyMode.SINGLE else StickyMode.MULTI
            ta.recycle()
        }
    }

    /**
     * Sets a listener to receive callbacks when sticky state changes.
     *
     * @param listener The listener to set, or null to remove the current listener
     */
    fun setOnStickyStateChangedListener(listener: OnStickyStateChangedListener?) {
        stickyStateChangedListener = listener
    }

    /**
     * Returns the current list of pinned views.
     *
     * @return List of views that are currently pinned, in order from top to bottom
     */
    fun getPinnedViews(): List<View> {
        return lastPinnedViews.toList()
    }

    /**
     * Returns the total height of all currently pinned views.
     *
     * @return The total height in pixels
     */
    fun getPinnedHeight(): Int {
        return lastPinnedHeight
    }

    /**
     * Checks if a specific view is currently pinned.
     *
     * @param view The view to check
     * @return true if the view is currently pinned, false otherwise
     */
    fun isViewPinned(view: View): Boolean {
        val index = indexOfChild(view)
        return pinnedStateMap[index] == true
    }

    // 每个子View及其上方所有View的高度累计
    private var mTopStickyViewMargins: SparseArray<Int>? = null

    // 记录最后一个pin View的高度（用于SINGLE模式）
    private var lastPinViewHeight = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var heightSum = 0
        var minHeightSum = 0
        lastPinViewHeight = 0
        mTopStickyViewMargins = SparseArray(childCount)
        (0 until childCount).forEach {
            heightSum += getChildAt(it).measuredHeight
            mTopStickyViewMargins?.put(it, heightSum)
            if (((getChildAt(it).layoutParams) as? LayoutParams)?.pin == true) {
                lastPinViewHeight = getChildAt(it).measuredHeight
                minHeightSum += getChildAt(it).measuredHeight
            }
        }
        // SINGLE模式只需要保留最后一个pin View的高度
        // MULTI模式需要保留所有pin View的高度
        minimumHeight = when (stickyMode) {
            StickyMode.SINGLE -> lastPinViewHeight
            StickyMode.MULTI -> minHeightSum
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Add OnOffsetChangedListener
        (parent as? AppBarLayout)?.let {
            ViewCompat.setFitsSystemWindows(this, ViewCompat.getFitsSystemWindows(it))
            if (mOnOffsetChangedListener == null) {
                mOnOffsetChangedListener = this.OffsetUpdateListener()
            }
            it.addOnOffsetChangedListener(mOnOffsetChangedListener)
            ViewCompat.requestApplyInsets(this)
        }
    }

    override fun onDetachedFromWindow() {
        if (mOnOffsetChangedListener != null && parent is AppBarLayout) {
            (parent as AppBarLayout).removeOnOffsetChangedListener(mOnOffsetChangedListener)
        }
        super.onDetachedFromWindow()
    }

    private fun getViewOffsetHelper(view: View): ViewOffsetHelper {
        var offsetHelper = view.getTag(com.google.android.material.R.id.view_offset_helper) as? ViewOffsetHelper
        if (offsetHelper == null) {
            offsetHelper = ViewOffsetHelper(view)
            view.setTag(com.google.android.material.R.id.view_offset_helper, offsetHelper)
        }
        return offsetHelper
    }

    /**
     * 自定义绘制顺序：
     * - MULTI模式：先出现的pin View在上层（后绘制），这样堆叠时第一个Header在最上面
     * - SINGLE模式：后出现的pin View在上层（后绘制），这样新Header可以覆盖旧Header
     */
    override fun getChildDrawingOrder(childCount: Int, drawingPosition: Int): Int {
        // 收集非pin和pin的索引
        val nonPinIndices = mutableListOf<Int>()
        val pinIndices = mutableListOf<Int>()

        for (i in 0 until childCount) {
            val lp = getChildAt(i).layoutParams as? LayoutParams
            if (lp?.pin == true) {
                pinIndices.add(i)
            } else {
                nonPinIndices.add(i)
            }
        }

        // 绘制顺序：先非pin的（按原顺序），再pin的
        val orderedPinIndices = when (stickyMode) {
            // MULTI模式：逆序绘制pin View，第一个pin View最后绘制（在最上层）
            StickyMode.MULTI -> pinIndices.reversed()
            // SINGLE模式：正序绘制pin View，最后一个pin View最后绘制（在最上层）
            StickyMode.SINGLE -> pinIndices
        }

        val drawOrder = nonPinIndices + orderedPinIndices

        return if (drawingPosition < drawOrder.size) {
            drawOrder[drawingPosition]
        } else {
            drawingPosition
        }
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?) = p is LayoutParams

    override fun generateDefaultLayoutParams() = LayoutParams(MATCH_PARENT, MATCH_PARENT)

    override fun generateLayoutParams(attrs: AttributeSet?) = LayoutParams(context, attrs)

    override fun generateLayoutParams(lp: ViewGroup.LayoutParams?) = LayoutParams(lp)

    /**
     * Per-child layout information for children of [StickyLinearLayout].
     *
     * @property pin Whether this child should stick to the top when scrolled.
     *               Set via `app:layout_pin="true"` in XML.
     */
    class LayoutParams : LinearLayout.LayoutParams {
        /**
         * Whether this child view should stick to the top when scrolled.
         *
         * When `true`, this view will be pinned at the top of the visible area
         * (or below previously pinned views) when the user scrolls up.
         */
        var pin = false

        constructor(c: Context?, attrs: AttributeSet?) : super(c, attrs) {
            val ta = c?.obtainStyledAttributes(attrs, R.styleable.StickyLinearLayout_Layout)
            pin = ta?.getBoolean(R.styleable.StickyLinearLayout_Layout_layout_pin, false) ?: false
            ta?.recycle()
        }

        constructor(width: Int, height: Int) : super(width, height)
        constructor(width: Int, height: Int, weight: Float) : super(width, height, weight)
        constructor(p: ViewGroup.LayoutParams?) : super(p) {
            if (p is LayoutParams) {
                pin = p.pin
            }
        }

        constructor(p: LinearLayout.LayoutParams?) : super(p) {
            if (p is LayoutParams) {
                pin = p.pin
            }
        }

        constructor(p: MarginLayoutParams?) : super(p) {
            if (p is LayoutParams) {
                pin = p.pin
            }
        }
    }

    private inner class OffsetUpdateListener : AppBarLayout.OnOffsetChangedListener {
        override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
            when (stickyMode) {
                StickyMode.MULTI -> handleMultiMode(verticalOffset)
                StickyMode.SINGLE -> handleSingleMode(verticalOffset)
            }
        }

        /**
         * MULTI模式：多个Header堆叠吸顶
         */
        private fun handleMultiMode(verticalOffset: Int) {
            // 用于存储已经吸顶的pin View累计高度
            var pinnedStackHeight = 0

            // 当前吸顶的View列表
            val currentPinnedViews = mutableListOf<View>()

            (0 until childCount).forEach { i ->
                val child = getChildAt(i)
                val lp = child.layoutParams as? LayoutParams
                if (lp?.pin == true) {
                    // stickyStartHeight: 该子View上方所有View的高度总和（即该View的原始top位置）
                    val stickyStartHeight = (mTopStickyViewMargins?.get(i) ?: 0) - child.measuredHeight
                    if (stickyStartHeight < 0) {
                        Log.e(TAG, "Impossible!!! stickyStartHeight=$stickyStartHeight, i=$i")
                        return
                    }

                    // 基础偏移：AppBarLayout向上滚动的距离 - StickyLinearLayout在AppBarLayout中的位置
                    val baseOffset = -verticalOffset - top

                    // 该View需要吸顶的触发位置
                    val triggerOffset = stickyStartHeight - pinnedStackHeight
                    val shouldPin = baseOffset >= triggerOffset

                    val finalOffset: Int
                    if (shouldPin) {
                        // 需要吸顶：设置偏移使View停在目标位置（已吸顶View的下方）
                        finalOffset = baseOffset - stickyStartHeight + pinnedStackHeight
                        pinnedStackHeight += child.measuredHeight
                        currentPinnedViews.add(child)
                    } else {
                        // 不需要吸顶：保持原位
                        finalOffset = 0
                    }

                    getViewOffsetHelper(child).topAndBottomOffset = finalOffset
                    notifyStateChange(child, i, shouldPin, finalOffset)
                }
            }

            updatePinnedViewsState(currentPinnedViews, pinnedStackHeight)
        }

        /**
         * SINGLE模式：只有一个Header吸顶，后面的会把前面的顶出去
         */
        private fun handleSingleMode(verticalOffset: Int) {
            // 当前吸顶的View列表
            val currentPinnedViews = mutableListOf<View>()

            // 收集所有pin View的信息
            data class PinViewInfo(val index: Int, val child: View, val stickyStartHeight: Int)
            val pinViews = mutableListOf<PinViewInfo>()

            (0 until childCount).forEach { i ->
                val child = getChildAt(i)
                val lp = child.layoutParams as? LayoutParams
                if (lp?.pin == true) {
                    val stickyStartHeight = (mTopStickyViewMargins?.get(i) ?: 0) - child.measuredHeight
                    if (stickyStartHeight >= 0) {
                        pinViews.add(PinViewInfo(i, child, stickyStartHeight))
                    }
                }
            }

            if (pinViews.isEmpty()) return

            val baseOffset = -verticalOffset - top

            // 处理每个pin View
            for (j in pinViews.indices) {
                val info = pinViews[j]
                val child = info.child
                val stickyStartHeight = info.stickyStartHeight
                val index = info.index
                val childHeight = child.measuredHeight

                // 检查是否有下一个pin View
                val nextInfo = if (j + 1 < pinViews.size) pinViews[j + 1] else null

                val finalOffset: Int
                val shouldPin: Boolean

                if (baseOffset < stickyStartHeight) {
                    // 还没到吸顶位置：保持原位
                    finalOffset = 0
                    shouldPin = false
                } else if (nextInfo == null) {
                    // 最后一个pin View：正常吸顶，不会被顶出
                    finalOffset = baseOffset - stickyStartHeight
                    shouldPin = true
                    currentPinnedViews.add(child)
                } else {
                    // 有下一个pin View：检查是否正在被顶出
                    val nextStickyStartHeight = nextInfo.stickyStartHeight

                    // 当下一个pin View到达当前pin View的底部时，开始顶出
                    // 顶出开始位置 = nextStickyStartHeight - childHeight
                    // 顶出结束位置 = nextStickyStartHeight（此时当前View完全被顶出）
                    val pushStartOffset = nextStickyStartHeight - childHeight

                    when {
                        baseOffset < pushStartOffset -> {
                            // 还没开始被顶出：正常吸顶
                            finalOffset = baseOffset - stickyStartHeight
                            shouldPin = true
                            currentPinnedViews.add(child)
                        }
                        baseOffset < nextStickyStartHeight -> {
                            // 正在被顶出：向上偏移
                            // 被推出的距离 = baseOffset - pushStartOffset
                            val pushAmount = baseOffset - pushStartOffset
                            finalOffset = (baseOffset - stickyStartHeight - pushAmount).coerceAtLeast(0)
                            shouldPin = finalOffset > 0
                            if (shouldPin) {
                                currentPinnedViews.add(child)
                            }
                        }
                        else -> {
                            // 完全被顶出：不显示
                            finalOffset = 0
                            shouldPin = false
                        }
                    }
                }

                getViewOffsetHelper(child).topAndBottomOffset = finalOffset
                notifyStateChange(child, index, shouldPin, finalOffset)
            }

            // SINGLE模式下，吸顶高度只计算当前显示的最后一个pin View
            val pinnedHeight = if (currentPinnedViews.isNotEmpty()) {
                currentPinnedViews.last().measuredHeight
            } else {
                0
            }

            updatePinnedViewsState(currentPinnedViews, pinnedHeight)
        }

        private fun notifyStateChange(child: View, index: Int, shouldPin: Boolean, finalOffset: Int) {
            // 检测状态变化并触发回调
            val wasPinned = pinnedStateMap[index] ?: false
            if (shouldPin != wasPinned) {
                pinnedStateMap[index] = shouldPin
                if (shouldPin) {
                    stickyStateChangedListener?.onViewPinned(child, index)
                } else {
                    stickyStateChangedListener?.onViewUnpinned(child, index)
                }
            }

            // 如果正在吸顶，通知偏移量变化
            if (shouldPin && finalOffset > 0) {
                stickyStateChangedListener?.onPinnedViewOffsetChanged(child, index, finalOffset)
            }
        }

        private fun updatePinnedViewsState(currentPinnedViews: List<View>, pinnedHeight: Int) {
            // 检测吸顶View列表变化
            if (currentPinnedViews != lastPinnedViews) {
                lastPinnedViews = currentPinnedViews.toList()
                stickyStateChangedListener?.onPinnedViewsChanged(lastPinnedViews)
            }

            // 检测吸顶总高度变化
            if (pinnedHeight != lastPinnedHeight) {
                lastPinnedHeight = pinnedHeight
                stickyStateChangedListener?.onPinnedHeightChanged(pinnedHeight)
            }
        }
    }
}