package com.parthenios.skillmatch.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Message(
    var id: String = "",
    val matchId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "", // Geriye dönük uyum için düz içerik (opsiyonel)
    val messageType: MessageType = MessageType.TEXT,
    @ServerTimestamp
    val timestamp: Date? = null,
    val isRead: Boolean = false,
    // Şifreleme alanları (E2E)
    val encVersion: Int = 0, // 0: plaintext, 1: E2E v1
    val ciphertext: String? = null, // Base64
    val nonce: String? = null, // Base64 (AES-GCM IV)
    val senderPub: String? = null // Gönderici public key (peer keşfi için)
)

enum class MessageType {
    TEXT,
    IMAGE,
    SYSTEM // Eşleşme başladı, eşleşme bitti gibi sistem mesajları
}
