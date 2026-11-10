package com.enes.movyswipe.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enes.movyswipe.data.Movie
import com.enes.movyswipe.data.Result
import com.enes.movyswipe.data.repository.MovieRepository
import com.enes.movyswipe.data.repository.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EslesmeViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {

    private val _movie = MutableStateFlow<Movie?>(null)
    val movie: StateFlow<Movie?> = _movie.asStateFlow()

    private var loadedId: Int? = null

    fun loadMatchedMovie(movieId: Int) {
        if (loadedId == movieId) return
        loadedId = movieId
        viewModelScope.launch {
            _movie.value = (movieRepository.getMovieDetails(movieId) as? Result.Success)?.data
        }
    }

    fun addCurrentMovieToWatchlist(onDone: (Boolean) -> Unit) {
        val current = _movie.value ?: return onDone(false)
        viewModelScope.launch { onDone(watchlistRepository.insertMovie(current.toWatchlistMovie())) }
    }
}
