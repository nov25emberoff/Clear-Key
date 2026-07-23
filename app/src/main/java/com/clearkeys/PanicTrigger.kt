package com.clearkeys

import android.content.Context
import android.content.Intent
import android.util.Log

class PanicTrigger {

    companion object {
        private const val TAG = "FCCPanic"
    }

    fun triggerPanic(context: Context) {
        Log.w(TAG, "🚨 PANIC TRIGGERED")
        val intent = Intent(context, PanicService::class.java).apply {
            action = "com.clearkeys.PANIC"
        }
        context.startService(intent)
        showPanicNotification(context)
    }

    fun stopPanic(context: Context) {
        PanicService.isPanicActive = false
        val intent = Intent(context, PanicService::class.java).apply {
            action = "com.clearkeys.STOP_PANIC"
        }
        context.startService(intent)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancel(999)
        Log.w(TAG, "🚨 PANIC STOPPED")
    }

    private fun showPanicNotification(context: Context) {
        val channel = android.app.NotificationChannel("panic", "Panic Mode", android.app.NotificationManager.IMPORTANCE_HIGH)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.createNotificationChannel(channel)

        val stopIntent = Intent(context, PanicService::class.java).apply {
            action = "com.clearkeys.STOP_PANIC"
        }
        val stopPendingIntent = android.app.PendingIntent.getService(
            context, 0, stopIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = android.app.Notification.Builder(context, "panic")
            .setContentTitle("🛡 FCC Panic Mode")
            .setContentText("All visible messages are being cleaned")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "STOP", stopPendingIntent)
            .setPriority(android.app.Notification.PRIORITY_HIGH)
            .build()

        notificationManager.notify(999, notification)
    }
}
