package com.enes.movyswipe

import com.enes.movyswipe.data.DuelResult
import com.enes.movyswipe.data.MatchmakingLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchmakingLogicTest {

    @Test
    fun `en eski bekleyen rakip secilir`() {
        val queue = mapOf("me" to 300L, "b" to 200L, "c" to 100L)
        assertEquals("c", MatchmakingLogic.pickOpponent(queue, "me"))
    }

    @Test
    fun `ben kuyrukta degilsem kimseyi secmem`() {
        // Başka biri beni zaten eşleştirip kuyruktan çıkardıysa ikinci bir eşleşme kurulmamalı.
        assertNull(MatchmakingLogic.pickOpponent(mapOf("b" to 1L, "c" to 2L), "me"))
    }

    @Test
    fun `kuyrukta tek basimaysam rakip yok`() {
        assertNull(MatchmakingLogic.pickOpponent(mapOf("me" to 1L), "me"))
    }

    @Test
    fun `engellenen kullanici secilmez`() {
        val queue = mapOf("me" to 3L, "bad" to 1L, "good" to 2L)
        assertEquals("good", MatchmakingLogic.pickOpponent(queue, "me", blocked = setOf("bad")))
        assertNull(MatchmakingLogic.pickOpponent(mapOf("me" to 3L, "bad" to 1L), "me", setOf("bad")))
    }

    @Test
    fun `zaman damgasi olmayan kayitlar en sona kalir ve sonuc deterministiktir`() {
        val queue = mapOf("me" to 5L, "z" to null, "y" to null, "x" to 9L)
        assertEquals("x", MatchmakingLogic.pickOpponent(queue, "me"))
        assertEquals("y", MatchmakingLogic.pickOpponent(mapOf("me" to 1L, "z" to null, "y" to null), "me"))
    }

    @Test
    fun `iki taraf ayni kuyrugu gorse de tutarli ciftler olusur`() {
        // A, B ve C aynı kuyruğu görüyor; her biri farklı rakip seçebilir ama transaction
        // yalnızca birinin (kendisi + rakibi) kaydını silmesine izin verir. Burada seçimlerin
        // her zaman kuyrukta olan ve kendisi olmayan biri olduğunu doğruluyoruz.
        val queue = mapOf("a" to 1L, "b" to 2L, "c" to 3L)
        for (me in queue.keys) {
            val opp = MatchmakingLogic.pickOpponent(queue, me)
            assertTrue(opp != null && opp != me && opp in queue)
        }
    }

    @Test
    fun `eslesme yok kararı iki taraf da bitirince verilir`() {
        val both = setOf("host", "guest")
        assertFalse(DuelResult.isNoMatch(both, finished = setOf("host"), match = null))
        assertTrue(DuelResult.isNoMatch(both, finished = setOf("host", "guest"), match = null))
        assertFalse(DuelResult.isNoMatch(both, finished = setOf("host", "guest"), match = 550))
        assertFalse(DuelResult.isNoMatch(emptySet(), finished = setOf("host"), match = null))
    }
}
