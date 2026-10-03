package com.enes.movyswipe.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.enes.movyswipe.data.ChatMessage
import com.enes.movyswipe.databinding.ItemMessageReceivedBinding
import com.enes.movyswipe.databinding.ItemMessageSentBinding
import java.text.SimpleDateFormat
import java.util.*

// --- DEĞİŞİKLİK 1: Constructor'a 'currentUserId'yi ekliyoruz ---
class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DiffCallback()) {

    var currentUserId: String = ""
    // Artık sabit bir 'currentUserId' değişkenine ihtiyacımız yok, çünkü constructor'dan geliyor.

    // İki farklı ViewHolder'ımızı tanımlıyoruz. Biri gönderilen, diğeri alınan mesajlar için.
    inner class SentMessageViewHolder(private val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chatMessage: ChatMessage) {
            binding.tvMessage.text = chatMessage.text
            // Zaman damgasını okunabilir bir formata çeviriyoruz
            if (chatMessage.timestamp is Long) {
                binding.tvTime.text = formatTimestamp(chatMessage.timestamp)
            }
        }
    }

    inner class ReceivedMessageViewHolder(private val binding: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chatMessage: ChatMessage) {
            binding.tvMessage.text = chatMessage.text
            binding.tvSenderNickname.text = chatMessage.senderNickname
            if (chatMessage.timestamp is Long) {
                binding.tvTime.text = formatTimestamp(chatMessage.timestamp)
            }
        }
    }

    // Bu metot, her bir pozisyondaki mesajın tipini belirler (Gönderilen mi? Alınan mı?).
    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        // Artık 'currentUserId' constructor'dan gelen dinamik bir değer.
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    // Bu metot, belirlenen view tipine göre doğru XML layout'unu seçip ViewHolder oluşturur.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val binding = ItemMessageSentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            SentMessageViewHolder(binding)
        } else { // viewType == VIEW_TYPE_RECEIVED
            val binding = ItemMessageReceivedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ReceivedMessageViewHolder(binding)
        }
    }

    // Bu metot, oluşturulan ViewHolder'a doğru veriyi (mesaj içeriği) bağlar.
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
        }
    }

    // Zaman damgasını (Long) "14:32" gibi bir String'e çeviren yardımcı fonksiyon.
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // DiffUtil, listenin verimli bir şekilde güncellenmesini sağlar.
    class DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            // Mesajların timestamp'leri genellikle benzersizdir.
            return oldItem.timestamp.toString() == newItem.timestamp.toString()
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }
}