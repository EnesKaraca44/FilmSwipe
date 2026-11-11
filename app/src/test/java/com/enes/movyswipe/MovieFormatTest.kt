package com.enes.movyswipe

import com.enes.movyswipe.data.Movie
import com.enes.movyswipe.data.MovieFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MovieFormatTest {

    @Test
    fun `yil normal tarihten alinir`() {
        assertEquals("2024", MovieFormat.releaseYear("2024-05-01"))
    }

    @Test
    fun `bos veya hatali tarih cokmez null doner`() {
        // Eskiden "".substring(0, 4) StringIndexOutOfBoundsException fırlatıyordu.
        assertNull(MovieFormat.releaseYear(""))
        assertNull(MovieFormat.releaseYear(null))
        assertNull(MovieFormat.releaseYear("20"))
        assertNull(MovieFormat.releaseYear("abcd-01-01"))
        assertNull(MovieFormat.releaseYear("   "))
    }

    @Test
    fun `poster yolu yoksa url uretilmez`() {
        assertNull(MovieFormat.posterUrl(null))
        assertNull(MovieFormat.posterUrl(""))
        assertEquals("https://image.tmdb.org/t/p/w500/abc.jpg", MovieFormat.posterUrl("/abc.jpg"))
    }

    @Test
    fun `puan tek ondalikla gosterilir`() {
        assertEquals("7.3", MovieFormat.rating(7.345))
        assertEquals("0.0", MovieFormat.rating(null))
    }

    @Test
    fun `eksik alanli film izleme listesine guvenle donusur`() {
        val movie = Movie(id = 5, title = null, overview = null, posterPath = null, releaseDate = null, voteAverage = null)
        val w = movie.toWatchlistMovie(now = 1000L)
        assertEquals(5, w.id)
        assertEquals("", w.title)
        assertEquals("", w.releaseDate)
        assertEquals(0.0, w.voteAverage, 0.0)
        assertEquals(1000L, w.addedTimestamp)
        assertNull(movie.releaseYear)
    }
}
