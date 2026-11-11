package com.enes.movyswipe.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enes.movyswipe.R
import com.enes.movyswipe.data.ChatMessage
import com.enes.movyswipe.data.ChatRateLimit
import com.enes.movyswipe.data.Result
import com.enes.movyswipe.data.repository.FirebaseRepository
import com.enes.movyswipe.util.AuthManager
import com.enes.movyswipe.util.SafetyPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val safetyPrefs: SafetyPrefs,
    @ApplicationContext private val context: Context
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
            val me = AuthManager.getCurrentUserId()
            _currentUserId.value = me
            if (me == null) {
                _toasts.send(context.getString(R.string.chat_sign_in_failed))
                return@launch
            }
            // Karşı tarafı odadan öğren: hiç mesaj yazmamış biri de şikayet edilip engellenebilsin.
            val room = (firebaseRepository.getRoom(code) as? Result.Success)?.data
            partner = listOf(room?.hostId, room?.guestId).firstOrNull { it != null && it != me }
        }
        viewModelScope.launch {
            firebaseRepository.listenToMessages(code)
                .combine(safetyPrefs.blockedUsers) { list, blocked -> list.filter { it.senderId !in blocked } }
                .catch { _toasts.send(context.getString(R.string.chat_messages_load_failed)) }
                .collect { _messages.value = it }
        }
    }

    private var recentSendTimes: List<Long> = emptyList()

    /** @return mesaj gönderime alındıysa true (giriş kutusu temizlenmeli) */
    fun sendMessage(rawText: String, nickname: String): Boolean {
        val code = roomCode ?: return false
        val userId = _currentUserId.value ?: return false
        val text = rawText.trim()
        if (text.isEmpty()) return false
        if (text.length > ChatMessage.MAX_LENGTH) {
            _toasts.trySend(context.getString(R.string.chat_message_too_long, ChatMessage.MAX_LENGTH))
            return false
        }
        // Sunucu sınırı aşan mesajı siler; kullanıcı mesajının kaybolduğunu görmesin diye baştan uyar.
        val now = System.currentTimeMillis()
        if (ChatRateLimit.isExceeded(recentSendTimes, now)) {
            _toasts.trySend(context.getString(R.string.chat_rate_limited))
            return false
        }
        recentSendTimes = recentSendTimes.filter { it > now - ChatRateLimit.WINDOW_MS } + now
        val message = ChatMessage(senderId = userId, senderNickname = nickname, text = text)
        viewModelScope.launch {
            if (!firebaseRepository.sendMessage(code, message)) _toasts.send(context.getString(R.string.chat_send_failed))
        }
        return true
    }

    private var partner: String? = null

    /** Sohbetteki karşı taraf (odanın diğer oyuncusu). Oda henüz yüklenmediyse mesajlardan bulunur. */
    fun partnerId(): String? {
        partner?.let { return it }
        val me = _currentUserId.value
        return _messages.value.lastOrNull { it.senderId != me }?.senderId
    }

    fun report(reportedUserId: String, reason: String, message: ChatMessage?) {
        val code = roomCode ?: return
        val me = _currentUserId.value ?: return
        viewModelScope.launch {
            val ok = firebaseRepository.reportUser(code, me, reportedUserId, reason, message?.text)
            _toasts.send(context.getString(if (ok) R.string.chat_report_sent else R.string.chat_report_failed))
        }
    }

    /**
     * Engel hem cihazda (mesajları anında gizlemek için) hem sunucuda tutulur. Sunucudaki kayıt
     * sayesinde eşleştirme iki yönde de bu kişiyi atlar ve kurallar onun bu odaya yazmasını reddeder.
     * Bağlantı yoksa yazma Firebase tarafından sıraya alınır; kaybolursa hızlı düello başlarken
     * [FirebaseRepository.syncBlocks] cihazdaki listeyi yeniden gönderir.
     */
    fun block(userId: String) {
        safetyPrefs.block(userId)
        _toasts.trySend(context.getString(R.string.chat_user_blocked))
        viewModelScope.launch {
            val me = _currentUserId.value ?: AuthManager.getCurrentUserId() ?: return@launch
            firebaseRepository.blockUser(me, userId)
        }
    }
}
