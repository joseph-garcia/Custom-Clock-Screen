package com.example.decorativeclock

import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.yalantis.ucrop.UCrop
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
        editor.putString("background_image_uri", uri.toString())
        editor.apply()
    }

    private fun isGifFile(context: Context, uri: Uri): Boolean {
        val mimeType: String? = context.contentResolver.getType(uri)
        return mimeType?.equals("image/gif", ignoreCase = true) == true
    }









}