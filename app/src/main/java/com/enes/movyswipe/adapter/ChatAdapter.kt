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
import java.util.Date
import java.util.Locale

class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DiffCallback()) {

    var currentUserId: String = ""
        set(value) {
            if (field == value) return
            field = value
            @Suppress("NotifyDataSetChanged") // görünüm tipleri (gönderilen/alınan) değişir
            notifyDataSetChanged()
        }

    /** Karşı tarafın mesajına uzun basınca (şikayet / engelle menüsü). */
    var onReceivedMessageLongClick: (ChatMessage) -> Unit = {}

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    inner class SentMessageViewHolder(private val binding: ItemMessageSentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.tvMessage.text = message.text
            binding.tvTime.text = message.timestampMillis?.let { timeFormat.format(Date(it)) }.orEmpty()
        }
    }

    inner class ReceivedMessageViewHolder(private val binding: ItemMessageReceivedBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.tvMessage.text = message.text
            binding.tvSenderNickname.text = message.senderNickname
            binding.tvTime.text = message.timestampMillis?.let { timeFormat.format(Date(it)) }.orEmpty()
            binding.root.setOnLongClickListener {
                onReceivedMessageLongClick(message)
                true
            }
        }
    }

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_SENT) {
            SentMessageViewHolder(ItemMessageSentBinding.inflate(inflater, parent, false))
        } else {
            ReceivedMessageViewHolder(ItemMessageReceivedBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        // Eskiden zaman damgası kıyaslanıyordu; aynı milisaniyedeki mesajlar karışabiliyordu.
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
            oldItem == newItem && oldItem.id == newItem.id
    }

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }
}
