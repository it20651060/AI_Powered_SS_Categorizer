package com.example.screenshootcategorizerapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ImageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertImage(image: ImageEntity)

    @Query("SELECT * FROM images WHERE description LIKE '%' || :query || '%' ORDER BY CASE WHEN description LIKE :query || '%' THEN 1 ELSE 2 END")
    suspend fun searchImages(query: String): List<ImageEntity>

    @Query("SELECT * FROM images")
    suspend fun getAllImages(): List<ImageEntity>

}
