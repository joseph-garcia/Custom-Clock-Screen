package com.example.decorativeclock

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Point
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.skydoves.colorpickerview.sliders.AlphaSlideBar
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar
import com.yalantis.ucrop.UCrop
import java.io.File
import java.io.FileOutputStream




class SettingsActivity : AppCompatActivity() {

    private lateinit var fontSpinner: Spinner
    private lateinit var previewTextView: TextView

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var colorPickerView: ColorPickerView

    private val fontMap = mapOf(
        "Default" to "sans-serif",
        "Serif" to "serif",
        "Monospace" to "monospace",
        "Cursive" to "cursive",
        "Fantasy" to "fantasy",
        "Press Start" to "pressstart2p_regular"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // initialize settings_layout LinearLayout to be scrollable
        val settingsLayout: LinearLayout = findViewById(R.id.settings_layout)


        // listen for change background button click
        val changeBackgroundButton = findViewById<Button>(R.id.changeBackgroundButton)
        changeBackgroundButton.setOnClickListener {
            openImagePicker()
        }

        val toggleMilitaryTimeSwitch: Switch = findViewById(R.id.toggleMilitaryTimeSwitch)

        sharedPreferences = getSharedPreferences("decorative_clock_preferences", MODE_PRIVATE)
        //val isMilitaryTime = sharedPreferences.getBoolean("is_military_time", false)
        val isMilitaryTime = sharedPreferences.getBoolean("is_24_hour_format", false)
        toggleMilitaryTimeSwitch.isChecked = isMilitaryTime
        var newIsMilitaryTime = isMilitaryTime


        toggleMilitaryTimeSwitch.setOnCheckedChangeListener { _, isChecked ->
            newIsMilitaryTime = isChecked
            // if military time is enabled, add to previewTextView, if not, remove from previewTextView
            if (isChecked) {
                previewTextView.text = "13:34"
            } else {
                previewTextView.text = "01:34 PM"
            }
        }

        previewTextView = findViewById(R.id.preview_text_view)
        val saveFontButton: Button = findViewById(R.id.save_font_button)

        // Initialize colorPickerView
        colorPickerView = findViewById(R.id.color_picker_view)


        // Retrieve the current clock text color from shared preferences
        val defaultClockTextColor = ContextCompat.getColor(this, R.color.icon_color)
        val currentClockTextColor = sharedPreferences.getInt("clock_text_color", defaultClockTextColor)

        Log.d("josephDebug", "currentClockTextColor: $currentClockTextColor")

        // Set the initial color for the color picker view
        colorPickerView.setInitialColor(currentClockTextColor)

        val colorPickerView = findViewById<ColorPickerView>(R.id.color_picker_view)
        val scrollView = findViewById<ScrollView>(R.id.scrollView)
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


        // Initialize fontSpinner
        fontSpinner = findViewById(R.id.font_spinner)
        val fontAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, fontMap.keys.toList())
        fontAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fontSpinner.adapter = fontAdapter

        // Retrieve the current clock font from shared preferences
        val currentClockFont = sharedPreferences.getString("clock_font", "sans-serif")

        // Find the position of the current font in the spinner
        val currentFontPosition = fontMap.entries.indexOfFirst { it.value == currentClockFont }

        // Set the spinner's selected item to the current font
        if (currentFontPosition != -1) {
            fontSpinner.setSelection(currentFontPosition)
        }

        fontSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedFont = fontMap[fontSpinner.selectedItem.toString()]
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

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        colorPickerView.setColorListener(object : ColorEnvelopeListener {
            override fun onColorSelected(envelope: ColorEnvelope, fromUser: Boolean) {
                previewTextView.setTextColor(envelope.color)
            }
        })


        // if military time is enabled, add to previewTextView, if not, remove from previewTextView
        if (isMilitaryTime) {
            previewTextView.text = "13:34"
        } else {
            previewTextView.text = "01:34 PM"
        }

        // Drop Shadow
        val toggleDropShadowSwitch: Switch = findViewById(R.id.toggleDropShadowSwitch)

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

        saveFontButton.setOnClickListener {
            val sharedPreferences = getSharedPreferences("decorative_clock_preferences", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            val selectedFont = fontMap[fontSpinner.selectedItem.toString()]
            val selectedColor = colorPickerView.colorEnvelope.color
            val is24HourFormat = toggleMilitaryTimeSwitch.isChecked

            if (selectedFont != null) {
                editor.putString("clock_font", selectedFont)
                editor.putInt("clock_text_color", selectedColor)
                // Save the state of the switches
                editor.putBoolean("is_drop_shadow_enabled", newIsDropShadowEnabled)
                editor.putBoolean("is_24_hour_format", is24HourFormat)
                editor.apply()
            }

            Log.d("josephDebug", "is_24_hour_format: $is24HourFormat")

            finish()
        }

    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMAGE_PICK_REQUEST_CODE)
    }
    companion object {
        const val IMAGE_PICK_REQUEST_CODE = 1001
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMAGE_PICK_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val selectedImageUri = data.data
            if (selectedImageUri != null) {
                if (isGifFile(this, selectedImageUri)) {
                    saveCroppedImageUri(selectedImageUri)
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
}