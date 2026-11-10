package com.enes.movyswipe.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enes.movyswipe.ads.AdsManager
import com.enes.movyswipe.billing.BillingManager
import com.enes.movyswipe.data.Movie
import com.enes.movyswipe.data.MovieFilters
import com.enes.movyswipe.data.Result
import com.enes.movyswipe.data.repository.MovieRepository
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

@HiltViewModel
class AnaMenuViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val watchlistRepository: WatchlistRepository,
    adsManager: AdsManager,
    billingManager: BillingManager
) : ViewModel() {

    private val _popularMovies = MutableStateFlow<List<Movie>>(emptyList())
    val popularMovies = _popularMovies.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    /** Reklam ancak onay alınmışsa VE kullanıcı premium değilse gösterilir. */
    val shouldShowAds: StateFlow<Boolean> =
        combine(adsManager.canShowAds, billingManager.isPremium) { canShow, premium -> canShow && !premium }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private var loaded = false

    /** Ekran her yeniden oluştuğunda değil, sadece ilk kez (veya hata sonrası) yükler. */
    fun loadData(force: Boolean = false) {
        if ((loaded && !force) || _isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            when (val result = movieRepository.getMovies(MovieFilters.discover(emptyList()))) {
                is Result.Success -> {
                    _popularMovies.value = result.data
                    loaded = true
                }
                is Result.Error -> _error.value = "Filmler yüklenemedi. İnternet bağlantını kontrol et."
                Result.Loading -> Unit
            }
            _isLoading.value = false
        }
    }

    fun addMovieToWatchlist(movie: Movie, onDone: (Boolean) -> Unit) {
        viewModelScope.launch { onDone(watchlistRepository.insertMovie(movie.toWatchlistMovie())) }
    }
}
