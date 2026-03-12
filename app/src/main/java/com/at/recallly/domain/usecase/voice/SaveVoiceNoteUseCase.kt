package com.at.recallly.domain.usecase.voice

import com.at.recallly.domain.model.VoiceNote
import com.at.recallly.domain.repository.VoiceNoteRepository

class SaveVoiceNoteUseCase(
    private val voiceNoteRepository: VoiceNoteRepository
) {
    suspend operator fun invoke(voiceNote: VoiceNote) {
        voiceNoteRepository.saveVoiceNote(voiceNote)
    }
}
