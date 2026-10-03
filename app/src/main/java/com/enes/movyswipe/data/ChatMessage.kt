package com.enes.movyswipe.data

import com.google.firebase.database.ServerValue

data class ChatMessage(
    val senderId: String = "", // Mesajı gönderenin ID'si
    val senderNickname: String = "Bilinmeyen",
    val text: String = "",     // Mesajın içeriği
    // timestamp'i Long olarak tutmak en iyisidir.
    // ServerValue.TIMESTAMP, mesajın Firebase sunucusuna ulaştığı anın zaman damgasını otomatik ekler.
    val timestamp: Any = ServerValue.TIMESTAMP
) {
    // Firebase için boş constructor
    constructor() : this("","Bilinmeyen", "", 0L)
}