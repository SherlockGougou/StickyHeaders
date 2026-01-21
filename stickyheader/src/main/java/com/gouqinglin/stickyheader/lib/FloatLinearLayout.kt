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
 * 竖向的LinearLayout，支持内部View声明为pin，以在滑动到顶部时悬停。
 *
 * 核心思想：在自定义的Layout中，设置子View的垂直offset，
 * 以实现当Layout移动时，子View相对静止（悬停效果）。
 *
 * 使用方式：
 * 1. 将此Layout放在AppBarLayout内部
 * 2. 为此Layout设置 app:layout_scrollFlags="scroll|exitUntilCollapsed"
 * 3. 在子View中使用 app:layout_pin="true" 来声明需要吸顶的View
 */
class FloatLinearLayout @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(ctx, attrs, defStyle) {
    private var mOnOffsetChangedListener: AppBarLayout.OnOffsetChangedListener? = null

    private val TAG = "FloatLinearLayout"

    init {
        // 启用自定义绘制顺序，让pin的View绘制在最上层
        isChildrenDrawingOrderEnabled = true
    }

    // 每个子View及其上方所有View的高度累计
    private var mTopFloatViewMargins: SparseArray<Int>? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var heightSum = 0
        var minHeightSum = 0
        mTopFloatViewMargins = SparseArray(childCount)
        (0 until childCount).forEach {
            heightSum += getChildAt(it).measuredHeight
            mTopFloatViewMargins?.put(it, heightSum)
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
     * 反转绘制顺序，让后面的View先绘制，前面的View后绘制
     * 这样当HEADER1悬停在顶部时，会覆盖在其他View上面
     */
    override fun getChildDrawingOrder(childCount: Int, drawingPosition: Int): Int {
        return childCount - drawingPosition - 1
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
            val ta = c?.obtainStyledAttributes(attrs, R.styleable.FloatLinearLayout_Layout)
            pin = ta?.getBoolean(R.styleable.FloatLinearLayout_Layout_layout_pin, false) ?: false
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
            (0 until childCount).forEach { i ->
                val child = getChildAt(i)
                val lp = child.layoutParams as? LayoutParams
                if (lp?.pin == true) {
                    // floatStartHeight: 该子View上方所有View的高度总和（即该View的原始top位置）
                    val floatStartHeight = (mTopFloatViewMargins?.get(i) ?: 0) - child.measuredHeight
                    if (floatStartHeight < 0) {
                        Log.e(TAG, "Impossible!!! floatStartHeight=$floatStartHeight, i=$i")
                        return
                    }

                    // offset: 需要补偿的偏移量，使View悬停在顶部
                    // -verticalOffset: AppBarLayout向上滚动的距离（正值）
                    // top: FloatLinearLayout在AppBarLayout中的位置
                    val offset = -verticalOffset - top

                    // 只有当offset > floatStartHeight时，说明该View已经到达需要悬停的位置
                    // 此时设置偏移量使其悬停
                    // 否则offset为0，View保持原位
                    val finalOffset = if (offset > floatStartHeight) {
                        offset
                    } else {
                        0
                    }
                    getViewOffsetHelper(child).topAndBottomOffset = finalOffset
                }
            }
        }
    }
}