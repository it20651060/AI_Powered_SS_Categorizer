package com.example.screenshootcategorizerapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [ImageEntity::class], version = 2)  // ← bumped version here
abstract class AppDatabase : RoomDatabase() {
    abstract fun imageDao(): ImageDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
                    .fallbackToDestructiveMigration()  // ← this deletes the old DB if migration not defined
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

//@Database(entities = [ImageEntity::class], version = 1)
//abstract class AppDatabase : RoomDatabase() {
//    abstract fun imageDao(): ImageDao
//
//    companion object {
//        @Volatile private var instance: AppDatabase? = null
//
//        fun getDatabase(context: Context): AppDatabase =
//            instance ?: synchronized(this) {
//                instance ?: Room.databaseBuilder(
//                    context.applicationContext,
//                    AppDatabase::class.java,
//                    "image_database"
//                ).build().also { instance = it }
//            }
//    }
//}
