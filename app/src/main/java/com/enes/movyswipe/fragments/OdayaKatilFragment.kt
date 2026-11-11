package com.enes.movyswipe.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.enes.movyswipe.R
import com.enes.movyswipe.data.JoinStatus
import com.enes.movyswipe.databinding.FragmentOdayaKatilBinding
import com.enes.movyswipe.viewmodels.OdaKatilViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OdayaKatilFragment : Fragment() {
    private var _binding: FragmentOdayaKatilBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OdaKatilViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOdayaKatilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnKatil.setOnClickListener { submit() }
        binding.etOdaKodu.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                submit(); true
            } else false
        }
        binding.btnGeri.setOnClickListener { findNavController().popBackStack() }
        observeViewModel()
    }

    private fun submit() {
        val roomCode = binding.etOdaKodu.text.toString()
        if (roomCode.isBlank()) {
            Toast.makeText(requireContext(), R.string.duel_join_enter_code, Toast.LENGTH_SHORT).show()
        } else {
            viewModel.joinRoom(roomCode)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.joinStatus.collect { status ->
                        val loading = status == JoinStatus.LOADING
                        binding.progressBar.isVisible = loading
                        binding.btnKatil.isEnabled = !loading
                        if (status is JoinStatus.FAILURE) {
                            Toast.makeText(requireContext(), status.message, Toast.LENGTH_LONG).show()
                            viewModel.consumeFailure()
                        }
                    }
                }
                launch {
                    // Tek seferlik olay: Düello'dan geri dönünce tekrar Düello'ya atılmıyor.
                    viewModel.navigateToDuel.collect { code ->
                        if (findNavController().currentDestination?.id != R.id.odayaKatilFragment) return@collect
                        Toast.makeText(requireContext(), R.string.duel_join_success, Toast.LENGTH_SHORT).show()
                        findNavController().navigate(OdayaKatilFragmentDirections.actionOdayaKatilFragmentToDuelloFragment(code))
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
