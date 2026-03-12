package com.at.recallly.data.repository

import com.at.recallly.data.local.datastore.PreferencesManager
import com.at.recallly.domain.model.OnboardingStep
import com.at.recallly.domain.model.Persona
import com.at.recallly.domain.model.WorkSchedule
import com.at.recallly.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalTime

class OnboardingRepositoryImpl(
    private val preferencesManager: PreferencesManager
) : OnboardingRepository {

    override fun getOnboardingStepForUser(uid: String): Flow<OnboardingStep> =
        preferencesManager.getOnboardingStepForUser(uid).map { OnboardingStep.fromValue(it) }

    override val selectedPersona: Flow<Persona?> =
        preferencesManager.selectedPersona.map { name ->
            name?.let { runCatching { Persona.valueOf(it) }.getOrNull() }
        }

    override val selectedFieldIds: Flow<Set<String>> =
        preferencesManager.selectedFieldIds

    override val workSchedule: Flow<WorkSchedule> =
        combine(
            preferencesManager.workDays,
            preferencesManager.workStartTime,
            preferencesManager.workEndTime
        ) { days, startTime, endTime ->
            WorkSchedule(
                workDays = if (days.isNotEmpty()) {
                    days.mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }.toSet()
                } else {
                    WorkSchedule.DEFAULT_WORK_DAYS
                },
                startTime = startTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
                    ?: WorkSchedule.DEFAULT_START_TIME,
                endTime = endTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
                    ?: WorkSchedule.DEFAULT_END_TIME
            )
        }

    override suspend fun savePersona(uid: String, persona: Persona) {
        preferencesManager.savePersonaStep(uid, persona.name)
    }

    override suspend fun saveFields(fieldIds: Set<String>) {
        preferencesManager.saveFieldsStep(fieldIds)
    }

    override suspend fun saveScheduleAndComplete(workSchedule: WorkSchedule) {
        preferencesManager.saveScheduleStep(
            workDays = workSchedule.workDays.map { it.name }.toSet(),
            workStartTime = workSchedule.startTime.toString(),
            workEndTime = workSchedule.endTime.toString()
        )
    }

    override val dataConsentAccepted: Flow<Boolean> =
        preferencesManager.dataConsentAccepted

    override suspend fun saveDataConsent(driveBackupEnabled: Boolean) {
        preferencesManager.saveDataConsent(driveBackupEnabled)
    }

    override suspend fun clearOnboarding() {
        preferencesManager.clearOnboarding()
    }
}
