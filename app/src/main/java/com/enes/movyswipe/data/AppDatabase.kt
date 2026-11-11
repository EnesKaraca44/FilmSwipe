package com.enes.movyswipe.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WatchlistMovie::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
}
