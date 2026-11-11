package com.enes.movyswipe.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.enes.movyswipe.R
import com.enes.movyswipe.data.MovieFilters
import com.enes.movyswipe.databinding.FragmentFiltreBinding
import java.util.Calendar

class FiltreFragment : Fragment() {
    private var _binding: FragmentFiltreBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFiltreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnGeri.setOnClickListener { findNavController().navigateUp() }

        // Eskiden liste 2024'te bitiyordu; artık güncel yıldan başlıyor.
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val yearOptions = listOf(ALL_YEARS) + (currentYear downTo 1950).map { it.toString() }
        binding.spinnerYear.adapter =
            ArrayAdapter(requireContext(), R.layout.item_spinner_year, yearOptions).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        binding.btnDuelloyuBaslat.setOnClickListener {
            val selectedGenres = buildList {
                if (binding.cbAksiyon.isChecked) add(MovieFilters.Genre.ACTION)
                if (binding.cbKomedi.isChecked) add(MovieFilters.Genre.COMEDY)
                if (binding.cbDram.isChecked) add(MovieFilters.Genre.DRAMA)
                // Bilim Kurgu'nun TMDB ID'si 878 (eskiden yanlışlıkla 876 yazılıydı → 0 film).
                if (binding.cbBilimKurgu.isChecked) add(MovieFilters.Genre.SCIENCE_FICTION)
                if (binding.cbKorku.isChecked) add(MovieFilters.Genre.HORROR)
                if (binding.cbRomantik.isChecked) add(MovieFilters.Genre.ROMANCE)
            }
            val selected = binding.spinnerYear.selectedItem?.toString()
            val startYear = selected?.toIntOrNull() ?: 0 // 0 = yıl filtresi yok

            findNavController().navigate(
                FiltreFragmentDirections.actionFiltreFragmentToOdaKoduFragment(selectedGenres.toIntArray(), startYear)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val ALL_YEARS = "Tüm yıllar"
    }
}
