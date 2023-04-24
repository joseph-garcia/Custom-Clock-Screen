package com.example.decorativeclock
import FontArrayAdapter
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Point
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.NestedScrollView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.skydoves.colorpickerview.preference.ColorPickerPreferenceManager
import com.skydoves.colorpickerview.sliders.AlphaSlideBar
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar
import com.yalantis.ucrop.UCrop
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
class SettingsActivity : AppCompatActivity() {

    private lateinit var fontAdapter: FontArrayAdapter
    private lateinit var fontAutocomplete: AutoCompleteTextView

    private lateinit var currentClockFont: String
    private lateinit var previewTextView: TextView
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var colorPickerView: ColorPickerView
    private var isColorPickerVisible = false

    private lateinit var toggleMilitaryTimeSwitch: SwitchMaterial
    private lateinit var toggleDropShadowSwitch: SwitchMaterial
    private val fontMap = mapOf(
        "Default" to "sans-serif",
        "Abril Fatface" to "abrilfatface",
        "Audiowide" to "audiowide",
        "Bebas Neue" to "bebasneue",
        "Big Shoulders Display" to "bigshouldersdisplay",
        "Cursive" to "cursive",
        "Fantasy" to "fantasy",
        "Gajraj One" to "gajraj_one",
        "Gruppo" to "gruppo",
        "Lilita One" to "lilita_one",
        "Monospace" to "monospace",
        "Poiret One" to "poiret_one",
        "Press Start" to "pressstart_regular",
        "Roboto" to "roboto",
        "Serif" to "serif",
        "Tilt Prism" to "tiltprism"
    )

