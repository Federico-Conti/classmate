package com.example.childapp.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.childapp.R

object Notifications {
    const val CHANNEL_ID = "WiFiScannerId"
    const val NOTIF_ID = 1001

    @RequiresApi(Build.VERSION_CODES.O)
    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "WiFi Scanner",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        nm.createNotificationChannel(channel)
    }

    fun foregroundNotification(context: Context, text: String): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("SchoolAlert")
            .setContentText(text)
            .setOngoing(true)
            .build()
    }
}
