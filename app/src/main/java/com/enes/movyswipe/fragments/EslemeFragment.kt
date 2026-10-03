package com.enes.movyswipe.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.enes.movyswipe.databinding.FragmentEslemeBinding
import com.enes.movyswipe.viewmodels.EslesmeViewModel
import kotlinx.coroutines.launch


class EslemeFragment : Fragment() {
    private var _binding: FragmentEslemeBinding? = null
    private val binding get() = _binding!!

    // 1. ViewModel'imizi ve Navigasyon Argümanlarımızı oluşturuyoruz.
    private val viewModel: EslesmeViewModel by viewModels()
    private val args: EslemeFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEslemeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 2. DuelloFragment'tan gelen film ID'sini kullanarak
        // ViewModel'den film detaylarını yüklemesini istiyoruz.
        viewModel.loadMatchedMovie(args.matchedMovieId)

        observeViewModel()
        setupClickListeners()
    }

    // ViewModel'den gelen film verisini dinleyen fonksiyon.
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 3. ViewModel'deki 'movie' StateFlow'unu dinliyoruz.
                viewModel.movie.collect { movie ->
                    // 4. Gelen movie nesnesi null değilse, arayüzü doldur.
                    if (movie != null) {
                        binding.tvEslesenFilmAdi.text = movie.title

                        // Yıl ve puan gibi diğer bilgileri de dolduralım.
                        if (movie.releaseDate.isNotBlank()) {
                            binding.tvEslesenFilmYili.text = "📅 ${movie.releaseDate.substring(0, 4)}"
                        }
                        binding.tvEslesenFilmPuani.text = "⭐ ${movie.voteAverage}/10"

                        // Glide ile posteri yüklüyoruz.
                        val posterUrl = "https://image.tmdb.org/t/p/w500${movie.posterPath}"
                        Glide.with(requireContext())
                            .load(posterUrl)
                            .into(binding.ivEslesenFilmPoster)
                    }
                }
            }
        }
    }

    // Butonların tıklama olaylarını ayarlayan fonksiyon
    private fun setupClickListeners() {
        binding.btnSohbetEt.setOnClickListener {
            // Sohbet ekranına yönlendirirken, hangi odaya ait olduğunu
            // belirtmek için 'roomCode'u da argüman olarak gönderiyoruz.
            val action = EslemeFragmentDirections.actionEslemeFragmentToSohbetFragment(args.roomCode)
            findNavController().navigate(action)
        }
        binding.btnListemeEkle.setOnClickListener {
            Log.e("EslesmeFragment_Click", "Listeme Ekle BUTONUNA BASILDI!")
            viewModel.addCurrentMovieToWatchlist()
            Toast.makeText(requireContext(), "İzleme listesine eklendi!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}