package com.enes.movyswipe.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import com.enes.movyswipe.ads.AdsManager
import com.enes.movyswipe.billing.BillingManager
import com.enes.movyswipe.util.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class AyarlarViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    billingManager: BillingManager,
    adsManager: AdsManager
) : ViewModel() {

    val isPremium = billingManager.isPremium
    val privacyOptionsRequired = adsManager.privacyOptionsRequired

    fun getCurrentNickname(): String = AuthManager.getUserNickname(context)

    /** @return kaydedilen ad; geçersizse null */
    fun saveNewNickname(raw: String): String? =
        AuthManager.normalizeNickname(raw)?.also { AuthManager.saveUserNickname(context, it) }

    fun signOut() = AuthManager.signOut(context)
}
