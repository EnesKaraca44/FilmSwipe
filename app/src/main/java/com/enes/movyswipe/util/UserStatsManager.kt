package com.enes.movyswipe.util

import android.content.Context
import android.content.SharedPreferences

data class UserStats(
    val duels: Int,
    val matches: Int,
    val compatibilityRate: Int
)

/**
 * Düello / eşleşme istatistikleri. İzleme listesi gibi cihaza aittir ve çıkış yapınca silinmez;
 * bu yüzden [AuthManager.signOut]'un temizlediği "FilmSwipePrefs" yerine ayrı bir dosyada tutulur.
 */
object UserStatsManager {
    private const val PREFS_NAME = "FilmSwipeStats"
    /** İlk sürüm istatistikleri burada tutuyordu; çıkışta silindikleri için taşındılar. */
    private const val LEGACY_PREFS_NAME = "FilmSwipePrefs"
    private const val KEY_DUEL_COUNT = "stats_duel_count"
    private const val KEY_MATCH_COUNT = "stats_match_count"
    private const val KEY_LAST_RECORDED_ROOM = "stats_last_room"
    private const val KEY_MIGRATED = "migrated_from_legacy"

    fun getStats(context: Context): UserStats {
        val prefs = prefs(context)
        val duels = prefs.getInt(KEY_DUEL_COUNT, 0)
        val matches = prefs.getInt(KEY_MATCH_COUNT, 0)
        val compatibilityRate = if (duels > 0) {
            ((matches.toDouble() / duels) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
        return UserStats(
            duels = duels,
            matches = matches,
            compatibilityRate = compatibilityRate
        )
    }

    @Synchronized
    fun recordDuel(context: Context, roomCode: String?, isMatch: Boolean) {
        if (roomCode.isNullOrBlank()) return
        val prefs = prefs(context)
        val lastRoom = prefs.getString(KEY_LAST_RECORDED_ROOM, null)
        if (lastRoom == roomCode) return

        val currentDuels = prefs.getInt(KEY_DUEL_COUNT, 0) + 1
        val currentMatches = prefs.getInt(KEY_MATCH_COUNT, 0) + if (isMatch) 1 else 0

        prefs.edit()
            .putString(KEY_LAST_RECORDED_ROOM, roomCode)
            .putInt(KEY_DUEL_COUNT, currentDuels)
            .putInt(KEY_MATCH_COUNT, currentMatches)
            .apply()
    }

    /** İstatistik dosyası; ilk erişimde eski dosyadaki değerler bir kez buraya taşınır. */
    @Synchronized
    private fun prefs(context: Context): SharedPreferences {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_MIGRATED, false)) return prefs

        val legacy = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        if (legacy.contains(KEY_DUEL_COUNT)) {
            editor.putInt(KEY_DUEL_COUNT, legacy.getInt(KEY_DUEL_COUNT, 0))
            editor.putInt(KEY_MATCH_COUNT, legacy.getInt(KEY_MATCH_COUNT, 0))
            legacy.getString(KEY_LAST_RECORDED_ROOM, null)?.let { editor.putString(KEY_LAST_RECORDED_ROOM, it) }
            legacy.edit()
                .remove(KEY_DUEL_COUNT)
                .remove(KEY_MATCH_COUNT)
                .remove(KEY_LAST_RECORDED_ROOM)
                .apply()
        }
        editor.putBoolean(KEY_MIGRATED, true).commit()
        return prefs
    }
}
