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
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import androidx.lifecycle.Observer
import com.google.android.material.button.MaterialButton


class MainActivity : AppCompatActivity() {

    // Initialize clockTextView
    private lateinit var clockTextView: ResizableClockTextView
    private var timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    private var isRotating = false

    // Initialize optionsIcon and fadeHandler for fading in/out the options icon
    private lateinit var optionsIcon: ImageView
    private lateinit var helpIcon: ImageView
    private val fadeHandler = Handler(Looper.getMainLooper())
    private var statusBarHeight = 0
    private val fadeOutHandler = Handler(Looper.getMainLooper())

    private lateinit var gestureDetector: GestureDetector

    // Initialize notificationBarHandler for fading in/out the notification bar
    private val notificationBarHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val REQUEST_IMAGE_PICK = 1000
        const val BACKGROUND_IMAGE_URI_KEY = "background_image_uri"
    }

    private lateinit var sharedPreferences: SharedPreferences

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Keep the screen on while this activity is running
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // initialize gesture detector to override later
        val doubleTapGestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                Log.d("josephDebug", "DOUBLE TAPPING")
                rotateClockTextView()
                return true
            }
        })

        sharedPreferences = getSharedPreferences("decorative_clock_preferences", MODE_PRIVATE)
        val isMilitaryTime = sharedPreferences.getBoolean("military_time", false)

        // Initialize optionsIcon and helpIcon
        optionsIcon = findViewById(R.id.optionsIcon)
        optionsIcon.y = statusBarHeight.toFloat()
        helpIcon = findViewById(R.id.helpIcon)
        helpIcon.y = statusBarHeight.toFloat()

        // Get the status bar height for use in the fade in/out behavior
        statusBarHeight = getStatusBarHeight()

        // Listen for options icon click
        optionsIcon.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        setupFadeInFadeOutBehavior()

        clockTextView = findViewById(R.id.clockTextView)

        // Gesture detector for rotating the clock
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                Log.d("josephDebug", "DOUBLE TAPPING")
                rotateClockTextView()
                return true
            }
        })

        val helpIcon = findViewById<ImageView>(R.id.helpIcon)
        helpIcon.setOnClickListener {
            showHelpOverlay()
        }


        // Load and set the saved clock data (position and scale factor)
        //val clockData = loadClockPosition()

        // Run resetClockPosition() on long press anywhere outside of the clock
        val mainLayout = findViewById<RelativeLayout>(R.id.root_layout)
        mainLayout.setOnLongClickListener {
            resetClockPosition()
            true
        }
        updateDropShadow()
        loadClockSettings()
    }

    private fun showHelpOverlay() {
        val helpOverlay = LayoutInflater.from(this).inflate(R.layout.help_overlay, null)
        val closeButton = helpOverlay.findViewById<MaterialButton>(R.id.close_help_button)
        closeButton.setOnClickListener {
            (helpOverlay.parent as? ViewGroup)?.removeView(helpOverlay)
        }

        val rootView = findViewById<ViewGroup>(android.R.id.content)
        rootView.addView(helpOverlay)
    }



    @SuppressLint("ClickableViewAccessibility")
    private fun setupFadeInFadeOutBehavior() {
        Log.d("josephDebug", "setting fade behavior")
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
        helpIcon.animate().alpha(0f).duration = 300

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

        helpIcon.animate()
            .alpha(1f)
            .setDuration(300)
            .withEndAction {
                // Reset the help icon's position
                helpIcon.y = statusBarHeight.toFloat()
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putFloat("x", clockTextView.x)
        outState.putFloat("y", clockTextView.y)
        outState.putInt("clock_left_margin", sharedPreferences.getInt("clock_left_margin", 0))
        outState.putInt("clock_top_margin", sharedPreferences.getInt("clock_top_margin", 0))
        outState.putFloat("clock_scale_x", sharedPreferences.getFloat("clock_scale_x", 1f))
        outState.putFloat("clock_scale_y", sharedPreferences.getFloat("clock_scale_y", 1f))
        outState.putFloat("clock_rotation", sharedPreferences.getFloat("clock_rotation", 0f))
    }

    fun resetClockPosition() {
        Log.d("josephDebug", "resetClockPosition running")

        // Reset the clock rotation
        clockTextView.rotation = 0f

        // Reset clock text size
        clockTextView.textSize = 50f

        // Reset clock size
        clockTextView.scaleX = 1f
        clockTextView.scaleY = 1f

        // Post a Runnable to the view's queue to make sure it runs after the view has been laid out
        clockTextView.post {
            val parent = clockTextView.parent as FrameLayout
            val leftMargin = (parent.width - clockTextView.width) / 2
            val topMargin = (parent.height - clockTextView.height) / 2

            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.leftMargin = leftMargin
            layoutParams.topMargin = topMargin
            clockTextView.layoutParams = layoutParams

            // Update shared preferences with the new values
            sharedPreferences.edit {
                putInt("clock_left_margin", layoutParams.leftMargin)
                putInt("clock_top_margin", layoutParams.topMargin)
                putFloat("clock_scale_x", clockTextView.scaleX)
                putFloat("clock_scale_y", clockTextView.scaleY)
                putFloat("clock_rotation", clockTextView.rotation)
                apply()
            }
        }
    }



//    private fun saveClockPosition(x: Float, y: Float) {
//        val sharedPreferences = getSharedPreferences("clock_data", MODE_PRIVATE)
//        val editor = sharedPreferences.edit()
//        val orientation = resources.configuration.orientation
//        val orientationKey = if (orientation == Configuration.ORIENTATION_PORTRAIT) "portrait" else "landscape"
//
//        editor.putFloat("${orientationKey}_x", x)
//        editor.putFloat("${orientationKey}_y", y)
//        //editor.putFloat("${orientationKey}_scaleFactor", scaleFactor)
//        editor.putFloat("${orientationKey}_rotation", clockTextView.rotation)
//        editor.apply()
//        Log.d("josephDebug", "saveClockPosition running")
//    }

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
                    //saveClockPosition(clockTextView.x, clockTextView.y)
                }
                .start()
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
//    fun updateClock(isMilitaryTime: Boolean) {
//        if (!::clockTextView.isInitialized) return
//
//        val currentTime = LocalDateTime.now()
//        val formattedTime = if (isMilitaryTime) {
//            currentTime.format(DateTimeFormatter.ofPattern("HH:mm"))
//        } else {
//            currentTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
//        }
//        clockTextView.text = formattedTime
//    }
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
        Log.d("josephDebug", "onResume running")
        val backgroundImageUriString = sharedPreferences.getString(BACKGROUND_IMAGE_URI_KEY, null)
        if (backgroundImageUriString != null) {
            val backgroundImageUri = Uri.fromFile(File(backgroundImageUriString))
            setBackgroundImage(backgroundImageUri)
        }

        val clockFont = sharedPreferences.getString("clock_font", "sans-serif")
        val clockTextView: ResizableClockTextView = findViewById(R.id.clockTextView)
        clockTextView.typeface = Typeface.create(clockFont, Typeface.NORMAL)

        updateDropShadow()
        loadClockSettings()


        clockTextView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val hasPositionSaved = sharedPreferences.contains("clock_left_margin") && sharedPreferences.contains("clock_top_margin")

                val leftMargin = if (hasPositionSaved) {
                    sharedPreferences.getInt("clock_left_margin", 0)
                } else {
                    (resources.displayMetrics.widthPixels / 2) - (clockTextView.measuredWidth / 2)
                }

                val topMargin = if (hasPositionSaved) {
                    sharedPreferences.getInt("clock_top_margin", 0)
                } else {
                    (resources.displayMetrics.heightPixels / 2) - (clockTextView.measuredHeight / 2)
                }

                val scaleX = sharedPreferences.getFloat("clock_scale_x", 1f)
                val scaleY = sharedPreferences.getFloat("clock_scale_y", 1f)
                val rotation = sharedPreferences.getFloat("clock_rotation", 0f)

                val layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams.leftMargin = leftMargin
                layoutParams.topMargin = topMargin

                clockTextView.layoutParams = layoutParams
                clockTextView.scaleX = scaleX
                clockTextView.scaleY = scaleY
                clockTextView.rotation = rotation

                // Remove the listener to avoid multiple callbacks
                clockTextView.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    override fun onPause() {
        super.onPause()
        val clockTextView: ResizableClockTextView = findViewById(R.id.clockTextView)
        Log.d("josephDebug", "onPause running")
        val layoutParams = clockTextView.layoutParams as FrameLayout.LayoutParams
        sharedPreferences.edit {
            putInt("clock_left_margin", layoutParams.leftMargin)
            putInt("clock_top_margin", layoutParams.topMargin)
            putFloat("clock_scale_x", clockTextView.scaleX)
            putFloat("clock_scale_y", clockTextView.scaleY)
            putFloat("clock_rotation", clockTextView.rotation)
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
        val textColor = sharedPreferences.getInt("clock_text_color", ContextCompat.getColor(this, R.color.icon_color))
        val defaultFonts = listOf("sans-serif", "serif", "monospace", "cursive", "fantasy")

        if (defaultFonts.contains(selectedFont)) {
            clockTextView.typeface = Typeface.create(selectedFont, Typeface.NORMAL)
        } else {
            val fontResourceId = getFontResourceId(selectedFont)
            if (fontResourceId != null) {
                val customTypeface = ResourcesCompat.getFont(this, fontResourceId)
                clockTextView.typeface = customTypeface
            }
        }

        clockTextView.setTextColor(textColor)

        // get isColonEnabled from SharedPreferences
        val isColonEnabled = sharedPreferences.getBoolean("is_colon_enabled", true)
        val colonFormatString = if (isColonEnabled) {
            ":"
        } else {
            " "
        }

        // get is_24_hour_format from SharedPreferences
        val is24HourFormat = sharedPreferences.getBoolean("is_24_hour_format", false)
        // set the ClockText based on the value of is_24_hour_format with clockTextView.setFormat12Hour() or clockTextView.setFormat24Hour()
        if (is24HourFormat) {
            clockTextView.format12Hour = null
            clockTextView.format24Hour = "HH" + colonFormatString + "mm"
        } else {
            clockTextView.format24Hour = null
            clockTextView.format12Hour = "hh" + colonFormatString + "mm a"
        }



    }


    private fun updateDropShadow() {
        val sharedPreferences = getSharedPreferences("decorative_clock_preferences", MODE_PRIVATE)
        val isDropShadowEnabled = sharedPreferences.getBoolean("is_drop_shadow_enabled", true)

        if (isDropShadowEnabled) {
            clockTextView.setShadowLayer(6f, 2f, 2f, Color.BLACK)
        } else {
            clockTextView.setShadowLayer(0f, 0f, 0f, Color.BLACK)
        }
    }



}



data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
