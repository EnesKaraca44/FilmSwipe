package com.enes.movyswipe.fragments

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.enes.movyswipe.BuildConfig
import com.enes.movyswipe.R
import com.enes.movyswipe.adapter.IzlemeListesiAdapter
import com.enes.movyswipe.data.WatchlistSort
import com.enes.movyswipe.data.WatchlistStats
import com.enes.movyswipe.databinding.FragmentIzlemeListesiBinding
import com.enes.movyswipe.util.WatchlistShareHelper
import com.enes.movyswipe.viewmodels.IzlemeListesiViewModel
import com.enes.movyswipe.viewmodels.WatchlistUiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        binding.tvAltBaslik.text = "FilmSwipe Ortak Arşiv • v${BuildConfig.VERSION_NAME}"
        setupRecyclerView()
        setupSearch()
        setupClickListeners()
        observeState()
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

    private fun setupClickListeners() {
        binding.btnAra.setOnClickListener {
            if (binding.layoutArama.isVisible) closeSearch() else openSearch()
        }
        binding.btnFiltrele.setOnClickListener { showSortMenu(it) }
        binding.btnSiralama.setOnClickListener { showSortMenu(it) }

        binding.btnDuelloBaslat.setOnClickListener {
            navigate(IzlemeListesiFragmentDirections.actionIzlemeListesiFragmentToHizliDuelloBeklemeFragment())
        }
        binding.rowListeyiTemizle.setOnClickListener {
            if (viewModel.uiState.value.allMovies.isNotEmpty()) showClearListConfirmationDialog()
            else toast("Listeniz zaten boş.")
        }
        binding.rowListeyiPaylas.setOnClickListener { showShareOptions() }
        binding.rowFilmOdasinaDavetEt.setOnClickListener {
            // Oda kurma akışı: Filtre -> Oda Kodu (kod arkadaşla paylaşılır).
            navigate(IzlemeListesiFragmentDirections.actionIzlemeListesiFragmentToFiltreFragment())
        }
    }

    // ---------- Arama ----------

    private fun setupSearch() {
        // Ekran döndürülünce / sekmeye geri gelince açık arama korunur.
        val savedQuery = viewModel.currentQuery.value
        if (savedQuery.isNotEmpty()) {
            binding.layoutArama.isVisible = true
            binding.etArama.setText(savedQuery)
        }
        binding.btnAra.isSelected = binding.layoutArama.isVisible
        binding.etArama.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }
        binding.etArama.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                true
            } else false
        }
        binding.btnAramaKapat.setOnClickListener { closeSearch() }
    }

    private fun openSearch() {
        if (viewModel.uiState.value.allMovies.isEmpty()) {
            toast("Listende henüz aranacak film yok.")
            return
        }
        binding.layoutArama.isVisible = true
        binding.btnAra.isSelected = true
        binding.etArama.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.etArama, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun closeSearch() {
        binding.etArama.text?.clear()
        binding.etArama.clearFocus()
        binding.layoutArama.isVisible = false
        binding.btnAra.isSelected = false
        hideKeyboard()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etArama.windowToken, 0)
    }

    // ---------- Sıralama ----------

    private fun showSortMenu(anchor: View) {
        val state = viewModel.uiState.value
        if (state.allMovies.isEmpty()) {
            toast("Sıralamak için listende film bulunmuyor.")
            return
        }
        PopupMenu(requireContext(), anchor).apply {
            WatchlistSort.entries.forEachIndexed { index, sort ->
                menu.add(SORT_GROUP, index, index, sort.label).isChecked = sort == state.sort
            }
            menu.setGroupCheckable(SORT_GROUP, true, true)
            setOnMenuItemClickListener { item ->
                viewModel.setSort(WatchlistSort.entries[item.itemId])
                true
            }
        }.show()
    }

    // ---------- Durum ----------

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    private fun render(state: WatchlistUiState) {
        watchlistAdapter.submitList(state.visibleMovies)

        val hasMovies = state.allMovies.isNotEmpty()
        binding.cardEmptyState.isVisible = state.isListEmpty
        binding.layoutListeBaslik.isVisible = hasMovies
        binding.recyclerViewFilmler.isVisible = state.visibleMovies.isNotEmpty()
        binding.tvSonucYok.isVisible = state.hasNoSearchResults
        if (state.hasNoSearchResults) binding.tvSonucYok.text = "“${state.query.trim()}” ile eşleşen film yok."

        // Liste temizlenince açık kalan aramayı kapat.
        if (state.isListEmpty && binding.layoutArama.isVisible) closeSearch()

        binding.tvListeBaslik.text = if (state.query.isBlank()) "FİLMLERİM (${state.allMovies.size})"
        else "SONUÇLAR (${state.visibleMovies.size}/${state.allMovies.size})"
        binding.tvSiralama.text = state.sort.label
        binding.btnFiltrele.isSelected = state.sort != WatchlistSort.NEWEST

        renderStats(state.stats)
    }

    private fun renderStats(stats: WatchlistStats) {
        binding.tvToplamFilm.text = stats.total.toString()
        binding.tvToplamFilmAlt.text = if (stats.estimatedHours > 0) "≈${stats.estimatedHours} saat" else "0 saat"
        binding.tvBuAy.text = stats.thisMonth.toString()
        binding.tvBuAyAlt.text = "+${stats.addedToday} bugün"
        binding.tvBuHafta.text = stats.thisWeek.toString()
        binding.tvBuHaftaAlt.text = "Seri: ${stats.streakDays} gün"
        binding.tvIstatistikDurum.text = when {
            stats.thisWeek > 0 -> "Bu Hafta Güncel"
            stats.daysSinceLastAdded != null -> "Son ekleme: ${WatchlistStats.relativeDayLabel(stats.daysSinceLastAdded).lowercase()}"
            else -> "Henüz veri yok"
        }
    }

    // ---------- Liste yönetimi ----------

    private fun showClearListConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Listeyi Temizle")
            .setMessage("İzleme listenizdeki tüm filmleri silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.")
            .setPositiveButton("Evet, Sil") { _, _ ->
                viewModel.clearWatchlist()
                toast("İzleme listesi temizlendi.")
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    private fun showShareOptions() {
        if (viewModel.uiState.value.allMovies.isEmpty()) {
            toast("Paylaşmak için listenizde film bulunmuyor.")
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Listeyi Paylaş")
            .setItems(arrayOf("Hikaye görseli oluştur", "Metin ve link olarak paylaş")) { _, which ->
                if (which == 0) shareStoryImage() else shareAsText()
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    private fun shareAsText() {
        // Arama/sıralama ekranda nasılsa paylaşılan liste de o sırada olsun.
        val movies = viewModel.uiState.value.let { if (it.visibleMovies.isNotEmpty()) it.visibleMovies else it.allMovies }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, WatchlistShareHelper.buildShareText(movies))
        }
        startChooser(sendIntent)
    }

    private fun shareStoryImage() {
        val movies = viewModel.uiState.value.let { if (it.visibleMovies.isNotEmpty()) it.visibleMovies else it.allMovies }
        val appContext = requireContext().applicationContext
        toast("Hikaye görseli hazırlanıyor…")
        viewLifecycleOwner.lifecycleScope.launch {
            val uri = try {
                withContext(Dispatchers.IO) { WatchlistShareHelper.createStoryImage(appContext, movies) }
            } catch (e: Exception) {
                Log.e(TAG, "Hikaye görseli oluşturulamadı", e)
                toast("Görsel oluşturulamadı. Metin olarak paylaşmayı dene.")
                return@launch
            }
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "FilmSwipe İzleme Listem 🎬 ${WatchlistShareHelper.storeLink}")
                clipData = ClipData.newRawUri("", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startChooser(sendIntent)
        }
    }

    private fun startChooser(intent: Intent) {
        try {
            startActivity(Intent.createChooser(intent, "İzleme Listeni Paylaş"))
        } catch (e: ActivityNotFoundException) {
            toast("Paylaşım için uygun uygulama bulunamadı.")
        }
    }

    // ---------- Yardımcılar ----------

    private fun navigate(directions: NavDirections) {
        val navController = findNavController()
        // Hızlı çift dokunuşta ikinci navigate çağrısı çökmesin.
        if (navController.currentDestination?.id == R.id.izlemeListesiFragment) {
            hideKeyboard()
            navController.navigate(directions)
        }
    }

    private fun toast(message: String) {
        context?.let { Toast.makeText(it, message, Toast.LENGTH_SHORT).show() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val TAG = "IzlemeListesiFragment"
        const val SORT_GROUP = 1
    }
}
