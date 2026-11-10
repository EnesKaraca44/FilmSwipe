package com.enes.movyswipe.fragments

import android.content.Intent
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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.enes.movyswipe.R
import com.enes.movyswipe.databinding.FragmentOdaKoduBinding
import com.enes.movyswipe.viewmodels.OdaKoduViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OdaKoduFragment : Fragment() {

    private var _binding: FragmentOdaKoduBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OdaKoduViewModel by viewModels()
    private val args: OdaKoduFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOdaKoduBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // ViewModel odayı yalnızca bir kez oluşturur (döndürmede yeni oda açılmaz).
        viewModel.createRoomFromFilters(args.genres, args.StartYear)
        binding.tvOdaKodu.setOnClickListener { shareCode() }
        observeViewModelStates()
    }

    private fun shareCode() {
        val code = viewModel.roomCode.value ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "FilmSwipe'ta film düellosuna gel! Oda kodu: $code")
        }
        startActivity(Intent.createChooser(intent, "Oda kodunu paylaş"))
    }

    private fun observeViewModelStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.isVisible = isLoading
                        binding.contentLayout.isVisible = !isLoading
                    }
                }
                launch {
                    viewModel.roomCode.collect { roomCode -> if (roomCode != null) binding.tvOdaKodu.text = roomCode }
                }
                launch {
                    viewModel.error.collect { error ->
                        if (error != null) Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                    }
                }
                launch {
                    viewModel.navigateToDuel.collect { code ->
                        if (findNavController().currentDestination?.id != R.id.odaKoduFragment) return@collect
                        Toast.makeText(requireContext(), "Partnerin katıldı, düello başlıyor!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(OdaKoduFragmentDirections.actionOdaKoduFragmentToDuelloFragment(code))
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
