package com.at.recallly.domain.usecase.fields

import com.at.recallly.domain.model.FieldType
import com.at.recallly.domain.model.Persona
import com.at.recallly.domain.model.PersonaField
import com.at.recallly.domain.repository.CustomFieldRepository
import com.at.recallly.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.first
import java.util.UUID

class AddCustomFieldUseCase(
    private val customFieldRepository: CustomFieldRepository,
    private val onboardingRepository: OnboardingRepository
) {
    suspend operator fun invoke(name: String, description: String, persona: Persona, fieldType: FieldType = FieldType.TEXT) {
        val field = PersonaField(
            id = "custom_${UUID.randomUUID()}",
            displayName = name,
            description = description,
            persona = persona,
            fieldType = fieldType
        )
        customFieldRepository.addCustomField(field)

        // Auto-select the new field
        val currentIds = onboardingRepository.selectedFieldIds.first()
        onboardingRepository.saveFieldsOnly(currentIds + field.id)
    }
}
