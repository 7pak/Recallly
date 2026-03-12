package com.at.recallly.data.local.file

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File

class VoiceNoteFileStorage(private val context: Context) {

    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val file: File
        get() = File(context.filesDir, FILE_NAME)

    suspend fun readAll(): List<VoiceNoteDto> = mutex.withLock {
        try {
            if (!file.exists()) return@withLock emptyList()
            val text = file.readText()
            if (text.isBlank()) return@withLock emptyList()
            json.decodeFromString<VoiceNoteListDto>(text).notes
        } catch (e: Exception) {
            Timber.e(e, "Failed to read voice notes from disk")
            emptyList()
        }
    }

    suspend fun writeAll(notes: List<VoiceNoteDto>) = mutex.withLock {
        try {
            val dto = VoiceNoteListDto(notes)
            file.writeText(json.encodeToString(VoiceNoteListDto.serializer(), dto))
        } catch (e: Exception) {
            Timber.e(e, "Failed to write voice notes to disk")
        }
    }

    companion object {
        private const val FILE_NAME = "voice_notes.json"
    }
}
