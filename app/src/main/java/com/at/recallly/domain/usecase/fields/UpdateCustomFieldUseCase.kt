package com.at.recallly.domain.usecase.fields

import com.at.recallly.domain.model.PersonaField
import com.at.recallly.domain.repository.CustomFieldRepository

class UpdateCustomFieldUseCase(
    private val customFieldRepository: CustomFieldRepository
) {
    suspend operator fun invoke(field: PersonaField) {
        customFieldRepository.updateCustomField(field)
    }
}
