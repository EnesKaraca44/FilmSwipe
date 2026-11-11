package com.enes.movyswipe.data

/**
 * Hızlı düello kuyruğuna kayıtla birlikte gönderilen deste. Eşleşmede daha uzun bekleyen
 * tarafın destesiyle oda kurulur (functions/index.js: pairFromQueue).
 */
data class QueueDeck(val genreName: String, val genreId: Int, val movieIds: List<Int>) {
    companion object {
        /** database.rules.json ve functions/match.js ile aynı sınır. */
        const val MAX_SIZE = 40
    }
}
