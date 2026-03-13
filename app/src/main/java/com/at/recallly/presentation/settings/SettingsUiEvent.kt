package com.at.recallly.presentation.settings

import com.at.recallly.domain.model.Persona
import java.time.DayOfWeek
import java.time.LocalTime

sealed interface SettingsUiEvent {
    // Persona
    data class SelectPersona(val persona: Persona) : SettingsUiEvent
    data object ConfirmPersonaChange : SettingsUiEvent
    data object ResetPersonaSaved : SettingsUiEvent

    // Fields
    data class ToggleField(val fieldId: String) : SettingsUiEvent
    data object ToggleSelectAll : SettingsUiEvent
    data object SaveFields : SettingsUiEvent
    data object ResetFieldsSaved : SettingsUiEvent

    // Schedule
    data class ToggleWorkDay(val day: DayOfWeek) : SettingsUiEvent
    data class SetStartTime(val time: LocalTime) : SettingsUiEvent
    data class SetEndTime(val time: LocalTime) : SettingsUiEvent
    data object SaveSchedule : SettingsUiEvent
    data object ResetScheduleSaved : SettingsUiEvent

    // Model
    data object DownloadWhisperModel : SettingsUiEvent
    data object CancelModelDownload : SettingsUiEvent

    // Language
    data class ChangeLanguage(val code: String) : SettingsUiEvent

    // Export
    data object ExportPdf : SettingsUiEvent
    data object ResetExportState : SettingsUiEvent

    // Other
    data object ClearValidationError : SettingsUiEvent
    data object DismissError : SettingsUiEvent
}
