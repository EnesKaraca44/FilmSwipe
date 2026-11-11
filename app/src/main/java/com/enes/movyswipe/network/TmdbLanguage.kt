package com.enes.movyswipe.network

import java.util.Locale

/**
 * TMDB'den film adı/özetinin hangi dilde isteneceği. Arayüzle aynı kural: cihaz Türkçeyse
 * Türkçe, değilse İngilizce (uygulamanın varsayılan dili).
 */
object TmdbLanguage {
    fun current(locale: Locale = Locale.getDefault()): String =
        if (locale.language == "tr") "tr-TR" else "en-US"
}
