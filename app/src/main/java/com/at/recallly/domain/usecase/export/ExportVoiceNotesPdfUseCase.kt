package com.at.recallly.domain.usecase.export

import android.net.Uri
import com.at.recallly.core.result.Result
import com.at.recallly.data.export.PdfExportService
import com.at.recallly.domain.repository.CustomFieldRepository
import com.at.recallly.domain.repository.OnboardingRepository
import com.at.recallly.domain.repository.VoiceNoteRepository
import com.at.recallly.domain.usecase.auth.GetCurrentUserUseCase
import com.at.recallly.domain.usecase.onboarding.GetFieldsForPersonaUseCase
import kotlinx.coroutines.flow.first

class ExportVoiceNotesPdfUseCase(
    private val voiceNoteRepository: VoiceNoteRepository,
    private val onboardingRepository: OnboardingRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getFieldsForPersonaUseCase: GetFieldsForPersonaUseCase,
    private val pdfExportService: PdfExportService,
    private val customFieldRepository: CustomFieldRepository
) {
    suspend operator fun invoke(): Result<Uri> {
        return try {
            val voiceNotes = voiceNoteRepository.getAllVoiceNotes().first()
            if (voiceNotes.isEmpty()) {
                return Result.Error(Exception("empty"))
            }

            val persona = onboardingRepository.selectedPersona.first()
                ?: return Result.Error(Exception("No persona selected"))

            val selectedFieldIds = onboardingRepository.selectedFieldIds.first()
            val builtInFields = getFieldsForPersonaUseCase(persona)
            val customFields = customFieldRepository.getCustomFieldsForPersona(persona).first()
            val allFields = builtInFields + customFields
            val user = getCurrentUserUseCase().first()

            val uri = pdfExportService.generatePdf(
                voiceNotes = voiceNotes,
                persona = persona,
                selectedFieldIds = selectedFieldIds,
                allPersonaFields = allFields,
                displayName = user?.displayName
            )

            Result.Success(uri)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
