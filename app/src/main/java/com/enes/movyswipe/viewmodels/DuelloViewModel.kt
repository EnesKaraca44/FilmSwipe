package com.enes.movyswipe.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enes.movyswipe.data.DuelResult
import com.enes.movyswipe.data.Movie
import com.enes.movyswipe.data.Result
import com.enes.movyswipe.data.RoomInfo
import com.enes.movyswipe.data.repository.FirebaseRepository
import com.enes.movyswipe.data.repository.MatchState
import com.enes.movyswipe.data.repository.MovieRepository
import com.enes.movyswipe.util.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DuelloViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {

    private val _movies = MutableStateFlow<List<Movie>>(emptyList())
    val movies: StateFlow<List<Movie>> = _movies.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _matchState = MutableStateFlow<MatchState>(MatchState.IDLE)
    val matchState = _matchState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _genreName = MutableStateFlow<String?>(null)
    val genreName: StateFlow<String?> = _genreName.asStateFlow()

    /** Ben desteyi bitirdim, partnerimi bekliyorum. */
    private val _waitingForPartner = MutableStateFlow(false)
    val waitingForPartner: StateFlow<Boolean> = _waitingForPartner.asStateFlow()

    private var roomCode: String? = null
    private var userId: String? = null
    private var room: RoomInfo? = null
    private var currentMatch: Int? = null
    private var finishedUsers: Set<String> = emptySet()
    private var noMatchJob: Job? = null

    /**
     * Odayı tek kez yükler. Ekran döndürmede desteyi baştan çekmez, dinleyicileri çoğaltmaz.
     */
    fun start(code: String) {
        if (roomCode != null) return
        roomCode = code
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            userId = AuthManager.getCurrentUserId()
            if (userId == null) {
                _error.value = "Giriş yapılamadı. İnternet bağlantını kontrol et."
                _isLoading.value = false
                return@launch
            }
            when (val roomResult = firebaseRepository.getRoom(code)) {
                is Result.Success -> {
                    val info = roomResult.data
                    room = info
                    _genreName.value = info.genreName
                    // Filmleri sırayla değil, paralel çekiyoruz (20 film ≈ 1 istek süresi).
                    val movies = movieRepository.getMovieDetailsList(info.movieDeck)
                    if (movies.isEmpty()) _error.value = "Filmler yüklenemedi. İnternet bağlantını kontrol et."
                    _movies.value = movies
                    listenToRoom(code)
                }
                is Result.Error -> _error.value = roomResult.exception.message ?: "Oda yüklenemedi."
                Result.Loading -> Unit
            }
            _isLoading.value = false
        }
    }

    private fun listenToRoom(code: String) {
        viewModelScope.launch {
            firebaseRepository.listenForMatch(code)
                .catch { Log.w(TAG, "Eşleşme dinlenemedi", it) }
                .collect { matched ->
                    currentMatch = matched
                    if (matched != null) _matchState.value = MatchState.MATCH_FOUND(matched)
                }
        }
        viewModelScope.launch {
            firebaseRepository.listenToFinished(code)
                .catch { Log.w(TAG, "Bitiş durumu dinlenemedi", it) }
                .collect { finished ->
                    finishedUsers = finished
                    evaluateNoMatch()
                }
        }
    }

    /** Kaydırılan kart sayısı; ekran döndürmede destenin kaldığı yerden devam etmesi için. */
    var swipedCount = 0
        private set

    fun submitChoice(movieId: Int, liked: Boolean) {
        val code = roomCode ?: return
        val uid = userId ?: return
        swipedCount++
        viewModelScope.launch { firebaseRepository.submitChoice(code, movieId, uid, liked) }
    }

    /**
     * Eskiden 2 sn sonra doğrudan "eşleşme yok" deniyordu; partner hâlâ kaydırıyorsa
     * sonradan oluşan eşleşme kaçırılıyordu. Artık iki taraf da bitirene kadar beklenir.
     */
    fun onDeckFinished() {
        val code = roomCode ?: return
        val uid = userId ?: return
        if (_waitingForPartner.value) return
        _waitingForPartner.value = true
        viewModelScope.launch {
            firebaseRepository.markFinished(code, uid)
            // Partner uygulamayı kapattıysa sonsuza kadar bekletme.
            delay(PARTNER_TIMEOUT_MS)
            if (_matchState.value == MatchState.IDLE) _matchState.value = MatchState.NO_MATCH
        }
        evaluateNoMatch()
    }

    private fun evaluateNoMatch() {
        val participants = room?.participants ?: return
        if (!_waitingForPartner.value) return
        if (!DuelResult.isNoMatch(participants, finishedUsers, currentMatch)) return
        if (noMatchJob?.isActive == true) return
        noMatchJob = viewModelScope.launch {
            // Son kaydırmanın Cloud Function tarafından işlenmesi için kısa bir süre tanı.
            delay(MATCH_GRACE_MS)
            if (_matchState.value == MatchState.IDLE) _matchState.value = MatchState.NO_MATCH
        }
    }

    companion object {
        private const val TAG = "DuelloViewModel"
        private const val MATCH_GRACE_MS = 3_000L
        private const val PARTNER_TIMEOUT_MS = 120_000L
    }
}
