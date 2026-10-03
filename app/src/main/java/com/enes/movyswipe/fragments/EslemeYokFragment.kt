package com.enes.movyswipe.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.enes.movyswipe.databinding.FragmentEslemeYokBinding



class EslemeYokFragment : Fragment() {
    private var _binding: FragmentEslemeYokBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding=FragmentEslemeYokBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Butonların tıklama olaylarını ayarlayacağımız fonksiyonu çağırıyoruz.
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnTekrarDene.setOnClickListener {
            // Şimdilik filtre ekranına yönlendiriyoruz.
            findNavController().navigate(EslemeYokFragmentDirections.actionEslemeYokFragmentToFiltreFragment())
        }

        binding.btnFiltreleriDegistir.setOnClickListener {
            findNavController().navigate(EslemeYokFragmentDirections.actionEslemeYokFragmentToFiltreFragment())
        }

        binding.btnHizliDuello.setOnClickListener {
            findNavController().navigate(EslemeYokFragmentDirections.actionEslemeYokFragmentToHizliDuelloBeklemeFragment())
        }

        binding.btnAnaMenu.setOnClickListener {
            findNavController().navigate(EslemeYokFragmentDirections.actionEslemeYokFragmentToAnaMenuFragment())
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

