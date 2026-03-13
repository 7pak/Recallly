package com.at.recallly.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.at.recallly.core.util.ConnectivityChecker
import com.at.recallly.core.util.LanguageManager
import com.at.recallly.data.local.datastore.PreferencesManager
import com.at.recallly.domain.model.WorkSchedule
import com.at.recallly.domain.repository.OnboardingRepository
import com.at.recallly.domain.repository.WhisperRepository
import com.at.recallly.domain.usecase.auth.GetCurrentUserUseCase
import com.at.recallly.domain.usecase.auth.LogoutUseCase
import com.at.recallly.core.result.Result
import com.at.recallly.domain.usecase.export.ExportVoiceNotesPdfUseCase
import com.at.recallly.domain.usecase.onboarding.GetFieldsForPersonaUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val onboardingRepository: OnboardingRepository,
    private val getFieldsForPersonaUseCase: GetFieldsForPersonaUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val whisperRepository: WhisperRepository,
    private val connectivityChecker: ConnectivityChecker,
    private val preferencesManager: PreferencesManager,
    private val exportVoiceNotesPdfUseCase: ExportVoiceNotesPdfUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var currentUid: String? = null

    init {
        // Observe reactive data flows for main settings screen
        viewModelScope.launch {
            combine(
                getCurrentUserUseCase().filterNotNull(),
                onboardingRepository.selectedPersona,
                onboardingRepository.selectedFieldIds,
                onboardingRepository.workSchedule,
                whisperRepository.downloadState
            ) { user, persona, fieldIds, schedule, modelState ->
                currentUid = user.id
                val totalFields = persona?.let { getFieldsForPersonaUseCase(it).size } ?: 0
                _uiState.value.copy(
                    currentPersona = persona,
                    selectedFieldCount = fieldIds.size,
                    totalFieldCount = totalFields,
                    workSchedule = schedule,
                    whisperModelState = modelState
                )
            }.collectLatest { state ->
                _uiState.update { current ->
                    state.copy(
                        pendingPersona = current.pendingPersona,
                        availableFields = current.availableFields,
                        selectedFieldIds = current.selectedFieldIds,
                        validationError = current.validationError,
                        fieldsSaved = current.fieldsSaved,
                        personaSaved = current.personaSaved,
                        workDays = current.workDays,
                        startTime = current.startTime,
                        endTime = current.endTime,
                        scheduleSaved = current.scheduleSaved,
                        isExporting = current.isExporting,
                        exportedFileUri = current.exportedFileUri,
                        exportError = current.exportError,
                        errorMessage = current.errorMessage
                    )
                }
            }
        }
    }

    fun loadFieldsForCurrentPersona() {
        viewModelScope.launch {
            val persona = onboardingRepository.selectedPersona.first() ?: return@launch
            val fields = getFieldsForPersonaUseCase(persona)
            val savedFieldIds = onboardingRepository.selectedFieldIds.first()
            _uiState.update {
                it.copy(
                    currentPersona = persona,
                    availableFields = fields,
                    selectedFieldIds = savedFieldIds,
                    validationError = null
                )
            }
        }
    }

    fun loadFieldsForNewPersona() {
        viewModelScope.launch {
            val persona = onboardingRepository.selectedPersona.first() ?: return@launch
            val fields = getFieldsForPersonaUseCase(persona)
            _uiState.update {
                it.copy(
                    currentPersona = persona,
                    availableFields = fields,
                    selectedFieldIds = emptySet(),
                    validationError = null
                )
            }
        }
    }

    fun loadCurrentSchedule() {
        viewModelScope.launch {
            val schedule = onboardingRepository.workSchedule.first()
            _uiState.update {
                it.copy(
                    workDays = schedule.workDays,
                    startTime = schedule.startTime,
                    endTime = schedule.endTime,
                    validationError = null
                )
            }
        }
    }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            // Persona
            is SettingsUiEvent.SelectPersona -> {
                _uiState.update { it.copy(pendingPersona = event.persona) }
            }

            is SettingsUiEvent.ConfirmPersonaChange -> {
                val persona = _uiState.value.pendingPersona ?: return
                viewModelScope.launch {
                    onboardingRepository.savePersonaOnly(persona)
                    onboardingRepository.saveFieldsOnly(emptySet())
                    _uiState.update { it.copy(personaSaved = true) }
                }
            }

            is SettingsUiEvent.ResetPersonaSaved -> {
                _uiState.update { it.copy(personaSaved = false) }
            }

            // Fields
            is SettingsUiEvent.ToggleField -> {
                _uiState.update { state ->
                    val newSet = state.selectedFieldIds.toMutableSet()
                    if (event.fieldId in newSet) newSet.remove(event.fieldId)
                    else newSet.add(event.fieldId)
                    state.copy(selectedFieldIds = newSet, validationError = null)
                }
            }

            is SettingsUiEvent.ToggleSelectAll -> {
                _uiState.update { state ->
                    val allIds = state.availableFields.map { it.id }.toSet()
                    val newSelection = if (state.selectedFieldIds == allIds) emptySet() else allIds
                    state.copy(selectedFieldIds = newSelection, validationError = null)
                }
            }

            is SettingsUiEvent.SaveFields -> {
                val state = _uiState.value
                if (state.selectedFieldIds.size < 3) {
                    _uiState.update {
                        it.copy(validationError = "Please select at least 3 fields")
                    }
                    return
                }
                viewModelScope.launch {
                    onboardingRepository.saveFieldsOnly(state.selectedFieldIds)
                    _uiState.update {
                        it.copy(fieldsSaved = true, validationError = null)
                    }
                }
            }

            is SettingsUiEvent.ResetFieldsSaved -> {
                _uiState.update { it.copy(fieldsSaved = false) }
            }

            // Schedule
            is SettingsUiEvent.ToggleWorkDay -> {
                _uiState.update { state ->
                    val newDays = state.workDays.toMutableSet()
                    if (event.day in newDays) newDays.remove(event.day)
                    else newDays.add(event.day)
                    state.copy(workDays = newDays, validationError = null)
                }
            }

            is SettingsUiEvent.SetStartTime -> {
                _uiState.update { it.copy(startTime = event.time, validationError = null) }
            }

            is SettingsUiEvent.SetEndTime -> {
                _uiState.update { it.copy(endTime = event.time, validationError = null) }
            }

            is SettingsUiEvent.SaveSchedule -> {
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
                    onboardingRepository.saveSchedule(schedule)
                    _uiState.update {
                        it.copy(scheduleSaved = true, validationError = null)
                    }
                }
            }

            is SettingsUiEvent.ResetScheduleSaved -> {
                _uiState.update { it.copy(scheduleSaved = false) }
            }

            // Model
            is SettingsUiEvent.DownloadWhisperModel -> {
                if (!connectivityChecker.isOnline()) {
                    _uiState.update {
                        it.copy(errorMessage = "You need an internet connection to download the offline voice model.")
                    }
                    return
                }
                viewModelScope.launch {
                    preferencesManager.setHasSeenModelPrompt()
                    whisperRepository.downloadModel()
                }
            }

            is SettingsUiEvent.CancelModelDownload -> {
                viewModelScope.launch {
                    whisperRepository.cancelDownload()
                }
            }

            // Export
            is SettingsUiEvent.ExportPdf -> {
                _uiState.update { it.copy(isExporting = true, exportError = null) }
                viewModelScope.launch {
                    when (val result = exportVoiceNotesPdfUseCase()) {
                        is Result.Success -> {
                            _uiState.update {
                                it.copy(isExporting = false, exportedFileUri = result.data)
                            }
                        }
                        is Result.Error -> {
                            _uiState.update {
                                it.copy(
                                    isExporting = false,
                                    exportError = result.exception.message
                                )
                            }
                        }
                    }
                }
            }

            is SettingsUiEvent.ResetExportState -> {
                _uiState.update {
                    it.copy(exportedFileUri = null, exportError = null)
                }
            }

            // Language
            is SettingsUiEvent.ChangeLanguage -> {
                viewModelScope.launch { preferencesManager.setAppLanguage(event.code) }
                LanguageManager.applyLanguage(event.code)
            }

            // Other
            is SettingsUiEvent.ClearValidationError -> {
                _uiState.update { it.copy(validationError = null) }
            }

            is SettingsUiEvent.DismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
        }
    }
}
