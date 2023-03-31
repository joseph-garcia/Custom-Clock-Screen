package com.example.decorativeclock


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.decorativeclock.SettingsActivity.Companion.IMAGE_PICK_REQUEST_CODE
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.max
import kotlin.math.min
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.model.AspectRatio
import android.graphics.Point
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions


class MainActivity : AppCompatActivity() {

    // Initialize clockTextView
    private lateinit var clockTextView: TextView


    private lateinit var gestureDetector: GestureDetector

    // Initialize scaleGestureDetector for pinch-to-resize
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var scaleFactor = 1f

    // Initialize variables for dragging the clock
    private var dX = 0f
    private var dY = 0f

    // Initialize optionsIcon and fadeHandler for fading in/out the options icon
    private lateinit var optionsIcon: ImageView
    private val fadeHandler = Handler(Looper.getMainLooper())
    private var statusBarHeight = 0
    private val fadeOutHandler = Handler(Looper.getMainLooper())

    // Initialize notificationBarHandler for fading in/out the notification bar
    private val notificationBarHandler = Handler(Looper.getMainLooper())

    private lateinit var saveBackgroundImageUri: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    companion object {
        private const val REQUEST_IMAGE_PICK = 1000
        private const val BACKGROUND_IMAGE_URI_KEY = "background_image_uri"
    }

    private lateinit var sharedPreferences: SharedPreferences


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("decorative_clock_preferences", MODE_PRIVATE)

        // Initialize optionsIcon
        optionsIcon = findViewById(R.id.optionsIcon)
        optionsIcon.y = statusBarHeight.toFloat()

        // Get the status bar height for use in the fade in/out behavior
        statusBarHeight = getStatusBarHeight()

        // Listen for options icon click
        optionsIcon.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // Listen for options icon long click
        val fadeOutRunnable = Runnable {
            fadeOutViews()
        }

        // Add this line to cancel the fade out timer when the notification bar is tapped
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
        val contentView = findViewById<View>(android.R.id.content)
        val controller = WindowCompat.getInsetsController(window, contentView)
        controller?.addOnControllableInsetsChangedListener { _, mask ->
            if (mask and WindowInsetsCompat.Type.statusBars() == 0) {
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

        // Gesture detector for rotating the clock
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                rotateClockTextView()
                return true
            }
        })

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
            gestureDetector.onTouchEvent(event)

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

    private fun rotateClockTextView() {
        clockTextView.animate()
            .rotationBy(90f)
            .setDuration(500)
            .start()
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
        val contentView = findViewById<View>(android.R.id.content)
        val controller = WindowCompat.getInsetsController(window, contentView)
        controller?.hide(WindowInsetsCompat.Type.statusBars())
    }


    private fun showSystemUI() {
        val contentView = findViewById<View>(android.R.id.content)
        val controller = WindowCompat.getInsetsController(window, contentView)
        controller?.show(WindowInsetsCompat.Type.statusBars())
    }



    // Add this method to save the clock position and scale factor when the app is paused?
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

    // Add this method to save the background image URI
    override fun onResume() {
        super.onResume()
        val backgroundImageUriString = sharedPreferences.getString(BACKGROUND_IMAGE_URI_KEY, null)
        if (backgroundImageUriString != null) {
            val backgroundImageUri = Uri.parse(backgroundImageUriString)
            setBackgroundImage(backgroundImageUri)
        }
    }

    private fun setBackgroundImage(resultUri: Uri) {
        val backgroundImageView = findViewById<ImageView>(R.id.background_image)

        val requestOptions = RequestOptions()
            .centerCrop()
            .error(com.bumptech.glide.R.drawable.abc_control_background_material) // Replace with your own error image


        Glide.with(this)
            .load(resultUri)
            .apply(requestOptions)
            .transition(DrawableTransitionOptions.withCrossFade())
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(backgroundImageView)
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val backgroundImageUriString = sharedPreferences.getString(BACKGROUND_IMAGE_URI_KEY, null)
        if (backgroundImageUriString != null) {
            val backgroundImageUri = Uri.parse(backgroundImageUriString)
            setBackgroundImage(backgroundImageUri)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            val selectedImageUri = data.data
            if (selectedImageUri != null) {
                val display = windowManager.defaultDisplay
                val size = Point()
                display.getSize(size)
                val screenWidth = size.x
                val screenHeight = size.y

                val uCrop = UCrop.of(selectedImageUri, Uri.fromFile(File(cacheDir, "background_image.jpg")))
                    .withAspectRatio(screenWidth.toFloat(), screenHeight.toFloat())
                uCrop.start(this)
            }
        } else if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            val resultUri = UCrop.getOutput(data!!)
            if (resultUri != null) {
                setBackgroundImage(resultUri)
                editor.putString("background_image_uri", resultUri.toString()).apply()
            }
        }
    }


}