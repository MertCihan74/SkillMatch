package com.parthenios.skillmatch.notifications

import android.app.PendingIntent
import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.parthenios.skillmatch.MainActivity
import com.parthenios.skillmatch.utils.NotificationHelper

class SkillMatchMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token yenilendiğinde app açıldığında da kaydedeceğiz; burada da log tutabiliriz
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "SkillMatch"
        val body = message.notification?.body ?: message.data["body"] ?: "Yeni bildirim"

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        NotificationHelper.showMessageNotification(this, title, body, intent)
    }
}


