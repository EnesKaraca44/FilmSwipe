package com.enes.movyswipe.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.enes.movyswipe.data.Result
import androidx.lifecycle.viewModelScope
import com.enes.movyswipe.data.repository.FirebaseRepository
import com.enes.movyswipe.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OdaKoduViewModel : ViewModel() {

    // İki farklı veri kaynağıyla konuşacak olan temsilcilerimizi (Repository) oluşturuyoruz.
    private val movieRepository = MovieRepository()
    private val firebaseRepository = FirebaseRepository()

    // --- Arayüze göndereceğimiz durumları (state) tutan değişkenler ---

    // Yükleme durumunu tutar (ProgressBar göster/gizle)
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Oluşturulan oda kodunu tutar
    private val _roomCode = MutableStateFlow<String?>(null)
    val roomCode = _roomCode.asStateFlow()

    // Odanın anlık durumunu ("waiting", "active" vb.) tutar.
    private val _roomStatus = MutableStateFlow<String?>(null)
    val roomStatus = _roomStatus.asStateFlow()

    // Bir hata oluşursa, hata mesajını tutar
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    /**
     * Filtrelerden yola çıkarak yeni bir düello odası oluşturma sürecini başlatır.
     * @param genres Seçilen türlerin ID listesi.
     * @param startYear Seçilen minimum çıkış yılı.
     */
    fun createRoomFromFilters(genres: IntArray, startYear: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Tür ID'lerini ve yılı bir Map'e koyuyoruz
                val genresString = genres.joinToString(separator = ",")
                val filters = mapOf(
                    "with_genres" to genresString,
                    "primary_release_year.gte" to startYear.toString()
                )

                // 1. MovieRepository'den sonucu alıyoruz (Bu artık bir 'Result' nesnesi)
                val movieResult = movieRepository.getMovies(filters)

                // 2. 'when' ile gelen sonucu kontrol ediyoruz.
                when (movieResult) {
                    is Result.Success -> {
                        // BAŞARILI DURUMU
                        val movies = movieResult.data
                        if (movies.isEmpty()) {
                            throw Exception("Bu filtrelere uygun film bulunamadı.")
                        }

                        val movieDeck = movies.map { it.id }

                        val firebaseFilters =
                            mapOf("genres" to genres.toList(), "startYear" to startYear)

                        // TODO: FirebaseRepository'yi de Result döndürecek şekilde güncelle
                        val generatedRoomCode =
                            firebaseRepository.createRoom(firebaseFilters, movieDeck)

                        _roomCode.value = generatedRoomCode
                        if (generatedRoomCode != null) {
                            // Bu launch bloğu, dinlemeyi ayrı bir coroutine'de başlatır ki
                            // ana işlem akışını engellemesin.
                            viewModelScope.launch {
                                firebaseRepository.listenToRoomStatus(generatedRoomCode).collect { status ->
                                    // Dinleyiciden gelen her yeni 'status' değerini,
                                    // StateFlow'umuza atıyoruz.
                                    _roomStatus.value = status
                                    Log.d("OdaKoduViewModel", "Yeni oda durumu alındı: $status")
                                }
                            }
                        }
                    }

                    is Result.Error -> {
                        // HATA DURUMU
                        // Repository'den gelen hata mesajını doğrudan arayüze iletiyoruz.
                        throw movieResult.exception
                    }

                    is Result.Loading -> {
                        // Yükleniyor durumu (şimdilik bir şey yapmıyoruz)
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Bilinmeyen bir hata oluştu."
            } finally {
                _isLoading.value = false
            }
        }
    }
}