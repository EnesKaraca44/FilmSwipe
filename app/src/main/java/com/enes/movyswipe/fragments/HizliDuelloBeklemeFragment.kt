package com.enes.movyswipe.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.enes.movyswipe.R
import com.enes.movyswipe.databinding.FragmentHizliDuelloBeklemeBinding
import com.enes.movyswipe.viewmodels.HizliDuelloBeklemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HizliDuelloBeklemeFragment : Fragment() {
    private var _binding: FragmentHizliDuelloBeklemeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HizliDuelloBeklemeViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHizliDuelloBeklemeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.startSearch() // ViewModel tekrar çağrıları yok sayar
        binding.btnIptal.setOnClickListener { findNavController().popBackStack() }
        binding.btnAnaMenu.setOnClickListener { findNavController().popBackStack(R.id.anaMenuFragment, false) }
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.matchmakingStatus.collect { binding.tvBeklemeZamani.text = it } }
                launch {
                    viewModel.queueSize.collect { size ->
                        // Eskiden sabit "12 kişi çevrimiçi" yazıyordu.
                        binding.tvOnlineKullanici.text = "$size kişi eşleşme bekliyor"
                    }
                }
                launch {
                    viewModel.navigateToRoom.collect { roomCode ->
                        if (findNavController().currentDestination?.id == R.id.hizliDuelloBeklemeFragment) {
                            findNavController().navigate(
                                HizliDuelloBeklemeFragmentDirections
                                    .actionHizliDuelloBeklemeFragmentToDuelloFragment(roomCode)
                            )
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
