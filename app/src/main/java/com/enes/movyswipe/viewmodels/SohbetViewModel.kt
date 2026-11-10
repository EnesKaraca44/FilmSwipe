package com.enes.movyswipe.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enes.movyswipe.data.ChatMessage
import com.enes.movyswipe.data.repository.FirebaseRepository
import com.enes.movyswipe.util.AuthManager
import com.enes.movyswipe.util.SafetyPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SohbetViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val safetyPrefs: SafetyPrefs
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    /** Engellenen kullanıcıların mesajları gösterilmez. */
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _toasts = Channel<String>(Channel.BUFFERED)
    val toasts = _toasts.receiveAsFlow()

    val communityRulesAccepted: Boolean get() = safetyPrefs.communityRulesAccepted
    fun acceptCommunityRules() {
        safetyPrefs.communityRulesAccepted = true
    }

    private var roomCode: String? = null

    /** Dinleyiciyi tek kez başlatır (eskiden her ekran oluşumunda iki dinleyici ekleniyordu). */
    fun start(code: String) {
        if (roomCode != null) return
        roomCode = code
        viewModelScope.launch {
            _currentUserId.value = AuthManager.getCurrentUserId()
            if (_currentUserId.value == null) _toasts.send("Giriş yapılamadı. İnternet bağlantını kontrol et.")
        }
        viewModelScope.launch {
            firebaseRepository.listenToMessages(code)
                .combine(safetyPrefs.blockedUsers) { list, blocked -> list.filter { it.senderId !in blocked } }
                .catch { _toasts.send("Mesajlar yüklenemedi.") }
                .collect { _messages.value = it }
        }
    }

    /** @return mesaj gönderime alındıysa true (giriş kutusu temizlenmeli) */
    fun sendMessage(rawText: String, nickname: String): Boolean {
        val code = roomCode ?: return false
        val userId = _currentUserId.value ?: return false
        val text = rawText.trim()
        if (text.isEmpty()) return false
        if (text.length > ChatMessage.MAX_LENGTH) {
            _toasts.trySend("Mesaj en fazla ${ChatMessage.MAX_LENGTH} karakter olabilir.")
            return false
        }
        val message = ChatMessage(senderId = userId, senderNickname = nickname, text = text)
        viewModelScope.launch {
            if (!firebaseRepository.sendMessage(code, message)) _toasts.send("Mesaj gönderilemedi.")
        }
        return true
    }

    /** Sohbetteki karşı taraf: mesaj atmış, ben olmayan kullanıcı. */
    fun partnerId(): String? {
        val me = _currentUserId.value
        return _messages.value.lastOrNull { it.senderId != me }?.senderId
    }

    fun report(reportedUserId: String, reason: String, message: ChatMessage?) {
        val code = roomCode ?: return
        val me = _currentUserId.value ?: return
        viewModelScope.launch {
            val ok = firebaseRepository.reportUser(code, me, reportedUserId, reason, message?.text)
            _toasts.send(if (ok) "Şikayetin alındı. İncelenecek." else "Şikayet gönderilemedi, tekrar dene.")
        }
    }

    fun block(userId: String) {
        safetyPrefs.block(userId)
        _toasts.trySend("Kullanıcı engellendi. Mesajlarını görmeyecek ve onunla eşleşmeyeceksin.")
    }
}
