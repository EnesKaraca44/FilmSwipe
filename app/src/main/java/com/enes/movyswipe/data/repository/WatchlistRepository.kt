package com.enes.movyswipe.data.repository

import android.util.Log
import com.enes.movyswipe.data.WatchlistDao
import com.enes.movyswipe.data.WatchlistMovie
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistRepository @Inject constructor(private val watchlistDao: WatchlistDao) {

    fun getAllMovies() = watchlistDao.getAllMovies()

    /** @return true ise kayıt başarılı. */
    suspend fun insertMovie(movie: WatchlistMovie): Boolean = try {
        watchlistDao.insertMovie(movie)
        true
    } catch (e: Exception) {
        Log.e("WatchlistRepository", "Film eklenemedi", e)
        false
    }

    suspend fun clearWatchlist() = watchlistDao.clearWatchlist()

    suspend fun deleteMovie(movie: WatchlistMovie) = watchlistDao.deleteMovie(movie)
}
