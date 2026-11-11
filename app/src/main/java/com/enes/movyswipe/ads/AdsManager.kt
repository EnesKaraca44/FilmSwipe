package com.enes.movyswipe.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reklam onayını (UMP / GDPR) ve AdMob'un tek seferlik başlatılmasını yönetir.
 * Google, AB/BK/İsviçre kullanıcılarından reklam öncesi onay alınmasını zorunlu tutar.
 */
@Singleton
class AdsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)
    private val initialized = AtomicBoolean(false)

    private val _canShowAds = MutableStateFlow(false)
    /** true olduğunda reklam istenebilir: onay alınmış veya gerekmiyor ve SDK başlatıldı. */
    val canShowAds: StateFlow<Boolean> = _canShowAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    /** true ise Ayarlar'da "Reklam gizlilik seçenekleri" düğmesi gösterilmeli (Google şartı). */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    /** MainActivity.onCreate içinde çağrılır. */
    fun gatherConsent(activity: Activity) {
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(activity, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                formError?.let { Log.w(TAG, "Onay formu hatası: ${it.message}") }
                onConsentResolved()
            }
        }, { requestError ->
            Log.w(TAG, "Onay bilgisi alınamadı: ${requestError.message}")
            onConsentResolved()
        })
        // Önceki oturumda alınmış onay varsa beklemeden başlat.
        if (consentInformation.canRequestAds()) initializeSdk()
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            formError?.let { Log.w(TAG, "Gizlilik formu hatası: ${it.message}") }
            onConsentResolved()
        }
    }

    private fun onConsentResolved() {
        _privacyOptionsRequired.value = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (consentInformation.canRequestAds()) initializeSdk() else _canShowAds.value = false
    }

    private fun initializeSdk() {
        if (initialized.getAndSet(true)) {
            _canShowAds.value = consentInformation.canRequestAds()
            return
        }
        MobileAds.initialize(context) { _canShowAds.value = consentInformation.canRequestAds() }
    }

    companion object {
        private const val TAG = "AdsManager"
    }
}
