package com.enes.movyswipe.fragments

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
import com.enes.movyswipe.data.MovieFormat
import com.enes.movyswipe.data.WatchlistMovie
import com.enes.movyswipe.databinding.FragmentIzlemeListesiBinding
import com.enes.movyswipe.viewmodels.IzlemeListesiViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class IzlemeListesiFragment : Fragment() {

    private var _binding: FragmentIzlemeListesiBinding? = null
    private val binding get() = _binding!!
    private val viewModel: IzlemeListesiViewModel by viewModels()
    private lateinit var watchlistAdapter: IzlemeListesiAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentIzlemeListesiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeWatchlist()
        binding.rowListeyiTemizle.setOnClickListener {
            if (viewModel.watchlistMovies.value.isNotEmpty()) showClearListConfirmationDialog()
            else Toast.makeText(requireContext(), "Listeniz zaten boş.", Toast.LENGTH_SHORT).show()
        }
        binding.rowListeyiPaylas.setOnClickListener { shareWatchlist() }
    }

    private fun setupRecyclerView() {
        watchlistAdapter = IzlemeListesiAdapter()
        watchlistAdapter.onDeleteClick = { movie ->
            AlertDialog.Builder(requireContext())
                .setTitle("Filmi Kaldır")
                .setMessage("'${movie.title}' filmini izleme listenizden kaldırmak istediğinizden emin misiniz?")
                .setPositiveButton("Evet, Kaldır") { _, _ ->
                    viewModel.deleteMovie(movie)
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
                    watchlistAdapter.submitList(movies)
                    binding.cardEmptyState.isVisible = movies.isEmpty()
                    binding.recyclerViewFilmler.isVisible = movies.isNotEmpty()
                    // Eskiden üç istatistik de toplam sayıyı gösteriyordu.
                    val now = System.currentTimeMillis()
                    binding.tvToplamFilm.text = movies.size.toString()
                    binding.tvBuAy.text = movies.countAddedWithin(now, 30).toString()
                    binding.tvBuHafta.text = movies.countAddedWithin(now, 7).toString()
                }
            }
        }
    }

    private fun List<WatchlistMovie>.countAddedWithin(now: Long, days: Long): Int =
        count { now - it.addedTimestamp <= TimeUnit.DAYS.toMillis(days) }

    private fun showClearListConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Listeyi Temizle")
            .setMessage("İzleme listenizdeki tüm filmleri silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.")
            .setPositiveButton("Evet, Sil") { _, _ ->
                viewModel.clearWatchlist()
                Toast.makeText(requireContext(), "İzleme listesi temizlendi.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    private fun shareWatchlist() {
        val movies = viewModel.watchlistMovies.value
        if (movies.isEmpty()) {
            Toast.makeText(requireContext(), "Paylaşmak için listenizde film bulunmuyor.", Toast.LENGTH_SHORT).show()
            return
        }
        val shareText = buildString {
            append("FilmSwipe İzleme Listem:\n\n")
            movies.forEachIndexed { index, movie ->
                // Çıkış tarihi boş filmlerde eskiden substring() çöküyordu.
                val year = MovieFormat.releaseYear(movie.releaseDate)?.let { " ($it)" }.orEmpty()
                append("${index + 1}. ${movie.title}$year\n")
            }
        }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, "İzleme Listeni Paylaş"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
