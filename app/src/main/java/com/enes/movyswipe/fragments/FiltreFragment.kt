package com.enes.movyswipe.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.navigation.fragment.findNavController
import com.enes.movyswipe.databinding.FragmentFiltreBinding
import com.enes.movyswipe.fragments.FiltreFragmentDirections


class FiltreFragment : Fragment() {
   private lateinit var binding: FragmentFiltreBinding



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       binding=FragmentFiltreBinding.inflate(inflater, container, false)
        val years=(2024 downTo 1980).map{
            it.toString()

        }
        val yearAdapter= ArrayAdapter(requireContext(),android.R.layout.simple_spinner_item,years)
        binding.spinnerYear.adapter=yearAdapter


        binding.btnDuelloyuBaslat.setOnClickListener {

            val selectedGenres = mutableListOf<Int>()
            if (binding.cbAksiyon.isChecked) {
                selectedGenres.add(28)
            }
            if (binding.cbKomedi.isChecked) {
                selectedGenres.add(35)
            }
            if (binding.cbDram.isChecked) {
                selectedGenres.add(18)
            }
            if (binding.cbBilimKurgu.isChecked) {
                selectedGenres.add(876)
            }

            if (binding.cbKorku.isChecked) {
                selectedGenres.add(27)
            }
            if (binding.cbRomantik.isChecked) {
                selectedGenres.add(10749)
            }
            val selectedYearString = binding.spinnerYear.selectedItem.toString()
            val selectedYearInt = selectedYearString.toInt()

            val actıon = FiltreFragmentDirections.actionFiltreFragmentToOdaKoduFragment(
                selectedGenres.toIntArray(),
                selectedYearInt
            )
            findNavController().navigate(actıon)

        }



        return binding.root
    }


}