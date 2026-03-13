package com.at.recallly.domain.repository

import com.at.recallly.domain.model.OnboardingStep
import com.at.recallly.domain.model.Persona
import com.at.recallly.domain.model.WorkSchedule
import kotlinx.coroutines.flow.Flow

interface OnboardingRepository {
    fun getOnboardingStepForUser(uid: String): Flow<OnboardingStep>
    val selectedPersona: Flow<Persona?>
    val selectedFieldIds: Flow<Set<String>>
    val workSchedule: Flow<WorkSchedule>
    suspend fun savePersona(uid: String, persona: Persona)
    suspend fun savePersonaOnly(persona: Persona)
    suspend fun saveFields(fieldIds: Set<String>)
    suspend fun saveFieldsOnly(fieldIds: Set<String>)
    suspend fun saveScheduleAndComplete(workSchedule: WorkSchedule)
    suspend fun saveSchedule(workSchedule: WorkSchedule)
    val dataConsentAccepted: Flow<Boolean>
    suspend fun saveDataConsent(driveBackupEnabled: Boolean)
    suspend fun clearOnboarding()
}