    private val customFontResourceMap = mapOf(
        "pressstart_regular" to R.font.pressstart_regular,
        "abrilfatface" to R.font.abrilfatface,
        "audiowide" to R.font.audiowide,
        "bebasneue" to R.font.bebas_neue,
        "bigshouldersdisplay" to R.font.bigshouldersdisplay,
        "gajraj_one" to R.font.gajraj_one,
        "gruppo" to R.font.gruppo,
        "lilita_one" to R.font.lilita_one,
        "poiret_one" to R.font.poiret_one,
        "roboto" to R.font.roboto,
        "tiltprism" to R.font.tiltprism
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Retrieve data from shared preferences
        sharedPreferences = getSharedPreferences("decorative_clock_preferences", Context.MODE_PRIVATE)
        currentClockFont = sharedPreferences.getString("clock_font", "sans-serif") ?: "sans-serif"
        val savedColor = sharedPreferences.getInt("clock_text_color", Color.WHITE)

        previewTextView = findViewById(R.id.preview_text_view)
        previewTextView.setTextColor(savedColor)

        fontAutocomplete = findViewById(R.id.font_autocomplete)
        fontAdapter = FontArrayAdapter(this, R.layout.dropdown_menu_popup_item, fontMap.keys.toList(), fontMap, customFontResourceMap)
        fontAutocomplete.setAdapter(fontAdapter)

        // On Click Listeners
        val cancelIcon = findViewById<ImageView>(R.id.cancel_icon)
        cancelIcon.setOnClickListener {
            finish()
        }
        val changeBackgroundButton = findViewById<Button>(R.id.changeBackgroundButton)
        changeBackgroundButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        val toggleColorPickerButton = findViewById<MaterialButton>(R.id.toggle_color_picker_button)
        toggleColorPickerButton.setOnClickListener {
            toggleColorPickerVisibility()
        }

        setupDropShadow()
        setupMilitarySwitch()
        setupColorPicker()
        setupFontDropDown()
        setCustomClockFormat()

        // set background image
        val backgroundImageUriString = sharedPreferences.getString(MainActivity.BACKGROUND_IMAGE_URI_KEY, null)
        if (backgroundImageUriString != null) {
            val backgroundImageUri = Uri.fromFile(File(backgroundImageUriString))
            setBackgroundImage(backgroundImageUri)
        }
        // Restore the font dropdown menu state if there's a saved instance state
        if (savedInstanceState != null) {
            val selectedFontPosition = savedInstanceState.getInt("selected_font_position")
            if (selectedFontPosition != -1) {
                fontAutocomplete.setText(fontAdapter.getItem(selectedFontPosition), false)
            }
        }
    }
    private fun setupFontDropDown() {
        val fontAutocomplete = findViewById<AutoCompleteTextView>(R.id.font_autocomplete)
        fontAutocomplete.setAdapter(fontAdapter)
        val currentClockFont = sharedPreferences.getString("clock_font", "sans-serif")
        val currentFontPosition = fontMap.entries.indexOfFirst { it.value == currentClockFont }
        // Set the selected item in the AutoCompleteTextView to the current font
        if (currentFontPosition != -1) {
            fontAutocomplete.setText(fontAdapter.getItem(currentFontPosition), false)
            updatePreviewFont(fontMap[fontAdapter.getItem(currentFontPosition)])
        }
        // Set an ItemClickListener for fontAutocomplete
        fontAutocomplete.setOnItemClickListener { parent, _, position, _ ->
            val selectedFont = fontMap[parent.getItemAtPosition(position).toString()]
            updatePreviewFont(selectedFont)
            val defaultFonts = listOf("sans-serif", "serif", "monospace", "cursive", "fantasy")
            if (selectedFont != null) {
                if (defaultFonts.contains(selectedFont)) {
                    previewTextView.typeface = Typeface.create(selectedFont, Typeface.NORMAL)
                } else {
                    val fontResourceId = getFontResourceId(selectedFont)
                    if (fontResourceId != null) {
                        val customTypeface = ResourcesCompat.getFont(this@SettingsActivity, fontResourceId)
                        previewTextView.typeface = customTypeface
                    }
                }
            }
        }
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun setupColorPicker() {
        colorPickerView = findViewById(R.id.color_picker_view)
        val currentClockTextColor = sharedPreferences.getInt("clock_text_color", Color.WHITE)
        colorPickerView = findViewById(R.id.color_picker_view)
        colorPickerView.setInitialColor(currentClockTextColor)
        val colorPickerView = findViewById<ColorPickerView>(R.id.color_picker_view)
        val scrollView = findViewById<NestedScrollView>(R.id.scrollView)
        // Disable scrolling on the color picker view when the user is interacting with it
        colorPickerView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    scrollView.requestDisallowInterceptTouchEvent(true)
                    false
                }
                MotionEvent.ACTION_UP -> {
                    scrollView.requestDisallowInterceptTouchEvent(false)
                    false
                }
                else -> false
            }
        }
        // Disable scrolling on the brightness slider when the user is interacting with it
        val brightnessSlideBar = findViewById<BrightnessSlideBar>(R.id.brightnessSlideBar)
        brightnessSlideBar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    scrollView.requestDisallowInterceptTouchEvent(true)
                    false
                }
                MotionEvent.ACTION_UP -> {
                    scrollView.requestDisallowInterceptTouchEvent(false)
                    false
                }
                else -> false
            }
        }
        // Disable scrolling on the alpha slider when the user is interacting with it
        val alphaSlideBar = findViewById<AlphaSlideBar>(R.id.alphaSlideBar)
        alphaSlideBar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    scrollView.requestDisallowInterceptTouchEvent(true)
                    false
                }
                MotionEvent.ACTION_UP -> {
                    scrollView.requestDisallowInterceptTouchEvent(false)
                    false
                }
                else -> false
            }
        }
        colorPickerView.setColorListener(object : ColorEnvelopeListener {
            override fun onColorSelected(envelope: ColorEnvelope, fromUser: Boolean) {
                if (isColorPickerVisible) {
                    previewTextView.setTextColor(envelope.color)
                }
            }
        })
        // Attach alpha and brightness sliders to the color picker view
        colorPickerView.post {
            colorPickerView.attachAlphaSlider(findViewById(R.id.alphaSlideBar))
            colorPickerView.attachBrightnessSlider(findViewById(R.id.brightnessSlideBar))
        }
        // Assign the color picker view to the previewTextView color
        val currentColor = previewTextView.currentTextColor
        colorPickerView.setInitialColor(currentColor)
    }
    private fun setupMilitarySwitch() {
        toggleMilitaryTimeSwitch = findViewById(R.id.toggleMilitaryTimeSwitch)
        val isMilitaryTime = sharedPreferences.getBoolean("is_24_hour_format", false)
        toggleMilitaryTimeSwitch.isChecked = isMilitaryTime
        toggleMilitaryTimeSwitch.setOnCheckedChangeListener { _, _ ->
            setCustomClockFormat()
        }
        if (isMilitaryTime) {
            previewTextView.text = "13:34"
        } else {
            previewTextView.text = "01:34 PM"
        }
    }
    private fun setupDropShadow() {
        toggleDropShadowSwitch = findViewById(R.id.toggleDropShadowSwitch)
        val isDropShadowEnabled = sharedPreferences.getBoolean("is_drop_shadow_enabled", true)
        toggleDropShadowSwitch.isChecked = isDropShadowEnabled
        if (isDropShadowEnabled) {
            addDropShadow(previewTextView)
        } else {
            removeDropShadow(previewTextView)
        }
        var newIsDropShadowEnabled = isDropShadowEnabled
        toggleDropShadowSwitch.setOnCheckedChangeListener { _, isChecked ->
            newIsDropShadowEnabled = isChecked
            if (isChecked) {
                addDropShadow(previewTextView)
            } else {
                removeDropShadow(previewTextView)
            }
        }
        val saveFontButton: Button = findViewById(R.id.save_font_button)
        saveFontButton.setOnClickListener {
            val sharedPreferences = getSharedPreferences("decorative_clock_preferences", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            val selectedFont = fontMap[fontAutocomplete.text.toString()]
            val selectedColor = colorPickerView.colorEnvelope.color
            val is24HourFormat = toggleMilitaryTimeSwitch.isChecked
            //val isColonEnabled = toggleColonSwitch.isChecked

            if (selectedFont != null) {
                editor.putString("clock_font", selectedFont)
                editor.putInt("clock_text_color", selectedColor)
                editor.putBoolean("is_drop_shadow_enabled", newIsDropShadowEnabled)
                editor.putBoolean("is_24_hour_format", is24HourFormat)
                //editor.putBoolean("is_colon_enabled", isColonEnabled)
                editor.apply()
            }
            ColorPickerPreferenceManager.getInstance(this).saveColorPickerData(colorPickerView)
            finish()
        }
    }
    private fun toggleColorPickerVisibility() {
        val colorPickerContainer = findViewById<LinearLayout>(R.id.color_picker_container)
        if (colorPickerContainer.visibility == View.VISIBLE) {
            isColorPickerVisible = false
            animateColorPickerContainerVisibility(View.GONE)
        } else {
            isColorPickerVisible = true
            colorPickerView.setInitialColor(previewTextView.currentTextColor)
            animateColorPickerContainerVisibility(View.VISIBLE)
        }
    }
    private fun updatePreviewFont(selectedFont: String?) {
        val defaultFonts = listOf("sans-serif", "serif", "monospace", "cursive", "fantasy")
        if (selectedFont != null) {
            if (defaultFonts.contains(selectedFont)) {
                previewTextView.typeface = Typeface.create(selectedFont, Typeface.NORMAL)
            } else {
                val fontResourceId = customFontResourceMap[selectedFont]
                if (fontResourceId != null) {
                    val customTypeface = ResourcesCompat.getFont(this@SettingsActivity, fontResourceId)
                    previewTextView.typeface = customTypeface
                }
            }
        }
    }
    private fun setBackgroundImage(resultUri: Uri) {
        val backgroundImageView = findViewById<ImageView>(R.id.background_preview)
        val requestOptions = RequestOptions()
            .centerCrop()
            .error(com.bumptech.glide.R.drawable.abc_control_background_material)
        Glide.with(this)
            .load(resultUri)
            .apply(requestOptions)
            .transition(DrawableTransitionOptions.withCrossFade())
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(backgroundImageView)
    }
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            if (isGifFile(this, it)) {
                saveCroppedImageUri(it)
                setBackgroundImage(it)
            } else {
                val size = getScreenSize()
                val screenWidth = size.x
                val screenHeight = size.y
                val uCrop = UCrop.of(it, Uri.fromFile(File(cacheDir, "background_image.jpg")))
                    .withAspectRatio(screenWidth.toFloat(), screenHeight.toFloat())
                cropImageLauncher.launch(uCrop.getIntent(this))
            }
        }
    }
    private fun getScreenSize(): Point {
        val size = Point()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            size.x = windowMetrics.bounds.width() - insets.left - insets.right
            size.y = windowMetrics.bounds.height() - insets.top - insets.bottom
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getSize(size)
        }
        return size
    }
    private val cropImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            if (resultUri != null) {
                saveCroppedImageUri(resultUri)
                setBackgroundImage(resultUri)
            }
        }
    }
    private fun saveCroppedImageUri(uri: Uri) {
        val sharedPreferences = getSharedPreferences("decorative_clock_preferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val inputStream = contentResolver.openInputStream(uri)
        val backgroundFile = File(filesDir, "background_image")
        val outputStream = FileOutputStream(backgroundFile)

        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()

        editor.putString("background_image_uri", backgroundFile.absolutePath)
        editor.apply()
    }
    private fun isGifFile(context: Context, uri: Uri): Boolean {
        val mimeType: String? = context.contentResolver.getType(uri)
        return mimeType?.equals("image/gif", ignoreCase = true) == true
    }
    private fun addDropShadow(textView: TextView) {
        textView.setShadowLayer(6f, 2f, 2f, Color.BLACK)
    }
    private fun removeDropShadow(textView: TextView) {
        textView.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
    }
    private fun setCustomClockFormat() {
        val isMilitaryTime = toggleMilitaryTimeSwitch.isChecked
        val hourFormat = if (isMilitaryTime) "HH" else "h"
        val minuteFormat = "mm"
        val amPmFormat = if (isMilitaryTime) "" else " a"
        val formatString = "$hourFormat:$minuteFormat$amPmFormat"
        val sdf = SimpleDateFormat(formatString, Locale.getDefault())
        val currentTime = Calendar.getInstance().time
        previewTextView.text = sdf.format(currentTime)
    }
    private fun animateColorPickerContainerVisibility(targetVisibility: Int) {
        val colorPickerContainer = findViewById<LinearLayout>(R.id.color_picker_container)
        if (colorPickerContainer.visibility != targetVisibility) {
            val spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            colorPickerContainer.measure(spec, spec)
            val endHeight = colorPickerContainer.measuredHeight

            if (endHeight != 0 || colorPickerContainer.visibility == View.GONE) {
                val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
                valueAnimator.duration = 300
                valueAnimator.addUpdateListener { animation ->
                    val animatedValue = animation.animatedValue as Float
                    val newHeight = if (targetVisibility == View.VISIBLE) {
                        (animatedValue * endHeight).toInt()
                    } else {
                        ((1 - animatedValue) * endHeight).toInt()
                    }
                    colorPickerContainer.layoutParams.height = newHeight
                    colorPickerContainer.requestLayout()
                }
                valueAnimator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        if (targetVisibility == View.VISIBLE) {
                            colorPickerContainer.visibility = View.VISIBLE
                            colorPickerView.setInitialColor(previewTextView.currentTextColor)
                        }
                    }
                    override fun onAnimationEnd(animation: Animator) {
                        if (targetVisibility == View.GONE) {
                            colorPickerContainer.visibility = View.GONE
                        }
                    }
                })
                valueAnimator.start()
            }
        }
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val colorPickerContainer = findViewById<LinearLayout>(R.id.color_picker_container)
            colorPickerContainer.visibility = View.GONE
        }
    }
    override fun onResume() {
        super.onResume()

        // Reinitialize the fontMap with the new context
        val newFontMap = fontMap

        // Update the fontAdapter with the new fontMap keys
        fontAdapter = FontArrayAdapter(this, R.layout.dropdown_menu_popup_item, fontMap.keys.toList(), fontMap, customFontResourceMap)
        fontAutocomplete.setAdapter(fontAdapter)

        // Set the selected item in the AutoCompleteTextView to the current font
        val currentFontPosition = newFontMap.entries.indexOfFirst { it.value == currentClockFont }
        if (currentFontPosition != -1) {
            fontAutocomplete.setText(fontAdapter.getItem(currentFontPosition), false)
        }
    }
}