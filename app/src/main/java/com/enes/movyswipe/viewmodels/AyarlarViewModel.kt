package com.enes.movyswipe.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import com.enes.movyswipe.ads.AdsManager
import com.enes.movyswipe.billing.BillingManager
import com.enes.movyswipe.util.AuthManager
import com.enes.movyswipe.util.UserStats
import com.enes.movyswipe.util.UserStatsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AyarlarViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    billingManager: BillingManager,
    adsManager: AdsManager
) : ViewModel() {

    val isPremium = billingManager.isPremium
    val privacyOptionsRequired = adsManager.privacyOptionsRequired

    private val _nickname = MutableStateFlow(AuthManager.getUserNickname(context))
    val nickname: StateFlow<String> = _nickname.asStateFlow()

    private val _userStats = MutableStateFlow(UserStatsManager.getStats(context))
    val userStats: StateFlow<UserStats> = _userStats.asStateFlow()

    fun refreshData() {
        _nickname.value = AuthManager.getUserNickname(context)
        _userStats.value = UserStatsManager.getStats(context)
    }

    fun getCurrentNickname(): String = _nickname.value

    /** @return kaydedilen ad; geçersizse null */
    fun saveNewNickname(raw: String): String? {
        val saved = AuthManager.normalizeNickname(raw)?.also {
            AuthManager.saveUserNickname(context, it)
            _nickname.value = it
        }
        return saved
    }

    fun getEmailForNickname(nickname: String): String {
        val turkishToEnglish = mapOf(
            'ç' to 'c', 'Ç' to 'c',
            'ğ' to 'g', 'Ğ' to 'g',
            'ı' to 'i', 'I' to 'i', 'İ' to 'i',
            'ö' to 'o', 'Ö' to 'o',
            'ş' to 's', 'Ş' to 's',
            'ü' to 'u', 'Ü' to 'u'
        )
        val filtered = buildString {
            for (ch in nickname.lowercase(Locale("tr", "TR"))) {
                val mapped = turkishToEnglish[ch] ?: ch
                if (mapped in 'a'..'z' || mapped in '0'..'9') {
                    append(mapped)
                }
            }
        }
        val clean = if (filtered.isEmpty()) "kullanici" else filtered
        return "$clean@filmswipe.app"
    }

    fun signOut() = AuthManager.signOut(context)
}
