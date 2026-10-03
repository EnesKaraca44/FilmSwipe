package com.enes.movyswipe.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.enes.movyswipe.adapter.ChatAdapter
import com.enes.movyswipe.databinding.FragmentSohbetBinding
import com.enes.movyswipe.util.AuthManager
import com.enes.movyswipe.viewmodels.SohbetViewModel
import kotlinx.coroutines.launch



class SohbetFragment : Fragment() {

    private var _binding: FragmentSohbetBinding? = null
    private val binding get() = _binding!!


    private var currentUserId: String? = null // <-- YENİ DEĞİŞKEN

    private val viewModel: SohbetViewModel by viewModels()
    private val args: SohbetFragmentArgs by navArgs()

    // 1. Adaptör için bir değişken tanımlıyoruz.
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSohbetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

// Cihazın benzersiz ID'sini bir kere alıp değişkene atıyoruz.
        viewLifecycleOwner.lifecycleScope.launch {

            currentUserId = AuthManager.getCurrentUserId()

            // 2. Kurulum fonksiyonlarımızı çağırıyoruz.
            setupRecyclerView()
            setupClickListeners()
            viewLifecycleOwner.lifecycleScope.launch {
                // 3. Firebase'den benzersiz kullanıcı ID'sini alıyoruz.
                val userId = AuthManager.getCurrentUserId()

                // 4. Aldığımız ID'yi, adaptörün public değişkenine atıyoruz.
                chatAdapter.currentUserId = userId

                // 5. Mesajları dinlemeyi ve gözlemlemeyi başlatıyoruz.
                viewModel.startListeningToMessages(args.roomCode)
                observeMessages()
            }


        // 3. Mesajları dinlemeyi başlatıyoruz.
        viewModel.startListeningToMessages(args.roomCode)
    }

}

    // 4. RecyclerView'ı kuran fonksiyon.
    private fun setupRecyclerView() {
        // Adaptörümüzü oluşturuyoruz.

        chatAdapter = ChatAdapter()
        binding.recyclerViewSohbet.adapter = chatAdapter

        // RecyclerView için bir LayoutManager oluşturuyoruz.
        val layoutManager = LinearLayoutManager(requireContext())
        // Bu ayar, yeni mesajlar eklendiğinde listenin en altından başlamasını sağlar.
        layoutManager.stackFromEnd = true

        // XML'deki RecyclerView'a adaptörümüzü ve layout manager'ımızı bağlıyoruz.
        binding.recyclerViewSohbet.adapter = chatAdapter
        binding.recyclerViewSohbet.layoutManager = layoutManager
    }

    // "Gönder" butonunu dinleyen fonksiyon.
    private fun setupClickListeners() {
        binding.btnGonder.setOnClickListener {
            val messageText = binding.etMesaj.text.toString().trim()
            if (messageText.isNotEmpty()&& currentUserId!=null) {
                val nickname = AuthManager.getUserNickname(requireContext())
                viewModel.sendMessage(args.roomCode, messageText,currentUserId
                !!,nickname)
                binding.etMesaj.text.clear()
            }
        }
    }

    // 5. ViewModel'den gelen mesajları dinleyip, adaptöre gönderen fonksiyon.
    private fun observeMessages() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.messages.collect { messageList ->
                    // Logcat'e yazdırmak yerine, şimdi listeyi adaptöre gönderiyoruz.
                    chatAdapter.submitList(messageList)

                    // Yeni mesaj geldiğinde veya ekran açıldığında,
                    // listeyi en son mesaja kaydırıyoruz.
                    if (messageList.isNotEmpty()) {
                        binding.recyclerViewSohbet.scrollToPosition(messageList.size - 1)
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