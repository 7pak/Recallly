package com.at.recallly.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.at.recallly.domain.model.OnboardingStep
import com.at.recallly.domain.model.WorkSchedule
import com.at.recallly.domain.repository.OnboardingRepository
import com.at.recallly.domain.usecase.auth.GetCurrentUserUseCase
import com.at.recallly.domain.usecase.onboarding.GetFieldsForPersonaUseCase
import com.at.recallly.domain.usecase.onboarding.SaveFieldsUseCase
import com.at.recallly.domain.usecase.onboarding.SavePersonaUseCase
import com.at.recallly.domain.usecase.onboarding.SaveScheduleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getFieldsForPersonaUseCase: GetFieldsForPersonaUseCase,
    private val savePersonaUseCase: SavePersonaUseCase,
    private val saveFieldsUseCase: SaveFieldsUseCase,
    private val saveScheduleUseCase: SaveScheduleUseCase,
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    val dataConsentAccepted: StateFlow<Boolean> = onboardingRepository.dataConsentAccepted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var currentUid: String? = null

    init {
        viewModelScope.launch {
            // Wait for user to be available, then load their onboarding state
            getCurrentUserUseCase()
                .filterNotNull()
                .flatMapLatest { user ->
                    currentUid = user.id
                    onboardingRepository.getOnboardingStepForUser(user.id)
                }
                .collectLatest { step ->
                    _uiState.update { it.copy(onboardingStep = step, isLoading = false) }
                    restoreSavedState(step)
                }
        }
    }

    private suspend fun restoreSavedState(step: OnboardingStep) {
        if (step.value >= OnboardingStep.PERSONA_COMPLETED.value) {
            val persona = onboardingRepository.selectedPersona.first()
            if (persona != null) {
                val fields = getFieldsForPersonaUseCase(persona)
                _uiState.update {
                    it.copy(selectedPersona = persona, availableFields = fields)
                }
            }
        }
        if (step.value >= OnboardingStep.FIELDS_COMPLETED.value) {
            val fieldIds = onboardingRepository.selectedFieldIds.first()
            if (fieldIds.isNotEmpty()) {
                _uiState.update { it.copy(selectedFieldIds = fieldIds) }
            }
        }
    }

    fun saveDataConsent(driveBackupEnabled: Boolean) {
        viewModelScope.launch {
            onboardingRepository.saveDataConsent(driveBackupEnabled)
        }
    }

    fun onEvent(event: OnboardingUiEvent) {
        when (event) {
            is OnboardingUiEvent.SelectPersona -> {
                _uiState.update { it.copy(selectedPersona = event.persona) }
            }

            is OnboardingUiEvent.ConfirmPersona -> {
                val persona = _uiState.value.selectedPersona ?: return
                val uid = currentUid ?: return
                val fields = getFieldsForPersonaUseCase(persona)
                _uiState.update {
                    it.copy(availableFields = fields, selectedFieldIds = emptySet())
                }
                viewModelScope.launch {
                    savePersonaUseCase(uid, persona)
                    _uiState.update {
                        it.copy(onboardingStep = OnboardingStep.PERSONA_COMPLETED)
                    }
                }
            }

            is OnboardingUiEvent.ToggleField -> {
                _uiState.update { state ->
                    val newSet = state.selectedFieldIds.toMutableSet()
                    if (event.fieldId in newSet) newSet.remove(event.fieldId)
                    else newSet.add(event.fieldId)
                    state.copy(selectedFieldIds = newSet, validationError = null)
                }
            }

            is OnboardingUiEvent.ToggleSelectAll -> {
                _uiState.update { state ->
                    val allIds = state.availableFields.map { it.id }.toSet()
                    val newSelection = if (state.selectedFieldIds == allIds) emptySet() else allIds
                    state.copy(selectedFieldIds = newSelection, validationError = null)
                }
            }

            is OnboardingUiEvent.ConfirmFields -> {
                val state = _uiState.value
                if (state.selectedFieldIds.size < 3) {
                    _uiState.update {
                        it.copy(validationError = "Please select at least 3 fields")
                    }
                    return
                }
                viewModelScope.launch {
                    saveFieldsUseCase(state.selectedFieldIds)
                    _uiState.update {
                        it.copy(
                            isFieldsConfirmed = true,
                            validationError = null,
                            onboardingStep = OnboardingStep.FIELDS_COMPLETED
                        )
                    }
                }
            }

            is OnboardingUiEvent.ToggleWorkDay -> {
                _uiState.update { state ->
                    val newDays = state.workDays.toMutableSet()
                    if (event.day in newDays) newDays.remove(event.day)
                    else newDays.add(event.day)
                    state.copy(workDays = newDays, validationError = null)
                }
            }

            is OnboardingUiEvent.SetStartTime -> {
                _uiState.update { it.copy(startTime = event.time, validationError = null) }
            }

            is OnboardingUiEvent.SetEndTime -> {
                _uiState.update { it.copy(endTime = event.time, validationError = null) }
            }

            is OnboardingUiEvent.CompleteOnboarding -> {
                val state = _uiState.value
                if (state.workDays.isEmpty()) {
                    _uiState.update {
                        it.copy(validationError = "Please select at least one work day")
                    }
                    return
                }
                if (!state.endTime.isAfter(state.startTime)) {
                    _uiState.update {
                        it.copy(validationError = "End time must be after start time")
                    }
                    return
                }
                viewModelScope.launch {
                    val schedule = WorkSchedule(
                        workDays = state.workDays,
                        startTime = state.startTime,
                        endTime = state.endTime
                    )
                    saveScheduleUseCase(schedule)
                    _uiState.update {
                        it.copy(
                            onboardingStep = OnboardingStep.COMPLETED,
                            isComplete = true
                        )
                    }
                }
            }

            is OnboardingUiEvent.ClearValidationError -> {
                _uiState.update { it.copy(validationError = null) }
            }
        }
    }
}
