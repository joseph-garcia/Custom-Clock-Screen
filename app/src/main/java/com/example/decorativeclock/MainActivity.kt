package com.example.decorativeclock


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
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

    private lateinit var optionsIcon: ImageView
    private val fadeHandler = Handler(Looper.getMainLooper())
    private var statusBarHeight = 0

    private val fadeOutHandler = Handler(Looper.getMainLooper())
    private val fadeOutRunnable = Runnable { fadeOutViews() }

    private val notificationBarHandler = Handler(Looper.getMainLooper())



    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize optionsIcon
        optionsIcon = findViewById(R.id.optionsIcon)
        optionsIcon.y = statusBarHeight.toFloat()

        statusBarHeight = getStatusBarHeight()

        optionsIcon.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        val fadeOutRunnable = Runnable {
            fadeOutViews()
        }

        val frameLayout = findViewById<FrameLayout>(R.id.frameLayout)
        frameLayout.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                fadeInViews()

                // Cancel any pending fade-out actions
                fadeOutHandler.removeCallbacks(fadeOutRunnable)
                notificationBarHandler.removeCallbacksAndMessages(null) // Add this line

                // Schedule a new fade-out action
                fadeOutHandler.postDelayed(fadeOutRunnable, 5000)
            }
            false
        }


        // Set up the fade in/out behavior
        val decorView = window.decorView
        decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                fadeInViews()
            } else {
                fadeOutViews()
            }
        }

        // Add this line to start the fade out timer when the app starts
        fadeHandler.postDelayed(fadeOutRunnable, 5000)

        clockTextView = findViewById(R.id.clockTextView)
        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())

        val handler = Handler(Looper.getMainLooper())

        val updateClockRunnable = object : Runnable {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun run() {
                updateClock()
                handler.postDelayed(this, 60000)
            }
        }
        handler.post(updateClockRunnable)

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

    override fun onResume() {
        super.onResume()
        setBackgroundImage()
    }

    // Add these helper methods to fade in and fade out the views
    private fun fadeOutViews() {
        optionsIcon.animate().alpha(0f).duration = 300

        // Use the new notification bar handler to schedule a new action
        notificationBarHandler.postDelayed({
            hideSystemUI()
        }, 3000) // 3 seconds idle threshold
    }

    private fun fadeInViews() {


        showSystemUI()

        optionsIcon.animate()
            .alpha(1f)
            .setDuration(300)
            .withEndAction {
                // Reset the options icon's position
                optionsIcon.y = statusBarHeight.toFloat()

            }
            .start()
    }

    private fun hideSystemUI() {
        val decorView = window.decorView
        val uiOptions = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        decorView.systemUiVisibility = uiOptions
    }

    private fun showSystemUI() {
        val decorView = window.decorView
        val uiOptions = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        decorView.systemUiVisibility = uiOptions
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateClock() {
        val currentTime = LocalDateTime.now()
        val formattedTime = currentTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
        clockTextView.text = formattedTime
    }

    // Add this method to get the status bar height for the options icon
    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

    private fun setBackgroundImage() {
        val sharedPreferences = getSharedPreferences("clock_data", Context.MODE_PRIVATE)
        val backgroundImageUriString = sharedPreferences.getString("background_image_uri", null)

        if (backgroundImageUriString != null) {
            val backgroundImageUri = Uri.parse(backgroundImageUriString)
            val frameLayout = findViewById<FrameLayout>(R.id.frameLayout)

            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, backgroundImageUri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(contentResolver, backgroundImageUri)
                }
                frameLayout.background = BitmapDrawable(resources, bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error loading background image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setBackgroundImage()
    }





}