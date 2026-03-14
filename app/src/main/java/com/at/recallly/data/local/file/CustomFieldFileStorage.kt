package com.at.recallly.data.local.file

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File

class CustomFieldFileStorage(private val context: Context) {

    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val file: File
        get() = File(context.filesDir, FILE_NAME)

    suspend fun readAll(): List<CustomFieldDto> = mutex.withLock {
        try {
            if (!file.exists()) return@withLock emptyList()
            val text = file.readText()
            if (text.isBlank()) return@withLock emptyList()
            json.decodeFromString<CustomFieldListDto>(text).fields
        } catch (e: Exception) {
            Timber.e(e, "Failed to read custom fields from disk")
            emptyList()
        }
    }

    suspend fun writeAll(fields: List<CustomFieldDto>) = mutex.withLock {
        try {
            val dto = CustomFieldListDto(fields)
            file.writeText(json.encodeToString(CustomFieldListDto.serializer(), dto))
        } catch (e: Exception) {
            Timber.e(e, "Failed to write custom fields to disk")
        }
    }

    companion object {
        private const val FILE_NAME = "custom_fields.json"
    }
}
