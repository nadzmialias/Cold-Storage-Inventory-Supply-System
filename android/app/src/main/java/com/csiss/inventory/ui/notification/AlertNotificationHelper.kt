package com.csiss.inventory.ui.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.csiss.inventory.data.SystemAlertRow

object AlertNotificationHelper {
    private const val CHANNEL_ID = "csiss_critical_alerts"
    private var lastNotifiedCount = -1

    fun showSystemPopupIfNeeded(context: Context, openAlerts: List<SystemAlertRow>) {
        if (openAlerts.isEmpty() || openAlerts.size == lastNotifiedCount) return
        lastNotifiedCount = openAlerts.size

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CSISS Critical Storage Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pop-up alerts for Low Stock, Expiry, and Temperature breaches"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val topAlert = openAlerts.first()
        val summaryText = if (openAlerts.size == 1) {
            topAlert.message
        } else {
            "${topAlert.title} (+${openAlerts.size - 1} more active alerts require attention)"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("⚠️ CSISS Alert (${openAlerts.size} Active)")
            .setContentText(summaryText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(1001, notification)
    }
}