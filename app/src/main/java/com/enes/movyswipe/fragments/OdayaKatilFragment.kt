package com.enes.movyswipe.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.enes.movyswipe.data.JoinStatus
import com.enes.movyswipe.databinding.FragmentOdayaKatilBinding
import com.enes.movyswipe.viewmodels.OdaKatilViewModel
import kotlinx.coroutines.launch


class OdayaKatilFragment : Fragment() {
    private var _binding: FragmentOdayaKatilBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OdaKatilViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding=FragmentOdayaKatilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListener()
        observeJoinStatus()

    }

    private fun setupClickListener() {
        binding.btnKatil.setOnClickListener {
            val roomCode = binding.etOdaKodu.text.toString().trim()

            // Eğer kullanıcı bir kod girdiyse, ViewModel'deki işlemi başlat.
            if (roomCode.isNotEmpty()) {
                viewModel.joinRoom(roomCode)
            } else {
                // Kod girilmediyse kullanıcıyı uyar.
                Toast.makeText(requireContext(), "Lütfen bir oda kodu girin.", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnGeri.setOnClickListener {
            // Geri tuşuna basma eylemini tetikle
            findNavController().popBackStack()
        }
    }
    private fun observeJoinStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.joinStatus.collect { status ->
                    // Gelen yeni duruma göre arayüzü güncelle.
                    when (status) {
                        JoinStatus.IDLE -> {
                            // Başlangıç durumu. Butonu aktif, ProgressBar'ı pasif yap.
                            binding.progressBar.isVisible = false
                            binding.btnKatil.isEnabled = true
                        }
                        JoinStatus.LOADING -> {
                            // Yükleniyor... Butonu pasif, ProgressBar'ı aktif yap.
                            binding.progressBar.isVisible = true
                            binding.btnKatil.isEnabled = false
                        }
                        JoinStatus.SUCCESS -> {
                            // BAŞARILI! Duello ekranına yönlendir.
                            Toast.makeText(requireContext(), "Odaya başarıyla katıldın!", Toast.LENGTH_SHORT).show()

                            // Düello ekranına geçerken, hangi odaya katıldığımızı da argüman olarak gönderiyoruz.
                            val roomCode = binding.etOdaKodu.text.toString().trim()
                            val action = OdayaKatilFragmentDirections.actionOdayaKatilFragmentToDuelloFragment(roomCode)
                            findNavController().navigate(action)
                        }
                        is JoinStatus.FAILURE -> {
                            // BAŞARISIZ! Hata mesajını göster.
                            Toast.makeText(requireContext(), status.message, Toast.LENGTH_LONG).show()
                            // İşlem bittiği için arayüzü tekrar başlangıç durumuna getir.
                            binding.progressBar.isVisible = false
                            binding.btnKatil.isEnabled = true
                        }
                    }
                }
            }
        }
    }

}