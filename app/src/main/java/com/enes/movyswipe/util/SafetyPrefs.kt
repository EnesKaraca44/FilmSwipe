package com.enes.movyswipe.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kullanıcı güvenliği tercihleri (engellenenler, topluluk kuralları onayı).
 * Çıkış yapınca silinmez: engellemek kişiyi korumak içindir.
 */
@Singleton
class SafetyPrefs @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("FilmSwipeSafety", Context.MODE_PRIVATE)

    private val _blockedUsers = MutableStateFlow(prefs.getStringSet(KEY_BLOCKED, emptySet())!!.toSet())
    val blockedUsers: StateFlow<Set<String>> = _blockedUsers.asStateFlow()

    fun block(userId: String) {
        val updated = _blockedUsers.value + userId
        _blockedUsers.value = updated
        prefs.edit().putStringSet(KEY_BLOCKED, updated).apply()
    }

    var communityRulesAccepted: Boolean
        get() = prefs.getBoolean(KEY_RULES, false)
        set(value) = prefs.edit().putBoolean(KEY_RULES, value).apply()

    private companion object {
        const val KEY_BLOCKED = "blocked_users"
        const val KEY_RULES = "community_rules_accepted"
    }
}
