package com.enes.movyswipe.fragments


import android.os.Bundle

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.enes.movyswipe.R
import com.enes.movyswipe.fragments.SplashFragmentDirections
import com.enes.movyswipe.util.AuthManager

class SplashFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Senin FilmSwipe görselini içeren XML dosyasını burada yüklüyoruz.
        // Bu dosyanın adının "fragment_splash.xml" olduğunu varsayıyorum.
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 2 saniyelik bir gecikme başlatıyoruz.
        // Bu süre boyunca kullanıcı senin güzel açılış ekranı görselini görecek.
        Handler(Looper.getMainLooper()).postDelayed({
            // 2 saniye dolduktan sonra, yönlendirme kontrolünü yapıyoruz.
            checkNicknameAndNavigate()
        }, 2000) // 2000 milisaniye = 2 saniye
    }

    private fun checkNicknameAndNavigate() {
        // Eğer bu fragment hala ekrandaysa (kullanıcı bu 2 saniyede uygulamayı kapatmadıysa)
        if (isAdded) {
            // AuthManager'ı kullanarak kullanıcı adı daha önce kaydedilmiş mi diye kontrol et.
            if (AuthManager.isNicknameSet(requireContext())) {
                // Kullanıcı adı VARSA: Doğrudan Ana Menü'ye git.
                // Geri tuşu yığınını temizleyerek, kullanıcının geri bastığında
                // splash ekranına dönmesini engelliyoruz.
                val action = SplashFragmentDirections.actionSplashFragmentToAnaMenuFragment()
                findNavController().navigate(action)
            } else {
                // Kullanıcı adı YOKSA: Profil Oluşturma ekranına git.
                // Burada da geri tuşu yığınını temizliyoruz.
                val action = SplashFragmentDirections.actionSplashFragmentToProfilOlusturFragment()
                findNavController().navigate(action)
            }
        }
    }
}