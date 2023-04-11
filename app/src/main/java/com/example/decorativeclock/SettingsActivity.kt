package com.example.decorativeclock

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Point
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.NestedScrollView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputLayout
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

    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private lateinit var previewTextView: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var colorPickerView: ColorPickerView
    private lateinit var colorPickerAlphaSlideBar: AlphaSlideBar
    private lateinit var colorPickerBrightnessSlideBar: BrightnessSlideBar
    private var isColorPickerVisible = false
    private lateinit var toggleColorPickerButton: MaterialButton
    private lateinit var toggleMilitaryTimeSwitch: SwitchMaterial
    //private lateinit var toggleColonSwitch: SwitchMaterial
    private lateinit var toggleDropShadowSwitch: SwitchMaterial

    companion object {
        const val IMAGE_PICK_REQUEST_CODE = 1001
    }

    private val fontMap = mapOf(
        "Default" to "sans-serif",
        "Serif" to "serif",
        "Monospace" to "monospace",
        "Cursive" to "cursive",
        "Fantasy" to "fantasy",
        "Press Start" to "pressstart_regular"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        previewTextView = findViewById(R.id.preview_text_view)

        sharedPreferences = getSharedPreferences("decorative_clock_preferences", Context.MODE_PRIVATE)
        val defaultColor = Color.WHITE // Use any default color you want
        val savedColor = sharedPreferences.getInt("clock_text_color", defaultColor)
        previewTextView.setTextColor(savedColor)

        val cancelIcon = findViewById<ImageView>(R.id.cancel_icon)
        cancelIcon.setOnClickListener {
            finish()
        }


        // initialize colorpicker views
        colorPickerView = findViewById(R.id.color_picker_view)
        colorPickerAlphaSlideBar = findViewById(R.id.alphaSlideBar)
        colorPickerBrightnessSlideBar = findViewById(R.id.brightnessSlideBar)
        toggleColorPickerButton = findViewById(R.id.toggle_color_picker_button)

        // listen for change background button click
        val changeBackgroundButton = findViewById<Button>(R.id.changeBackgroundButton)
        changeBackgroundButton.setOnClickListener {
            openImagePicker()
        }

        toggleMilitaryTimeSwitch = findViewById(R.id.toggleMilitaryTimeSwitch)
        //toggleColonSwitch = findViewById(R.id.toggleColonSwitch)

        sharedPreferences = getSharedPreferences("decorative_clock_preferences", MODE_PRIVATE)

        val isColonEnabled = sharedPreferences.getBoolean("is_colon_enabled", true)
        val isMilitaryTime = sharedPreferences.getBoolean("is_24_hour_format", false)
        toggleMilitaryTimeSwitch.isChecked = isMilitaryTime
        //toggleColonSwitch.isChecked = isColonEnabled

        toggleMilitaryTimeSwitch.setOnCheckedChangeListener { _, _ ->
            updatePreviewText()
        }

//        toggleColonSwitch.setOnCheckedChangeListener { _, _ ->
//            updatePreviewText()
//        }

        // Call updatePreviewText to initialize the previewTextView with the correct format
        updatePreviewText()
        val saveFontButton: Button = findViewById(R.id.save_font_button)

        // Initialize colorPickerView
        colorPickerView = findViewById(R.id.color_picker_view)

        // Retrieve the current clock text color from shared preferences
        val defaultClockTextColor = ContextCompat.getColor(this, R.color.icon_color)
        val currentClockTextColor = sharedPreferences.getInt("clock_text_color", defaultClockTextColor)

        // Set the initial color for the color picker view
        colorPickerView.setInitialColor(currentClockTextColor)

        val colorPickerView = findViewById<ColorPickerView>(R.id.color_picker_view)
        val scrollView = findViewById<NestedScrollView>(R.id.scrollView)
        // Disable scrolling on the color picker view when the user is interacting with it
        colorPickerView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Disallow ScrollView to intercept touch events when interacting with the ColorPickerView
                    scrollView.requestDisallowInterceptTouchEvent(true)
                    false
                }
                MotionEvent.ACTION_UP -> {
                    // Allow ScrollView to intercept touch events after interaction with the ColorPickerView
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
                    // Disallow ScrollView to intercept touch events when interacting with the ColorPickerView
                    scrollView.requestDisallowInterceptTouchEvent(true)
                    false
                }
                MotionEvent.ACTION_UP -> {
                    // Allow ScrollView to intercept touch events after interaction with the ColorPickerView
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
                    // Disallow ScrollView to intercept touch events when interacting with the ColorPickerView
                    scrollView.requestDisallowInterceptTouchEvent(true)
                    false
                }
                MotionEvent.ACTION_UP -> {
                    // Allow ScrollView to intercept touch events after interaction with the ColorPickerView
                    scrollView.requestDisallowInterceptTouchEvent(false)
                    false
                }
                else -> false
            }
        }

        // Initialize fontDropdownMenu and fontAutocomplete
        val fontAutocomplete = findViewById<AutoCompleteTextView>(R.id.font_autocomplete)
        val fontAdapter = ArrayAdapter<String>(this, R.layout.dropdown_menu_popup_item, fontMap.keys.toList())
        fontAutocomplete.setAdapter(fontAdapter)

        // Retrieve the current clock font from shared preferences
        val currentClockFont = sharedPreferences.getString("clock_font", "sans-serif")

        // Find the position of the current font in the adapter
        val currentFontPosition = fontMap.entries.indexOfFirst { it.value == currentClockFont }

        // Set the selected item in the AutoCompleteTextView to the current font
        if (currentFontPosition != -1) {
            fontAutocomplete.setText(fontAdapter.getItem(currentFontPosition), false)
            updatePreviewFont(fontMap[fontAdapter.getItem(currentFontPosition)])
        }

        // Set an ItemClickListener for fontAutocomplete
        fontAutocomplete.setOnItemClickListener { parent, view, position, id ->
            val selectedFont = fontMap[parent.getItemAtPosition(position).toString()]
            updatePreviewFont(selectedFont)
            val defaultFonts = listOf("sans-serif", "serif", "monospace", "cursive", "fantasy")
            if (selectedFont != null) {
                if (defaultFonts.contains(selectedFont)) {
                    previewTextView.typeface = Typeface.create(selectedFont, Typeface.NORMAL)
                } else {
                    val fontResourceId = resources.getIdentifier(selectedFont, "font", packageName)
                    val customTypeface = ResourcesCompat.getFont(this@SettingsActivity, fontResourceId)
                    previewTextView.typeface = customTypeface
                }
            }
        }

        colorPickerView.setColorListener(object : ColorEnvelopeListener {
            override fun onColorSelected(envelope: ColorEnvelope, fromUser: Boolean) {
                if (isColorPickerVisible) {
                    previewTextView.setTextColor(envelope.color)
                    Log.d("josephDebug", "onColorSelected: ${envelope.color}")
                }
            }
        })

        // if military time is enabled, add to previewTextView, if not, remove from previewTextView
        if (isMilitaryTime) {
            previewTextView.text = "13:34"
        } else {
            previewTextView.text = "01:34 PM"
        }

        // Drop Shadow
        toggleDropShadowSwitch = findViewById(R.id.toggleDropShadowSwitch)

        val isDropShadowEnabled = sharedPreferences.getBoolean("is_drop_shadow_enabled", true)
        toggleDropShadowSwitch.isChecked = isDropShadowEnabled
        // if drop shadow is enabled, add to previewTextView, if not, remove from previewTextView
        if (isDropShadowEnabled) {
            addDropShadow(previewTextView)
        } else {
            removeDropShadow(previewTextView)
        }

        var newIsDropShadowEnabled = isDropShadowEnabled

        toggleDropShadowSwitch.setOnCheckedChangeListener { _, isChecked ->
            newIsDropShadowEnabled = isChecked
            if (isChecked) {
                // Add drop shadow to both previewTextView and clockTextView
                addDropShadow(previewTextView)
            } else {
                // Remove drop shadow from both previewTextView and clockTextView
                removeDropShadow(previewTextView)
            }
        }

        colorPickerView.post {
            // Attach AlphaSliderBar and BrightnessSliderBar to colorPickerView
            val alphaSlideBar = findViewById<AlphaSlideBar>(R.id.alphaSlideBar)
            colorPickerView.attachAlphaSlider(alphaSlideBar)

            val brightnessSlideBar = findViewById<BrightnessSlideBar>(R.id.brightnessSlideBar)
            colorPickerView.attachBrightnessSlider(brightnessSlideBar)
        }

        updatePreviewText()
        toggleColorPickerButton.setOnClickListener {
            toggleColorPickerVisibility()
        }

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
                // Save the state of the switches
                editor.putBoolean("is_drop_shadow_enabled", newIsDropShadowEnabled)
                editor.putBoolean("is_24_hour_format", is24HourFormat)
                editor.putBoolean("is_colon_enabled", isColonEnabled)
                editor.apply()
            }
            ColorPickerPreferenceManager.getInstance(this).saveColorPickerData(colorPickerView);
            finish()
        }

        // set background image
        val backgroundImageUriString = sharedPreferences.getString(MainActivity.BACKGROUND_IMAGE_URI_KEY, null)
        if (backgroundImageUriString != null) {
            val backgroundImageUri = Uri.fromFile(File(backgroundImageUriString))
            setBackgroundImage(backgroundImageUri)
        }

        // Get the current text color of previewTextView
        val currentColor = previewTextView.currentTextColor

        // Set the initial color of the ColorPickerView
        colorPickerView.setInitialColor(currentColor)
        //toggleColorPickerVisibility()

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
                val fontResourceId = resources.getIdentifier(selectedFont, "font", packageName)
                val customTypeface = ResourcesCompat.getFont(this@SettingsActivity, fontResourceId)
                previewTextView.typeface = customTypeface
            }
        }
    }


    private fun setBackgroundImage(resultUri: Uri) {
        val backgroundImageView = findViewById<ImageView>(R.id.background_preview)

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

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMAGE_PICK_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMAGE_PICK_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val selectedImageUri = data.data
            if (selectedImageUri != null) {
                if (isGifFile(this, selectedImageUri)) {
                    saveCroppedImageUri(selectedImageUri)
                    setBackgroundImage(selectedImageUri)
                } else {
                    val display = windowManager.defaultDisplay
                    val size = Point()
                    display.getSize(size)
                    val screenWidth = size.x
                    val screenHeight = size.y

                    val uCrop = UCrop.of(selectedImageUri, Uri.fromFile(File(cacheDir, "background_image.jpg")))
                        .withAspectRatio(screenWidth.toFloat(), screenHeight.toFloat())
                    uCrop.start(this)
                }
            }

        } else if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            val resultUri = UCrop.getOutput(data!!)
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

    fun updatePreviewText() {
        val isMilitaryTime = toggleMilitaryTimeSwitch.isChecked
        //val isColonEnabled = toggleColonSwitch.isChecked

        val hourFormat = if (isMilitaryTime) "HH" else "h"
        //val separator = if (isColonEnabled) ":" else " "
        val minuteFormat = "mm"
        val amPmFormat = if (isMilitaryTime) "" else " a"

        //val formatString = "$hourFormat$separator$minuteFormat$amPmFormat"
        val formatString = "$hourFormat:$minuteFormat$amPmFormat"
        val sdf = SimpleDateFormat(formatString, Locale.getDefault())
        val currentTime = Calendar.getInstance().time

        // Log sdf.format(currentTime) to Logcat
        Log.d("josephDebug", sdf.format(currentTime))

        previewTextView.text = sdf.format(currentTime)

        // set previewTextView color from saved preferences
        val sharedPreferences = getSharedPreferences("decorative_clock_preferences", Context.MODE_PRIVATE)
        val currentClockTextColor = sharedPreferences.getInt("clock_text_color", Color.WHITE)
        previewTextView.setTextColor(currentClockTextColor)
        Log.d("josephDebug", "currentClockTextColor: $currentClockTextColor")
    }

    private fun animateColorPickerContainerVisibility(targetVisibility: Int) {
        Log.d("josephDebug", "animateColorPickerContainerVisibility: $targetVisibility")
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




}