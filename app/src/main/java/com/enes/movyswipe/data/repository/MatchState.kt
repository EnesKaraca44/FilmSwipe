package com.enes.movyswipe.data.repository

sealed class MatchState {
    object IDLE : MatchState() // Başlangıç durumu, henüz bir sonuç yok.
    data class MATCH_FOUND(val movieId: Int) : MatchState() // Eşleşme bulundu, filmin ID'sini taşıyor.
    object NO_MATCH : MatchState() // Filmler bitti ama eşleşme bulunamadı.
}