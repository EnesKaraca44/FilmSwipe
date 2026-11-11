package com.enes.movyswipe.data

import com.google.gson.annotations.SerializedName

/**
 * TMDB film modeli. Gson, Kotlin null güvenliğini bilmediği için TMDB'nin boş/eksik
 * döndürebileceği alanlar nullable tutuluyor.
 */
data class Movie(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("vote_average") val voteAverage: Double?
) {
    val displayTitle: String get() = title.orEmpty()
    val releaseYear: String? get() = MovieFormat.releaseYear(releaseDate)

    fun toWatchlistMovie(now: Long = System.currentTimeMillis()) = WatchlistMovie(
        id = id,
        title = displayTitle,
        posterPath = posterPath,
        releaseDate = releaseDate.orEmpty(),
        voteAverage = voteAverage ?: 0.0,
        addedTimestamp = now
    )
}

object MovieFormat {
    private const val IMAGE_BASE = "https://image.tmdb.org/t/p/w500"

    /** "2024-05-01" -> "2024". Boş veya hatalı tarihte null döner (çökme yerine). */
    fun releaseYear(date: String?): String? =
        date?.trim()?.takeIf { it.length >= 4 && it.take(4).all(Char::isDigit) }?.take(4)

    fun posterUrl(posterPath: String?): String? =
        posterPath?.takeIf { it.isNotBlank() }?.let { IMAGE_BASE + it }

    fun rating(voteAverage: Double?): String =
        String.format(java.util.Locale.US, "%.1f", voteAverage ?: 0.0)
}
