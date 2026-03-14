package com.at.recallly.domain.usecase.auth

import com.at.recallly.data.local.datastore.PreferencesManager
import com.at.recallly.domain.repository.CustomFieldRepository
import com.at.recallly.domain.repository.VoiceNoteRepository

class DeleteAllDataUseCase(
    private val voiceNoteRepository: VoiceNoteRepository,
    private val customFieldRepository: CustomFieldRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke() {
        voiceNoteRepository.deleteAllVoiceNotes()
        customFieldRepository.deleteAllCustomFields()
        preferencesManager.clearAllData()
    }
}
