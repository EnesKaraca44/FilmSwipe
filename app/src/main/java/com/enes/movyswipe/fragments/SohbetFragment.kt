package com.enes.movyswipe.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.enes.movyswipe.R
import com.enes.movyswipe.adapter.ChatAdapter
import com.enes.movyswipe.data.ChatMessage
import com.enes.movyswipe.databinding.FragmentSohbetBinding
import com.enes.movyswipe.util.AuthManager
import com.enes.movyswipe.viewmodels.SohbetViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Eşleşen iki kişinin sohbeti. Google Play kullanıcı içeriği (UGC) politikası gereği:
 * topluluk kuralları onayı, mesaj şikayet etme ve kullanıcı engelleme içerir.
 */
@AndroidEntryPoint
class SohbetFragment : Fragment() {

    private var _binding: FragmentSohbetBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SohbetViewModel by viewModels()
    private val args: SohbetFragmentArgs by navArgs()
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSohbetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        binding.etMesaj.filters = arrayOf(InputFilter.LengthFilter(ChatMessage.MAX_LENGTH))
        binding.btnGonder.setOnClickListener {
            val sent = viewModel.sendMessage(
                binding.etMesaj.text.toString(),
                AuthManager.getUserNickname(requireContext())
            )
            if (sent) binding.etMesaj.text.clear()
        }
        binding.btnGeri.setOnClickListener { findNavController().popBackStack() }
        binding.btnMenu.setOnClickListener { showSafetyMenu(it) }
        observeViewModel()

        if (viewModel.communityRulesAccepted) viewModel.start(args.roomCode) else showCommunityRules()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        chatAdapter.onReceivedMessageLongClick = { message -> showMessageActions(message) }
        binding.recyclerViewSohbet.adapter = chatAdapter
        binding.recyclerViewSohbet.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.currentUserId.collect { chatAdapter.currentUserId = it.orEmpty() } }
                launch {
                    viewModel.messages.collect { messages ->
                        chatAdapter.submitList(messages) {
                            if (messages.isNotEmpty()) binding.recyclerViewSohbet.scrollToPosition(messages.size - 1)
                        }
                        binding.emptyStateLayout.isVisible = messages.isEmpty()
                    }
                }
                launch {
                    viewModel.toasts.collect { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
                }
            }
        }
    }

    private fun showCommunityRules() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.chat_rules_title)
            .setMessage(R.string.chat_rules_message)
            .setCancelable(false)
            .setPositiveButton(R.string.chat_rules_accept) { _, _ ->
                viewModel.acceptCommunityRules()
                viewModel.start(args.roomCode)
            }
            .setNegativeButton(R.string.chat_cancel) { _, _ -> findNavController().popBackStack() }
            .show()
    }

    private fun showSafetyMenu(anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            menu.add(0, MENU_REPORT, 0, R.string.chat_report_user)
            menu.add(0, MENU_BLOCK, 1, R.string.chat_block_user)
            setOnMenuItemClickListener { item ->
                val partner = viewModel.partnerId()
                if (partner == null) {
                    Toast.makeText(requireContext(), R.string.chat_partner_unavailable, Toast.LENGTH_SHORT).show()
                } else if (item.itemId == MENU_REPORT) {
                    showReportReasons(partner, null)
                } else {
                    confirmBlock(partner)
                }
                true
            }
        }.show()
    }

    private fun showMessageActions(message: ChatMessage) {
        AlertDialog.Builder(requireContext())
            .setTitle(ChatAdapter.displayNickname(requireContext(), message.senderNickname))
            .setItems(arrayOf(getString(R.string.chat_report_message), getString(R.string.chat_block_user))) { _, which ->
                if (which == 0) showReportReasons(message.senderId, message) else confirmBlock(message.senderId)
            }
            .show()
    }

    private fun showReportReasons(userId: String, message: ChatMessage?) {
        val reasons = arrayOf(
            getString(R.string.chat_reason_harassment),
            getString(R.string.chat_reason_sexual),
            getString(R.string.chat_reason_spam),
            getString(R.string.chat_reason_hate),
            getString(R.string.chat_reason_other)
        )
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.chat_report_reason_title)
            .setItems(reasons) { _, which -> viewModel.report(userId, reasons[which], message) }
            .setNegativeButton(R.string.chat_cancel, null)
            .show()
    }

    private fun confirmBlock(userId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.chat_block_user)
            .setMessage(R.string.chat_block_confirm_message)
            .setPositiveButton(R.string.chat_block) { _, _ -> viewModel.block(userId) }
            .setNegativeButton(R.string.chat_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val MENU_REPORT = 1
        const val MENU_BLOCK = 2
    }
}
