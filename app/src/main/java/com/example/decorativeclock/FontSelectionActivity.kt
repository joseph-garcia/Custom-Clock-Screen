package com.example.decorativeclock

import android.content.Context
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*

class FontSelectionActivity : AppCompatActivity() {

    private lateinit var fontSpinner: Spinner
    private lateinit var previewTextView: TextView

    private val fontMap = mapOf(
        "Default" to "sans-serif",
        "Serif" to "serif",
        "Monospace" to "monospace",
        "Cursive" to "cursive",
        "Fantasy" to "fantasy"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_font_selection)

        fontSpinner = findViewById(R.id.font_spinner)
        previewTextView = findViewById(R.id.preview_text_view)
        val saveFontButton: Button = findViewById(R.id.save_font_button)


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

        saveFontButton.setOnClickListener {
            val sharedPreferences = getSharedPreferences("decorative_clock_preferences", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            val selectedFont = fontMap[fontSpinner.selectedItem.toString()]
            if (selectedFont != null) {
                editor.putString("clock_font", selectedFont)
                editor.apply()
            }
            finish()
        }
    }
}
