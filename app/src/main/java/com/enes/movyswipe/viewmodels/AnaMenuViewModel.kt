package com.enes.movyswipe.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enes.movyswipe.MyApplication
import com.enes.movyswipe.data.Movie
import com.enes.movyswipe.data.Result
import com.enes.movyswipe.data.WatchlistMovie
import com.enes.movyswipe.data.repository.MovieRepository
import com.enes.movyswipe.util.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AnaMenuViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {

    private val _popularMovies = MutableStateFlow<List<Movie>>(emptyList())
    val popularMovies = _popularMovies.asStateFlow()

    // --- YENİ EKLENEN STATEFLOW'LAR ---
    private val _isLoading = MutableStateFlow(true) // Başlangıçta true yapalım
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    // --------------------------------

    // Artık 'init' bloğunda değil, Fragment'tan context alarak başlatacağız.
    // Bu, 'AndroidViewModel' olmadan 'Context' kullanmanın bir yoludur.
    fun loadData() {
        fetchPopularMovies()
    }

    private fun fetchPopularMovies() {
        viewModelScope.launch {
            Log.d("AnaMenuVM_Fetch", "fetchPopularMovies başlatıldı.")
            _isLoading.value = true
            _error.value = null

            Log.d("AnaMenuVM_Fetch", "Repository'den filmler isteniyor...")
            val result = movieRepository.getMovies(emptyMap())
            Log.d("AnaMenuVM_Fetch", "Repository'den cevap geldi: $result")

            when (result) {
                is Result.Success -> {
                    Log.d("AnaMenuVM_Fetch", "Başarılı: ${result.data.size} film bulundu.")
                    _popularMovies.value = result.data
                }
                is Result.Error -> {
                    Log.e("AnaMenuVM_Fetch", "Hata: ${result.exception.message}")
                    _error.value = result.exception.message ?: "Filmler yüklenemedi."
                }
                else -> {}
            }

            _isLoading.value = false
            Log.d("AnaMenuVM_Fetch", "fetchPopularMovies bitti. isLoading = false")
        }
    }

    fun addMovieToWatchlist(movie: Movie) {
        viewModelScope.launch {
            val watchlistMovie = WatchlistMovie(
                id = movie.id,
                title = movie.title,
                posterPath = movie.posterPath,
                releaseDate = movie.releaseDate,
                voteAverage = movie.voteAverage
            )
            watchlistRepository.insertMovie(watchlistMovie)
        }
    }
}