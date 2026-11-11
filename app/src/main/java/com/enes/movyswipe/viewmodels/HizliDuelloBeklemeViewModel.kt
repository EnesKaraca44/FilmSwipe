package com.enes.movyswipe.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enes.movyswipe.data.MovieFilters
import com.enes.movyswipe.data.QueueDeck
import com.enes.movyswipe.data.Result
import com.enes.movyswipe.data.repository.FirebaseRepository
import com.enes.movyswipe.data.repository.MovieRepository
import com.enes.movyswipe.di.ApplicationScope
import com.enes.movyswipe.util.AuthManager
import com.enes.movyswipe.util.SafetyPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Hızlı düello eşleştirmesi.
 *
 * Akış:
 * 1. Deste baştan hazırlanır (eşleşince rakibi bekletmemek için).
 * 2. Cihazdaki engeller sunucuya eşitlenir (eşleştirme iki yönlü engele bakar).
 * 3. Kullanıcı desteyle birlikte kuyruğa girer; bağlantı koparsa sunucu kaydı siler (onDisconnect).
 * 4. Çifti sunucu seçer, odayı kurar ve iki tarafın posta kutusuna oda kodunu yazar
 *    (functions/index.js: pairFromQueue). İstemci yalnızca kendi kaydını yazabilir.
 */
@HiltViewModel
class HizliDuelloBeklemeViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val movieRepository: MovieRepository,
    private val safetyPrefs: SafetyPrefs,
    @ApplicationScope private val appScope: CoroutineScope
) : ViewModel() {

    private val _matchmakingStatus = MutableStateFlow("Eşleşme aranıyor...")
    val matchmakingStatus = _matchmakingStatus.asStateFlow()

    private val _queueSize = MutableStateFlow(0)
    val queueSize: StateFlow<Int> = _queueSize.asStateFlow()

    private val _navigateToRoom = Channel<String>(Channel.BUFFERED)
    val navigateToRoom = _navigateToRoom.receiveAsFlow()

    private var myUserId: String? = null
    private var started = false
    private var matched = false
    private var inQueue = false
    private var rejoinJob: Job? = null
    private var deck: QueueDeck? = null

    fun startSearch() {
        if (started) return
        started = true
        viewModelScope.launch {
            val userId = AuthManager.getCurrentUserId()
            if (userId == null) {
                _matchmakingStatus.value = "Giriş yapılamadı. İnternet bağlantını kontrol et."
                started = false
                return@launch
            }
            myUserId = userId

            val prepared = prepareDeck()
            if (prepared == null) {
                _matchmakingStatus.value = "Film listesi alınamadı. İnternet bağlantını kontrol et."
                started = false
                return@launch
            }
            deck = prepared

            firebaseRepository.syncBlocks(userId, safetyPrefs.blockedUsers.value)
            firebaseRepository.clearMatchmakingData(userId)
            listenForAssignment(userId)
            try {
                firebaseRepository.joinQueue(userId, prepared)
                inQueue = true
            } catch (e: Exception) {
                Log.w(TAG, "Kuyruğa katılınamadı", e)
                _matchmakingStatus.value = "Eşleştirme sunucusuna bağlanılamadı."
                started = false
                return@launch
            }
            listenToQueue(userId)
        }
    }

    private suspend fun prepareDeck(): QueueDeck? {
        val genres = listOf(
            "Aksiyon" to MovieFilters.Genre.ACTION,
            "Komedi" to MovieFilters.Genre.COMEDY,
            "Korku" to MovieFilters.Genre.HORROR,
            "Romantik" to MovieFilters.Genre.ROMANCE,
            "Bilim Kurgu" to MovieFilters.Genre.SCIENCE_FICTION
        ).shuffled()
        for ((name, id) in genres.take(2)) {
            val result = movieRepository.getMovies(MovieFilters.discover(listOf(id)))
            if (result is Result.Success && result.data.isNotEmpty()) {
                return QueueDeck(name, id, result.data.map { it.id })
            }
        }
        return null
    }

    /** Sunucu bizi biriyle eşleştirince oda kodunu buraya yazar. */
    private fun listenForAssignment(userId: String) {
        viewModelScope.launch {
            firebaseRepository.listenForAssignedRoom(userId)
                .catch { Log.w(TAG, "Posta kutusu dinlenemedi", it) }
                .collect { code ->
                    if (code == null || matched) return@collect
                    matched = true
                    inQueue = false
                    rejoinJob?.cancel()
                    firebaseRepository.clearMatchmakingData(userId)
                    _matchmakingStatus.value = "Rakip bulundu!"
                    _navigateToRoom.send(code)
                }
        }
    }

    private fun listenToQueue(userId: String) {
        viewModelScope.launch {
            firebaseRepository.listenToQueue()
                .catch { Log.w(TAG, "Kuyruk dinlenemedi", it) }
                .collect { queue ->
                    _queueSize.value = queue.size
                    if (matched) return@collect
                    // Sunucu bizi eşleştirmek için kuyruktan çıkardı; oda kodu gelmezse geri dön.
                    if (userId in queue) rejoinJob?.cancel()
                    else if (inQueue) scheduleRejoin(userId)
                }
        }
    }

    private fun scheduleRejoin(userId: String) {
        if (rejoinJob?.isActive == true) return
        rejoinJob = viewModelScope.launch {
            delay(REJOIN_TIMEOUT_MS)
            if (!matched) rejoin(userId)
        }
    }

    private suspend fun rejoin(userId: String) {
        val prepared = deck ?: return
        try {
            firebaseRepository.joinQueue(userId, prepared)
            inQueue = true
            _matchmakingStatus.value = "Eşleşme aranıyor..."
        } catch (e: Exception) {
            Log.w(TAG, "Kuyruğa yeniden katılınamadı", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // viewModelScope burada zaten iptal edilmiş olur; temizliği uygulama scope'unda yap.
        val userId = myUserId ?: return
        val wasMatched = matched
        appScope.launch {
            firebaseRepository.leaveQueue(userId)
            if (!wasMatched) firebaseRepository.clearMatchmakingData(userId)
        }
    }

    companion object {
        private const val TAG = "HizliDuelloVM"
        private const val REJOIN_TIMEOUT_MS = 15_000L
    }
}
