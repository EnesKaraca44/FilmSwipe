package com.enes.movyswipe.viewmodels

import com.enes.movyswipe.data.JoinStatus
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enes.movyswipe.data.repository.FirebaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OdaKatilViewModel: ViewModel() {

    private val firebaseRepository = FirebaseRepository()

    // Odaya katılma durumunu tutan ve dışarıya yayınlayan StateFlow.
    private val _joinStatus=MutableStateFlow<JoinStatus>(JoinStatus.IDLE)
    val joinStatus = _joinStatus.asStateFlow()

    fun joinRoom(roomCode: String){
        viewModelScope.launch {
            // 1. İşlem başlıyor, arayüze "Yükleniyor" durumunu bildir.
            _joinStatus.value = JoinStatus.LOADING

            // guestId'yi şimdilik sabit bir metin yapabiliriz.
            // Daha sonra Firebase Authentication kullanırsak, kullanıcının gerçek ID'sini alırız.
            val guestId = "guest_${System.currentTimeMillis()}"

            // 2. Repository aracılığıyla Firebase'e katılma isteği gönder.
            val isSuccess = firebaseRepository.joinRoom(roomCode, guestId)

            // 3. Gelen sonuca göre durumu güncelle.
            if (isSuccess) {
                // Başarılıysa, arayüze "Başarılı" durumunu bildir.
                _joinStatus.value = JoinStatus.SUCCESS
            } else {
                // Başarısızsa, arayüze hata mesajıyla birlikte "Başarısız" durumunu bildir.
                _joinStatus.value = JoinStatus.FAILURE("Oda kodu geçersiz veya bir hata oluştu.")
            }
        }
    }

}