package com.enes.movyswipe.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enes.movyswipe.data.Result
import com.enes.movyswipe.data.repository.FirebaseRepository
import com.enes.movyswipe.data.repository.MovieRepository
import com.enes.movyswipe.util.AuthManager
import kotlinx.coroutines.flow.launchIn // YENİ IMPORT
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HizliDuelloBeklemeViewModel : ViewModel() {

    private val firebaseRepository = FirebaseRepository()
    private val movieRepository = MovieRepository()

    private var myUserId: String? = null
    private var queueListenerJob: Job? = null

    private val _matchmakingStatus = MutableStateFlow<String>("Eşleşme aranıyor...")
    val matchmakingStatus = _matchmakingStatus.asStateFlow()

    private val _navigateToRoom = MutableStateFlow<String?>(null)
    val navigateToRoom = _navigateToRoom.asStateFlow()

    private val _queueSize = MutableStateFlow(0)
    val queueSize: StateFlow<Int> = _queueSize.asStateFlow()

    fun startSearch() {

        viewModelScope.launch {
        // Önceki dinleyicileri iptal et
        queueListenerJob?.cancel()
            val userId = AuthManager.getCurrentUserId()
        myUserId = userId

        // Tüm işlemleri tek bir ana coroutine içinde, sırayla yapalım.
        queueListenerJob = viewModelScope.launch {
            // 1. Önce temizlik yap ve havuza katıl.
            if (queueListenerJob?.isActive == true) return@launch
            firebaseRepository.clearMatchmakingData(userId)
            firebaseRepository.joinQueue(userId)
            Log.d("HizliDuelloVM", "$userId havuza katıldı ve eski verileri temizlendi.")

            // 2. Posta kutusunu dinlemek için AYRI bir coroutine başlat.
            // Bu, ana dinleyiciyi engellemez.
            launch {
                firebaseRepository.listenForAssignedRoom(userId).collect { assignedRoomCode ->
                    if (assignedRoomCode != null) {
                        Log.d("HizliDuelloVM", "Bir odaya atandık: $assignedRoomCode")
                        _navigateToRoom.value = assignedRoomCode
                        queueListenerJob?.cancel() // Kendimizi ana dinleyiciden de çıkaralım.
                    } }
            }
        }

            // 3. Ana işlem olarak havuzu dinle.
            firebaseRepository.listenToQueue().collect { userIdsInQueue ->
                Log.d("HizliDuelloVM", "Havuz güncellendi: $userIdsInQueue")
                _queueSize.value = userIdsInQueue.size

                if (userIdsInQueue.size >= 2 && userIdsInQueue.contains(myUserId)) {
                    val opponentId = userIdsInQueue.firstOrNull { it != myUserId }
                    if (opponentId != null) {
                        if (myUserId!! < opponentId) {
                            // Eşleşme bulunduğunda, bu coroutine'i ve dolayısıyla collect'i
                            // durdurmak için başka bir coroutine'de oda kuruyoruz.
                            launch {
                                createRoomForMatch(myUserId!!, opponentId)
                            }
                            queueListenerJob?.cancel() // Ana dinleyiciyi durdur.
                        }
                    }
                }
            }
        }
    }

    private fun createRoomForMatch(hostId: String, guestId: String) {
        viewModelScope.launch {
            // 1. Rastgele bir tür seç
            val genresMap = mapOf(
                "Aksiyon" to 28,
                "Komedi" to 35,
                "Korku" to 27,
                "Romantik" to 10749,
                "Bilim Kurgu" to 878
            )
            val randomGenreEntry = genresMap.entries.random()
            val randomGenreName = randomGenreEntry.key
            val randomGenreId = randomGenreEntry.value.toString()

            // 2. TMDB için filtre hazırla
            val movieFilters = mapOf("with_genres" to randomGenreId)

            // 3. Seçilen türe göre filmleri çek
            when (val movieResult = movieRepository.getMovies(movieFilters)) {
                is Result.Success -> {
                    val movies = movieResult.data
                    if (movies.isNotEmpty()) {
                        val movieDeck = movies.map { movie -> movie.id }

                        // 4. Firebase için filtre hazırla
                        val firebaseFilters = mapOf(
                            "genreName" to randomGenreName,
                            "genreId" to randomGenreId
                        )

                        // 5. Firebase'de odayı bu bilgilerle kur
                        val roomCode = firebaseRepository.createRoom(firebaseFilters, movieDeck)
                        firebaseRepository.joinRoom(roomCode, guestId)

                        // 6. Kullanıcıları havuzdan çıkar
                        firebaseRepository.leaveQueue(hostId)
                        firebaseRepository.leaveQueue(guestId)

                        firebaseRepository.notifyOpponent(guestId, roomCode)
                        // --- YENİ LOG ---
                        Log.e("HizliDuello_Host", "Rakibe (${guestId}) haber gönderildi. Oda Kodu: ${roomCode}")

                        // 7. Fragment'a odaya gitmesini söyle
                        _navigateToRoom.value = roomCode
                    } else {
                        _matchmakingStatus.value = "Film bulunamadı, tekrar deneniyor..."
                    }
                }
                is Result.Error -> {
                    _matchmakingStatus.value = "Film listesi alınamadı: ${movieResult.exception.message}"
                }
                else -> {}
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        myUserId?.let {
            viewModelScope.launch {
                Log.d("HizliDuelloVM", "$it havuzdan ayrılıyor.")
                firebaseRepository.leaveQueue(it)
            }
        }
        queueListenerJob?.cancel()
    }
}