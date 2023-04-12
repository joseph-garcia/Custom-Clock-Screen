package com.example.decorativeclock
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.GestureDetector
import android.view.ScaleGestureDetector
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextClock
import android.animation.ObjectAnimator
import android.graphics.Matrix
import kotlin.math.sqrt

class ResizableClockTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : TextClock(context, attrs, defStyleAttr) {

    private var rotationAngle = 0f
    private val gestureDetector: GestureDetector
    private var resetLastTouchPosition = true
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var originalDistance = 0f
    private var prevScaleFactor = 1f
    private var isSecondFingerTouched = false

    init {
        gestureDetector = GestureDetector(context, DoubleTapListener())
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSpec = MeasureSpec.makeMeasureSpec(Int.MAX_VALUE shr 2, MeasureSpec.AT_MOST)
        val heightSpec = MeasureSpec.makeMeasureSpec(Int.MAX_VALUE shr 2, MeasureSpec.AT_MOST)
        super.onMeasure(widthSpec, heightSpec)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    isSecondFingerTouched = true
                    originalDistance = calculateDistance(event) / prevScaleFactor
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isSecondFingerTouched && event.pointerCount == 2) {
                    // Calculate the distance between the two fingers
                    val distance = calculateDistance(event)

                    // Calculate the new scale factor
                    val newScaleFactor = distance / originalDistance

                    // Apply a low-pass filter to the scaleFactor
                    val smoothFactor = 0.2f
                    val smoothScaleFactor = (newScaleFactor * smoothFactor) + (prevScaleFactor * (1 - smoothFactor))

                    // Apply the smooth scale factor to the TextClock
                    this@ResizableClockTextView.scaleX = smoothScaleFactor
                    this@ResizableClockTextView.scaleY = smoothScaleFactor

                    // Update the previous scale factor
                    prevScaleFactor = smoothScaleFactor
                } else if (!isSecondFingerTouched && event.pointerCount == 1) {
                    if (!resetLastTouchPosition) {
                        val dx = event.rawX - lastTouchX
                        val dy = event.rawY - lastTouchY

                        val layoutParams = layoutParams as FrameLayout.LayoutParams
                        layoutParams.leftMargin += dx.toInt()
                        layoutParams.topMargin += dy.toInt()
                        this.layoutParams = layoutParams

                        lastTouchX = event.rawX
                        lastTouchY = event.rawY
                    } else {
                        lastTouchX = event.rawX
                        lastTouchY = event.rawY
                        resetLastTouchPosition = false
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount == 2) {
                    isSecondFingerTouched = false
                    resetLastTouchPosition = true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                lastTouchX = 0f
                lastTouchY = 0f
                resetLastTouchPosition = true
                isSecondFingerTouched = false
                if (event.pointerCount == 1) {
                    resetLastTouchPosition = true
                }
            }
        }
        return true
    }

    private fun calculateDistance(event: MotionEvent): Float {
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return sqrt(dx * dx + dy * dy)
    }
    private inner class DoubleTapListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val nextRotationAngle = rotationAngle + 90f

            val rotateAnimator = ObjectAnimator.ofFloat(
                this@ResizableClockTextView,
                "rotation",
                rotationAngle,
                nextRotationAngle
            )
            rotateAnimator.duration = 300 // Duration of the animation in milliseconds
            rotateAnimator.start()

            rotationAngle = nextRotationAngle % 360
            return true
        }
    }
}











