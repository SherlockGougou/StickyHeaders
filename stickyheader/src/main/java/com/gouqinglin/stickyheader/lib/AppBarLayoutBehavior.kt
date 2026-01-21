package com.gouqinglin.stickyheader.lib

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.OverScroller
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import java.lang.reflect.Field

/**
 * A custom [AppBarLayout.Behavior] that fixes common scrolling issues.
 *
 * This behavior addresses the following problems:
 * 1. **Bounce-back**: Fast scrolling causes AppBarLayout to bounce back unexpectedly
 * 2. **Jitter**: Quickly changing scroll direction causes visual jitter
 * 3. **Unstoppable fling**: Unable to stop scrolling by touching the screen
 *
 * ## Usage
 *
 * Apply this behavior to your AppBarLayout in XML:
 *
 * ```xml
 * <com.google.android.material.appbar.AppBarLayout
 *     app:layout_behavior="com.gouqinglin.stickyheader.lib.AppBarLayoutBehavior"
 *     ... >
 * ```
 *
 * ---
 *
 * 解决 AppBarLayout 的常见滚动问题：
 * 1. **回弹问题**：快速滑动 AppBarLayout 会出现回弹
 * 2. **抖动问题**：快速滑动到折叠状态后立即下滑会出现抖动
 * 3. **无法停止**：滑动过程中无法通过触摸屏幕停止滚动
 *
 * @param context The context
 * @param attrs The attribute set
 */
class AppBarLayoutBehavior(context: Context?, attrs: AttributeSet?) : AppBarLayout.Behavior(context, attrs) {
    private var isFlinging = false
    private var shouldBlockNestedScroll = false

    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: AppBarLayout, ev: MotionEvent): Boolean {
        shouldBlockNestedScroll = false
        if (isFlinging) {
            shouldBlockNestedScroll = true
        }

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> stopAppbarLayoutFling(child) // 手指触摸屏幕的时候停止fling事件
        }

        return super.onInterceptTouchEvent(parent, child, ev)
    }

    @get:Throws(NoSuchFieldException::class)
    private val flingRunnableField: Field
        /**
         * 反射获取私有的flingRunnable 属性，考虑support 28以后变量名修改的问题
         * 
         * @return Field
         */
        get() {
            try {
                // support design 27及以下版本
                val headerBehaviorType: Class<*>? = this.javaClass.getSuperclass().getSuperclass()
                return headerBehaviorType!!.getDeclaredField("mFlingRunnable")
            } catch (e: NoSuchFieldException) {
                // 可能是28及以上版本
                val headerBehaviorType: Class<*>? = this.javaClass.getSuperclass().getSuperclass().getSuperclass()
                return headerBehaviorType!!.getDeclaredField("flingRunnable")
            }
        }

    @get:Throws(NoSuchFieldException::class)
    private val scrollerField: Field
        /**
         * 反射获取私有的scroller 属性，考虑support 28以后变量名修改的问题
         * 
         * @return Field
         */
        get() {
            try {
                // support design 27及以下版本
                val headerBehaviorType: Class<*>? = this.javaClass.getSuperclass().getSuperclass()
                return headerBehaviorType!!.getDeclaredField("mScroller")
            } catch (e: NoSuchFieldException) {
                // 可能是28及以上版本
                val headerBehaviorType: Class<*>? = this.javaClass.getSuperclass().getSuperclass().getSuperclass()
                return headerBehaviorType!!.getDeclaredField("scroller")
            }
        }

    /**
     * 停止appbarLayout的fling事件
     * 
     * @param appBarLayout
     */
    private fun stopAppbarLayoutFling(appBarLayout: AppBarLayout) {
        // 通过反射拿到HeaderBehavior中的flingRunnable变量
        try {
            val flingRunnableField = this.flingRunnableField
            val scrollerField = this.scrollerField
            flingRunnableField.isAccessible = true
            scrollerField.isAccessible = true

            val flingRunnable = flingRunnableField.get(this) as Runnable?
            val overScroller = scrollerField.get(this) as OverScroller?
            if (flingRunnable != null) {
                appBarLayout.removeCallbacks(flingRunnable)
                flingRunnableField.set(this, null)
            }
            if (overScroller != null && !overScroller.isFinished) {
                overScroller.abortAnimation()
            }
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    override fun onStartNestedScroll(
        parent: CoordinatorLayout,
        child: AppBarLayout,
        directTargetChild: View,
        target: View,
        nestedScrollAxes: Int,
        type: Int
    ): Boolean {
        stopAppbarLayoutFling(child)
        return super.onStartNestedScroll(parent, child, directTargetChild, target, nestedScrollAxes, type)
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: AppBarLayout,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        // type返回1时，表示当前target处于非touch的滑动，
        // 该bug的引起是因为appbar在滑动时，CoordinatorLayout内的实现NestedScrollingChild2接口的滑动子类还未结束其自身的fling
        // 所以这里监听子类的非touch时的滑动，然后block掉滑动事件传递给AppBarLayout
        if (type == TYPE_FLING) {
            isFlinging = true
        }
        if (!shouldBlockNestedScroll) {
            super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: AppBarLayout,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int
    ) {
        if (!shouldBlockNestedScroll) {
            super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type)
        }
    }

    override fun onStopNestedScroll(coordinatorLayout: CoordinatorLayout, abl: AppBarLayout, target: View, type: Int) {
        super.onStopNestedScroll(coordinatorLayout, abl, target, type)
        isFlinging = false
        shouldBlockNestedScroll = false
    }

    companion object {
        private const val TYPE_FLING = 1
    }
}