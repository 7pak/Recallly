package com.at.recallly.domain.usecase.voice

import com.at.recallly.core.result.Result
import com.at.recallly.domain.model.ExtractionResult
import com.at.recallly.domain.model.Persona
import com.at.recallly.domain.model.PersonaField
import com.at.recallly.domain.repository.ExtractionService

class ExtractFieldsUseCase(
    private val extractionService: ExtractionService
) {
    suspend operator fun invoke(
        transcript: String,
        persona: Persona,
        fields: List<PersonaField>
    ): Result<ExtractionResult> {
        if (transcript.isBlank()) {
            return Result.Error(IllegalArgumentException("Transcript is empty"))
        }
        return extractionService.extractFields(transcript, persona, fields)
    }
}
