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
import com.enes.movyswipe.MainActivity
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
        setupThemeSwitch()
        setupClickListeners()
        observeState()
    }

    private fun setupThemeSwitch() {
        val context = requireContext()
        val currentlyDark = ThemePrefs.isDarkSelected(context)
            ?: ((resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES)
        binding.switchKoyuTema.isChecked = currentlyDark
        binding.switchKoyuTema.setOnCheckedChangeListener { _, isChecked ->
            ThemePrefs.setDark(context.applicationContext, isChecked)
        }
    }

    private fun setupClickListeners() {
        // Eskiden bu dinleyici yanlışlıkla tüm sayfanın kök görünümüne bağlıydı.
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
                "Kullanıcı adı ${AuthManager.NICKNAME_MIN}-${AuthManager.NICKNAME_MAX} karakter olmalıdır."
            } else {
                binding.etKullaniciAdiAyarlar.setText(saved)
                "Kullanıcı adı güncellendi!"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
        binding.btnCikisYap.setOnClickListener { showSignOutConfirmationDialog() }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isPremium.collect { premium ->
                        binding.btnReklamlariKaldir.isEnabled = !premium
                        binding.btnReklamlariKaldir.text = if (premium) "Reklamsız sürüm aktif ✓" else "Reklamları Kaldır"
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
            .setTitle("Çıkış Yap")
            .setMessage(
                "Çıkış yapmak istediğinden emin misin? İzleme listen ve reklamsız sürüm bu cihazda kalır, " +
                    "ancak yeni bir kimlikle devam edeceksin."
            )
            .setPositiveButton("Evet, Çıkış Yap") { _, _ ->
                viewModel.signOut()
                val intent = Intent(requireActivity(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            .setNegativeButton("Hayır", null)
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
