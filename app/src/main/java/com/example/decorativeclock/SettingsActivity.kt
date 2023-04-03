package com.example.decorativeclock

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Point
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
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
        "Fantasy" to "fantasy"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // listen for change background button click
        val changeBackgroundButton = findViewById<Button>(R.id.changeBackgroundButton)
        changeBackgroundButton.setOnClickListener {
            openImagePicker()
        }

        val toggleMilitaryTimeSwitch: Switch = findViewById(R.id.toggleMilitaryTimeSwitch)

//        val changeFontButton = findViewById<Button>(R.id.change_font_button)
//        changeFontButton.setOnClickListener {
//            val intent = Intent(this, FontSelectionActivity::class.java)
//            startActivity(intent)
//        }

        sharedPreferences = getSharedPreferences("decorative_clock_preferences", MODE_PRIVATE)
        val isMilitaryTime = sharedPreferences.getBoolean("is_military_time", false)
        toggleMilitaryTimeSwitch.isChecked = isMilitaryTime

        toggleMilitaryTimeSwitch.setOnCheckedChangeListener { _, isChecked ->
            val sharedPreferences = getSharedPreferences("decorative_clock_preferences", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putBoolean("military_time", isChecked)
                apply()
            }
            // Restart MainActivity to apply changes
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }



        fontSpinner = findViewById(R.id.font_spinner)
        previewTextView = findViewById(R.id.preview_text_view)
        val saveFontButton: Button = findViewById(R.id.save_font_button)

        // Initialize colorPickerView
        colorPickerView = findViewById(R.id.color_picker_view)

        // Retrieve the current clock text color from shared preferences
        val currentClockTextColor = sharedPreferences.getInt("clock_text_color", Color.BLACK)

        Log.d("josephDebug", "currentClockTextColor: $currentClockTextColor")

        // Set the initial color for the color picker view
        colorPickerView.setInitialColor(currentClockTextColor)

        val fontAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, fontMap.keys.toList())
        fontAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fontSpinner.adapter = fontAdapter

        fontSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedFont = fontMap[fontSpinner.selectedItem.toString()]
                if (selectedFont != null) {
                    previewTextView.typeface = Typeface.create(selectedFont, Typeface.NORMAL)
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





        saveFontButton.setOnClickListener {
            val sharedPreferences = getSharedPreferences("decorative_clock_preferences", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            val selectedFont = fontMap[fontSpinner.selectedItem.toString()]
            val selectedColor = colorPickerView.colorEnvelope.color
            if (selectedFont != null) {
                editor.putString("clock_font", selectedFont)
                editor.putInt("clock_text_color", selectedColor)
                editor.apply()
            }
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









}