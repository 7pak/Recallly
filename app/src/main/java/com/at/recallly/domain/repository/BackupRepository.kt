package com.at.recallly.domain.repository

import com.at.recallly.core.result.Result
import com.at.recallly.domain.model.BackupMetadata
import kotlinx.coroutines.flow.Flow

interface BackupRepository {
    suspend fun backup(): Result<BackupMetadata>
    suspend fun restore(): Result<BackupMetadata>
    suspend fun getRemoteBackupMetadata(): Result<BackupMetadata?>
    val lastBackupTimestamp: Flow<Long?>
}
