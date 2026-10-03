package com.enes.movyswipe.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist_movies")
data class WatchlistMovie(
    @PrimaryKey val id: Int, // Film ID'sini birincil anahtar yapıyoruz
    val title: String,
    val posterPath: String?,
    val releaseDate: String,
    val voteAverage: Double,
    val addedTimestamp: Long = System.currentTimeMillis() // Ekleme zamanı
)