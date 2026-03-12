package com.at.recallly.domain.usecase.onboarding

import com.at.recallly.domain.model.WorkSchedule
import com.at.recallly.domain.repository.OnboardingRepository

class SaveScheduleUseCase(private val repository: OnboardingRepository) {
    suspend operator fun invoke(workSchedule: WorkSchedule) {
        repository.saveScheduleAndComplete(workSchedule)
    }
}
