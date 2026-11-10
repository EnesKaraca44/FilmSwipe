package com.enes.movyswipe.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enes.movyswipe.data.WatchlistMovie
import com.enes.movyswipe.data.repository.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IzlemeListesiViewModel @Inject constructor(
    private val repository: WatchlistRepository
) : ViewModel() {

    val watchlistMovies: StateFlow<List<WatchlistMovie>> = repository.getAllMovies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clearWatchlist() {
        viewModelScope.launch { repository.clearWatchlist() }
    }

    fun deleteMovie(movie: WatchlistMovie) {
        viewModelScope.launch { repository.deleteMovie(movie) }
    }
}
