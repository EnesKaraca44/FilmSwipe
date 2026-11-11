package com.enes.movyswipe

import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withStarted
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.enes.movyswipe.ads.AdsManager
import com.enes.movyswipe.billing.BillingManager
import com.enes.movyswipe.databinding.ActivityMainBinding
import com.enes.movyswipe.util.AuthManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var billingManager: BillingManager
    @Inject lateinit var adsManager: AdsManager

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    /** Alt menünün göründüğü ana sekmeler. Düello/sohbet gibi akışlarda gizlenir. */
    private val topLevelDestinations = setOf(
        R.id.anaMenuFragment, R.id.izlemeListesiFragment, R.id.ayarlarFragment
    )
    private var isTopLevel = true

    /** Açık/koyu tema anahtarına uyan ekranlar; diğerleri tasarım gereği hep koyu. */
    private val themedDestinations = setOf(R.id.izlemeListesiFragment, R.id.ayarlarFragment)

    override fun onCreate(savedInstanceState: Bundle?) {
        // targetSdk 35+ cihazlarda edge-to-edge zorunlu; insets'i kendimiz yönetiyoruz.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNavView.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            isTopLevel = destination.id in topLevelDestinations
            updateSystemBars(destination.id)
            ViewCompat.requestApplyInsets(binding.root)
        }
        setupInsets()

        billingManager.start()
        adsManager.gatherConsent(this)
        observeBillingEvents()

        if (savedInstanceState == null && !AuthManager.isNicknameSet(this)) {
            lifecycleScope.launch {
                lifecycle.withStarted { }
                if (navController.currentDestination?.id == R.id.anaMenuFragment) {
                    navController.navigate(R.id.action_anaMenuFragment_to_profilOlusturFragment)
                }
            }
        }
    }

    /**
     * Durum çubuğu şeridi kök görünümün arka planını gösterir. Açık/koyu temaya uyan
     * ekranlarda (Listem, Profilim) şerit sayfa rengini alır; diğer ekranlar hep koyu.
     * Simgeler de zemine göre açık/koyu seçilir ki her iki temada okunabilsin.
     */
    private fun updateSystemBars(destinationId: Int) {
        val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val themedPage = destinationId in themedDestinations
        binding.root.setBackgroundResource(
            when (destinationId) {
                R.id.izlemeListesiFragment -> R.color.wl_page_start
                R.id.ayarlarFragment -> R.color.settings_page_background
                else -> R.drawable.light_gradient_background
            }
        )
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = themedPage && !isNight
            // Alt menü (tema renkli) gezinme çubuğunun arkasına kadar uzanır.
            isAppearanceLightNavigationBars = destinationId in topLevelDestinations && !isNight
        }
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { root, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

            // Klavye açıkken alt menüyü gizle ki mesaj kutusu klavyenin üstünde kalsın.
            val showBottomNav = isTopLevel && !imeVisible
            binding.bottomNavView.isVisible = showBottomNav

            val bottom = when {
                imeVisible -> ime.bottom
                showBottomNav -> 0 // BottomNavigationView gezinme çubuğu boşluğunu kendisi ekler
                else -> bars.bottom
            }
            root.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bottom)
            insets
        }
    }

    private fun observeBillingEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                billingManager.events.collect { event ->
                    val message = when (event) {
                        BillingManager.Event.PurchaseCompleted -> getString(R.string.settings_purchase_completed)
                        BillingManager.Event.PurchasePending -> getString(R.string.settings_purchase_pending)
                        is BillingManager.Event.Error -> event.message
                    }
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Uygulama dışında tamamlanan / iade edilen satın alımları yakala.
        billingManager.refresh()
    }

    fun launchPurchaseFlow() = billingManager.launchPurchaseFlow(this)

    fun showAdPrivacyOptions() = adsManager.showPrivacyOptions(this)
}
