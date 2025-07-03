package com.example.screenshootcategorizerapp

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class ScreenshotAdapter(private val screenshotPaths: List<String>,private val context: Context) :
    RecyclerView.Adapter<ScreenshotAdapter.ScreenshotViewHolder>() {

    class ScreenshotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.screenshotItemImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScreenshotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_screenshot, parent, false)
        return ScreenshotViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScreenshotViewHolder, position: Int) {
        val filePath = screenshotPaths[position]
        val bitmap = BitmapFactory.decodeFile(filePath)
        holder.imageView.setImageBitmap(bitmap)

        holder.imageView.setOnClickListener {
            val intent = Intent(context, FullscreenImageActivity::class.java)
            intent.putExtra("imagePath", screenshotPaths[position])
            context.startActivity(intent)
        }

    }

    override fun getItemCount(): Int = screenshotPaths.size
}