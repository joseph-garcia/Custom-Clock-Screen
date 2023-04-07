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





class ResizableClockTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : TextClock(context, attrs, defStyleAttr) {

    private val scaleDetector: ScaleGestureDetector
    private val gestureDetector: GestureDetector
    private val originalTextSize = 50f;


    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var rotationAngle = 0f

    init {
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, DoubleTapListener())
        var initialTextSize = textSize
        setFormatting()

    }

    fun setFormatting() {
        val sharedPreferences = context.getSharedPreferences("decorative_clock_preferences", Context.MODE_PRIVATE)
        val is24HourFormat = sharedPreferences.getBoolean("is_24_hour_format", false)
        Log.d("josephDebug", "getting called here?.. is24HourFormat: $is24HourFormat")
        if (is24HourFormat) {
            format12Hour = null
            format24Hour = "H:mm"
        } else {
            format24Hour = null
            format12Hour = "h:mm a"
        }
    }



    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
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
        private var scaleFactor = 1f

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceAtLeast(0.1f).coerceAtMost(5.0f)

            val newSize = originalTextSize * scaleFactor
            textSize = newSize.coerceAtLeast(10f).coerceAtMost(500f)
            return true
        }
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











