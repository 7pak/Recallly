package com.at.recallly.domain.usecase.backup

import com.at.recallly.core.result.Result
import com.at.recallly.domain.model.BackupMetadata
import com.at.recallly.domain.repository.BackupRepository

class GetBackupInfoUseCase(private val backupRepository: BackupRepository) {
    suspend operator fun invoke(): Result<BackupMetadata?> = backupRepository.getRemoteBackupMetadata()
}
