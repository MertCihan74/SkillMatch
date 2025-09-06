package com.parthenios.skillmatch.data

import java.util.Date

data class MatchRequest(
    val id: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val fromUserName: String = "",
    val toUserName: String = "",
    val fromUserPhoto: String = "",
    val toUserPhoto: String = "",
    val status: MatchRequestStatus = MatchRequestStatus.PENDING,
    val createdAt: Date = Date(),
    val matchedAt: Date? = null
)

enum class MatchRequestStatus {
    PENDING,    // Beklemede
    ACCEPTED,   // Kabul edildi
    REJECTED,   // Reddedildi
    MATCHED     // Eşleşme tamamlandı
}
