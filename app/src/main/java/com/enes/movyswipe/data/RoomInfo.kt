package com.enes.movyswipe.data

/** Bir düello odasının okunan özeti. */
data class RoomInfo(
    val code: String,
    val status: String,
    val hostId: String?,
    val guestId: String?,
    val genreName: String?,
    val movieDeck: List<Int>
) {
    val participants: Set<String> get() = setOfNotNull(hostId, guestId)
}

enum class JoinResult { SUCCESS, NOT_FOUND, FULL, OWN_ROOM, INVALID_CODE, ERROR }
