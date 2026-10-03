package com.enes.movyswipe.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.billingclient.api.BillingFlowParams
import com.enes.movyswipe.MainActivity
import com.enes.movyswipe.R
import com.enes.movyswipe.databinding.FragmentAyarlarBinding
import com.enes.movyswipe.util.AuthManager
import com.enes.movyswipe.viewmodels.AyarlarViewModel

class AyarlarFragment : Fragment() {

    private var _binding: FragmentAyarlarBinding? = null
    private val binding get() = _binding!!

    // ViewModel'i oluşturuyoruz.
    private val viewModel: AyarlarViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAyarlarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadCurrentNickname()
        setupClickListeners()
    }

    // Mevcut kullanıcı adını yükleyip EditText'e yazan fonksiyon.
    private fun loadCurrentNickname() {
        val currentNickname = viewModel.getCurrentNickname()
        binding.etKullaniciAdiAyarlar.setText(currentNickname)
    }

    // Tüm tıklama olaylarını yöneten fonksiyon.
    private fun setupClickListeners() {
        binding.btnReklamlariKaldir.setOnClickListener { // ID'yi kendi XML'ine göre düzelt
            // 1. Fragment'ın içinde bulunduğu Activity'yi alıyoruz.
            val mainActivity = activity as? MainActivity

            // 2. Eğer Activity gerçekten bir MainActivity ise (ki öyle olmalı),
            //    içindeki 'launchPurchaseFlow' fonksiyonunu çağırıyoruz.
            if (mainActivity != null) {
                mainActivity.launchPurchaseFlow()
            } else {
                // Bu durumun normalde hiç olmaması gerekir.
                Toast.makeText(requireContext(), "Bir hata oluştu.", Toast.LENGTH_SHORT).show()
            }

        }
        binding.btnKullaniciAdiKaydet.setOnClickListener {
            val newNickname = binding.etKullaniciAdiAyarlar.text.toString().trim()

            if (newNickname.length < 3) {
                Toast.makeText(requireContext(), "Kullanıcı adı en az 3 karakter olmalıdır.", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.saveNewNickname(newNickname)
                Toast.makeText(requireContext(), "Kullanıcı adı güncellendi!", Toast.LENGTH_SHORT).show()
            }
        }

        // Diğer butonlar için şimdilik Toast mesajları gösterelim.
       // binding.rowGizlilik.setOnClickListener {
         //   Toast.makeText(requireContext(), "Gizlilik ayarları yakında!", Toast.LENGTH_SHORT).show()
       // }

        binding.btnCikisYap.setOnClickListener {
            showSignOutConfirmationDialog()
        }

        binding.switchBildirimler.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(requireContext(), "Bildirimler açıldı", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Bildirimler kapandı", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // Bu yeni fonksiyonu Fragment'ın içine ekle
    private fun showSignOutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Çıkış Yap")
            .setMessage("Çıkış yapmak istediğinizden emin misiniz? İzleme listeniz gibi verileriniz bu cihazda kalmaya devam edebilir, ancak yeni bir kimlikle devam edeceksiniz.")
            .setPositiveButton("Evet, Çıkış Yap") { _, _ ->
                // 1. ViewModel aracılığıyla çıkış işlemini başlat.
                viewModel.signOut()

                // 2. Kullanıcıyı uygulamanın en başına (Splash/Yönlendirici ekrana) gönder.
                // Intent ile MainActivity'yi yeniden başlatmak, tüm geri tuşu yığınını
                // temizlemenin en garantili yoludur.
                val intent = Intent(requireActivity(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)

                // Mevcut activity'yi de sonlandır.
                requireActivity().finish()
            }
            .setNegativeButton("Hayır", null)
            .show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}