package com.velocity.launcher.ui.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class ScrollAwareAppWidgetHost(
    context: Context,
    hostId: Int
) : AppWidgetHost(context, hostId) {
    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidgetInfo: AppWidgetProviderInfo?
    ): AppWidgetHostView {
        return ScrollAwareAppWidgetHostView(context)
    }
}

class ScrollAwareAppWidgetHostView(context: Context) : AppWidgetHostView(context) {

    var onLongPressListener: (() -> Unit)? = null

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            onLongPressListener?.invoke()
        }
    })

    private var startX = 0f
    private var startY = 0f
    private var isVerticallyScrollableTarget = false
    private var isHorizontallyScrollableTarget = false
    private var verticalScrollableTarget: View? = null
    private var horizontalScrollableTarget: View? = null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.x
                startY = ev.y
                verticalScrollableTarget = findChild(this, ev.x, ev.y) { isVerticallyScrollable(it) }
                isVerticallyScrollableTarget = verticalScrollableTarget != null
                horizontalScrollableTarget = findChild(this, ev.x, ev.y) { isHorizontallyScrollable(it) }
                isHorizontallyScrollableTarget = horizontalScrollableTarget != null
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - startX
                val dy = ev.y - startY
                val absDx = abs(dx)
                val absDy = abs(dy)

                if (absDx > touchSlop || absDy > touchSlop) {
                    if (absDy > absDx) {
                        val targetView = verticalScrollableTarget
                        val canScroll = targetView != null && (
                            (dy < 0 && targetView.canScrollVertically(1)) ||
                            (dy > 0 && targetView.canScrollVertically(-1))
                        )
                        parent?.requestDisallowInterceptTouchEvent(isVerticallyScrollableTarget && canScroll)
                    } else {
                        val targetView = horizontalScrollableTarget
                        val canScroll = targetView != null && (
                            (dx < 0 && targetView.canScrollHorizontally(1)) ||
                            (dx > 0 && targetView.canScrollHorizontally(-1))
                        )
                        parent?.requestDisallowInterceptTouchEvent(isHorizontallyScrollableTarget && canScroll)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                isVerticallyScrollableTarget = false
                isHorizontallyScrollableTarget = false
                verticalScrollableTarget = null
                horizontalScrollableTarget = null
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun findChild(
        view: View,
        x: Float,
        y: Float,
        predicate: (View) -> Boolean
    ): View? {
        if (view !is ViewGroup) {
            return if (predicate(view)) view else null
        }

        for (i in view.childCount - 1 downTo 0) {
            val child = view.getChildAt(i)
            if (child.visibility != View.VISIBLE) continue

            val childLeft = child.left + child.translationX
            val childTop = child.top + child.translationY
            val childRight = childLeft + child.width
            val childBottom = childTop + child.height

            if (x >= childLeft && x <= childRight && y >= childTop && y <= childBottom) {
                val localX = x - childLeft + child.scrollX
                val localY = y - childTop + child.scrollY
                val result = findChild(child, localX, localY, predicate)
                if (result != null) {
                    return result
                }
            }
        }

        return if (predicate(view)) view else null
    }

    private fun isVerticallyScrollable(view: View): Boolean {
        if (view.canScrollVertically(1) || view.canScrollVertically(-1)) {
            return true
        }
        if (view is AbsListView) {
            val adapter = view.adapter
            return adapter != null && adapter.count > 0
        }
        if (view is ScrollView) {
            return view.childCount > 0 && view.getChildAt(0).height > view.height
        }
        if (view is NestedScrollView) {
            return view.childCount > 0 && view.getChildAt(0).height > view.height
        }
        if (view is RecyclerView) {
            val adapter = view.adapter
            val lm = view.layoutManager
            return adapter != null && adapter.itemCount > 0 && (lm == null || lm.canScrollVertically())
        }
        if (view is AdapterView<*>) {
            val adapter = view.adapter
            return adapter != null && adapter.count > 0
        }
        return false
    }

    private fun isHorizontallyScrollable(view: View): Boolean {
        if (view.canScrollHorizontally(1) || view.canScrollHorizontally(-1)) {
            return true
        }
        if (view is HorizontalScrollView) {
            return view.childCount > 0 && view.getChildAt(0).width > view.width
        }
        if (view is RecyclerView) {
            val adapter = view.adapter
            val lm = view.layoutManager
            return adapter != null && adapter.itemCount > 0 && (lm != null && lm.canScrollHorizontally())
        }
        return false
    }
}
