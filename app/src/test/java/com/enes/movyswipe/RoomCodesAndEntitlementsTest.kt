package com.enes.movyswipe

import com.enes.movyswipe.billing.Entitlements
import com.enes.movyswipe.billing.Entitlements.OwnedProduct
import com.enes.movyswipe.data.RoomCodes
import com.enes.movyswipe.util.AuthManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomCodesAndEntitlementsTest {

    @Test
    fun `uretilen oda kodlari gecerli ve karistirilabilir karakter icermez`() {
        repeat(2_000) {
            val code = RoomCodes.generate()
            assertTrue(code, RoomCodes.isValid(code))
            assertFalse(code, code.any { it in "01OI" })
        }
    }

    @Test
    fun `oda kodlari yeterince rastgele`() {
        val codes = (1..5_000).map { RoomCodes.generate() }.toSet()
        assertTrue("çakışma çok fazla: ${codes.size}", codes.size >= 4_995)
    }

    @Test
    fun `kullanicinin yazdigi kod normalize edilir`() {
        assertEquals("AB12CD", RoomCodes.normalize(" ab12 cd "))
        assertTrue(RoomCodes.isAcceptable("AB12CD"))
        assertFalse(RoomCodes.isAcceptable("AB12C"))
        assertFalse(RoomCodes.isAcceptable("AB-2CD"))
    }

    @Test
    fun `sadece satin alinmis remove_ads urunu premium yapar`() {
        assertTrue(Entitlements.hasRemoveAds(listOf(OwnedProduct(listOf("remove_ads"), purchased = true))))
        assertFalse(Entitlements.hasRemoveAds(listOf(OwnedProduct(listOf("remove_ads"), purchased = false))))
        assertFalse(Entitlements.hasRemoveAds(listOf(OwnedProduct(listOf("baska_urun"), purchased = true))))
        // İade edilen / Play'de olmayan satın alım: liste boş -> premium kalkar.
        assertFalse(Entitlements.hasRemoveAds(emptyList()))
    }

    @Test
    fun `kullanici adi kurallari`() {
        assertEquals("Film Sever", AuthManager.normalizeNickname("  Film    Sever "))
        assertNull(AuthManager.normalizeNickname("ab"))
        assertNull(AuthManager.normalizeNickname("x".repeat(21)))
        assertEquals("abc", AuthManager.normalizeNickname("abc"))
    }
}
