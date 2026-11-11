package com.enes.movyswipe.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enes.movyswipe.R
import com.enes.movyswipe.data.MovieFilters
import com.enes.movyswipe.data.QueueDeck
import com.enes.movyswipe.data.Result
import com.enes.movyswipe.data.repository.FirebaseRepository
import com.enes.movyswipe.data.repository.MovieRepository
import com.enes.movyswipe.di.ApplicationScope
import com.enes.movyswipe.util.AuthManager
import com.enes.movyswipe.util.SafetyPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
    private val firebaseRepository: FirebaseRepository,
    private val movieRepository: MovieRepository,
    private val safetyPrefs: SafetyPrefs,
    @ApplicationScope private val appScope: CoroutineScope
) : ViewModel() {

    private val _matchmakingStatus = MutableStateFlow(context.getString(R.string.duel_quick_status_searching))
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

    /** Arama bir hatayla durdu; ekranda "Tekrar dene" düğmesi gösterilir. */
    private val _canRetry = MutableStateFlow(false)
    val canRetry: StateFlow<Boolean> = _canRetry.asStateFlow()

    private var listeningForAssignment = false

    fun startSearch() {
        if (started) return
        started = true
        _canRetry.value = false
        _matchmakingStatus.value = context.getString(R.string.duel_quick_status_searching)
        viewModelScope.launch {
            val userId = myUserId ?: AuthManager.getCurrentUserId()
            if (userId == null) {
                fail(context.getString(R.string.duel_error_sign_in))
                return@launch
            }
            myUserId = userId

            val prepared = deck ?: prepareDeck()
            if (prepared == null) {
                fail(context.getString(R.string.duel_error_movie_list))
                return@launch
            }
            deck = prepared

            firebaseRepository.syncBlocks(userId, safetyPrefs.blockedUsers.value)
            if (!listeningForAssignment) {
                firebaseRepository.clearMatchmakingData(userId)
                listeningForAssignment = true
                listenForAssignment(userId)
            }
            try {
                firebaseRepository.joinQueue(userId, prepared)
                inQueue = true
            } catch (e: Exception) {
                Log.w(TAG, "Kuyruğa katılınamadı", e)
                fail(context.getString(R.string.duel_quick_error_server))
                return@launch
            }
            listenToQueue(userId)
        }
    }

    /** "Tekrar dene": hazırlanan deste ve dinleyiciler korunur, kalan adımlar yeniden denenir. */
    fun retry() = startSearch()

    private fun fail(message: String) {
        _matchmakingStatus.value = message
        started = false
        _canRetry.value = true
    }

    private suspend fun prepareDeck(): QueueDeck? {
        val genres = listOf(
            // Tür adı odaya yazılır ve iki oyuncuya da gösterilir; kurucunun cihaz dilinde olur.
            context.getString(R.string.duel_genre_action) to MovieFilters.Genre.ACTION,
            context.getString(R.string.duel_genre_comedy) to MovieFilters.Genre.COMEDY,
            context.getString(R.string.duel_genre_horror) to MovieFilters.Genre.HORROR,
            context.getString(R.string.duel_genre_romance) to MovieFilters.Genre.ROMANCE,
            context.getString(R.string.duel_genre_scifi) to MovieFilters.Genre.SCIENCE_FICTION
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
                    _matchmakingStatus.value = context.getString(R.string.duel_quick_status_found)
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
            _matchmakingStatus.value = context.getString(R.string.duel_quick_status_searching)
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
