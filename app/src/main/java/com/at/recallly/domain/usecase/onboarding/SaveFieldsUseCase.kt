package com.at.recallly.domain.usecase.onboarding

import com.at.recallly.domain.repository.OnboardingRepository

class SaveFieldsUseCase(private val repository: OnboardingRepository) {
    suspend operator fun invoke(fieldIds: Set<String>) {
        repository.saveFields(fieldIds)
    }
}
