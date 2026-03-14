package com.at.recallly.data.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.TimeUnit

class BackupWorkScheduler(private val context: Context) {

    companion object {
        private const val PERIODIC_BACKUP_TAG = "recallly_periodic_backup"
    }

    fun schedulePeriodicBackup() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<BackupWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(PERIODIC_BACKUP_TAG)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                PERIODIC_BACKUP_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        Timber.d("Scheduled periodic backup (24h)")
    }

    fun cancelPeriodicBackup() {
        WorkManager.getInstance(context)
            .cancelUniqueWork(PERIODIC_BACKUP_TAG)
        Timber.d("Cancelled periodic backup")
    }

    fun enqueueManualBackup(): UUID {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(request)
        Timber.d("Enqueued manual backup")
        return request.id
    }
}
