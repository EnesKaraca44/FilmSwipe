package com.enes.movyswipe.data

/**
 * Sohbet sıklık sınırı. Asıl sınırı sunucu uygular (functions/chat.js: fazla mesajı siler);
 * uygulama aynı değerlerle kullanıcıyı baştan uyarır ki mesajı gönderilip sonra kaybolmasın.
 */
object ChatRateLimit {
    const val MAX_MESSAGES = 5
    const val WINDOW_MS = 10_000L

    /** @param earlier bu kullanıcının daha önce gönderdiği mesajların zamanları */
    fun isExceeded(earlier: List<Long>, now: Long): Boolean =
        earlier.count { it <= now && it > now - WINDOW_MS } >= MAX_MESSAGES
}
