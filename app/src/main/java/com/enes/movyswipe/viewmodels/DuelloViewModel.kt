package com.enes.movyswipe.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enes.movyswipe.data.Result
import com.enes.movyswipe.data.Movie
import com.enes.movyswipe.data.repository.FirebaseRepository
import com.enes.movyswipe.data.repository.MatchState
import com.enes.movyswipe.data.repository.MovieRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DuelloViewModel : ViewModel() {

    // Repository nesnelerini oluşturuyoruz.
    private val movieRepository = MovieRepository()
    // Değişken adını düzeltiyoruz: küçük 'f' ile başlamalı ve doğru yazılmalı.
    private val firebaseRepository = FirebaseRepository()

    // Arayüze göndereceğimiz film listesini tutan StateFlow.
    private val _movies = MutableStateFlow<List<Movie>>(emptyList())
    val movies: StateFlow<List<Movie>> = _movies.asStateFlow()

    // Yükleme durumunu tutmak için ekleyelim (Fragment'ta kullanmak için)
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _matchState = MutableStateFlow<MatchState>(MatchState.IDLE)
    val matchState = _matchState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _genreName = MutableStateFlow<String?>(null)
    val genreName: StateFlow<String?> = _genreName.asStateFlow()

    // Artık ViewModel oluşturulduğunda otomatik olarak film çekmiyoruz.
    // Film çekme işlemini Fragment'ın kendisi, oda kodunu aldığında tetikleyecek.
    // Bu yüzden 'init' bloğunu ve 'fetchPopularMovies' fonksiyonunu siliyoruz.

    /**
     * Verilen oda koduna ait film listesini yükler.
     * Önce Firebase'den film ID'lerini alır, sonra TMDB'den bu filmlerin detaylarını çeker.
     */
    fun loadMovieDeck(roomCode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null // Her yeni yüklemede eski hatayı temizle

            // 1. Adım: Odanın filtre bilgilerini (tür adı gibi) çekiyoruz.
            // Bu, film listesini çekme işlemiyle aynı anda yapılabilir.
            when (val filterResult = firebaseRepository.getRoomFilters(roomCode)) {
                is Result.Success -> {
                    // Gelen Map'ten "genreName"i alıp String'e çeviriyoruz.
                    _genreName.value = filterResult.data["genreName"] as? String
                }
                is Result.Error -> {
                    // Filtre çekilemezse bir şey yapma veya varsayılan bir değer ata
                    _genreName.value = "Genel"
                }
                else -> {}
            }
            // 2. Adım: Firebase'den film ID'lerinin listesini (movieDeck) Result olarak alıyoruz.
            when (val idResult = firebaseRepository.getMovieDeck(roomCode)) {
                is Result.Success -> {
                    // Firebase'den ID'ler başarıyla geldiyse...
                    val movieIds = idResult.data
                    val moviesToShow = mutableListOf<Movie>()

                    // 3. Adım: Her bir ID için TMDB'den film detaylarını çekiyoruz.
                    for (id in movieIds) {
                        when (val movieDetailResult = movieRepository.getMovieDetails(id)) {
                            is Result.Success -> {
                                // Film detayı başarıyla geldiyse listeye ekle.
                                moviesToShow.add(movieDetailResult.data)
                            }
                            is Result.Error -> {
                                // Bir filmin detayı çekilemezse, hatayı log'la ama devam et.
                                Log.e("DuelloViewModel", "Film detayı çekilemedi (ID: $id): ${movieDetailResult.exception.message}")
                            }
                            else -> {} // Loading durumu için
                        }
                    }

                    // 4. Adım: Başarıyla çekilen filmleri arayüze gönderiyoruz.
                    _movies.value = moviesToShow
                }
                is Result.Error -> {
                    // Film ID listesi hiç çekilemezse, hatayı _error StateFlow'una atıyoruz.
                    _error.value = idResult.exception.message ?: "Film listesi yüklenirken bir hata oluştu."
                    _movies.value = emptyList() // Hata durumunda listeyi boşalt
                }
                else -> {} // Loading durumu için
            }

            _isLoading.value = false
        }
    }

    /**
     * Kullanıcının seçimini Firebase'e kaydeder.
     * (Bu, Gün 8'in bir sonraki adımı için şimdiden hazır.)
     */
    fun submitChoice(roomCode: String, movieId: Int, choice: String,userId: String) {
        viewModelScope.launch {

            firebaseRepository.submitChoice(roomCode, movieId, userId, choice)
        }
    }
    fun startListeningForMatch(roomCode: String) {
        Log.d("DuelloViewModel_Listen", "${roomCode} için match dinleyicisi başlatıldı.")
        viewModelScope.launch {
            firebaseRepository.listenForMatch(roomCode).collect { matchedMovieId ->
                Log.d("DuelloViewModel", "Yeni match durumu geldi: $matchedMovieId")
                if (matchedMovieId != null) {
                    // Eğer bir film ID'si geldiyse, durumu MATCH_FOUND olarak güncelle.
                    _matchState.value = MatchState.MATCH_FOUND(matchedMovieId)
                }
            }
        }
    }

    fun onDeckFinished() {
        // Küçük bir gecikme ekleyerek, Cloud Function'ın çalışması için zaman tanıyoruz.
        // 2 saniye genellikle yeterlidir.
        viewModelScope.launch {
            delay(2000L) // 2 saniye bekle

            // 2 saniye sonra, eğer hala bir eşleşme bulunmadıysa
            // (yani _matchState hala başlangıç durumundaysa),
            // o zaman "Eşleşme Yok" durumunu tetikle.
            if (_matchState.value is MatchState.IDLE) {
                _matchState.value = MatchState.NO_MATCH
            }
        }
    }

   // fun leaveRoom(roomCode: String) {
      //  viewModelScope.launch {
      //     firebaseRepository.deleteRoom(roomCode)
      //  }
  //  }
}

