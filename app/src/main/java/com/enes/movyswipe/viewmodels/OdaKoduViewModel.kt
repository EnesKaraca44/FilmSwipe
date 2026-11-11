package com.enes.movyswipe.viewmodels

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enes.movyswipe.R
import com.enes.movyswipe.data.MovieFilters
import com.enes.movyswipe.data.Result
import com.enes.movyswipe.data.repository.FirebaseRepository
import com.enes.movyswipe.data.repository.MovieRepository
import com.enes.movyswipe.di.ApplicationScope
import com.enes.movyswipe.util.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OdaKoduViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val movieRepository: MovieRepository,
    private val firebaseRepository: FirebaseRepository,
    @ApplicationScope private val appScope: CoroutineScope
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
    private var guestJoined = false

    /** Host, misafir gelmeden ekrandan çıktıysa oda silinir (misafir terk edilmiş odaya girmesin). */
    override fun onCleared() {
        super.onCleared()
        val code = _roomCode.value ?: return
        if (guestJoined) return
        appScope.launch { firebaseRepository.deleteRoom(code) }
    }

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
                    ?: throw RoomSetupException(R.string.duel_error_sign_in)
                val filters = MovieFilters.discover(genres.toList(), startYear)
                val movies = when (val r = movieRepository.getMovies(filters)) {
                    is Result.Success -> r.data
                    is Result.Error -> throw RoomSetupException(R.string.duel_error_movie_list)
                    Result.Loading -> emptyList()
                }
                if (movies.isEmpty()) {
                    throw RoomSetupException(R.string.duel_error_no_movies_for_filters)
                }

                val code = firebaseRepository.createRoom(
                    hostId = hostId,
                    filters = mapOf("genres" to genres.toList(), "startYear" to startYear),
                    movieDeck = movies.map { it.id }
                )
                _roomCode.value = code
                _isLoading.value = false
                firebaseRepository.deleteRoomOnDisconnect(code)

                firebaseRepository.listenToRoomStatus(code).first { it == FirebaseRepository.STATUS_ACTIVE }
                guestJoined = true
                firebaseRepository.keepRoomOnDisconnect(code)
                _navigateToDuel.send(code)
            } catch (e: Exception) {
                // started true kalır: ekran döndürmede kendiliğinden yeniden denenmez, "Tekrar dene" beklenir.
                _error.value = context.getString((e as? RoomSetupException)?.messageRes ?: R.string.duel_error_create_room)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** "Tekrar dene": yarım kalmış odayı siler ve yeni bir oda kurmayı dener. */
    fun retry(genres: IntArray, startYear: Int) {
        if (_isLoading.value || _error.value == null) return
        _roomCode.value?.let { stale -> viewModelScope.launch { firebaseRepository.deleteRoom(stale) } }
        _roomCode.value = null
        started = false
        createRoomFromFilters(genres, startYear)
    }

    /** Kullanıcıya gösterilecek, yerelleştirilmiş oda kurma hatası. */
    private class RoomSetupException(@StringRes val messageRes: Int) : Exception()
}
