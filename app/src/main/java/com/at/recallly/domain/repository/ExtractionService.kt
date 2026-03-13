package com.at.recallly.domain.repository

import com.at.recallly.domain.model.ExtractionResult
import com.at.recallly.domain.model.Persona
import com.at.recallly.domain.model.PersonaField
import com.at.recallly.core.result.Result

interface ExtractionService {
    suspend fun extractFields(
        transcript: String,
        persona: Persona,
        fields: List<PersonaField>,
        language: String = "en"
    ): Result<ExtractionResult>
}
