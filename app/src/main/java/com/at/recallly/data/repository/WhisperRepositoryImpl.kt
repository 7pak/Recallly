package com.at.recallly.data.repository

import com.at.recallly.core.result.Result
import com.at.recallly.data.whisper.WhisperContext
import com.at.recallly.data.whisper.WhisperModelManager
import com.at.recallly.domain.model.ModelDownloadState
import com.at.recallly.domain.repository.WhisperRepository
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

class WhisperRepositoryImpl(
    private val modelManager: WhisperModelManager
) : WhisperRepository {

    private var whisperContext: WhisperContext? = null

    override val downloadState: StateFlow<ModelDownloadState> = modelManager.downloadState

    override fun isModelDownloaded(): Boolean = modelManager.isModelDownloaded()

    override suspend fun downloadModel() {
        modelManager.downloadModel()
    }

    override suspend fun cancelDownload() {
        modelManager.cancelDownload()
    }

    override suspend fun deleteModel() {
        whisperContext?.release()
        whisperContext = null
        modelManager.deleteModel()
    }

    override fun needsModelMigration(): Boolean = modelManager.needsMigration()

    override suspend fun migrateModel() {
        whisperContext?.release()
        whisperContext = null
        modelManager.migrateFromEnglishModel()
    }

    override suspend fun transcribe(audioSamples: FloatArray, language: String): Result<String> {
        return try {
            val ctx = getOrCreateContext()
            val text = ctx.transcribeData(audioSamples, language)
            if (text.isBlank()) {
                Result.Error(Exception("No speech detected in the recording"))
            } else {
                Result.Success(text)
            }
        } catch (e: Exception) {
            Timber.e(e, "Whisper transcription failed")
            Result.Error(e)
        }
    }

    private fun getOrCreateContext(): WhisperContext {
        whisperContext?.let { return it }
        if (!modelManager.isModelDownloaded()) {
            throw IllegalStateException("Whisper model not downloaded")
        }
        val ctx = WhisperContext.createFromFile(modelManager.getModelPath())
        whisperContext = ctx
        return ctx
    }
}
