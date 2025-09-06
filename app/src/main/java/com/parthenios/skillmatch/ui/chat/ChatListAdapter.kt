package com.parthenios.skillmatch.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.parthenios.skillmatch.data.Match
import com.parthenios.skillmatch.databinding.ItemChatBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatListAdapter(
    private val onChatClick: (Match) -> Unit,
    private val currentUserId: String
) : ListAdapter<Match, ChatListAdapter.ChatViewHolder>(ChatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatViewHolder(
        private val binding: ItemChatBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(match: Match) {
            binding.apply {
                // Kullanıcı adını ayarla
                tvUserName.text = if (match.user1Id == currentUserId) {
                    match.user2Name
                } else {
                    match.user1Name
                }
                
                // Son mesaj zamanını göster
                tvLastMessageTime.text = formatTimestamp(match.lastMessageAt)
                
                // Eşleşme durumunu göster
                tvMatchStatus.text = "Aktif Eşleşme"
                tvMatchStatus.setTextColor(binding.root.context.getColor(android.R.color.holo_green_dark))
                
                // Kalan süreyi hesapla ve göster
                val remainingTime = calculateRemainingTime(match.expiresAt)
                tvRemainingTime.text = remainingTime
                
                // Chat'e tıklama
                root.setOnClickListener {
                    onChatClick(match)
                }
            }
        }
        
        
        private fun formatTimestamp(timestamp: Date?): String {
            return if (timestamp != null) {
                val now = Date()
                val diff = now.time - timestamp.time
                
                when {
                    diff < 60 * 1000 -> "Az önce"
                    diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} dk önce"
                    diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} saat önce"
                    else -> {
                        val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
                        sdf.format(timestamp)
                    }
                }
            } else {
                "Henüz mesaj yok"
            }
        }
        
        private fun calculateRemainingTime(expiresAt: Date?): String {
            return if (expiresAt != null) {
                val now = Date()
                val diff = expiresAt.time - now.time
                
                if (diff <= 0) {
                    "Süre doldu"
                } else {
                    val hours = diff / (60 * 60 * 1000)
                    val minutes = (diff % (60 * 60 * 1000)) / (60 * 1000)
                    "${hours}s ${minutes}dk kaldı"
                }
            } else {
                "Süre bilinmiyor"
            }
        }
    }

    class ChatDiffCallback : DiffUtil.ItemCallback<Match>() {
        override fun areItemsTheSame(oldItem: Match, newItem: Match): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Match, newItem: Match): Boolean {
            return oldItem == newItem
        }
    }
}
