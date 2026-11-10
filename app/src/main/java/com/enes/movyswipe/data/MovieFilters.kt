package com.enes.movyswipe.data

/** TMDB /discover/movie parametrelerini tek yerde üretir. */
object MovieFilters {

    /** TMDB tür ID'leri (https://developer.themoviedb.org/reference/genre-movie-list). */
    object Genre {
        const val ACTION = 28
        const val COMEDY = 35
        const val DRAMA = 18
        const val SCIENCE_FICTION = 878
        const val HORROR = 27
        const val ROMANCE = 10749
    }

    /**
     * @param genreIds Seçilen türler. TMDB'de virgül "VE", dikey çizgi "VEYA" demektir;
     *                 kullanıcı birden fazla tür seçtiğinde bunlardan herhangi birini istiyor.
     * @param startYear Bu yıldan itibaren çıkan filmler. ('primary_release_year.gte'
     *                 diye bir parametre yok, TMDB onu sessizce yok sayıyor.)
     */
    fun discover(genreIds: Collection<Int>, startYear: Int? = null): Map<String, String> {
        val params = linkedMapOf("sort_by" to "popularity.desc")
        val genres = genreIds.distinct()
        if (genres.isNotEmpty()) params["with_genres"] = genres.joinToString("|")
        if (startYear != null && startYear > 0) params["primary_release_date.gte"] = "$startYear-01-01"
        return params
    }
}
