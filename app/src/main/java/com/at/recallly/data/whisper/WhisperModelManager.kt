package com.at.recallly.data.whisper

import android.content.Context
import com.at.recallly.domain.model.ModelDownloadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

class WhisperModelManager(private val context: Context) {

    private val modelDir = File(context.filesDir, "models")
    private val modelFile = File(modelDir, MODEL_FILENAME)
    private val tempFile = File(modelDir, "$MODEL_FILENAME.tmp")

    private val _downloadState = MutableStateFlow<ModelDownloadState>(
        if (isModelDownloaded()) ModelDownloadState.Downloaded
        else ModelDownloadState.NotDownloaded
    )
    val downloadState: StateFlow<ModelDownloadState> = _downloadState.asStateFlow()

    @Volatile
    private var isCancelled = false

    fun isModelDownloaded(): Boolean = modelFile.exists() && modelFile.length() > 0

    fun getModelPath(): String = modelFile.absolutePath

    suspend fun downloadModel() = withContext(Dispatchers.IO) {
        if (isModelDownloaded()) {
            _downloadState.value = ModelDownloadState.Downloaded
            return@withContext
        }

        isCancelled = false
        modelDir.mkdirs()
        tempFile.delete()

        try {
            _downloadState.value = ModelDownloadState.Downloading(0f)

            val url = URL(MODEL_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Server returned ${connection.responseCode}")
            }

            val totalBytes = connection.contentLengthLong
            var downloadedBytes = 0L

            connection.inputStream.buffered(8192).use { input ->
                tempFile.outputStream().buffered(8192).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isCancelled || !coroutineContext.isActive) {
                            tempFile.delete()
                            _downloadState.value = ModelDownloadState.NotDownloaded
                            return@withContext
                        }
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0) {
                            _downloadState.value = ModelDownloadState.Downloading(
                                (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                            )
                        }
                    }
                }
            }

            if (isCancelled) {
                tempFile.delete()
                _downloadState.value = ModelDownloadState.NotDownloaded
                return@withContext
            }

            tempFile.renameTo(modelFile)
            _downloadState.value = ModelDownloadState.Downloaded
            Timber.i("Whisper model downloaded: ${modelFile.length()} bytes")
        } catch (e: Exception) {
            tempFile.delete()
            Timber.e(e, "Model download failed")
            _downloadState.value = ModelDownloadState.Error(
                e.message ?: "Download failed"
            )
        }
    }

    fun cancelDownload() {
        isCancelled = true
    }

    suspend fun deleteModel() = withContext(Dispatchers.IO) {
        modelFile.delete()
        tempFile.delete()
        _downloadState.value = ModelDownloadState.NotDownloaded
    }

    fun needsMigration(): Boolean {
        val oldFile = File(modelDir, OLD_MODEL_FILENAME)
        return oldFile.exists() && oldFile.length() > 0
    }

    suspend fun migrateFromEnglishModel() = withContext(Dispatchers.IO) {
        val oldFile = File(modelDir, OLD_MODEL_FILENAME)
        if (oldFile.exists()) {
            oldFile.delete()
            Timber.i("Deleted old English-only whisper model")
        }
        _downloadState.value = ModelDownloadState.NotDownloaded
    }

    companion object {
        const val MODEL_FILENAME = "ggml-base.bin"
        const val MODEL_SIZE_MB = 142
        private const val OLD_MODEL_FILENAME = "ggml-base.en.bin"
        private const val MODEL_URL =
            "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin"
    }
}
