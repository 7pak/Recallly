package com.at.recallly.data.repository

import com.at.recallly.data.local.file.VoiceNoteDto
import com.at.recallly.data.local.file.VoiceNoteFileStorage
import com.at.recallly.domain.model.Persona
import com.at.recallly.domain.model.VoiceNote
import com.at.recallly.domain.repository.VoiceNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class VoiceNoteRepositoryImpl(
    private val fileStorage: VoiceNoteFileStorage
) : VoiceNoteRepository {

    private val cache = MutableStateFlow<List<VoiceNoteDto>>(emptyList())

    suspend fun loadFromDisk() {
        cache.value = fileStorage.readAll()
    }

    override fun getAllVoiceNotes(): Flow<List<VoiceNote>> {
        return cache.map { dtos -> dtos.map { it.toDomain() }.sortedByDescending { it.createdAt } }
    }

    override suspend fun saveVoiceNote(voiceNote: VoiceNote) {
        val dto = voiceNote.toDto()
        val updated = cache.value + dto
        cache.value = updated
        fileStorage.writeAll(updated)
    }

    override suspend fun updateVoiceNote(voiceNote: VoiceNote) {
        val dto = voiceNote.toDto()
        val updated = cache.value.map { if (it.id == dto.id) dto else it }
        cache.value = updated
        fileStorage.writeAll(updated)
    }

    override suspend fun deleteVoiceNote(id: String) {
        val updated = cache.value.filter { it.id != id }
        cache.value = updated
        fileStorage.writeAll(updated)
    }

    fun getVoiceNoteById(id: String): VoiceNote? {
        return cache.value.find { it.id == id }?.toDomain()
    }

    private fun VoiceNoteDto.toDomain(): VoiceNote = VoiceNote(
        id = id,
        transcript = transcript,
        extractedFields = extractedFields,
        additionalNotes = additionalNotes,
        persona = try { Persona.valueOf(persona) } catch (_: Exception) { Persona.SALES_REP },
        createdAt = createdAt,
        extractionPending = extractionPending
    )

    private fun VoiceNote.toDto(): VoiceNoteDto = VoiceNoteDto(
        id = id,
        transcript = transcript,
        extractedFields = extractedFields,
        additionalNotes = additionalNotes,
        persona = persona.name,
        createdAt = createdAt,
        extractionPending = extractionPending
    )
}
