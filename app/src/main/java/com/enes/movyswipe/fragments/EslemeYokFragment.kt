package com.enes.movyswipe.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.enes.movyswipe.R
import com.enes.movyswipe.databinding.FragmentEslemeYokBinding

class EslemeYokFragment : Fragment() {
    private var _binding: FragmentEslemeYokBinding? = null
    private val binding get() = _binding!!
    private val args: EslemeYokFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEslemeYokBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Eskiden sabit "25 film kontrol edildi" yazıyordu; artık gerçek deste boyutu.
        binding.layoutIstatistik.isVisible = args.checkedCount > 0
        binding.tvFilmSayisi.text = "${args.checkedCount} film kontrol edildi"

        // Aksiyonlar ana menüye kadar geri yığını temizler (nav_host.xml'de popUpTo).
        binding.btnTekrarDene.setOnClickListener {
            findNavController().navigate(EslemeYokFragmentDirections.actionEslemeYokFragmentToFiltreFragment())
        }
        binding.btnFiltreleriDegistir.setOnClickListener {
            findNavController().navigate(EslemeYokFragmentDirections.actionEslemeYokFragmentToFiltreFragment())
        }
        binding.btnHizliDuello.setOnClickListener {
            findNavController().navigate(EslemeYokFragmentDirections.actionEslemeYokFragmentToHizliDuelloBeklemeFragment())
        }
        binding.btnAnaMenu.setOnClickListener {
            val nav = findNavController()
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
