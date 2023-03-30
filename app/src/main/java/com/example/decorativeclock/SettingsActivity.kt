package com.example.decorativeclock

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream


class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // listen for change background button click
        val changeBackgroundButton = findViewById<Button>(R.id.changeBackgroundButton)
        changeBackgroundButton.setOnClickListener {
            openImagePicker()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
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
                saveSelectedImageUri(selectedImageUri)
            }
        }
    }

    private fun saveSelectedImageUri(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        val directory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File(directory, "background_image.jpg")

        try {
            inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    input?.copyTo(output)
                }
            }

            val sharedPreferences = getSharedPreferences("clock_data", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            val fileUri = FileProvider.getUriForFile(this, applicationContext.packageName + ".fileprovider", file)
            editor.putString("background_image_uri", fileUri.toString())
            editor.apply()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error saving background image", Toast.LENGTH_SHORT).show()
            Log.d("josephDebug", "Error saving background image: ${e.message}")
        }
    }


}