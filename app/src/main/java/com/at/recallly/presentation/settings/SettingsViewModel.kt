package com.at.recallly.presentation.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.at.recallly.core.util.ConnectivityChecker
import com.at.recallly.core.util.LanguageManager
import com.at.recallly.data.local.datastore.PreferencesManager
import com.at.recallly.domain.model.ModelDownloadState
import com.at.recallly.domain.model.Persona
import com.at.recallly.domain.model.WorkSchedule
import com.at.recallly.domain.repository.OnboardingRepository
import com.at.recallly.domain.repository.WhisperRepository
import com.at.recallly.domain.usecase.auth.GetCurrentUserUseCase
import com.at.recallly.domain.usecase.auth.DeleteAccountUseCase
import com.at.recallly.domain.usecase.auth.DeleteAllDataUseCase
import com.at.recallly.domain.usecase.auth.LogoutUseCase
import com.at.recallly.core.result.Result
import com.at.recallly.domain.model.PersonaField
import com.at.recallly.domain.repository.CustomFieldRepository
import com.at.recallly.domain.repository.BillingRepository
import com.at.recallly.domain.usecase.billing.ObservePremiumStatusUseCase
import com.at.recallly.BuildConfig
import com.at.recallly.data.backup.DriveBackupService
import com.at.recallly.data.backup.DriveAuthRequiredException
import com.at.recallly.data.worker.BackupWorkScheduler
import com.at.recallly.domain.repository.BackupRepository
import com.at.recallly.domain.usecase.backup.BackupDataUseCase
import com.at.recallly.domain.usecase.backup.GetBackupInfoUseCase
import com.at.recallly.domain.usecase.backup.RestoreDataUseCase
import com.at.recallly.domain.usecase.export.ExportVoiceNotesPdfUseCase
import com.at.recallly.domain.usecase.fields.AddCustomFieldUseCase
import com.at.recallly.domain.usecase.fields.DeleteCustomFieldUseCase
import com.at.recallly.domain.usecase.fields.UpdateCustomFieldUseCase
import com.at.recallly.domain.usecase.onboarding.GetFieldsForPersonaUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val onboardingRepository: OnboardingRepository,
    private val getFieldsForPersonaUseCase: GetFieldsForPersonaUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val whisperRepository: WhisperRepository,
    private val connectivityChecker: ConnectivityChecker,
    private val preferencesManager: PreferencesManager,
    private val exportVoiceNotesPdfUseCase: ExportVoiceNotesPdfUseCase,
    private val observePremiumStatusUseCase: ObservePremiumStatusUseCase,
    private val customFieldRepository: CustomFieldRepository,
    private val addCustomFieldUseCase: AddCustomFieldUseCase,
    private val updateCustomFieldUseCase: UpdateCustomFieldUseCase,
    private val deleteCustomFieldUseCase: DeleteCustomFieldUseCase,
    private val deleteAllDataUseCase: DeleteAllDataUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val billingRepository: BillingRepository,
    private val backupDataUseCase: BackupDataUseCase,
    private val restoreDataUseCase: RestoreDataUseCase,
    private val getBackupInfoUseCase: GetBackupInfoUseCase,
    private val backupWorkScheduler: BackupWorkScheduler,
    private val backupRepository: BackupRepository,
    private val driveBackupService: DriveBackupService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var currentUid: String? = null

    init {
        // Observe reactive data flows for main settings screen
        viewModelScope.launch {
            val customFieldsFlow = onboardingRepository.selectedPersona.flatMapLatest { persona ->
                persona?.let { customFieldRepository.getCustomFieldsForPersona(it) } ?: flowOf(emptyList())
            }

            combine(
                getCurrentUserUseCase().filterNotNull(),
                onboardingRepository.selectedPersona,
                onboardingRepository.selectedFieldIds,
                onboardingRepository.workSchedule,
                whisperRepository.downloadState
            ) { user, persona, fieldIds, schedule, modelState ->
                currentUid = user.id
                SettingsHolder(user.id, persona, fieldIds, schedule, modelState)
            }.combine(customFieldsFlow) { holder, customFields ->
                val builtInCount = holder.persona?.let { getFieldsForPersonaUseCase(it).size } ?: 0
                val totalFields = builtInCount + customFields.size
                _uiState.value.copy(
                    currentPersona = holder.persona,
                    selectedFieldCount = holder.fieldIds.size,
                    totalFieldCount = totalFields,
                    workSchedule = holder.schedule,
                    whisperModelState = holder.modelState,
                    customFields = customFields
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
                        errorMessage = current.errorMessage,
                        isPremium = current.isPremium,
                        calendarSyncEnabled = current.calendarSyncEnabled,
                        showCustomFieldDialog = current.showCustomFieldDialog,
                        editingCustomField = current.editingCustomField,
                        isDeletingData = current.isDeletingData,
                        dataDeleted = current.dataDeleted,
                        isDeletingAccount = current.isDeletingAccount,
                        accountDeleted = current.accountDeleted,
                        subscriptionPrice = current.subscriptionPrice,
                        isPurchasing = current.isPurchasing,
                        driveBackupEnabled = current.driveBackupEnabled,
                        isBackingUp = current.isBackingUp,
                        isRestoring = current.isRestoring,
                        backupSuccess = current.backupSuccess,
                        restoreSuccess = current.restoreSuccess,
                        backupError = current.backupError,
                        restoreError = current.restoreError,
                        lastBackupTimestamp = current.lastBackupTimestamp,
                        remoteBackupInfo = current.remoteBackupInfo,
                        showRestoreConfirmDialog = current.showRestoreConfirmDialog,
                        isGoogleUser = current.isGoogleUser,
                        needsDriveAuth = current.needsDriveAuth,
                        pendingBackupAction = current.pendingBackupAction
                    )
                }
            }
        }

        // Observe premium status
        viewModelScope.launch {
            observePremiumStatusUseCase().collect { status ->
                _uiState.update { it.copy(isPremium = status.isPremium) }
            }
        }

        // Observe calendar sync preference
        viewModelScope.launch {
            preferencesManager.calendarSyncEnabled.collect { enabled ->
                _uiState.update { it.copy(calendarSyncEnabled = enabled) }
            }
        }

        // Observe reminder notifications preference
        viewModelScope.launch {
            preferencesManager.reminderNotificationsEnabled.collect { enabled ->
                _uiState.update { it.copy(reminderNotificationsEnabled = enabled) }
            }
        }

        // Fetch subscription price
        viewModelScope.launch {
            try {
                val price = billingRepository.getSubscriptionPrice()
                _uiState.update { it.copy(subscriptionPrice = price) }
            } catch (_: Exception) { }
        }

        // Observe drive backup preference
        viewModelScope.launch {
            preferencesManager.driveBackupEnabled.collect { enabled ->
                _uiState.update { it.copy(driveBackupEnabled = enabled) }
            }
        }

        // Observe last backup timestamp
        viewModelScope.launch {
            backupRepository.lastBackupTimestamp.collect { ts ->
                _uiState.update { it.copy(lastBackupTimestamp = ts) }
            }
        }

        // Set isGoogleUser flag
        _uiState.update { it.copy(isGoogleUser = driveBackupService.isGoogleUser()) }
    }

    fun loadFieldsForCurrentPersona() {
        viewModelScope.launch {
            val persona = onboardingRepository.selectedPersona.first() ?: return@launch
            val builtInFields = getFieldsForPersonaUseCase(persona)
            val customFields = customFieldRepository.getCustomFieldsForPersona(persona).first()
            val allFields = builtInFields + customFields
            val savedFieldIds = onboardingRepository.selectedFieldIds.first()
            _uiState.update {
                it.copy(
                    currentPersona = persona,
                    availableFields = allFields,
                    selectedFieldIds = savedFieldIds,
                    validationError = null
                )
            }
        }
    }

    fun loadFieldsForNewPersona() {
        viewModelScope.launch {
            val persona = onboardingRepository.selectedPersona.first() ?: return@launch
            val builtInFields = getFieldsForPersonaUseCase(persona)
            val customFields = customFieldRepository.getCustomFieldsForPersona(persona).first()
            val allFields = builtInFields + customFields
            _uiState.update {
                it.copy(
                    currentPersona = persona,
                    availableFields = allFields,
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
                _uiState.value.pendingPersona ?: return
                _uiState.update { it.copy(personaSaved = true) }
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
                    val persona = state.pendingPersona
                    if (persona != null) {
                        onboardingRepository.savePersonaOnly(persona)
                    }
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

            // Custom Fields
            is SettingsUiEvent.ShowAddCustomFieldDialog -> {
                _uiState.update { it.copy(showCustomFieldDialog = true, editingCustomField = null) }
            }

            is SettingsUiEvent.ShowEditCustomFieldDialog -> {
                _uiState.update { it.copy(showCustomFieldDialog = true, editingCustomField = event.field) }
            }

            is SettingsUiEvent.DismissCustomFieldDialog -> {
                _uiState.update { it.copy(showCustomFieldDialog = false, editingCustomField = null) }
            }

            is SettingsUiEvent.AddCustomField -> {
                val persona = _uiState.value.currentPersona ?: return
                viewModelScope.launch {
                    addCustomFieldUseCase(
                        name = event.name,
                        description = event.description,
                        persona = persona,
                        fieldType = event.fieldType
                    )
                    _uiState.update { it.copy(showCustomFieldDialog = false) }
                    loadFieldsForCurrentPersona()
                }
            }

            is SettingsUiEvent.EditCustomField -> {
                val existing = _uiState.value.editingCustomField ?: return
                viewModelScope.launch {
                    updateCustomFieldUseCase(
                        existing.copy(displayName = event.name, description = event.description, fieldType = event.fieldType)
                    )
                    _uiState.update { it.copy(showCustomFieldDialog = false, editingCustomField = null) }
                    loadFieldsForCurrentPersona()
                }
            }

            is SettingsUiEvent.DeleteCustomField -> {
                viewModelScope.launch {
                    deleteCustomFieldUseCase(event.fieldId)
                    loadFieldsForCurrentPersona()
                }
            }

            // Calendar Sync
            is SettingsUiEvent.ToggleCalendarSync -> {
                if (!_uiState.value.isPremium) return
                viewModelScope.launch {
                    val current = _uiState.value.calendarSyncEnabled
                    preferencesManager.setCalendarSyncEnabled(!current)
                }
            }

            is SettingsUiEvent.ToggleReminderNotifications -> {
                if (!_uiState.value.isPremium) return
                viewModelScope.launch {
                    val current = _uiState.value.reminderNotificationsEnabled
                    preferencesManager.setReminderNotificationsEnabled(!current)
                }
            }

            // Data Management
            is SettingsUiEvent.DeleteAllData -> {
                _uiState.update { it.copy(isDeletingData = true) }
                viewModelScope.launch {
                    try {
                        deleteAllDataUseCase()
                        _uiState.update { it.copy(isDeletingData = false, dataDeleted = true) }
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(isDeletingData = false, errorMessage = e.message)
                        }
                    }
                }
            }

            is SettingsUiEvent.DeleteAccount -> {
                _uiState.update { it.copy(isDeletingAccount = true) }
                viewModelScope.launch {
                    try {
                        deleteAccountUseCase()
                        _uiState.update { it.copy(isDeletingAccount = false, accountDeleted = true) }
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(isDeletingAccount = false, errorMessage = e.message)
                        }
                    }
                }
            }

            // Premium Purchase
            is SettingsUiEvent.LaunchPurchase -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isPurchasing = true) }
                    try {
                        val launched = billingRepository.launchPurchaseFlow(event.activity)
                        if (!launched) {
                            _uiState.update { it.copy(errorMessage = "Subscription is not available yet. Please try again later.") }
                        }
                    } catch (_: Exception) {
                        _uiState.update { it.copy(errorMessage = "Purchase could not be started. Please check your internet connection.") }
                    }
                    _uiState.update { it.copy(isPurchasing = false) }
                }
            }

            // Backup & Restore
            is SettingsUiEvent.ToggleDriveBackup -> {
                val current = _uiState.value.driveBackupEnabled
                if (!current && !driveBackupService.hasRequiredScopes()) {
                    _uiState.update {
                        it.copy(needsDriveAuth = true, pendingBackupAction = PendingBackupAction.ENABLE)
                    }
                    return
                }
                viewModelScope.launch {
                    preferencesManager.setDriveBackupEnabled(!current)
                    if (!current) {
                        backupWorkScheduler.schedulePeriodicBackup()
                    } else {
                        backupWorkScheduler.cancelPeriodicBackup()
                    }
                }
            }

            is SettingsUiEvent.BackupNow -> {
                if (!connectivityChecker.isOnline()) {
                    _uiState.update { it.copy(errorMessage = "Backup requires an internet connection") }
                    return
                }
                if (!driveBackupService.hasRequiredScopes()) {
                    _uiState.update {
                        it.copy(needsDriveAuth = true, pendingBackupAction = PendingBackupAction.BACKUP)
                    }
                    return
                }
                performBackup()
            }

            is SettingsUiEvent.RestoreFromBackup -> {
                if (!connectivityChecker.isOnline()) {
                    _uiState.update { it.copy(errorMessage = "Restore requires an internet connection") }
                    return
                }
                if (!driveBackupService.hasRequiredScopes()) {
                    _uiState.update {
                        it.copy(needsDriveAuth = true, pendingBackupAction = PendingBackupAction.RESTORE)
                    }
                    return
                }
                performFetchBackupInfo()
            }

            is SettingsUiEvent.ConfirmRestore -> {
                _uiState.update { it.copy(showRestoreConfirmDialog = false, isRestoring = true) }
                viewModelScope.launch {
                    when (val result = restoreDataUseCase()) {
                        is Result.Success -> {
                            _uiState.update {
                                it.copy(isRestoring = false, restoreSuccess = true)
                            }
                        }
                        is Result.Error -> {
                            _uiState.update {
                                it.copy(isRestoring = false, restoreError = result.exception.message)
                            }
                        }
                    }
                }
            }

            is SettingsUiEvent.DismissRestoreDialog -> {
                _uiState.update { it.copy(showRestoreConfirmDialog = false, remoteBackupInfo = null) }
            }

            is SettingsUiEvent.ResetBackupSuccess -> {
                _uiState.update { it.copy(backupSuccess = false, backupError = null) }
            }

            is SettingsUiEvent.ResetRestoreSuccess -> {
                _uiState.update { it.copy(restoreSuccess = false, restoreError = null) }
            }

            is SettingsUiEvent.DriveAuthCompleted -> {
                val pending = _uiState.value.pendingBackupAction
                _uiState.update { it.copy(needsDriveAuth = false, pendingBackupAction = null) }
                when (pending) {
                    PendingBackupAction.ENABLE -> {
                        viewModelScope.launch {
                            preferencesManager.setDriveBackupEnabled(true)
                            backupWorkScheduler.schedulePeriodicBackup()
                        }
                    }
                    PendingBackupAction.BACKUP -> performBackup()
                    PendingBackupAction.RESTORE -> performFetchBackupInfo()
                    null -> { /* no-op */ }
                }
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

    fun getAuthIntent(): Intent = driveBackupService.getAuthorizationIntent(BuildConfig.WEB_CLIENT_ID)

    private fun performBackup() {
        _uiState.update { it.copy(isBackingUp = true, backupError = null) }
        viewModelScope.launch {
            when (val result = backupDataUseCase()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isBackingUp = false, backupSuccess = true) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isBackingUp = false, backupError = result.exception.message) }
                }
            }
        }
    }

    private fun performFetchBackupInfo() {
        _uiState.update { it.copy(isRestoring = true, restoreError = null) }
        viewModelScope.launch {
            when (val result = getBackupInfoUseCase()) {
                is Result.Success -> {
                    if (result.data != null) {
                        _uiState.update {
                            it.copy(isRestoring = false, remoteBackupInfo = result.data, showRestoreConfirmDialog = true)
                        }
                    } else {
                        _uiState.update { it.copy(isRestoring = false, restoreError = "No backup found") }
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isRestoring = false, restoreError = result.exception.message) }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
        }
    }

    private data class SettingsHolder(
        val uid: String,
        val persona: Persona?,
        val fieldIds: Set<String>,
        val schedule: WorkSchedule,
        val modelState: ModelDownloadState
    )
}
