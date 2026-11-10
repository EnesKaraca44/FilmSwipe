package com.enes.movyswipe.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enes.movyswipe.data.MovieFilters
import com.enes.movyswipe.data.Result
import com.enes.movyswipe.data.repository.FirebaseRepository
import com.enes.movyswipe.data.repository.MovieRepository
import com.enes.movyswipe.util.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OdaKoduViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _roomCode = MutableStateFlow<String?>(null)
    val roomCode = _roomCode.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _navigateToDuel = Channel<String>(Channel.BUFFERED)
    val navigateToDuel = _navigateToDuel.receiveAsFlow()

    private var started = false

    /**
     * Odayı SADECE BİR KEZ oluşturur. Ekran döndürme veya tema değişimi Fragment'ı yeniden
     * oluştursa bile yeni oda açılmaz; arkadaşına verdiğin kod geçerli kalır.
     */
    fun createRoomFromFilters(genres: IntArray, startYear: Int) {
        if (started) return
        started = true
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val hostId = AuthManager.getCurrentUserId()
                    ?: throw IllegalStateException("Giriş yapılamadı. İnternet bağlantını kontrol et.")
                val filters = MovieFilters.discover(genres.toList(), startYear)
                val movies = when (val r = movieRepository.getMovies(filters)) {
                    is Result.Success -> r.data
                    is Result.Error -> throw IllegalStateException("Film listesi alınamadı. İnternet bağlantını kontrol et.")
                    Result.Loading -> emptyList()
                }
                if (movies.isEmpty()) {
                    throw IllegalStateException("Bu filtrelere uygun film bulunamadı. Filtreleri genişlet.")
                }

                val code = firebaseRepository.createRoom(
                    hostId = hostId,
                    filters = mapOf("genres" to genres.toList(), "startYear" to startYear),
                    movieDeck = movies.map { it.id }
                )
                _roomCode.value = code
                _isLoading.value = false

                firebaseRepository.listenToRoomStatus(code).first { it == FirebaseRepository.STATUS_ACTIVE }
                _navigateToDuel.send(code)
            } catch (e: Exception) {
                _error.value = e.message ?: "Oda oluşturulamadı."
                started = false // kullanıcı tekrar deneyebilsin
            } finally {
                _isLoading.value = false
            }
        }
    }
}
