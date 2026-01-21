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

class StickyLinearLayout @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(ctx, attrs, defStyle) {
    private var mOnOffsetChangedListener: AppBarLayout.OnOffsetChangedListener? = null

    private val TAG = "StickyLinearLayout"

    init {
        // 启用自定义绘制顺序，让pin的View绘制在最上层
        isChildrenDrawingOrderEnabled = true
    }

    // 每个子View及其上方所有View的高度累计
    private var mTopStickyViewMargins: SparseArray<Int>? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var heightSum = 0
        var minHeightSum = 0
        mTopStickyViewMargins = SparseArray(childCount)
        (0 until childCount).forEach {
            heightSum += getChildAt(it).measuredHeight
            mTopStickyViewMargins?.put(it, heightSum)
            if (((getChildAt(it).layoutParams) as? LayoutParams)?.pin == true) {
                minHeightSum += getChildAt(it).measuredHeight
            }
        }
        minimumHeight = minHeightSum
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
     * 1. 非pin的View先绘制（在底层）
     * 2. pin的View后绘制（在上层），按照在布局中的逆序绘制
     *    这样先出现的pin View会在后出现的上面
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

        // 绘制顺序：先非pin的（按原顺序），再pin的（按逆序，这样第一个pin的View最后绘制，在最上层）
        val drawOrder = nonPinIndices + pinIndices.reversed()

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
     * 增加pin属性
     */
    class LayoutParams : LinearLayout.LayoutParams {
        var pin = false

        constructor(c: Context?, attrs: AttributeSet?) : super(c, attrs) {
            val ta = c?.obtainStyledAttributes(attrs, R.styleable.StickyLinearLayout)
            pin = ta?.getBoolean(R.styleable.StickyLinearLayout_layout_pin, false) ?: false
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
            // 用于存储已经吸顶的pin View累计高度
            var pinnedStackHeight = 0

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

                    // 该View需要吸顶的触发位置：
                    // 当View的顶部到达已吸顶View的底部时，应该开始吸顶
                    // 触发条件：stickyStartHeight - baseOffset <= pinnedStackHeight
                    // 即：baseOffset >= stickyStartHeight - pinnedStackHeight
                    val triggerOffset = stickyStartHeight - pinnedStackHeight
                    val shouldPin = baseOffset >= triggerOffset

                    val finalOffset: Int
                    if (shouldPin) {
                        // 需要吸顶：设置偏移使View停在目标位置（已吸顶View的下方）
                        finalOffset = baseOffset - stickyStartHeight + pinnedStackHeight
                        pinnedStackHeight += child.measuredHeight
                    } else {
                        // 不需要吸顶：保持原位
                        finalOffset = 0
                    }

                    getViewOffsetHelper(child).topAndBottomOffset = finalOffset
                }
            }
        }
    }
}