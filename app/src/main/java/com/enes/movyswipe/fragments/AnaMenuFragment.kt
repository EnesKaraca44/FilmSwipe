package com.enes.movyswipe.fragments

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import com.enes.movyswipe.R
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.enes.movyswipe.MainActivity
import com.enes.movyswipe.adapter.PopulerFilmAdapter
import com.enes.movyswipe.data.Movie
import com.enes.movyswipe.databinding.FragmentAnaMenuBinding
import com.enes.movyswipe.util.AuthManager
import com.enes.movyswipe.viewmodels.AnaMenuViewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.launch

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AnaMenuFragment : Fragment() {

    // _binding ve binding yapısını kullanmak en güvenlisidir.
    private var _binding: FragmentAnaMenuBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AnaMenuViewModel by viewModels()
    private lateinit var populerFilmAdapter: PopulerFilmAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnaMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ana eylem kartlarının tıklama olayları
        setupActionCards()

        // Alt navigasyon barının tıklama olayları
        setupBottomNavigation()

        observePopularMovies()
        setupRecyclerView()
        loadWelcomeMessage()

        // ViewModel'e "Verileri yüklemeye başla" komutunu veriyoruz.
        // Gerekli olan 'context'i de Fragment'tan sağlıyoruz.
        viewModel.loadData()

        // 1. AdMob SDK'sını başlat. Bu, uygulama boyunca sadece bir kez yapılmalıdır.
        // MainActivity'de yapmak daha iyi olabilir ama test için burası da olur.
        MobileAds.initialize(requireContext()) {}
        // 2. Bir reklam isteği oluştur.
        val adRequest = AdRequest.Builder().build()

        // 3. XML'deki AdView'a bu isteği göndererek reklamı yükle.
        binding.adView.loadAd(adRequest)

        // Reklamları yüklemeden önce, kullanıcının premium olup olmadığını kontrol et.
        val isPremium = (activity as? MainActivity)?.isUserPremium() ?: false

        if (!isPremium) {
            // Kullanıcı premium DEĞİLSE, reklamları yükle.
            MobileAds.initialize(requireContext()) {}
            val adRequest = AdRequest.Builder().build()
            binding.adView.loadAd(adRequest)
        } else {
            // Kullanıcı premium İSE, reklam alanını tamamen gizle.
            binding.adView.isVisible = false
        }

    }
    private fun loadWelcomeMessage() {
        // AuthManager'ı kullanarak kaydedilmiş kullanıcı adını alıyoruz.
        val nickname = AuthManager.getUserNickname(requireContext())

        // Aldığımız kullanıcı adıyla birlikte karşılama mesajını oluşturuyoruz.
        binding.tvWelcomeMessage.text = "Hoş geldin, $nickname!"
    }
    private fun setupRecyclerView() {
        // Adapter'ı oluştururken, tıklama olayında ne olacağını belirtiyoruz.
        populerFilmAdapter = PopulerFilmAdapter { movie ->
            showMovieDetailsDialog(movie)
        }
        binding.recyclerViewPopulerFilmler.apply { // XML'deki RecyclerView ID'si
            adapter = populerFilmAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
    }
    private fun observePopularMovies() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Popüler filmleri dinle
                launch {
                    viewModel.popularMovies.collect { movies ->
                        populerFilmAdapter.submitList(movies)
                    }
                }

                // Yükleme durumunu dinle
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.isVisible = isLoading
                    }
                }

                // Hata durumunu dinle
                launch {
                    viewModel.error.collect { errorMessage ->
                        val hasError = !errorMessage.isNullOrBlank()

                        // Hata varsa hata layout'unu göster, film kartını gizle.
                        binding.layoutError.isVisible = hasError
                        binding.cardViewPopulerFilmler.isVisible = !hasError // CardView'ı gizliyoruz.

                        if (hasError) {
                            binding.tvErrorMessage.text = errorMessage
                        }
                    }
                }
            }
        }
    }
    // Film detaylarını gösteren diyalog fonksiyonu.
    private fun showMovieDetailsDialog(movie: Movie) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_film_detay) // Diyalog XML'imiz

        // Diyalog içindeki view'ları bul ve doldur
        val poster = dialog.findViewById<ImageView>(R.id.imageViewDialogPoster)
        val title = dialog.findViewById<TextView>(R.id.textViewDialogTitle)
        val overview = dialog.findViewById<TextView>(R.id.textViewDialogOverview)
        val closeButton = dialog.findViewById<View>(R.id.textViewDialogClose)
        val watchlistButtonLayout = dialog.findViewById<LinearLayout>(R.id.layoutWatchlistButton) // Listeme Ekle butonu
        val tmdbButtonLayout = dialog.findViewById<LinearLayout>(R.id.layoutTmdbButton) // TMDB butonu
        title.text = movie.title
        overview.text = movie.overview
        val posterUrl = "https://image.tmdb.org/t/p/w500${movie.posterPath}"
         Glide.with(this).load(posterUrl).into(poster)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        // "Listeme Ekle" butonuna tıklandığında...
        watchlistButtonLayout.setOnClickListener {
            // 1. ÖNCE: ViewModel'deki veritabanına ekleme fonksiyonunu çağır.
            viewModel.addMovieToWatchlist(movie)
            // TODO: viewModel.addMovieToWatchlist(movie)
            Toast.makeText(requireContext(), "${movie.title} listeye eklendi!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        tmdbButtonLayout.setOnClickListener {
            val tmdbUrl = "https://www.themoviedb.org/movie/${movie.id}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tmdbUrl))
            startActivity(intent)
        }
        dialog.show()
    }

    private fun setupActionCards() {
        binding.cardOdaKur.setOnClickListener {
            val action = AnaMenuFragmentDirections.actionAnaMenuFragmentToFiltreFragment()
            findNavController().navigate(action)
        }
        binding.cardOdayaKatil.setOnClickListener {
            val action = AnaMenuFragmentDirections.actionAnaMenuFragmentToOdayaKatilFragment()
            findNavController().navigate(action)
        }
        binding.cardAnindaEslestir.setOnClickListener {
            val action = AnaMenuFragmentDirections.actionAnaMenuFragmentToHizliDuelloBeklemeFragment()
            findNavController().navigate(action)
        }
    }

    // --- YENİ EKLENEN FONKSİYON ---
    private fun setupBottomNavigation() {
        // "Ana Sayfa" tabına tıklandığında bir şey yapmaya gerek yok,
        // çünkü zaten bu ekrandayız.
       // binding.tabAnaSayfa.setOnClickListener {
            // İsteğe bağlı: Listenin en üstüne kaydırma gibi bir eylem eklenebilir.
       // }

        // "Ayarlar" tabına tıklandığında AyarlarFragment'a git.
       // binding.tabAyarlar.setOnClickListener {
       //     val action = AnaMenuFragmentDirections.actionAnaMenuFragmentToAyarlarFragment()
       //     findNavController().navigate(action)
        //   }

        // "İzleme Listesi" tabına tıklandığında IzlemeListesiFragment'a git.
        //   binding.tabIzlemeListesi.setOnClickListener {
        //      val action = AnaMenuFragmentDirections.actionAnaMenuFragmentToIzlemeListesiFragment()
        //     findNavController().navigate(action)
        //   }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}