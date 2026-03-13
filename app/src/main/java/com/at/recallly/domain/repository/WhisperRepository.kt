package com.at.recallly.domain.repository

import com.at.recallly.domain.model.ModelDownloadState
import kotlinx.coroutines.flow.StateFlow
import com.at.recallly.core.result.Result

interface WhisperRepository {
    val downloadState: StateFlow<ModelDownloadState>
    fun isModelDownloaded(): Boolean
    suspend fun downloadModel()
    suspend fun cancelDownload()
    suspend fun deleteModel()
    fun needsModelMigration(): Boolean
    suspend fun migrateModel()
    suspend fun transcribe(audioSamples: FloatArray, language: String = "en"): Result<String>
}
