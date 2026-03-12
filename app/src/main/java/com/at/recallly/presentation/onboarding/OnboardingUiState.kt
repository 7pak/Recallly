package com.at.recallly.presentation.onboarding

import com.at.recallly.domain.model.OnboardingStep
import com.at.recallly.domain.model.Persona
import com.at.recallly.domain.model.PersonaField
import com.at.recallly.domain.model.WorkSchedule
import java.time.DayOfWeek
import java.time.LocalTime

data class OnboardingUiState(
    val onboardingStep: OnboardingStep = OnboardingStep.NOT_STARTED,
    val isLoading: Boolean = true,
    val selectedPersona: Persona? = null,
    val availableFields: List<PersonaField> = emptyList(),
    val selectedFieldIds: Set<String> = emptySet(),
    val workDays: Set<DayOfWeek> = WorkSchedule.DEFAULT_WORK_DAYS,
    val startTime: LocalTime = WorkSchedule.DEFAULT_START_TIME,
    val endTime: LocalTime = WorkSchedule.DEFAULT_END_TIME,
    val validationError: String? = null,
    val isFieldsConfirmed: Boolean = false,
    val isComplete: Boolean = false
)
