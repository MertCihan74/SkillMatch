package com.parthenios.skillmatch.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Message(
    var id: String = "",
    val matchId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val messageType: MessageType = MessageType.TEXT,
    @ServerTimestamp
    val timestamp: Date? = null,
    val isRead: Boolean = false
)

enum class MessageType {
    TEXT,
    IMAGE,
    SYSTEM // Eşleşme başladı, eşleşme bitti gibi sistem mesajları
}
