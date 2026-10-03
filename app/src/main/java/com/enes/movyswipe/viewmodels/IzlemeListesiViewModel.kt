package com.enes.movyswipe.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enes.movyswipe.MyApplication
import com.enes.movyswipe.data.WatchlistMovie
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// 'Application' context'ine ihtiyacımız olduğu için 'AndroidViewModel' kullanıyoruz.
class IzlemeListesiViewModel(application: Application) : AndroidViewModel(application) {

    // Repository'ye Application sınıfı üzerinden erişiyoruz.
    private val repository = (application as MyApplication).watchlistRepository

    // Repository'den gelen Flow'u, bir StateFlow'a dönüştürüyoruz.
    // Bu sayede Fragment, veriyi anlık olarak ve yaşam döngüsüne uygun bir şekilde dinleyebilir.
    val watchlistMovies: StateFlow<List<WatchlistMovie>> = repository.getAllMovies()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Dinleyici varken 5 saniye bekle
            initialValue = emptyList() // Başlangıç değeri boş liste
        )

    fun clearWatchlist() {
        viewModelScope.launch {
            repository.clearWatchlist()
        }
    }

    /**
     * Verilen bir filmi izleme listesinden siler.
     */
    fun deleteMovie(movie: WatchlistMovie) {
        viewModelScope.launch {
            repository.deleteMovie(movie)
        }
    }


    // TODO: Listeyi temizleme gibi diğer fonksiyonları buraya ekleyeceğiz.
}