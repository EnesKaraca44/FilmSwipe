package com.enes.movyswipe.di

import android.content.Context
import androidx.room.Room
import com.enes.movyswipe.data.AppDatabase
import com.enes.movyswipe.data.WatchlistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "film_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideWatchlistDao(appDatabase: AppDatabase): WatchlistDao {
        return appDatabase.watchlistDao()
    }
}
