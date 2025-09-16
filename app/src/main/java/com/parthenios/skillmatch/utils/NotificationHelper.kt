package com.parthenios.skillmatch.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.parthenios.skillmatch.R

object NotificationHelper {
    private const val CHANNEL_MESSAGES = "channel_messages"
    private const val CHANNEL_MATCH = "channel_match_requests"

    private fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_MESSAGES) == null) {
                val ch = NotificationChannel(
                    CHANNEL_MESSAGES,
                    "Mesajlar",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                ch.description = "Yeni mesaj bildirimleri"
                ch.enableLights(true)
                ch.lightColor = Color.BLUE
                nm.createNotificationChannel(ch)
            }
            if (nm.getNotificationChannel(CHANNEL_MATCH) == null) {
                val ch = NotificationChannel(
                    CHANNEL_MATCH,
                    "Eşleşme İstekleri",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                ch.description = "Yeni eşleşme isteği bildirimleri"
                ch.enableLights(true)
                ch.lightColor = Color.MAGENTA
                nm.createNotificationChannel(ch)
            }
        }
    }

    fun showMessageNotification(
        context: Context,
        title: String,
        body: String,
        intent: Intent? = null,
        notificationId: Int = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
    ) {
        ensureChannels(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        intent?.let {
            val pi = PendingIntent.getActivity(
                context,
                notificationId,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setContentIntent(pi)
        }

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }

    fun showMatchRequestNotification(
        context: Context,
        title: String,
        body: String,
        intent: Intent? = null,
        notificationId: Int = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
    ) {
        ensureChannels(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_MATCH)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        intent?.let {
            val pi = PendingIntent.getActivity(
                context,
                notificationId,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setContentIntent(pi)
        }

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }
}


