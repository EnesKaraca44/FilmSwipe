package com.enes.movyswipe.fragments

import android.R
import android.app.AlertDialog
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.enes.movyswipe.adapter.IzlemeListesiAdapter
import com.enes.movyswipe.databinding.FragmentIzlemeListesiBinding

import com.enes.movyswipe.viewmodels.IzlemeListesiViewModel
import kotlinx.coroutines.launch

class IzlemeListesiFragment : Fragment() {

    private var _binding: FragmentIzlemeListesiBinding? = null
    private val binding get() = _binding!!

    // ViewModel'i oluşturuyoruz.
    private val viewModel: IzlemeListesiViewModel by viewModels()
    // Adapter için bir değişken.
    private lateinit var watchlistAdapter: IzlemeListesiAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIzlemeListesiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeWatchlist()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        watchlistAdapter = IzlemeListesiAdapter()

        // Adaptörün 'onDeleteClick' fonksiyonunu, ne yapması gerektiğini söyleyerek dolduruyoruz.
        watchlistAdapter.onDeleteClick = { movieToDelete ->
            // Kullanıcıya silmek istediğinden emin olup olmadığını soralım.
            AlertDialog.Builder(requireContext())
                .setTitle("Filmi Kaldır")
                .setMessage("'${movieToDelete.title}' filmini izleme listenizden kaldırmak istediğinizden emin misiniz?")
                .setPositiveButton("Evet, Kaldır") { _, _ ->
                    // "Evet" derse, ViewModel'deki silme fonksiyonunu çağır.
                    viewModel.deleteMovie(movieToDelete)
                    Toast.makeText(requireContext(), "Film kaldırıldı.", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Hayır", null)
                .show()
        }

        binding.recyclerViewFilmler.adapter = watchlistAdapter
        binding.recyclerViewFilmler.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun observeWatchlist() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.watchlistMovies.collect { movies ->
                    // 1. Gelen yeni listeyi adaptöre göndererek RecyclerView'ı güncelle.
                    watchlistAdapter.submitList(movies)

                    // 2. Listenin boş olup olmamasına göre "Boş Durum" kartını göster/gizle.
                    binding.cardEmptyState.isVisible = movies.isEmpty()
                    binding.recyclerViewFilmler.isVisible = movies.isNotEmpty()

                    // 3. İstatistikleri güncelle.
                    binding.tvToplamFilm.text = movies.size.toString()

                    // TODO: 'Bu Ay' ve 'Bu Hafta' istatistiklerini hesaplama mantığı
                    // Bu, her filmin 'addedTimestamp' değerini kontrol ederek yapılır.
                    // Şimdilik onları da toplam sayı yapabiliriz.
                    binding.tvBuAy.text = movies.size.toString()
                    binding.tvBuHafta.text = movies.size.toString()
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.rowListeyiTemizle.setOnClickListener {
            // Listede film varsa uyarıyı göster.
            if (viewModel.watchlistMovies.value.isNotEmpty()) {
                showClearListConfirmationDialog()
            } else {
                Toast.makeText(requireContext(), "Listeniz zaten boş.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.rowListeyiPaylas.setOnClickListener {
            shareWatchlist()
        }
    }

    private fun showClearListConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Listeyi Temizle")
            .setMessage("İzleme listenizdeki tüm filmleri silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.")

            .setPositiveButton("Evet, Sil") { _, _ ->
                // Kullanıcı "Evet" derse, ViewModel'deki temizleme işlemini başlat.
                viewModel.clearWatchlist()
                Toast.makeText(requireContext(), "İzleme listesi temizlendi.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Hayır", null) // "Hayır" butonu hiçbir şey yapmaz, diyalog kapanır.
            .show()
    }

    private fun shareWatchlist() {
        // ViewModel'den o anki film listesini alıyoruz.
        val movies = viewModel.watchlistMovies.value

        if (movies.isEmpty()) {
            Toast.makeText(requireContext(), "Paylaşmak için listenizde film bulunmuyor.", Toast.LENGTH_SHORT).show()
            return
        }

        // Film listesini, alt alta sıralanmış, güzel bir metne dönüştürüyoruz.
        val shareText = buildString {
            append("FilmSwipe İzleme Listem:\n\n")
            movies.forEachIndexed { index, movie ->
                append("${index + 1}. ${movie.title} (${movie.releaseDate.substring(0, 4)})\n")
            }
        }

        // Android'in paylaşma mekanizmasını (Intent) oluşturuyoruz.
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        // Kullanıcının WhatsApp, E-posta vb. seçebileceği paylaşma menüsünü başlatıyoruz.
        val shareIntent = Intent.createChooser(sendIntent, "İzleme Listeni Paylaş")
        startActivity(shareIntent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}