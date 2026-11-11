package com.enes.movyswipe.data

import java.text.Collator
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/** Listem ekranındaki istatistik kartlarının verisi. */
data class WatchlistStats(
    val total: Int,
    /** Film süresi kaydedilmediği için ortalama süreyle yapılan tahmin. */
    val estimatedHours: Int,
    val thisMonth: Int,
    val addedToday: Int,
    val thisWeek: Int,
    /** Bugün (ya da dün) biten, art arda film eklenen gün sayısı. */
    val streakDays: Int,
    /** Son eklemeden bu yana geçen gün; liste boşsa null. */
    val daysSinceLastAdded: Int?
) {
    companion object {
        val EMPTY = WatchlistStats(0, 0, 0, 0, 0, 0, null)

        /** Ortalama uzun metraj süresi; "≈ N saat" tahmini için. */
        const val AVERAGE_RUNTIME_MINUTES = 120

        private val DAY_MS = TimeUnit.DAYS.toMillis(1)

        fun from(
            movies: List<WatchlistMovie>,
            now: Long,
            timeZone: TimeZone = TimeZone.getDefault(),
            locale: Locale = Locale.getDefault()
        ): WatchlistStats {
            if (movies.isEmpty()) return EMPTY

            val todayStart = Calendar.getInstance(timeZone, locale).apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val weekStart = (todayStart.clone() as Calendar).apply {
                while (get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) add(Calendar.DAY_OF_MONTH, -1)
            }.timeInMillis
            val monthStart = (todayStart.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, 1)
            }.timeInMillis

            val timestamps = movies.map { it.addedTimestamp }
            val today = epochDay(now, timeZone)
            val addedDays = timestamps.mapTo(HashSet()) { epochDay(it, timeZone) }

            var cursor = if (today in addedDays) today else today - 1
            var streak = 0
            while (cursor in addedDays) {
                streak++
                cursor--
            }

            return WatchlistStats(
                total = movies.size,
                estimatedHours = movies.size * AVERAGE_RUNTIME_MINUTES / 60,
                thisMonth = timestamps.count { it >= monthStart },
                addedToday = timestamps.count { it >= todayStart.timeInMillis },
                thisWeek = timestamps.count { it >= weekStart },
                streakDays = streak,
                daysSinceLastAdded = (today - epochDay(timestamps.max(), timeZone)).toInt().coerceAtLeast(0)
            )
        }

        /** Yerel saat dilimine göre 1970'ten bu yana geçen gün. */
        fun epochDay(timestamp: Long, timeZone: TimeZone = TimeZone.getDefault()): Long =
            (timestamp + timeZone.getOffset(timestamp)).floorDiv(DAY_MS)

        /** "Bugün", "Dün", "3 gün önce", "2 hafta önce", "4 ay önce". */
        fun relativeDayLabel(daysAgo: Int): String = when {
            daysAgo <= 0 -> "Bugün"
            daysAgo == 1 -> "Dün"
            daysAgo < 7 -> "$daysAgo gün önce"
            daysAgo < 30 -> "${daysAgo / 7} hafta önce"
            daysAgo < 365 -> "${daysAgo / 30} ay önce"
            else -> "${daysAgo / 365} yıl önce"
        }
    }
}

/** Listem sıralama seçenekleri (filtre butonu). */
enum class WatchlistSort(val label: String) {
    NEWEST("En yeni eklenen"),
    OLDEST("En eski eklenen"),
    TITLE("Ada göre (A-Z)"),
    RATING("En yüksek puan"),
    RELEASE("En yeni çıkan");

    fun sort(movies: List<WatchlistMovie>): List<WatchlistMovie> = when (this) {
        NEWEST -> movies.sortedByDescending { it.addedTimestamp }
        OLDEST -> movies.sortedBy { it.addedTimestamp }
        TITLE -> movies.sortedWith(compareBy(turkishCollator) { it.title })
        RATING -> movies.sortedByDescending { it.voteAverage }
        // Tarihler "yyyy-MM-dd" olduğu için metin sıralaması doğru; boş tarihler sona düşer.
        RELEASE -> movies.sortedByDescending { it.releaseDate }
    }

    companion object {
        private val turkishCollator: Collator = Collator.getInstance(Locale.forLanguageTag("tr-TR"))

        /** Arama metnine göre süzer ve seçilen sıraya dizer. */
        fun apply(movies: List<WatchlistMovie>, query: String, sort: WatchlistSort): List<WatchlistMovie> {
            val trimmed = query.trim()
            val filtered = if (trimmed.isEmpty()) movies
            else movies.filter { it.title.contains(trimmed, ignoreCase = true) }
            return sort.sort(filtered)
        }
    }
}
