package com.enes.movyswipe.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.enes.movyswipe.R
import com.enes.movyswipe.databinding.FragmentProfilOlusturBinding
import com.enes.movyswipe.util.AuthManager

class ProfilOlusturFragment : Fragment() {

    private var _binding: FragmentProfilOlusturBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfilOlusturBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnKaydet.setOnClickListener { saveProfileAndNavigate() }
        binding.btnAtla.setOnClickListener { skip() }
        // Geri tuşu "atla" gibi davranır; kullanıcı adı olmadan ana menüye dönüp tekrar buraya gelmez.
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = skip()
        })
    }

    private fun skip() {
        AuthManager.saveUserNickname(requireContext(), AuthManager.getUserNickname(requireContext()))
        navigateToMainMenu()
    }

    private fun saveProfileAndNavigate() {
        val nickname = AuthManager.normalizeNickname(binding.etKullaniciAdi.text.toString())
        if (nickname == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.settings_nickname_length_error, AuthManager.NICKNAME_MIN, AuthManager.NICKNAME_MAX),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        AuthManager.saveUserNickname(requireContext(), nickname)
        Toast.makeText(requireContext(), getString(R.string.settings_profile_saved), Toast.LENGTH_SHORT).show()
        navigateToMainMenu()
    }

    private fun navigateToMainMenu() {
        // nav_host.xml: popUpTo=anaMenuFragment inclusive → geri yığında profil ekranı kalmaz.
        findNavController().navigate(ProfilOlusturFragmentDirections.actionProfilOlusturFragmentToAnaMenuFragment())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
