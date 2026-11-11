package com.enes.movyswipe.data

import com.google.firebase.database.Exclude
import com.google.firebase.database.ServerValue

data class ChatMessage(
    val senderId: String = "",
    val senderNickname: String = "Bilinmeyen",
    val text: String = "",
    // Yazarken ServerValue.TIMESTAMP (Map), okurken Long gelir.
    val timestamp: Any = ServerValue.TIMESTAMP
) {
    constructor() : this("", "Bilinmeyen", "", 0L)

    /** Firebase'deki push anahtarı; veritabanına yazılmaz. */
    @get:Exclude
    @set:Exclude
    var id: String = ""

    @get:Exclude
    val timestampMillis: Long?
        get() = (timestamp as? Number)?.toLong()

    companion object {
        const val MAX_LENGTH = 500
    }
}
