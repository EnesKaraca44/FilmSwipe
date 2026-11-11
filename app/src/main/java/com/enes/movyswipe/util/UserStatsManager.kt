package com.enes.movyswipe.util

import android.content.Context

data class UserStats(
    val duels: Int,
    val matches: Int,
    val compatibilityRate: Int
)

object UserStatsManager {
    private const val PREFS_NAME = "FilmSwipePrefs"
    private const val KEY_DUEL_COUNT = "stats_duel_count"
    private const val KEY_MATCH_COUNT = "stats_match_count"
    private const val KEY_LAST_RECORDED_ROOM = "stats_last_room"

    fun getStats(context: Context): UserStats {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
}
