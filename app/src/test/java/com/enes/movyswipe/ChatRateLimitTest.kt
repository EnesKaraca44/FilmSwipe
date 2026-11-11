package com.enes.movyswipe

import com.enes.movyswipe.data.ChatRateLimit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRateLimitTest {
    private val now = 1_000_000L

    @Test
    fun `pencerede sinirdan az mesaj varsa izin verilir`() {
        val earlier = List(ChatRateLimit.MAX_MESSAGES - 1) { now - it * 100L }
        assertFalse(ChatRateLimit.isExceeded(earlier, now))
    }

    @Test
    fun `pencerede sinir kadar mesaj varsa yenisi reddedilir`() {
        val earlier = List(ChatRateLimit.MAX_MESSAGES) { now - it * 100L }
        assertTrue(ChatRateLimit.isExceeded(earlier, now))
    }

    @Test
    fun `pencereden eski mesajlar sayilmaz`() {
        val old = List(10) { now - ChatRateLimit.WINDOW_MS - it }
        assertFalse(ChatRateLimit.isExceeded(old, now))
        assertFalse(ChatRateLimit.isExceeded(emptyList(), now))
    }
}
