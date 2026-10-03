package com.enes.movyswipe.data.repository

import android.util.Log
import com.enes.movyswipe.data.WatchlistDao
import com.enes.movyswipe.data.WatchlistMovie

// Bu Repository, veritabanı işlemlerini yönetir.

import javax.inject.Inject

// DAO'yu constructor'dan alır. Bu, daha sonra Dependency Injection için önemlidir.
class WatchlistRepository @Inject constructor(private val watchlistDao: WatchlistDao) {

    // DAO'daki getAllMovies fonksiyonunu doğrudan dışarıya açıyoruz.
    // Bu, tüm filmleri bir Flow olarak döndürür.
    fun getAllMovies() = watchlistDao.getAllMovies()

    // Veritabanına yeni bir film ekler.
    suspend fun insertMovie(movie: WatchlistMovie) {
        try {
            watchlistDao.insertMovie(movie)
            // --- BAŞARI LOG'U ---
            Log.e("WatchlistRepo_Insert", "BAŞARILI: Film (ID: ${movie.id}) veritabanına eklendi.")
        } catch (e: Exception) {
            // --- HATA LOG'U ---
            Log.e("WatchlistRepo_Insert", "HATA: Film eklenirken hata oluştu!", e)
        }
    }

    // izleme listesindeki liste temizleme
    suspend fun clearWatchlist() {
        watchlistDao.clearWatchlist()
    }


    // Veritabanından bir filmi siler.
    suspend fun deleteMovie(movie: WatchlistMovie) {
        watchlistDao.deleteMovie(movie)
    }
}