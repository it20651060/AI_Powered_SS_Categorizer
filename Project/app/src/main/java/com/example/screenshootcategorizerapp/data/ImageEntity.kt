package com.example.screenshootcategorizerapp.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

//@Entity(tableName = "images")
@Entity(
    tableName = "images",
    indices = [Index(value = ["imagePath"], unique = true)]
)
data class ImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imagePath: String,
    val description: String
)
