package com.enes.movyswipe.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enes.movyswipe.data.JoinResult
import com.enes.movyswipe.data.JoinStatus
import com.enes.movyswipe.data.RoomCodes
import com.enes.movyswipe.data.repository.FirebaseRepository
import com.enes.movyswipe.util.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OdaKatilViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {

    private val _joinStatus = MutableStateFlow<JoinStatus>(JoinStatus.IDLE)
    val joinStatus = _joinStatus.asStateFlow()

    /** Tek seferlik yönlendirme olayı: ekrana geri dönüldüğünde tekrar tetiklenmez. */
    private val _navigateToDuel = Channel<String>(Channel.BUFFERED)
    val navigateToDuel = _navigateToDuel.receiveAsFlow()

    fun joinRoom(rawCode: String) {
        if (_joinStatus.value == JoinStatus.LOADING) return
        val code = RoomCodes.normalize(rawCode)
        viewModelScope.launch {
            _joinStatus.value = JoinStatus.LOADING
            // Sahte "guest_..." kimliği yerine gerçek Firebase UID kullanılıyor.
            val userId = AuthManager.getCurrentUserId()
            val result = if (userId == null) JoinResult.ERROR else firebaseRepository.joinRoom(code, userId)
            if (result == JoinResult.SUCCESS) {
                _joinStatus.value = JoinStatus.IDLE
                _navigateToDuel.send(code)
            } else {
                _joinStatus.value = JoinStatus.FAILURE(messageFor(result))
            }
        }
    }

    fun consumeFailure() {
        if (_joinStatus.value is JoinStatus.FAILURE) _joinStatus.value = JoinStatus.IDLE
    }

    private fun messageFor(result: JoinResult) = when (result) {
        JoinResult.INVALID_CODE -> "Oda kodu 6 karakter olmalı."
        JoinResult.NOT_FOUND -> "Bu kodla bir oda bulunamadı."
        JoinResult.FULL -> "Bu oda dolu veya düello başlamış."
        JoinResult.OWN_ROOM -> "Kendi odana katılamazsın; kodu arkadaşınla paylaş."
        JoinResult.ERROR, JoinResult.SUCCESS -> "Bağlantı hatası. İnternetini kontrol edip tekrar dene."
    }
}
