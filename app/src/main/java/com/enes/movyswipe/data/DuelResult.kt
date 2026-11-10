package com.enes.movyswipe.data

/** Deste bittiğinde eşleşme olup olmadığına karar veren saf mantık. */
object DuelResult {
    /**
     * @return true ise "eşleşme yok" denebilir: herkes desteyi bitirmiş ve eşleşme yazılmamış.
     */
    fun isNoMatch(participants: Set<String>, finished: Set<String>, match: Int?): Boolean =
        match == null && participants.isNotEmpty() && finished.containsAll(participants)
}
