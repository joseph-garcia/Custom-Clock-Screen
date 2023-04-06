package com.example.decorativeclock
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.GestureDetector
import android.view.ScaleGestureDetector
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextClock




class ResizableClockTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : TextClock(context, attrs, defStyleAttr) {

    private val scaleDetector: ScaleGestureDetector
    private val originalTextSize = textSize

    private var lastTouchX = 0f
    private var lastTouchY = 0f

    init {
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.rawX
                lastTouchY = event.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress) {
                    val dx = event.rawX - lastTouchX
                    val dy = event.rawY - lastTouchY

                    val layoutParams = layoutParams as FrameLayout.LayoutParams
                    layoutParams.leftMargin += dx.toInt()
                    layoutParams.topMargin += dy.toInt()
                    this.layoutParams = layoutParams

                    lastTouchX = event.rawX
                    lastTouchY = event.rawY
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                lastTouchX = 0f
                lastTouchY = 0f
            }
        }
        scaleDetector.onTouchEvent(event)
        return true
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private val initialTextSize: Float = textSize
        private var accumulatedScaleFactor = 1f

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            accumulatedScaleFactor *= detector.scaleFactor
            accumulatedScaleFactor = accumulatedScaleFactor.coerceAtLeast(0.1f).coerceAtMost(5.0f)

            val newSize = initialTextSize * accumulatedScaleFactor
            textSize = newSize.coerceAtLeast(10f).coerceAtMost(500f)
            return true
        }
    }


}









