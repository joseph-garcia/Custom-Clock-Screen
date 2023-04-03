package com.example.decorativeclock


import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.max
import kotlin.math.min
import com.yalantis.ucrop.UCrop
import android.graphics.Point
import android.graphics.Typeface
import android.os.*
import android.util.Log
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import androidx.lifecycle.Observer


class MainActivity : AppCompatActivity() {

    // Initialize clockTextView
    private lateinit var clockTextView: TextView
    private var timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            updateTime()
            clockTextView.postDelayed(this, 1000)
        }
    }

    private lateinit var gestureDetector: GestureDetector

    // Initialize scaleGestureDetector for pinch-to-resize
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var scaleFactor = 1f
    private var isRotating = false

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

    companion object {
        private const val REQUEST_IMAGE_PICK = 1000
        private const val BACKGROUND_IMAGE_URI_KEY = "background_image_uri"
    }

    private lateinit var sharedPreferences: SharedPreferences

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateTime()
            }

            override fun onFinish() {
            }
        }
        timer.start()

        sharedPreferences = getSharedPreferences("decorative_clock_preferences", MODE_PRIVATE)
        val isMilitaryTime = sharedPreferences.getBoolean("military_time", false)

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
        setupFadeInFadeOutBehavior()

        clockTextView = findViewById(R.id.clockTextView)
        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())

        val handler = Handler(Looper.getMainLooper())

        val updateClockRunnable = object : Runnable {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun run() {
                updateClock(isMilitaryTime)
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
                    clockTextView.rotation = clockData.fourth
                } else {
                    resetClockPosition()
                }
            }
        })

        clockTextView.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

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
                    view.x = newX
                    view.y = newY
                }


                MotionEvent.ACTION_UP -> {
                    saveClockPosition(view.x, view.y, scaleFactor)
                }
            }
            true
        }

        // Run resetClockPosition() on long press anywhere outside of the clock
        val mainLayout = findViewById<RelativeLayout>(R.id.root_layout)
        mainLayout.setOnLongClickListener {
            resetClockPosition()
            true
        }



        loadClockSettings()
    }

    fun updateClockTimeFormat()  {
        val sharedPreferences = getSharedPreferences("decorative_clock_preferences", MODE_PRIVATE)
        val isMilitaryTime = sharedPreferences.getBoolean("is_military_time", false)

        if (isMilitaryTime) {
            timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        } else {
            timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        }

        updateTime()
    }

    private fun updateTime() {
        val sharedPreferences = getSharedPreferences("decorative_clock_preferences", MODE_PRIVATE)
        val isMilitaryTime = sharedPreferences.getBoolean("is_military_time", false)

        val currentTime = Calendar.getInstance().time
        val timeFormat = if (isMilitaryTime) {
            SimpleDateFormat("HH:mm", Locale.getDefault())
        } else {
            SimpleDateFormat("hh:mm a", Locale.getDefault())
        }
        val formattedTime = timeFormat.format(currentTime)

        clockTextView.text = formattedTime
    }



    @SuppressLint("ClickableViewAccessibility")
    private fun setupFadeInFadeOutBehavior() {
        // Set up the fade in/out behavior
        val fadeOutRunnable = Runnable { fadeOutViews() }

        // Add this line to start the fade out timer when the app starts
        fadeHandler.postDelayed(fadeOutRunnable, 5000)

        // Cancels the fade out timer when the app is tapped
        val frameLayout = findViewById<FrameLayout>(R.id.frameLayout)
        frameLayout.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                fadeInViews()

                // Cancel any pending fade-out actions
                fadeOutHandler.removeCallbacks(fadeOutRunnable)
                notificationBarHandler.removeCallbacksAndMessages(null)

                // Schedule a new fade-out action
                fadeOutHandler.postDelayed(fadeOutRunnable, 5000)
            }
            false
        }

        // Set up the fade in/out behavior for the status bar
        val contentView = findViewById<View>(android.R.id.content)
        val controller = WindowCompat.getInsetsController(
            window,
            contentView
        ) // gets the insets controller which is used to control the visibility of the status bar
        // this listener is called when the status bar visibility changes
        controller.addOnControllableInsetsChangedListener { _, mask ->
            if (mask and WindowInsetsCompat.Type.statusBars() == 0) {
                fadeInViews()
            } else {
                fadeOutViews()
            }
        }
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
        controller.hide(WindowInsetsCompat.Type.statusBars())
        controller.hide(WindowInsetsCompat.Type.navigationBars())
    }

    private fun showSystemUI() {
        val contentView = findViewById<View>(android.R.id.content)
        val controller = WindowCompat.getInsetsController(window, contentView)
        controller.show(WindowInsetsCompat.Type.statusBars())
        controller.show(WindowInsetsCompat.Type.navigationBars())
    }

    // this method is called when the app is paused and it saves the clock position and scale factor
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
    fun resetClockPosition() {
        Log.d("josephDebug", "resetClockPosition running")
        // Reset the clock position
        clockTextView.x = (resources.displayMetrics.widthPixels - clockTextView.width) / 2f
        clockTextView.y = (resources.displayMetrics.heightPixels - clockTextView.height) / 2f

        // Reset the clock rotation
        clockTextView.rotation = 0f

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
        editor.putFloat("${orientationKey}_rotation", clockTextView.rotation)
        editor.apply()
    }

    private fun loadClockPosition(): Quadruple<Float, Float, Float, Float> {
        val sharedPreferences = getSharedPreferences("clock_data", MODE_PRIVATE)
        val orientation = resources.configuration.orientation
        val orientationKey = if (orientation == Configuration.ORIENTATION_PORTRAIT) "portrait" else "landscape"

        val x = sharedPreferences.getFloat("${orientationKey}_x", -1f)
        val y = sharedPreferences.getFloat("${orientationKey}_y", -1f)
        val scaleFactor = sharedPreferences.getFloat("${orientationKey}_scaleFactor", 1f)
        val rotation = sharedPreferences.getFloat("${orientationKey}_rotation", 0f) // Add this line
        return Quadruple(x, y, scaleFactor, rotation)
    }
    private fun rotateClockTextView() {
        if (!isRotating) {
            isRotating = true
            clockTextView.animate()
                .rotationBy(90f)
                .setDuration(500)
                .withEndAction {
                    isRotating = false
                    saveClockPosition(clockTextView.x, clockTextView.y, scaleFactor)
                }
                .start()
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun updateClock(isMilitaryTime: Boolean) {
        if (!::clockTextView.isInitialized) return

        val currentTime = LocalDateTime.now()
        val formattedTime = if (isMilitaryTime) {
            currentTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        } else {
            currentTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
        }
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
        updateClockTimeFormat()
        val backgroundImageUriString = sharedPreferences.getString(BACKGROUND_IMAGE_URI_KEY, null)
        if (backgroundImageUriString != null) {
            val backgroundImageUri = Uri.fromFile(File(backgroundImageUriString))
            setBackgroundImage(backgroundImageUri)
        }

        val clockFont = sharedPreferences.getString("clock_font", "sans-serif")
        val clockTextView = findViewById<TextView>(R.id.clockTextView)
        clockTextView.typeface = Typeface.create(clockFont, Typeface.NORMAL)

        loadClockSettings()
        clockTextView.post(updateTimeRunnable)
    }

    override fun onPause() {
        super.onPause()
        clockTextView.removeCallbacks(updateTimeRunnable)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Handles the result of the image picker, if the user selected an image, it will be cropped to the screen size
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
        // Handles the result of the image cropper, if the user cropped the image, it will be saved to the app's internal storage
        } else if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            val resultUri = UCrop.getOutput(data!!)
            if (resultUri != null) {
                val sharedPreferences = getSharedPreferences("decorative_clock_preferences", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()

                val inputStream = contentResolver.openInputStream(resultUri)
                val backgroundFile = File(filesDir, "background_image")
                val outputStream = FileOutputStream(backgroundFile)

                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()

                editor.putString("background_image_uri", backgroundFile.absolutePath)
                editor.apply()

                setBackgroundImage(resultUri)
            }
        }

    }

    private fun loadClockSettings() {
        val sharedPreferences = getSharedPreferences("decorative_clock_preferences", Context.MODE_PRIVATE)
        val selectedFont = sharedPreferences.getString("clock_font", "sans-serif")
        val textColor = sharedPreferences.getInt("clock_text_color", R.color.icon_color)
        Log.d("josephDebug", "default color: ${R.color.icon_color}")

        if (selectedFont == "pressstart2p_regular") {
            val customTypeface = ResourcesCompat.getFont(this, R.font.pressstart2p_regular)
            clockTextView.typeface = customTypeface
        } else {
            clockTextView.typeface = Typeface.create(selectedFont, Typeface.NORMAL)
        }

        clockTextView.setTextColor(textColor)
    }




}



data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
