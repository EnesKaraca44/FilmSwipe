package com.enes.movyswipe.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enes.movyswipe.R
import com.enes.movyswipe.data.DuelResult
import com.enes.movyswipe.data.Movie
import com.enes.movyswipe.data.Result
import com.enes.movyswipe.data.RoomInfo
import com.enes.movyswipe.data.repository.FirebaseRepository
import com.enes.movyswipe.data.repository.MatchState
import com.enes.movyswipe.data.repository.MovieRepository
import com.enes.movyswipe.util.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
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
    private var listeningToRoom = false

    /** Rakibin bağlantısı şu an kopuk (geri gelebilir). */
    private val _partnerOffline = MutableStateFlow(false)
    val partnerOffline: StateFlow<Boolean> = _partnerOffline.asStateFlow()

    /** Rakip [PARTNER_LEFT_GRACE_MS] boyunca geri gelmedi: odadan ayrıldı sayılır. */
    private val _partnerLeft = MutableStateFlow(false)
    val partnerLeft: StateFlow<Boolean> = _partnerLeft.asStateFlow()

    private var partnerSeen = false
    private var partnerLeftJob: Job? = null

    /**
     * Odayı tek kez yükler. Ekran döndürmede desteyi baştan çekmez, dinleyicileri çoğaltmaz.
     */
    fun start(code: String) {
        if (roomCode != null) return
        roomCode = code
        load(code)
    }

    /** Yükleme başarısız olduysa ("Tekrar dene") odayı ve filmleri yeniden çeker. */
    fun retry() {
        val code = roomCode ?: return
        if (_isLoading.value || _movies.value.isNotEmpty()) return
        load(code)
    }

    private fun load(code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val uid = userId ?: AuthManager.getCurrentUserId()
            userId = uid
            if (uid == null) {
                _error.value = context.getString(R.string.duel_error_sign_in)
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
                    if (movies.isEmpty()) _error.value = context.getString(R.string.duel_error_movies_load)
                    _movies.value = movies
                    if (!listeningToRoom) {
                        listeningToRoom = true
                        listenToRoom(code)
                        trackPresence(code, uid, info)
                    }
                }
                is Result.Error -> _error.value = context.getString(
                    if (roomResult.exception is FirebaseRepository.RoomNotFoundException) R.string.duel_error_room_not_found
                    else R.string.duel_error_room_load
                )
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

    /**
     * Kendi çevrimiçi kaydımızı tutar ve rakibinkini izler. Rakip bir kez görüldükten sonra
     * kaybolursa önce "bağlantısı koptu" gösterilir, süre dolunca "ayrıldı" sayılır.
     * Rakip hiç görünmediyse (çevrimiçi kaydı yazmayan eski sürüm olabilir) bir şey yapılmaz;
     * o durumda desteyi bitirince eski [PARTNER_TIMEOUT_MS] zaman aşımı devreye girer.
     */
    private fun trackPresence(code: String, me: String, info: RoomInfo) {
        val partner = info.participants.firstOrNull { it != me } ?: return
        viewModelScope.launch {
            firebaseRepository.stayOnlineInRoom(code, me)
                .catch { Log.w(TAG, "Çevrimiçi durumu yazılamadı", it) }
                .collect {}
        }
        viewModelScope.launch {
            firebaseRepository.listenToOnlinePlayers(code)
                .catch { Log.w(TAG, "Rakibin durumu dinlenemedi", it) }
                .collect { online ->
                    if (partner in online) {
                        partnerSeen = true
                        partnerLeftJob?.cancel()
                        _partnerOffline.value = false
                        return@collect
                    }
                    if (!partnerSeen || _matchState.value != MatchState.IDLE) return@collect
                    _partnerOffline.value = true
                    if (partnerLeftJob?.isActive == true) return@collect
                    partnerLeftJob = launch {
                        delay(PARTNER_LEFT_GRACE_MS)
                        if (_matchState.value == MatchState.IDLE) _partnerLeft.value = true
                    }
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
        /** Kısa bağlantı kopmalarında (tünel, ağ değişimi) hemen "ayrıldı" denmesin. */
        private const val PARTNER_LEFT_GRACE_MS = 20_000L
    }
}
