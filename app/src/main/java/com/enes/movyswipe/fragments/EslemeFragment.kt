package com.enes.movyswipe.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.enes.movyswipe.R
import com.enes.movyswipe.data.MovieFormat
import com.enes.movyswipe.databinding.FragmentEslemeBinding
import com.enes.movyswipe.viewmodels.EslesmeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EslemeFragment : Fragment() {
    private var _binding: FragmentEslemeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EslesmeViewModel by viewModels()
    private val args: EslemeFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEslemeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadMatchedMovie(args.matchedMovieId)
        observeViewModel()
        setupClickListeners()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.movie.collect { movie ->
                    if (movie == null) return@collect
                    binding.tvEslesenFilmAdi.text = movie.displayTitle
                    binding.tvEslesenFilmYili.text = "📅 ${movie.releaseYear ?: "Bilinmiyor"}"
                    binding.tvEslesenFilmPuani.text = "⭐ ${MovieFormat.rating(movie.voteAverage)}/10"
                    Glide.with(this@EslemeFragment)
                        .load(MovieFormat.posterUrl(movie.posterPath))
                        .placeholder(R.drawable.placeholder_movie_poster)
                        .error(R.drawable.placeholder_movie_poster)
                        .into(binding.ivEslesenFilmPoster)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSohbetEt.setOnClickListener {
            findNavController().navigate(EslemeFragmentDirections.actionEslemeFragmentToSohbetFragment(args.roomCode))
        }
        binding.btnListemeEkle.setOnClickListener {
            val appContext = requireContext().applicationContext
            viewModel.addCurrentMovieToWatchlist { ok ->
                val msg = if (ok) "İzleme listesine eklendi!" else "Film bilgisi henüz yüklenmedi, tekrar dene."
                Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
