package com.example.decorativeclock

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private lateinit var clockTextView: TextView
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var scaleFactor = 1f
    // Add a new instance variable to store the last MotionEvent
    private var lastEvent: MotionEvent? = null

    private lateinit var resetPositionButton: Button
    private var dX = 0f
    private var dY = 0f
    

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clockTextView = findViewById(R.id.clockTextView)
        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())

        val handler = Handler(Looper.getMainLooper())
        val longPressRunnable = Runnable {
            lastEvent?.let { event ->
                val x = event.x
                val y = event.y
                showMenu(x, y)
            }
        }

        val frameLayout = findViewById<FrameLayout>(R.id.frameLayout)
        frameLayout.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastEvent = event
                    handler.postDelayed(longPressRunnable, 1000) // Long press threshold (1000ms)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_MOVE -> {
                    handler.removeCallbacks(longPressRunnable)
                }
            }
            false
        }

        val updateClockRunnable = object : Runnable {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun run() {
                updateClock()
                handler.postDelayed(this, 60000)
            }
        }

        handler.post(updateClockRunnable)

//        resetPositionButton = findViewById(R.id.resetPositionButton)
//        resetPositionButton.setOnClickListener {
//            resetClockPosition()
//        }

        // Load and set the saved clock data (position and scale factor)
        val clockData = loadClockPosition()
        scaleFactor = clockData.third
        clockTextView.scaleX = scaleFactor
        clockTextView.scaleY = scaleFactor

        clockTextView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                clockTextView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val minX = 0f
                val minY = 0f
                val maxX = resources.displayMetrics.widthPixels - clockTextView.width.toFloat()
                val maxY = resources.displayMetrics.heightPixels - clockTextView.height.toFloat()

                if (clockData.first != -1f && clockData.second != -1f) {
                    clockTextView.x = clockData.first.coerceIn(minX, maxX)
                    clockTextView.y = clockData.second.coerceIn(minY, maxY)
                } else {
                    resetClockPosition()
                }
            }
        })

        clockTextView.setOnTouchListener { view, event ->
            scaleGestureDetector.onTouchEvent(event)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
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
        val orientation = resources.configuration.orientation
        val orientationKey = if (orientation == Configuration.ORIENTATION_PORTRAIT) "portrait" else "landscape"

        editor.putFloat("${orientationKey}_x", x)
        editor.putFloat("${orientationKey}_y", y)
        editor.putFloat("${orientationKey}_scaleFactor", scaleFactor)
        editor.apply()
    }

    private fun loadClockPosition(): Triple<Float, Float, Float> {
        val sharedPreferences = getSharedPreferences("clock_data", MODE_PRIVATE)
        val orientation = resources.configuration.orientation
        val orientationKey = if (orientation == Configuration.ORIENTATION_PORTRAIT) "portrait" else "landscape"

        val x = sharedPreferences.getFloat("${orientationKey}_x", -1f)
        val y = sharedPreferences.getFloat("${orientationKey}_y", -1f)
        val scaleFactor = sharedPreferences.getFloat("${orientationKey}_scaleFactor", 1f)
        return Triple(x, y, scaleFactor)
    }

    private fun showMenu(x: Float, y: Float) {
        val anchorView = View(this)
        anchorView.x = x
        anchorView.y = y
        val popupMenu = PopupMenu(this, anchorView)
        popupMenu.inflate(R.menu.menu_layout)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.changeBackground -> {
                    // Handle change background action
                }
                R.id.changeFont -> {
                    // Handle change font action
                }
                R.id.settings -> {
                    // Handle settings action
                }
            }
            true
        }
        popupMenu.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateClock() {
        val currentTime = LocalDateTime.now()
        val formattedTime = currentTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
        clockTextView.text = formattedTime
    }

}