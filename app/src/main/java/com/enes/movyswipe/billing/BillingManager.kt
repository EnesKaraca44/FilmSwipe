package com.enes.movyswipe.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.enes.movyswipe.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Play Billing'i uygulama ömrü boyunca yönetir.
 * - Her bağlantıda Play'deki satın alımları sorgular: yeniden yükleme, yeni cihaz veya
 *   iade durumunda premium durumu Play ile senkron kalır (geri yükleme).
 * - Bağlantı koparsa kütüphane otomatik yeniden bağlanır (Billing 8).
 * - Premium bilgisi, çıkış yapınca silinmeyen ayrı bir dosyada önbelleğe alınır.
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context
) : PurchasesUpdatedListener {

    sealed class Event {
        object PurchaseCompleted : Event()
        object PurchasePending : Event()
        data class Error(val message: String) : Event()
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _isPremium = MutableStateFlow(prefs.getBoolean(KEY_PREMIUM, legacyPremiumFlag()))
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    @Volatile
    private var removeAdsDetails: ProductDetails? = null

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    @Volatile
    private var connecting = false

    fun start() {
        if (client.isReady) {
            refresh()
            return
        }
        if (connecting) return
        connecting = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) refresh()
                else Log.w(TAG, "Billing kurulamadı: ${result.debugMessage}")
            }

            override fun onBillingServiceDisconnected() {
                connecting = false
                // enableAutoServiceReconnection() sayesinde bir sonraki çağrıda yeniden bağlanır.
            }
        })
    }

    /** Ürün detaylarını ve mevcut satın alımları (geri yükleme) sorgular. */
    fun refresh() {
        if (!client.isReady) {
            start()
            return
        }
        val productParams = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(Entitlements.REMOVE_ADS)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
        ).build()
        client.queryProductDetailsAsync(productParams) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                removeAdsDetails = details.productDetailsList.firstOrNull()
            }
        }

        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach(::acknowledgeIfNeeded)
                setPremium(Entitlements.hasRemoveAds(purchases.map { it.toOwned() }))
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        val details = removeAdsDetails
        if (details == null) {
            _events.trySend(Event.Error(context.getString(R.string.settings_purchase_unavailable)))
            start()
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build())
            )
            .build()
        client.launchBillingFlow(activity, params)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases.orEmpty().forEach { purchase ->
                when (purchase.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> {
                        acknowledgeIfNeeded(purchase)
                        if (Entitlements.REMOVE_ADS in purchase.products) {
                            setPremium(true)
                            _events.trySend(Event.PurchaseCompleted)
                        }
                    }
                    Purchase.PurchaseState.PENDING -> _events.trySend(Event.PurchasePending)
                    else -> Unit
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refresh()
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> _events.trySend(
                Event.Error(context.getString(R.string.settings_purchase_failed, result.responseCode))
            )
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED || purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        client.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "Acknowledge başarısız: ${result.debugMessage}")
            }
        }
    }

    private fun setPremium(value: Boolean) {
        _isPremium.value = value
        prefs.edit().putBoolean(KEY_PREMIUM, value).apply()
    }

    /** Eski sürüm premium bayrağını "FilmSwipePrefs" içinde tutuyordu. */
    private fun legacyPremiumFlag(): Boolean =
        context.getSharedPreferences("FilmSwipePrefs", Context.MODE_PRIVATE).getBoolean("is_premium", false)

    private fun Purchase.toOwned() =
        Entitlements.OwnedProduct(products, purchaseState == Purchase.PurchaseState.PURCHASED)

    companion object {
        private const val TAG = "BillingManager"
        private const val PREFS = "FilmSwipeBilling"
        private const val KEY_PREMIUM = "is_premium"
    }
}
