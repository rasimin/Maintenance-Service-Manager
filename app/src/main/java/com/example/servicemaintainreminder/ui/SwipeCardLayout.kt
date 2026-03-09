package com.example.servicemaintainreminder.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import com.example.servicemaintainreminder.R

class SwipeCardLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var startX = 0f
    private var startY = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var isDragging = false

    var onSwipeLeft: (() -> Unit)? = null
    var onSwipeRight: (() -> Unit)? = null
    var canSwipeLeft: () -> Boolean = { true }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.rawX
                startY = ev.rawY
                isDragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = Math.abs(ev.rawX - startX)
                val dy = Math.abs(ev.rawY - startY)
                if (dx > touchSlop && dx > dy) {
                    isDragging = true
                    parent.requestDisallowInterceptTouchEvent(true) // prevent nested scroll view from intercepting
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
        }
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (childCount < 2) return super.onTouchEvent(ev)
        
        // Assume the last child is the foreground, and the first child is the background container
        val foreground = getChildAt(childCount - 1)
        val bgDelete = findViewById<View>(R.id.llSwipeDeleteAreaDetail)
        val bgDone = findViewById<View>(R.id.llSwipeDoneAreaDetail)
        val threshold = width * 0.15f

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.rawX
                startY = ev.rawY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.rawX - startX
                val dy = ev.rawY - startY
                if (Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
                    parent.requestDisallowInterceptTouchEvent(true)
                }

                if (dx > 0) {
                    // Swipe right -> Delete
                    foreground.translationX = dx
                    bgDelete?.alpha = (dx / threshold).coerceIn(0f, 1f)
                    bgDone?.alpha = 0f
                } else if (dx < 0) {
                    if (canSwipeLeft()) {
                        // Swipe left -> Done
                        foreground.translationX = dx
                        bgDone?.alpha = (-dx / threshold).coerceIn(0f, 1f)
                        bgDelete?.alpha = 0f
                    } else {
                        // Resistance or no swipe
                        foreground.translationX = dx * 0.1f // small resistance
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val dx = ev.rawX - startX
                if (dx > threshold && ev.action == MotionEvent.ACTION_UP) {
                    onSwipeRight?.invoke()
                } else if (dx < -threshold && canSwipeLeft() && ev.action == MotionEvent.ACTION_UP) {
                    onSwipeLeft?.invoke()
                }

                foreground.animate().translationX(0f).setDuration(200).start()
                bgDelete?.animate()?.alpha(0f)?.setDuration(200)?.start()
                bgDone?.animate()?.alpha(0f)?.setDuration(200)?.start()
                isDragging = false
            }
        }
        return true
    }
}
