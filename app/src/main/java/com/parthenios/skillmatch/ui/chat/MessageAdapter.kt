package com.parthenios.skillmatch.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.parthenios.skillmatch.data.Message
import com.parthenios.skillmatch.data.MessageType
import com.parthenios.skillmatch.databinding.ItemMessageReceivedBinding
import com.parthenios.skillmatch.databinding.ItemMessageSentBinding
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val currentUserId: String
) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemMessageSentBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SentMessageViewHolder(binding)
            }
            VIEW_TYPE_RECEIVED -> {
                val binding = ItemMessageReceivedBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ReceivedMessageViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
        }
    }

    inner class SentMessageViewHolder(
        private val binding: ItemMessageSentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.apply {
                tvMessage.text = message.content
                tvTimestamp.text = formatTimestamp(message.timestamp)
            }
        }
    }

    inner class ReceivedMessageViewHolder(
        private val binding: ItemMessageReceivedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.apply {
                tvMessage.text = message.content
                tvTimestamp.text = formatTimestamp(message.timestamp)
            }
        }
    }

    private fun formatTimestamp(timestamp: Date?): String {
        return if (timestamp != null) {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.format(timestamp)
        } else {
            ""
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
    
    // Geçici mesajları filtrele (gerçek mesajlar yüklendiğinde)
    fun filterTempMessages(): List<Message> {
        return currentList.filter { !it.id.startsWith("temp_") }
    }
}
