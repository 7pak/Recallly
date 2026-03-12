package com.at.recallly.presentation.onboarding

import com.at.recallly.domain.model.Persona
import java.time.DayOfWeek
import java.time.LocalTime

sealed interface OnboardingUiEvent {
    data class SelectPersona(val persona: Persona) : OnboardingUiEvent
    data object ConfirmPersona : OnboardingUiEvent
    data class ToggleField(val fieldId: String) : OnboardingUiEvent
    data object ToggleSelectAll : OnboardingUiEvent
    data object ConfirmFields : OnboardingUiEvent
    data class ToggleWorkDay(val day: DayOfWeek) : OnboardingUiEvent
    data class SetStartTime(val time: LocalTime) : OnboardingUiEvent
    data class SetEndTime(val time: LocalTime) : OnboardingUiEvent
    data object CompleteOnboarding : OnboardingUiEvent
    data object ClearValidationError : OnboardingUiEvent
}
