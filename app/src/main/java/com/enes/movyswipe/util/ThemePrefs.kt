package com.enes.movyswipe.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/** Ayarlar'daki "Koyu Tema" anahtarı. Varsayılan: sistem temasını takip et. */
object ThemePrefs {
    private const val PREFS = "FilmSwipeTheme"
    private const val KEY_DARK = "dark_mode"

    fun isDarkSelected(context: Context): Boolean? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return if (prefs.contains(KEY_DARK)) prefs.getBoolean(KEY_DARK, false) else null
    }

    fun setDark(context: Context, dark: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_DARK, dark).apply()
        apply(context)
    }

    fun apply(context: Context) {
        val mode = when (isDarkSelected(context)) {
            true -> AppCompatDelegate.MODE_NIGHT_YES
            false -> AppCompatDelegate.MODE_NIGHT_NO
            null -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        if (AppCompatDelegate.getDefaultNightMode() != mode) AppCompatDelegate.setDefaultNightMode(mode)
    }
}
