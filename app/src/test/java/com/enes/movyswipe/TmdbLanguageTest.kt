package com.enes.movyswipe

import com.enes.movyswipe.network.TmdbLanguage
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class TmdbLanguageTest {
    @Test
    fun `turkce cihazda turkce, digerlerinde ingilizce`() {
        assertEquals("tr-TR", TmdbLanguage.current(Locale("tr", "TR")))
        assertEquals("en-US", TmdbLanguage.current(Locale.US))
        assertEquals("en-US", TmdbLanguage.current(Locale.GERMANY))
    }
}
