package com.enes.movyswipe

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.enes.movyswipe.databinding.ActivityMainBinding
import com.enes.movyswipe.util.AuthManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {


    // --- YENİ EKLENEN DEĞİŞKENLER ---

    private lateinit var billingClient: BillingClient
    private var removeAdsProductDetails: ProductDetails? = null

    // View Binding'i kullanmak, 'findViewById'dan daha modern ve güvenlidir.
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupBillingClient()
        // View Binding ile layout'u yüklüyoruz.
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // NavHostFragment'ı ve NavController'ı buluyoruz.
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // --- YENİ EKLENEN KISIM 1: BottomNavigationView'ı Bağlama ---
        // XML'deki 'bottomNavView'ı NavController ile bağlıyoruz.
        // Bu tek satır, tüm tıklama ve yönlendirme mantığını otomatik olarak halleder.
        binding.bottomNavView.setupWithNavController(navController)
        // -----------------------------------------------------------


        // -------------------------------------------------------------

        // --- BÖLÜM 2: YÖNLENDİRME MANTIĞI (Aynı kalıyor ama küçük bir iyileştirme ile) ---
        lifecycleScope.launch {
            delay(500)
            if (!AuthManager.isNicknameSet(this@MainActivity)) {
                // Eğer 'startDestination' zaten AnaMenu ise ve biz Profil'e gideceksek,
                // NavController hazır olmayabilir. Bu kontrolü eklemek daha güvenli.
                if (navController.currentDestination?.id == R.id.anaMenuFragment) {
                    navController.navigate(R.id.action_anaMenuFragment_to_profilOlusturFragment)
                }
            }
        }
    }
    private fun setupBillingClient() {
        val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
            // --- DEĞİŞİKLİK BURADA BAŞLIYOR ---
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    // Satın alma başarılı, şimdi bu satın almayı işlememiz gerekiyor.
                    handlePurchase(purchase)
                }
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                // Kullanıcı satın almayı iptal etti. Bir şey yapmaya gerek yok.
            } else {
                // Diğer hata durumları.
            }
        }

        billingClient = BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases() // Bekleyen satın alımları etkinleştir (ZORUNLU)
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Bağlantı başarılı! Şimdi ürünleri sorgulayabiliriz.
                    queryProducts()
                }
            }
            override fun onBillingServiceDisconnected() {
                // Bağlantı koptu, yeniden bağlanmayı dene.
            }
        })
    }
    private fun queryProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("remove_ads") // Play Console'daki ID ile aynı olmalı
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                // Ürünü bulduk! Detaylarını daha sonra kullanmak üzere değişkene kaydediyoruz.
                removeAdsProductDetails = productDetailsList[0]
            }
        }
    }
    private fun handlePurchase(purchase: Purchase) {
        // Bu, tüketilemeyen (non-consumable) bir ürün olduğu için,
        // satın alındığını onaylamamız (acknowledge) gerekir.
        // Eğer 3 gün içinde onaylanmazsa, Google ödemeyi otomatik olarak iade eder.
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        // ONAYLAMA BAŞARILI!
                        // Şimdi kullanıcıyı "premium" olarak kaydedebiliriz.
                        savePremiumStatus(true)
                        // Reklamları anında gizle
                        runOnUiThread {
                            Toast.makeText(this, "Reklamlar kaldırıldı! Değişiklik için uygulamayı yeniden başlatın.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    // --- BU YENİ FONKSİYONU DA EKLE ---
    private fun savePremiumStatus(isPremium: Boolean) {
        val prefs = getSharedPreferences("FilmSwipePrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_premium", isPremium).apply()
    }

   fun isUserPremium(): Boolean {
        val prefs = getSharedPreferences("FilmSwipePrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("is_premium", false)
    }

    fun launchPurchaseFlow() {
        if (removeAdsProductDetails == null) {
            Toast.makeText(this, "Satın alma şu an kullanılamıyor.", Toast.LENGTH_SHORT).show()
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(removeAdsProductDetails!!)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(this, billingFlowParams)
    }
}