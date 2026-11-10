package com.enes.movyswipe.data

/** Hızlı düello eşleştirmesinin saf (Firebase'den bağımsız, test edilebilir) kısmı. */
object MatchmakingLogic {

    /**
     * Kuyrukta beni bekleyen en eski rakibi seçer.
     * @param queue uid -> kuyruğa giriş zamanı (bilinmiyorsa null)
     * @return Rakip uid'si; ben kuyrukta değilsem veya uygun rakip yoksa null.
     */
    fun pickOpponent(queue: Map<String, Long?>, me: String, blocked: Set<String> = emptySet()): String? {
        if (me !in queue) return null
        return queue.entries
            .asSequence()
            .filter { it.key != me && it.key !in blocked }
            .sortedWith(compareBy<Map.Entry<String, Long?>> { it.value ?: Long.MAX_VALUE }.thenBy { it.key })
            .firstOrNull()
            ?.key
    }
}
