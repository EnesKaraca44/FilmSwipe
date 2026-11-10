package com.enes.movyswipe.data

import java.security.SecureRandom

object RoomCodes {
    /** Karıştırılabilen karakterler (0/O, 1/I) çıkarıldı. 32^6 ≈ 1 milyar kombinasyon. */
    const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    const val LENGTH = 6
    private val random = SecureRandom()

    fun generate(): String = buildString(LENGTH) {
        repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
    }

    /** Kullanıcının yazdığı kodu büyük harfe çevirir, boşlukları atar. */
    fun normalize(input: String): String = input.filterNot(Char::isWhitespace).uppercase(java.util.Locale.ROOT)

    fun isValid(code: String): Boolean = code.length == LENGTH && code.all { it in ALPHABET }

    /** Eski sürümlerin ürettiği kodlar (A-Z, 0-9) da kabul edilir. */
    fun isAcceptable(code: String): Boolean = code.length == LENGTH && code.all { it in 'A'..'Z' || it in '0'..'9' }
}
