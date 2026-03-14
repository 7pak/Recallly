package com.at.recallly.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.at.recallly.data.local.datastore.PreferencesManager
import com.at.recallly.domain.repository.ReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent
import timber.log.Timber

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        try {
            val preferencesManager: PreferencesManager =
                KoinJavaComponent.get(PreferencesManager::class.java)
            val reminderScheduler: ReminderScheduler =
                KoinJavaComponent.get(ReminderScheduler::class.java)

            runBlocking {
                val reminders = preferencesManager.getAllPendingReminders().first()
                val now = System.currentTimeMillis()

                for (reminder in reminders) {
                    if (reminder.triggerAtMillis > now) {
                        reminderScheduler.schedule(
                            title = reminder.title,
                            description = reminder.description,
                            triggerAtMillis = reminder.triggerAtMillis,
                            reminderId = reminder.id
                        )
                        Timber.d("Re-scheduled reminder ${reminder.id} after boot")
                    } else {
                        preferencesManager.removePendingReminder(reminder.id)
                        Timber.d("Removed expired reminder ${reminder.id}")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to re-schedule reminders after boot")
        }
    }
}
