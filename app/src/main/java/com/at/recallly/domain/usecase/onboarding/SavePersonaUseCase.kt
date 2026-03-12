package com.at.recallly.domain.usecase.onboarding

import com.at.recallly.domain.model.Persona
import com.at.recallly.domain.repository.OnboardingRepository

class SavePersonaUseCase(private val repository: OnboardingRepository) {
    suspend operator fun invoke(uid: String, persona: Persona) {
        repository.savePersona(uid, persona)
    }
}
