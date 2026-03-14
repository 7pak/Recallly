package com.at.recallly.domain.usecase.fields

import com.at.recallly.domain.repository.CustomFieldRepository
import com.at.recallly.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.first

class DeleteCustomFieldUseCase(
    private val customFieldRepository: CustomFieldRepository,
    private val onboardingRepository: OnboardingRepository
) {
    suspend operator fun invoke(fieldId: String) {
        customFieldRepository.deleteCustomField(fieldId)

        // Remove from selected fields
        val currentIds = onboardingRepository.selectedFieldIds.first()
        if (fieldId in currentIds) {
            onboardingRepository.saveFieldsOnly(currentIds - fieldId)
        }
    }
}
