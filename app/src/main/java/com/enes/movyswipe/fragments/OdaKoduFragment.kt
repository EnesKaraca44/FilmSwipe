package com.enes.movyswipe.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.enes.movyswipe.R
import com.enes.movyswipe.databinding.FragmentOdaKoduBinding
import com.enes.movyswipe.viewmodels.OdaKoduViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OdaKoduFragment : Fragment() {

    private var _binding: FragmentOdaKoduBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OdaKoduViewModel by viewModels()
    private val args: OdaKoduFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOdaKoduBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // ViewModel odayı yalnızca bir kez oluşturur (döndürmede yeni oda açılmaz).
        viewModel.createRoomFromFilters(args.genres, args.StartYear)
        binding.tvOdaKodu.setOnClickListener { shareCode() }
        binding.btnKopyala.setOnClickListener { copyCode() }
        binding.errorView.btnRetry.setOnClickListener { viewModel.retry(args.genres, args.StartYear) }
        observeViewModelStates()
    }

    /** "Kopyala" düğmesi eskiden hiçbir şey yapmıyordu. */
    private fun copyCode() {
        val code = viewModel.roomCode.value ?: return
        val clipboard = requireContext().getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.duel_room_code_clip_label), code))
        // Android 13+ panoya kopyalanınca kendi bildirimini gösterir; eski sürümlerde biz söyleyelim.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(requireContext(), R.string.duel_room_code_copied, Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareCode() {
        val code = viewModel.roomCode.value ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getString(R.string.duel_share_room_text, code))
        }
        startActivity(Intent.createChooser(intent, getString(R.string.duel_share_room_chooser)))
    }

    private fun observeViewModelStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    // Hata toast yerine kalıcı panelde: ekran döndürmede/geri gelmede tekrar çıkmaz.
                    combine(viewModel.isLoading, viewModel.error, ::Pair).collect { (isLoading, error) ->
                        binding.progressBar.isVisible = isLoading
                        binding.contentLayout.isVisible = !isLoading && error == null
                        binding.errorView.root.isVisible = !isLoading && error != null
                        if (error != null) binding.errorView.tvErrorMessage.text = error
                    }
                }
                launch {
                    viewModel.roomCode.collect { roomCode -> if (roomCode != null) binding.tvOdaKodu.text = roomCode }
                }
                launch {
                    viewModel.navigateToDuel.collect { code ->
                        if (findNavController().currentDestination?.id != R.id.odaKoduFragment) return@collect
                        Toast.makeText(requireContext(), R.string.duel_room_partner_joined, Toast.LENGTH_SHORT).show()
                        findNavController().navigate(OdaKoduFragmentDirections.actionOdaKoduFragmentToDuelloFragment(code))
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
