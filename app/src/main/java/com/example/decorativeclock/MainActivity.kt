package com.example.decorativeclock

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Button
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private lateinit var clockTextView: TextView
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var scaleFactor = 1f

    private lateinit var resetPositionButton: Button

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clockTextView = findViewById(R.id.clockTextView)
        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())

        // Load and set the saved clock position and scale
        val clockData = loadClockPosition()
        if (clockData.first != -1f && clockData.second != -1f) {
            clockTextView.x = clockData.first
            clockTextView.y = clockData.second
        }
        scaleFactor = clockData.third
        clockTextView.scaleX = scaleFactor
        clockTextView.scaleY = scaleFactor

        // Update clock every second
        val handler = Handler(Looper.getMainLooper())
        Timer().scheduleAtFixedRate(timerTask {
            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            handler.post {
                clockTextView.text = currentTime
            }
        }, 0, 1000)

        // Draggable and resizable clock
        var dX = 0f
        var dY = 0f
        clockTextView.setOnTouchListener { view, event ->
            scaleGestureDetector.onTouchEvent(event)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.performClick()
                    view.animate().cancel()
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    val newX = event.rawX + dX
                    val newY = event.rawY + dY

                    val minX = 0f
                    val minY = 0f
                    val maxX = resources.displayMetrics.widthPixels - view.width.toFloat()
                    val maxY = resources.displayMetrics.heightPixels - view.height.toFloat()

                    view.x = newX.coerceIn(minX, maxX)
                    view.y = newY.coerceIn(minY, maxY)
                }
                MotionEvent.ACTION_UP -> {
                    saveClockPosition(view.x, view.y, scaleFactor)
                }
            }
            true
        }

        resetPositionButton = findViewById(R.id.resetPositionButton)
        resetPositionButton.setOnClickListener {
            resetClockPosition()
        }

        savedInstanceState?.let {
            clockTextView.x = it.getFloat("x", clockTextView.x)
            clockTextView.y = it.getFloat("y", clockTextView.y)
            scaleFactor = it.getFloat("scaleFactor", scaleFactor)
            clockTextView.scaleX = scaleFactor
            clockTextView.scaleY = scaleFactor
        }

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            val minX = 0f
            val minY = 0f
            val maxX = resources.displayMetrics.widthPixels - clockTextView.width.toFloat()
            val maxY = resources.displayMetrics.heightPixels - clockTextView.height.toFloat()

            clockTextView.x = clockTextView.x.coerceIn(minX, maxX)
            clockTextView.y = clockTextView.y.coerceIn(minY, maxY)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putFloat("x", clockTextView.x)
        outState.putFloat("y", clockTextView.y)
        outState.putFloat("scaleFactor", scaleFactor)
    }

    private fun resetClockPosition() {
        clockTextView.x = (resources.displayMetrics.widthPixels - clockTextView.width) / 2f
        clockTextView.y = (resources.displayMetrics.heightPixels - clockTextView.height) / 2f
        saveClockPosition(clockTextView.x, clockTextView.y, 1.0f)
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = max(0.1f, min(scaleFactor, 5.0f))
            clockTextView.scaleX = scaleFactor
            clockTextView.scaleY = scaleFactor
            saveClockPosition(clockTextView.x, clockTextView.y, scaleFactor)
            return true
        }
    }

    private fun saveClockPosition(x: Float, y: Float, scaleFactor: Float) {
        val sharedPreferences = getSharedPreferences("clock_data", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putFloat("x", x)
        editor.putFloat("y", y)
        editor.putFloat("scaleFactor", scaleFactor)
        editor.apply()
    }

    private fun loadClockPosition(): Triple<Float, Float, Float> {
        val sharedPreferences = getSharedPreferences("clock_data", MODE_PRIVATE)
        val x = sharedPreferences.getFloat("x", -1f)
        val y = sharedPreferences.getFloat("y", -1f)
        val scaleFactor = sharedPreferences.getFloat("scaleFactor", 1f)
        return Triple(x, y, scaleFactor)
    }
}