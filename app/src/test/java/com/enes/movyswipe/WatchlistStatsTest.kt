package com.enes.movyswipe

import com.enes.movyswipe.data.WatchlistMovie
import com.enes.movyswipe.data.WatchlistSort
import com.enes.movyswipe.data.WatchlistStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class WatchlistStatsTest {

    private val istanbul = TimeZone.getTimeZone("Europe/Istanbul")
    private val turkish = Locale.forLanguageTag("tr-TR")

    /** 11 Kasım 2026 Çarşamba 12:00 (İstanbul). */
    private val now = at(2026, Calendar.NOVEMBER, 11, 12)

    private fun at(year: Int, month: Int, day: Int, hour: Int): Long =
        Calendar.getInstance(istanbul).apply {
            clear()
            set(year, month, day, hour, 0, 0)
        }.timeInMillis

    private fun movie(id: Int, added: Long, title: String = "Film $id", vote: Double = 7.0, release: String = "2020-01-01") =
        WatchlistMovie(id = id, title = title, posterPath = null, releaseDate = release, voteAverage = vote, addedTimestamp = added)

    private fun stats(vararg movies: WatchlistMovie, locale: Locale = turkish) =
        WatchlistStats.from(movies.toList(), now, istanbul, locale)

    @Test
    fun emptyList_returnsEmptyStats() {
        val result = WatchlistStats.from(emptyList(), now, istanbul, turkish)
        assertEquals(WatchlistStats.EMPTY, result)
        assertNull(result.daysSinceLastAdded)
    }

    @Test
    fun countsTodayWeekAndMonthByCalendar() {
        val result = stats(
            movie(1, at(2026, Calendar.NOVEMBER, 11, 9)),   // bugün
            movie(2, at(2026, Calendar.NOVEMBER, 9, 10)),   // bu haftanın pazartesisi
            movie(3, at(2026, Calendar.NOVEMBER, 1, 8)),    // bu ay, geçen hafta
            movie(4, at(2026, Calendar.OCTOBER, 31, 20))    // geçen ay
        )
        assertEquals(4, result.total)
        assertEquals(1, result.addedToday)
        assertEquals(2, result.thisWeek)
        assertEquals(3, result.thisMonth)
        assertEquals(8, result.estimatedHours)
        assertEquals(0, result.daysSinceLastAdded)
    }

    @Test
    fun weekStartFollowsLocale() {
        // 8 Kasım Pazar: ABD'de haftanın ilk günü, Türkiye'de geçen haftanın son günü.
        val sunday = movie(1, at(2026, Calendar.NOVEMBER, 8, 15))
        assertEquals(0, stats(sunday, locale = turkish).thisWeek)
        assertEquals(1, stats(sunday, locale = Locale.US).thisWeek)
    }

    @Test
    fun streakCountsConsecutiveDaysEndingToday() {
        val result = stats(
            movie(1, at(2026, Calendar.NOVEMBER, 11, 1)),
            movie(2, at(2026, Calendar.NOVEMBER, 10, 23)),
            movie(3, at(2026, Calendar.NOVEMBER, 10, 8)),
            movie(4, at(2026, Calendar.NOVEMBER, 9, 0)),
            movie(5, at(2026, Calendar.NOVEMBER, 7, 12))   // boşluktan sonra sayılmaz
        )
        assertEquals(3, result.streakDays)
    }

    @Test
    fun streakStillActiveWhenLastAddedYesterday() {
        val result = stats(
            movie(1, at(2026, Calendar.NOVEMBER, 10, 9)),
            movie(2, at(2026, Calendar.NOVEMBER, 9, 9))
        )
        assertEquals(2, result.streakDays)
        assertEquals(1, result.daysSinceLastAdded)
    }

    @Test
    fun streakBrokenAfterMissedDay() {
        val result = stats(movie(1, at(2026, Calendar.NOVEMBER, 9, 9)))
        assertEquals(0, result.streakDays)
        assertEquals(2, result.daysSinceLastAdded)
    }

    @Test
    fun relativeDayLabels() {
        assertEquals("Bugün", WatchlistStats.relativeDayLabel(0))
        assertEquals("Dün", WatchlistStats.relativeDayLabel(1))
        assertEquals("5 gün önce", WatchlistStats.relativeDayLabel(5))
        assertEquals("2 hafta önce", WatchlistStats.relativeDayLabel(14))
        assertEquals("3 ay önce", WatchlistStats.relativeDayLabel(95))
        assertEquals("1 yıl önce", WatchlistStats.relativeDayLabel(400))
    }

    @Test
    fun searchIsCaseInsensitiveIncludingTurkishLetters() {
        val movies = listOf(
            movie(1, now, title = "İstanbul Hatırası"),
            movie(2, now, title = "Matrix")
        )
        assertEquals(listOf(1), WatchlistSort.apply(movies, "  istanbul ", WatchlistSort.NEWEST).map { it.id })
        assertEquals(listOf(2), WatchlistSort.apply(movies, "MATR", WatchlistSort.NEWEST).map { it.id })
        assertEquals(2, WatchlistSort.apply(movies, "", WatchlistSort.NEWEST).size)
    }

    @Test
    fun sortOptions() {
        val movies = listOf(
            movie(1, at(2026, Calendar.NOVEMBER, 1, 9), title = "Şahsiyet", vote = 8.9, release = "2018-03-01"),
            movie(2, at(2026, Calendar.NOVEMBER, 3, 9), title = "Çiçek Abbas", vote = 7.5, release = ""),
            movie(3, at(2026, Calendar.NOVEMBER, 2, 9), title = "Zindan", vote = 6.1, release = "2024-05-10"),
            movie(4, at(2026, Calendar.NOVEMBER, 4, 9), title = "Ayla", vote = 7.9, release = "2017-10-27")
        )
        fun order(sort: WatchlistSort) = WatchlistSort.apply(movies, "", sort).map { it.id }

        assertEquals(listOf(4, 2, 3, 1), order(WatchlistSort.NEWEST))
        assertEquals(listOf(1, 3, 2, 4), order(WatchlistSort.OLDEST))
        // Türkçe alfabe: A, Ç, Ş, Z
        assertEquals(listOf(4, 2, 1, 3), order(WatchlistSort.TITLE))
        assertEquals(listOf(1, 4, 2, 3), order(WatchlistSort.RATING))
        // Tarihi olmayan film en sona
        assertEquals(listOf(3, 1, 4, 2), order(WatchlistSort.RELEASE))
    }
}
