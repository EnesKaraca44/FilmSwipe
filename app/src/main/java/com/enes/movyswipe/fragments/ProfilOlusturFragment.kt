package com.enes.movyswipe.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.enes.movyswipe.R
import com.enes.movyswipe.databinding.FragmentProfilOlusturBinding
import com.enes.movyswipe.util.AuthManager

class ProfilOlusturFragment : Fragment() {

    private var _binding: FragmentProfilOlusturBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilOlusturBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnKaydet.setOnClickListener {
            saveProfileAndNavigate()
        }

        binding.btnAtla.setOnClickListener {
            // Kullanıcı bu adımı atlamak isterse, ona rastgele bir isim atayıp devam ediyoruz.
            // Bu, AuthManager'daki mevcut mantığı kullanır.
            val randomNickname = AuthManager.getUserNickname(requireContext())
            // Eğer daha önce hiç isim atanmadıysa bu satır yeni bir isim oluşturur.
            // Eğer atandıysa mevcut olanı tekrar kaydeder, bir zararı olmaz.
            AuthManager.saveUserNickname(requireContext(), randomNickname)
            navigateToMainMenu()
        }
    }

    private fun saveProfileAndNavigate() {
        // XML'deki ID'nin 'etKullaniciAdi' olduğunu varsayıyorum
        val nickname = binding.etKullaniciAdi.text.toString().trim()

        if (nickname.length < 3) {
            Toast.makeText(requireContext(), "Kullanıcı adı en az 3 karakter olmalıdır.", Toast.LENGTH_SHORT).show()
            return
        }

        // Kullanıcı adını SharedPreferences'e kaydediyoruz.
        AuthManager.saveUserNickname(requireContext(), nickname)
        Toast.makeText(requireContext(), "Profil kaydedildi!", Toast.LENGTH_SHORT).show()

        // Ana Menü'ye yönlendiriyoruz.
        navigateToMainMenu()
    }

    private fun navigateToMainMenu() {
        // Profil oluşturma ekranından Ana Menü'ye geçerken, geri tuşu yığınını temizleriz.
        // Böylece kullanıcı geri bastığında tekrar bu ekrana dönmez.
        val action = ProfilOlusturFragmentDirections.actionProfilOlusturFragmentToAnaMenuFragment()
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}