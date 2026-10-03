package com.enes.movyswipe.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// --- BU SATIRI KONTROL ET ---
// Bu anotasyon, Room'a bu veritabanının içinde hangi tabloların ('entities') olacağını söyler.
// 'WatchlistMovie::class' burada mutlaka olmalıdır.
@Database(entities = [WatchlistMovie::class], version = 1, exportSchema = false)
// ----------------------------
abstract class AppDatabase : RoomDatabase() {

    abstract fun watchlistDao(): WatchlistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "film_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}