package com.at.recallly.data.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.at.recallly.MainActivity
import com.at.recallly.R
import timber.log.Timber

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val description = intent.getStringExtra(EXTRA_DESCRIPTION) ?: ""
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ReminderNotificationChannel.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(description)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            Timber.w(e, "Notification permission not granted")
        }

        // Clean up from pending reminders store
        try {
            val preferencesManager: com.at.recallly.data.local.datastore.PreferencesManager =
                org.koin.java.KoinJavaComponent.get(com.at.recallly.data.local.datastore.PreferencesManager::class.java)
            kotlinx.coroutines.runBlocking {
                preferencesManager.removePendingReminder(notificationId)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to clean up pending reminder")
        }
    }
}
