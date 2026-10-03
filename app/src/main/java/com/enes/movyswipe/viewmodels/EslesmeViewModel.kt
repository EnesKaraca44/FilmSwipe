package com.enes.movyswipe.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enes.movyswipe.MyApplication // MyApplication'ı import et
import com.enes.movyswipe.data.Movie
import com.enes.movyswipe.data.Result
import com.enes.movyswipe.data.WatchlistMovie
import com.enes.movyswipe.data.repository.MovieRepository
// WatchlistRepository import'u zaten vardı, harika.

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EslesmeViewModel(application: Application) : AndroidViewModel(application) {

    private val movieRepository = MovieRepository()
    // --- DEĞİŞİKLİK 1: WatchlistRepository nesnesini oluşturuyoruz ---
    // Repository'ye, Application sınıfı üzerinden ulaşıyoruz.
    private val watchlistRepository = (application as MyApplication).watchlistRepository
    // -----------------------------------------------------------

    private val _movie = MutableStateFlow<Movie?>(null)
    val movie: StateFlow<Movie?> = _movie.asStateFlow()


    fun loadMatchedMovie(movieId: Int) {
        // İşlemi arkaplanda ve ViewModel'in yaşam döngüsüne bağlı olarak yap.
        viewModelScope.launch {
            // MovieRepository'den sonucu Result olarak alıyoruz.
            val result = movieRepository.getMovieDetails(movieId)

            // Gelen sonucu 'when' ile kontrol ediyoruz.
            when (result) {
                is Result.Success -> {
                    // İşlem başarılıysa, gelen film verisini (_movie) StateFlow'umuza atıyoruz.
                    // Bu atama, Fragment'ın film bilgilerini ekranda göstermesini sağlar.
                    _movie.value = result.data
                }
                is Result.Error -> {
                    // Bir hata oluşursa, log atalım ve state'i temizleyelim.
                    println("Eşleşen film detayı çekilemedi: ${result.exception.message}")
                    _movie.value = null
                }
                is Result.Loading -> {
                    // Yükleniyor durumu (gerekirse bir _isLoading state'i yönetilebilir).
                }
            }
        }
    }
    fun addCurrentMovieToWatchlist() {
        _movie.value?.let { currentMovie ->
            viewModelScope.launch {
                val watchlistMovie = WatchlistMovie(
                    id = currentMovie.id,
                    title = currentMovie.title,
                    posterPath = currentMovie.posterPath,
                    releaseDate = currentMovie.releaseDate,
                    voteAverage = currentMovie.voteAverage
                )
                // --- DEĞİŞİKLİK 2: Fonksiyonu, sınıf adı üzerinden değil, oluşturduğumuz nesne üzerinden çağırıyoruz ---
                Log.d("WatchlistVM", "Film (${watchlistMovie.id}) veritabanına ekleniyor...")
                watchlistRepository.insertMovie(watchlistMovie)
                // ------------------------------------------------------------------------------------------------
            }
        }
    }
}

