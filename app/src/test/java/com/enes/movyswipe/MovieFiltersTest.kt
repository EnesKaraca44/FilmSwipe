package com.enes.movyswipe

import com.enes.movyswipe.data.MovieFilters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MovieFiltersTest {

    @Test
    fun `birden fazla tur VEYA ile birlestirilir`() {
        val params = MovieFilters.discover(listOf(28, 35, 27))
        assertEquals("28|35|27", params["with_genres"])
    }

    @Test
    fun `bilim kurgu dogru TMDB ID ile gonderilir`() {
        assertEquals(878, MovieFilters.Genre.SCIENCE_FICTION)
        assertEquals("878", MovieFilters.discover(listOf(MovieFilters.Genre.SCIENCE_FICTION))["with_genres"])
    }

    @Test
    fun `yil filtresi TMDB nin tanidigi parametreyle gonderilir`() {
        val params = MovieFilters.discover(emptyList(), 2015)
        assertEquals("2015-01-01", params["primary_release_date.gte"])
        assertFalse(params.containsKey("primary_release_year.gte"))
    }

    @Test
    fun `tur ve yil yoksa sadece siralama gonderilir`() {
        assertEquals(mapOf("sort_by" to "popularity.desc"), MovieFilters.discover(emptyList(), 0))
        assertEquals(mapOf("sort_by" to "popularity.desc"), MovieFilters.discover(emptyList(), null))
    }

    @Test
    fun `ayni tur iki kez secilse de bir kez gonderilir`() {
        assertEquals("28|35", MovieFilters.discover(listOf(28, 35, 28))["with_genres"])
    }
}
