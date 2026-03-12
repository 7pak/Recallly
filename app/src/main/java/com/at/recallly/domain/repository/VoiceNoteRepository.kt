package com.at.recallly.domain.repository

import com.at.recallly.domain.model.VoiceNote
import kotlinx.coroutines.flow.Flow

interface VoiceNoteRepository {
    fun getAllVoiceNotes(): Flow<List<VoiceNote>>
    suspend fun saveVoiceNote(voiceNote: VoiceNote)
    suspend fun updateVoiceNote(voiceNote: VoiceNote)
    suspend fun deleteVoiceNote(id: String)
}
