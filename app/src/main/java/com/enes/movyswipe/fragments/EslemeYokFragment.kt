package com.enes.movyswipe.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.enes.movyswipe.R
import com.enes.movyswipe.databinding.FragmentEslemeYokBinding

class EslemeYokFragment : Fragment() {
    private var _binding: FragmentEslemeYokBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEslemeYokBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val nav = findNavController()
        // Aksiyonlar ana menüye kadar geri yığını temizler (nav_host.xml'de popUpTo).
        binding.btnTekrarDene.setOnClickListener {
            nav.navigate(EslemeYokFragmentDirections.actionEslemeYokFragmentToFiltreFragment())
        }
        binding.btnFiltreleriDegistir.setOnClickListener {
            nav.navigate(EslemeYokFragmentDirections.actionEslemeYokFragmentToFiltreFragment())
        }
        binding.btnHizliDuello.setOnClickListener {
            nav.navigate(EslemeYokFragmentDirections.actionEslemeYokFragmentToHizliDuelloBeklemeFragment())
        }
        binding.btnAnaMenu.setOnClickListener {
            if (!nav.popBackStack(R.id.anaMenuFragment, false)) {
                nav.navigate(EslemeYokFragmentDirections.actionEslemeYokFragmentToAnaMenuFragment())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
