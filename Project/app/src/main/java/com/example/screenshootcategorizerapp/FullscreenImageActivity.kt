package com.example.screenshootcategorizerapp

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
//for sharing images
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File


class FullscreenImageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide() // Hide top bar
        setContentView(R.layout.activity_fullscreen_image)

        val imageView = findViewById<ImageView>(R.id.fullscreenImageView)
        val imagePath = intent.getStringExtra("imagePath")

        imagePath?.let {
            val bitmap = BitmapFactory.decodeFile(it)
            imageView.setImageBitmap(bitmap)
        }
        //share image onclick listner
        val shareIcon = findViewById<ImageView>(R.id.share_icon)
        shareIcon.setOnClickListener {
            if (imagePath != null) {
                shareImage(imagePath)
            }
        }


    }

    //function for sharing images

    private fun shareImage(filePath: String) {
        val imageFile = File(filePath)
        val imageUri: Uri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider", // Must match your manifest
            imageFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share image via"))
    }

}