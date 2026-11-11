package com.enes.movyswipe.fragments

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.enes.movyswipe.BuildConfig
import com.enes.movyswipe.MainActivity
import com.enes.movyswipe.R
import com.enes.movyswipe.databinding.FragmentAyarlarBinding
import com.enes.movyswipe.util.AuthManager
import com.enes.movyswipe.util.ThemePrefs
import com.enes.movyswipe.viewmodels.AyarlarViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AyarlarFragment : Fragment() {

    private var _binding: FragmentAyarlarBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AyarlarViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAyarlarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.etKullaniciAdiAyarlar.setText(viewModel.getCurrentNickname())
        binding.tvAyarlarSurum.text = getString(R.string.settings_version, BuildConfig.VERSION_NAME)
        setupThemeSwitch()
        setupClickListeners()
        observeState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshData()
    }

    private fun setupThemeSwitch() {
        val context = requireContext()
        val currentlyDark = ThemePrefs.isDarkSelected(context)
            ?: ((resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES)
        binding.switchKoyuTema.isChecked = currentlyDark
        updateThemeDescription(currentlyDark)
        binding.switchKoyuTema.setOnCheckedChangeListener { _, isChecked ->
            updateThemeDescription(isChecked)
            // Etkinlik yeniden oluşturulur; Profilim, Listem ve alt menü yeni temayla çizilir.
            ThemePrefs.setDark(context.applicationContext, isChecked)
        }
    }

    private fun updateThemeDescription(dark: Boolean) {
        binding.tvKoyuTemaAciklama.text =
            getString(if (dark) R.string.settings_dark_theme_on else R.string.settings_dark_theme_off)
    }

    private fun setupClickListeners() {
        // Profilim bir alt menü sekmesi: geri tuşu sistem geri tuşuyla aynı davranır (Ana Menü'ye döner).
        binding.btnAyarlarGeri.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.btnReklamlariKaldir.setOnClickListener {
            (activity as? MainActivity)?.launchPurchaseFlow()
        }
        binding.btnReklamGizlilik.setOnClickListener {
            (activity as? MainActivity)?.showAdPrivacyOptions()
        }
        binding.btnGizlilikPolitikasi.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(requireContext(), PRIVACY_POLICY_URL, Toast.LENGTH_LONG).show()
            }
        }
        binding.btnKullaniciAdiKaydet.setOnClickListener {
            val saved = viewModel.saveNewNickname(binding.etKullaniciAdiAyarlar.text.toString())
            val message = if (saved == null) {
                getString(R.string.settings_nickname_length_error, AuthManager.NICKNAME_MIN, AuthManager.NICKNAME_MAX)
            } else {
                binding.etKullaniciAdiAyarlar.setText(saved)
                getString(R.string.settings_nickname_updated)
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
        binding.btnCikisYap.setOnClickListener { showSignOutConfirmationDialog() }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.nickname.collect { nickname ->
                        binding.tvProfilKullaniciAdi.text = getString(R.string.settings_nickname_header, nickname)
                    }
                }
                launch {
                    viewModel.userStats.collect { stats ->
                        binding.tvDuelloSayisi.text = stats.duels.toString()
                        binding.tvEslesmeSayisi.text = stats.matches.toString()
                        binding.tvUyumOrani.text =
                            getString(R.string.settings_compatibility_rate, stats.compatibilityRate)
                    }
                }
                launch {
                    viewModel.isPremium.collect { premium ->
                        binding.tvProRozet.isVisible = premium
                        binding.btnReklamlariKaldir.isEnabled = !premium
                        binding.btnReklamlariKaldir.text = getString(
                            if (premium) R.string.settings_ad_free_active else R.string.settings_remove_ads
                        )
                    }
                }
                launch {
                    viewModel.privacyOptionsRequired.collect { binding.btnReklamGizlilik.isVisible = it }
                }
            }
        }
    }

    private fun showSignOutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_sign_out_title)
            .setMessage(R.string.settings_sign_out_message)
            .setPositiveButton(R.string.settings_sign_out_confirm) { _, _ ->
                viewModel.signOut()
                val intent = Intent(requireActivity(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            .setNegativeButton(R.string.settings_sign_out_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val PRIVACY_POLICY_URL = "https://filmswipee.web.app/"
    }
}
