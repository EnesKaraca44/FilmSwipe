package com.enes.movyswipe.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enes.movyswipe.data.ChatMessage
import com.enes.movyswipe.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SohbetViewModel: ViewModel() {

    private val firebaseRepository=FirebaseRepository()
    private val _messages= MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    fun startListeningToMessages(roomCode: String){
        viewModelScope.launch {
            firebaseRepository.listenToMessages(roomCode).collect{
                _messages.value=it
            }

        }
    }
    fun sendMessage(roomCode: String, text: String,userId: String,nickname: String) {
        if (text.isNotBlank()) {
            // --- DEĞİŞİKLİK BURADA ---
            // Fragment'tan gelen metin ile bir ChatMessage nesnesi oluşturuyoruz.
            val message = ChatMessage(
                senderId = userId, // TODO: Bu ID'yi ileride dinamik olarak alacağız.
                text = text,
                senderNickname = nickname
                // timestamp otomatik olarak ServerValue.TIMESTAMP ile eklenecek.
            )
            // Oluşturduğumuz nesneyi Repository'ye gönderiyoruz.
            viewModelScope.launch {
                firebaseRepository.sendMessage(roomCode, message)
            }
            // ------------------------
        }
    }
}