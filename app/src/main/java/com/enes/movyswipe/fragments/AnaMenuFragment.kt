package com.enes.movyswipe.fragments

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.enes.movyswipe.BuildConfig
import com.enes.movyswipe.R
import com.enes.movyswipe.adapter.PopulerFilmAdapter
import com.enes.movyswipe.data.Movie
import com.enes.movyswipe.data.MovieFormat
import com.enes.movyswipe.databinding.FragmentAnaMenuBinding
import com.enes.movyswipe.util.AuthManager
import com.enes.movyswipe.viewmodels.AnaMenuViewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AnaMenuFragment : Fragment() {

    private var _binding: FragmentAnaMenuBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AnaMenuViewModel by viewModels()
    private lateinit var populerFilmAdapter: PopulerFilmAdapter
    private var adView: AdView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAnaMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupActionCards()
        setupRecyclerView()
        observeViewModel()
        binding.tvWelcomeMessage.text = "Hoş geldin, ${AuthManager.getUserNickname(requireContext())}!"
        binding.layoutError.setOnClickListener { viewModel.loadData(force = true) }
        viewModel.loadData()
    }

    override fun onResume() {
        super.onResume()
        // Ayarlar'da ad değiştirilmiş olabilir.
        binding.tvWelcomeMessage.text = "Hoş geldin, ${AuthManager.getUserNickname(requireContext())}!"
    }

    private fun setupRecyclerView() {
        populerFilmAdapter = PopulerFilmAdapter { movie -> showMovieDetailsDialog(movie) }
        binding.recyclerViewPopulerFilmler.apply {
            adapter = populerFilmAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.popularMovies.collect { populerFilmAdapter.submitList(it) } }
                launch { viewModel.isLoading.collect { binding.progressBar.isVisible = it } }
                launch {
                    viewModel.error.collect { errorMessage ->
                        val hasError = !errorMessage.isNullOrBlank()
                        binding.layoutError.isVisible = hasError
                        binding.cardViewPopulerFilmler.isVisible = !hasError
                        if (hasError) binding.tvErrorMessage.text = "$errorMessage\n(Tekrar denemek için dokun)"
                    }
                }
                launch { viewModel.shouldShowAds.collect { show -> updateBanner(show) } }
            }
        }
    }

    /**
     * Banner yalnızca reklam onayı alındıysa ve kullanıcı premium değilse yüklenir.
     * (Eskiden premium kontrolünden önce de reklam isteniyordu ve ID yanlıştı.)
     */
    private fun updateBanner(show: Boolean) {
        val container = binding.adContainer
        if (!show || BuildConfig.ADMOB_BANNER_ID.isBlank()) {
            adView?.destroy()
            adView = null
            container.removeAllViews()
            container.isVisible = false
            return
        }
        if (adView != null) return
        val banner = AdView(requireContext()).apply {
            adUnitId = BuildConfig.ADMOB_BANNER_ID
            setAdSize(AdSize.BANNER)
        }
        container.addView(banner)
        container.isVisible = true
        banner.loadAd(AdRequest.Builder().build())
        adView = banner
    }

    private fun showMovieDetailsDialog(movie: Movie) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_film_detay)

        val poster = dialog.findViewById<ImageView>(R.id.imageViewDialogPoster)
        dialog.findViewById<TextView>(R.id.textViewDialogTitle).text = movie.displayTitle
        dialog.findViewById<TextView>(R.id.textViewDialogOverview).text =
            movie.overview?.takeIf { it.isNotBlank() } ?: "Özet bulunmuyor."
        Glide.with(this)
            .load(MovieFormat.posterUrl(movie.posterPath))
            .placeholder(R.drawable.placeholder_movie_poster)
            .error(R.drawable.placeholder_movie_poster)
            .into(poster)

        dialog.findViewById<View>(R.id.textViewDialogClose).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<LinearLayout>(R.id.layoutWatchlistButton).setOnClickListener {
            val appContext = requireContext().applicationContext
            viewModel.addMovieToWatchlist(movie) { ok ->
                val msg = if (ok) "${movie.displayTitle} listeye eklendi!" else "Film eklenemedi."
                Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.layoutTmdbButton).setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.themoviedb.org/movie/${movie.id}")))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(requireContext(), "Tarayıcı bulunamadı.", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun setupActionCards() {
        binding.cardOdaKur.setOnClickListener {
            findNavController().navigate(AnaMenuFragmentDirections.actionAnaMenuFragmentToFiltreFragment())
        }
        binding.cardOdayaKatil.setOnClickListener {
            findNavController().navigate(AnaMenuFragmentDirections.actionAnaMenuFragmentToOdayaKatilFragment())
        }
        binding.cardAnindaEslestir.setOnClickListener {
            findNavController().navigate(AnaMenuFragmentDirections.actionAnaMenuFragmentToHizliDuelloBeklemeFragment())
        }
    }

    override fun onDestroyView() {
        adView?.destroy()
        adView = null
        super.onDestroyView()
        _binding = null
    }
}
