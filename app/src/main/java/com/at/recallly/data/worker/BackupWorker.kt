package com.at.recallly.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.at.recallly.data.local.datastore.PreferencesManager
import com.at.recallly.domain.repository.BackupRepository
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import com.at.recallly.core.result.Result as AppResult

class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val backupRepository: BackupRepository by inject()
    private val preferencesManager: PreferencesManager by inject()

    override suspend fun doWork(): Result {
        val enabled = preferencesManager.driveBackupEnabled.first()
        if (!enabled) {
            Timber.d("BackupWorker: backup disabled, skipping")
            return Result.success()
        }

        return when (val result = backupRepository.backup()) {
            is AppResult.Success -> {
                Timber.d("BackupWorker: backup completed successfully")
                Result.success()
            }
            is AppResult.Error -> {
                Timber.e(result.exception, "BackupWorker: backup failed, will retry")
                Result.retry()
            }
        }
    }
}
