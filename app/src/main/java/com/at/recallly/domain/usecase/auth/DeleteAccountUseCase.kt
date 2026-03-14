package com.at.recallly.domain.usecase.auth

import com.at.recallly.data.local.datastore.PreferencesManager
import com.at.recallly.domain.repository.AuthRepository
import com.at.recallly.domain.repository.CustomFieldRepository
import com.at.recallly.domain.repository.VoiceNoteRepository

class DeleteAccountUseCase(
    private val authRepository: AuthRepository,
    private val voiceNoteRepository: VoiceNoteRepository,
    private val customFieldRepository: CustomFieldRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke() {
        voiceNoteRepository.deleteAllVoiceNotes()
        customFieldRepository.deleteAllCustomFields()
        preferencesManager.clearAllData()
        authRepository.deleteAccount()
    }
}
