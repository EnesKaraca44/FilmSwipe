package com.enes.movyswipe.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enes.movyswipe.data.WatchlistMovie
import com.enes.movyswipe.data.WatchlistSort
import com.enes.movyswipe.data.WatchlistStats
import com.enes.movyswipe.data.repository.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchlistUiState(
    /** Room'dan ilk veri gelene kadar false; boş ekranın yanıp sönmesini önler. */
    val isLoaded: Boolean = false,
    val allMovies: List<WatchlistMovie> = emptyList(),
    val visibleMovies: List<WatchlistMovie> = emptyList(),
    val stats: WatchlistStats = WatchlistStats.EMPTY,
    val query: String = "",
    val sort: WatchlistSort = WatchlistSort.NEWEST
) {
    val isListEmpty get() = isLoaded && allMovies.isEmpty()
    val hasNoSearchResults get() = allMovies.isNotEmpty() && visibleMovies.isEmpty()
}

@HiltViewModel
class IzlemeListesiViewModel @Inject constructor(
    private val repository: WatchlistRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(WatchlistSort.NEWEST)

    val currentQuery: StateFlow<String> = query.asStateFlow()

    val uiState: StateFlow<WatchlistUiState> =
        combine(repository.getAllMovies(), query, sort) { movies, q, s ->
            WatchlistUiState(
                isLoaded = true,
                allMovies = movies,
                visibleMovies = WatchlistSort.apply(movies, q, s),
                stats = WatchlistStats.from(movies, System.currentTimeMillis()),
                query = q,
                sort = s
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WatchlistUiState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun setSort(value: WatchlistSort) {
        sort.value = value
    }

    fun clearWatchlist() {
        viewModelScope.launch { repository.clearWatchlist() }
    }

    fun deleteMovie(movie: WatchlistMovie) {
        viewModelScope.launch { repository.deleteMovie(movie) }
    }
}
