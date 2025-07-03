package com.example.screenshootcategorizerapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

// ML Kit & Others
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.screenshootcategorizerapp.data.AppDatabase
import com.example.screenshootcategorizerapp.data.ImageEntity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import androidx.drawerlayout.widget.DrawerLayout


class MainActivity : AppCompatActivity() {

    private lateinit var imageDao: com.example.screenshootcategorizerapp.data.ImageDao

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                loadScreenshots()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        // Setup drawer & search bar references
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)

        val searchIcon = findViewById<ImageView>(R.id.search_icon)
        val menuIcon = findViewById<ImageView>(R.id.menu_icon)
        val searchInput = findViewById<EditText>(R.id.search_input)

        // Open drawer when menu icon is clicked
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Handle search logic
        searchIcon.setOnClickListener {
            val query = searchInput.text.toString().trim()

            if (query.isNotEmpty()) {
                val queryWords = query.lowercase().split("\\s+".toRegex())

                lifecycleScope.launch {
                    val allImages = imageDao.getAllImages()

                    // Score + Word Match List
                    val scoredImages = allImages.map { image ->
                        val desc = image.description.lowercase()
                        val matchedWords = queryWords.filter { word -> desc.contains(word) }
                        val matchScore = matchedWords.size
                        Triple(image, matchScore, matchedWords)
                    }.filter { it.second > 0 }

                    // Sort logic:
                    // 1. Higher match count
                    // 2. Priority of matched word (e.g., Netflix > movies > ...)
                    val sortedImages = scoredImages.sortedWith(compareByDescending<Triple<ImageEntity, Int, List<String>>> { it.second }
                        .thenComparator { a, b ->
                            // Only applies to tie-breaker within same match count
                            val aFirstMatchedIndex = queryWords.indexOfFirst { it in a.third }
                            val bFirstMatchedIndex = queryWords.indexOfFirst { it in b.third }
                            aFirstMatchedIndex.compareTo(bFirstMatchedIndex)
                        })

                    val resultPaths = sortedImages.map { it.first.imagePath }

                    val recyclerView = findViewById<RecyclerView>(R.id.screenshotRecyclerView)
                    recyclerView.adapter = ScreenshotAdapter(resultPaths, this@MainActivity)
                }
            } else {
                loadScreenshots()
            }
        }



        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrBlank()) {
                    // Reload all screenshots when search bar is cleared
                    loadSSForSearchClear()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })



        // Initialize DB here with proper context
        val db = AppDatabase.getDatabase(this)
        imageDao = db.imageDao()

        checkPermissionAndLoad()

        val howToUse = findViewById<TextView>(R.id.how_to_use)
        val howToUseContent = findViewById<TextView>(R.id.how_to_use_content)
        val aboutUs = findViewById<TextView>(R.id.about_us)
        val aboutUsContent = findViewById<TextView>(R.id.about_us_content)

        howToUse.setOnClickListener {
            howToUseContent.visibility =
                if (howToUseContent.visibility == android.view.View.VISIBLE)
                    android.view.View.GONE else android.view.View.VISIBLE
        }

        aboutUs.setOnClickListener {
            aboutUsContent.visibility =
                if (aboutUsContent.visibility == android.view.View.VISIBLE)
                    android.view.View.GONE else android.view.View.VISIBLE
        }

    }

    private fun checkPermissionAndLoad() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadScreenshots()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun loadSSForSearchClear() {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA
        )

        val selection = "${MediaStore.Images.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("%/Screenshots/%")
        val screenshotPaths = mutableListOf<String>()

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val filePath = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                screenshotPaths.add(filePath)
            }
        }



        val recyclerView = findViewById<RecyclerView>(R.id.screenshotRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = ScreenshotAdapter(screenshotPaths, this)

    }

    private fun loadScreenshots() {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA
        )

        val selection = "${MediaStore.Images.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("%/Screenshots/%")
        val screenshotPaths = mutableListOf<String>()

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val filePath = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                screenshotPaths.add(filePath)

                val bitmap = BitmapFactory.decodeFile(filePath)

                // Use callback to get detected text
                runTextRecognition(bitmap) { detectedText ->
                    lifecycleScope.launch {
                        imageDao.insertImage(ImageEntity(imagePath = filePath, description = detectedText))

//                        // Log the newly inserted data
//                        val allImages = imageDao.getAllImages()
//                        allImages.forEach {
//                            Log.d("DB_Check", "Image Path: ${it.imagePath}, Text: ${it.description}")
//                        }
                    }
                }
            }
        }

        val recyclerView = findViewById<RecyclerView>(R.id.screenshotRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = ScreenshotAdapter(screenshotPaths, this)
    }

    // ML Kit OCR with callback
    private fun runTextRecognition(bitmap: Bitmap, onResult: (String) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val detectedText = visionText.text
//                Log.d("OCR_Result", detectedText)
                onResult(detectedText)
            }
            .addOnFailureListener { e ->
                Log.e("OCR_Error", "Text recognition failed", e)
                onResult("") // Fallback: return empty string
            }
    }

    private fun searchImages(query: String) {
        lifecycleScope.launch {
            val results = imageDao.searchImages(query)
            val imagePaths = results.map { it.imagePath }

            val recyclerView = findViewById<RecyclerView>(R.id.screenshotRecyclerView)
            recyclerView.adapter = ScreenshotAdapter(imagePaths, this@MainActivity)
        }
    }
}
