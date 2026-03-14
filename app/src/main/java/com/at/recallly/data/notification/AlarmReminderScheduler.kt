package com.at.recallly.data.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.at.recallly.data.local.datastore.PreferencesManager
import com.at.recallly.domain.repository.ReminderScheduler
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class AlarmReminderScheduler(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) : ReminderScheduler {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(
        title: String,
        description: String,
        triggerAtMillis: Long,
        reminderId: Int
    ) {
        // Don't schedule if the time is in the past
        if (triggerAtMillis <= System.currentTimeMillis()) {
            Timber.d("Skipping reminder $reminderId — trigger time is in the past")
            return
        }

        // Persist for boot re-scheduling
        runBlocking {
            preferencesManager.savePendingReminder(
                PendingReminder(
                    id = reminderId,
                    title = title,
                    description = description,
                    triggerAtMillis = triggerAtMillis
                )
            )
        }

        val pendingIntent = createPendingIntent(reminderId, title, description)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                )
            }
            Timber.d("Scheduled reminder $reminderId at $triggerAtMillis")
        } catch (e: SecurityException) {
            // Fallback to inexact alarm
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
            )
            Timber.w(e, "Exact alarm denied, using inexact for reminder $reminderId")
        }
    }

    override fun cancel(reminderId: Int) {
        val pendingIntent = createPendingIntent(reminderId, "", "")
        alarmManager.cancel(pendingIntent)
        runBlocking { preferencesManager.removePendingReminder(reminderId) }
        Timber.d("Cancelled reminder $reminderId")
    }

    private fun createPendingIntent(
        reminderId: Int,
        title: String,
        description: String
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_DESCRIPTION, description)
            putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
