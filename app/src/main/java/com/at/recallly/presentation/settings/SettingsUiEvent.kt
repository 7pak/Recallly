package com.at.recallly.presentation.settings

import com.at.recallly.domain.model.FieldType
import com.at.recallly.domain.model.Persona
import com.at.recallly.domain.model.PersonaField
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

    // Custom Fields
    data class AddCustomField(val name: String, val description: String, val fieldType: FieldType = FieldType.TEXT) : SettingsUiEvent
    data class EditCustomField(val fieldId: String, val name: String, val description: String, val fieldType: FieldType = FieldType.TEXT) : SettingsUiEvent
    data class DeleteCustomField(val fieldId: String) : SettingsUiEvent
    data object ShowAddCustomFieldDialog : SettingsUiEvent
    data class ShowEditCustomFieldDialog(val field: PersonaField) : SettingsUiEvent
    data object DismissCustomFieldDialog : SettingsUiEvent

    // Calendar Sync
    data object ToggleCalendarSync : SettingsUiEvent

    // Reminder Notifications
    data object ToggleReminderNotifications : SettingsUiEvent

    // Data Management
    data object DeleteAllData : SettingsUiEvent
    data object DeleteAccount : SettingsUiEvent

    // Premium Purchase
    data class LaunchPurchase(val activity: android.app.Activity) : SettingsUiEvent

    // Other
    data object ClearValidationError : SettingsUiEvent
    data object DismissError : SettingsUiEvent
}
