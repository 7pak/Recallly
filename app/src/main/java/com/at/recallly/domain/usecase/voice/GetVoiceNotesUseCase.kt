package com.at.recallly.domain.usecase.voice

import com.at.recallly.domain.model.VoiceNote
import com.at.recallly.domain.repository.VoiceNoteRepository
import kotlinx.coroutines.flow.Flow

class GetVoiceNotesUseCase(
    private val voiceNoteRepository: VoiceNoteRepository
) {
    operator fun invoke(): Flow<List<VoiceNote>> {
        return voiceNoteRepository.getAllVoiceNotes()
    }
}
