package com.enes.movyswipe.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enes.movyswipe.R
import com.enes.movyswipe.data.JoinResult
import com.enes.movyswipe.data.JoinStatus
import com.enes.movyswipe.data.RoomCodes
import com.enes.movyswipe.data.repository.FirebaseRepository
import com.enes.movyswipe.util.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OdaKatilViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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

    private fun messageFor(result: JoinResult): String = context.getString(when (result) {
        JoinResult.INVALID_CODE -> R.string.duel_join_error_invalid
        JoinResult.NOT_FOUND -> R.string.duel_join_error_not_found
        JoinResult.FULL -> R.string.duel_join_error_full
        JoinResult.OWN_ROOM -> R.string.duel_join_error_own_room
        JoinResult.ERROR, JoinResult.SUCCESS -> R.string.duel_join_error_connection
    })
}
