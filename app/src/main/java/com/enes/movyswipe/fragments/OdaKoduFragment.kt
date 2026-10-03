package com.enes.movyswipe.fragments

import android.os.Bundle
import android.util.Log
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
import androidx.navigation.fragment.navArgs
import com.enes.movyswipe.databinding.FragmentOdaKoduBinding
import com.enes.movyswipe.viewmodels.OdaKoduViewModel

import com.enes.movyswipe.fragments.OdaKoduFragmentArgs
import kotlinx.coroutines.launch


class OdaKoduFragment : Fragment() {

    private var _binding: FragmentOdaKoduBinding? = null
    private val binding get() = _binding!!

    // ViewModel'imizi ve Navigasyon Argümanlarımızı oluşturuyoruz.
    private val viewModel: OdaKoduViewModel by viewModels()
    private val args: OdaKoduFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOdaKoduBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewModel'deki oda kurma sürecini, FiltreFragment'tan gelen argümanlarla başlatıyoruz.
        viewModel.createRoomFromFilters(args.genres, args.StartYear)

        // ViewModel'den gelen durumları dinlemek için gözlemcimizi ayarlıyoruz.
        observeViewModelStates()
    }

    private fun observeViewModelStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Yükleme durumunu dinle
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        // Yükleniyorsa ProgressBar'ı göster, diğer her şeyi gizle.
                        binding.progressBar.isVisible = isLoading
                        binding.contentLayout.isVisible = !isLoading
                    }
                }

                // Oda kodu durumunu dinle
                launch {
                    viewModel.roomCode.collect { roomCode ->
                        if (roomCode != null) {
                            // Oda kodu geldiyse, ekrandaki TextView'a yaz.
                            binding.tvOdaKodu.text = roomCode
                        }
                    }
                }

                // Hata durumunu dinle
                launch {
                    viewModel.error.collect { error ->
                        if (error != null) {
                            // Bir hata geldiyse, kullanıcıya Toast ile göster.
                            Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                        }
                    }
                }
                launch {
                    viewModel.roomStatus.collect { status ->
                        Log.d("OdaKoduFragment", "ViewModel'den yeni durum geldi: $status")
                        // Gelen durum "active" ise ve daha önce yönlendirme yapmadıysak...
                        if (status == "active") {
                            // O anki oda kodunu ViewModel'den alıyoruz.
                            val currentRoomCode = viewModel.roomCode.value
                            if (currentRoomCode != null) {
                                Toast.makeText(
                                    requireContext(),
                                    "Partnerin katıldı, düello başlıyor!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // DuelloFragment'a yönlendirme işlemini yapıyoruz.
                                val action =
                                    OdaKoduFragmentDirections.actionOdaKoduFragmentToDuelloFragment(
                                        currentRoomCode
                                    )
                                findNavController().navigate(action)
                            }
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
