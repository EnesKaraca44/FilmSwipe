package com.enes.movyswipe.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.enes.movyswipe.MainActivity
import com.enes.movyswipe.databinding.FragmentHizliDuelloBeklemeBinding
import com.enes.movyswipe.viewmodels.HizliDuelloBeklemeViewModel
import kotlinx.coroutines.launch
import com.enes.movyswipe.R


class HizliDuelloBeklemeFragment : Fragment() {
    private var _binding: FragmentHizliDuelloBeklemeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HizliDuelloBeklemeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHizliDuelloBeklemeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Cihazın benzersiz ID'sini alıp, ViewModel'deki arama sürecini başlatıyoruz.
       // val userId = UserIdManager.getUserId(requireContext())
        viewModel.startSearch()
        setupClickListeners()
        observeViewModel()
        
    }
    private fun setupClickListeners() {
        binding.btnIptal.setOnClickListener {
            Log.d("HizliDuello_Click", "İptal butonuna basıldı. Geri tuşu tetikleniyor...")
            // Activity'nin kendi geri tuşu mekanizmasını çağırıyoruz.
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnAnaMenu.setOnClickListener {
            Log.d("HizliDuello_Click", "Ana Menü butonuna basıldı. Geri tuşu tetikleniyor...")
            // Bu buton da aynı şekilde geri gitmeli.
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun restartApp() {
        // MainActivity'yi başlatmak için yeni bir Intent oluşturuyoruz.
        val intent = Intent(requireActivity(), MainActivity::class.java)

        // ÖNEMLİ: Bu flag'ler, mevcut tüm activity'leri (bizim durumumuzda MainActivity'yi)
        // temizler ve yeni bir tane başlatır. Bu, geri tuşu yığınını sıfırlar.
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        // Yeni Intent'i başlatıyoruz.
        startActivity(intent)

        // Güvenlik önlemi olarak mevcut activity'yi de kapatıyoruz.
        requireActivity().finish()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Eşleştirme durum metnini dinle ve ekrandaki TextView'ı güncelle
                launch {
                    viewModel.matchmakingStatus.collect { statusText ->
                        binding.tvBeklemeZamani.text = statusText
                    }
                }

                // Yönlendirme sinyalini dinle
                launch {
                    viewModel.navigateToRoom.collect { roomCode ->
                        if (roomCode != null) {
                            // 2. Eşleşme bulunduğunda ve oda kodu geldiğinde, DuelloFragment'a yönlendir.
                            val action = HizliDuelloBeklemeFragmentDirections.actionHizliDuelloBeklemeFragmentToDuelloFragment(roomCode)
                            findNavController().navigate(action)
                        }
                    }
                }
            }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}