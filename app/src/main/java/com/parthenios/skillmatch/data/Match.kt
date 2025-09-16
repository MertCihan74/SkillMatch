package com.parthenios.skillmatch.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Match(
    var id: String = "",
    val user1Id: String = "",
    val user2Id: String = "",
    val user1Name: String = "",
    val user2Name: String = "",
    val status: MatchStatus = MatchStatus.ACTIVE,
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val lastMessageAt: Date? = null,
    val expiresAt: Date? = null // 24 saat sonra eşleşme biter
)

enum class MatchStatus {
    ACTIVE,     // Aktif eşleşme
    EXPIRED,    // 24 saat içinde mesaj atılmadı
    ENDED       // Kullanıcılardan biri eşleşmeyi bitirdi
}

