package com.at.recallly.presentation.settings

import android.net.Uri
import com.at.recallly.domain.model.BackupMetadata
import com.at.recallly.domain.model.ModelDownloadState
import com.at.recallly.domain.model.Persona
import com.at.recallly.domain.model.PersonaField
import com.at.recallly.domain.model.WorkSchedule
import java.time.DayOfWeek
import java.time.LocalTime

data class SettingsUiState(
    val currentPersona: Persona? = null,
    val selectedFieldCount: Int = 0,
    val totalFieldCount: Int = 0,
    val workSchedule: WorkSchedule = WorkSchedule(),
    val whisperModelState: ModelDownloadState = ModelDownloadState.NotDownloaded,
    // Persona selection sub-screen
    val pendingPersona: Persona? = null,
    // Field selection sub-screen
    val availableFields: List<PersonaField> = emptyList(),
    val selectedFieldIds: Set<String> = emptySet(),
    val validationError: String? = null,
    val fieldsSaved: Boolean = false,
    val personaSaved: Boolean = false,
    // Work schedule sub-screen
    val workDays: Set<DayOfWeek> = WorkSchedule.DEFAULT_WORK_DAYS,
    val startTime: LocalTime = WorkSchedule.DEFAULT_START_TIME,
    val endTime: LocalTime = WorkSchedule.DEFAULT_END_TIME,
    val scheduleSaved: Boolean = false,
    // Export
    val isExporting: Boolean = false,
    val exportedFileUri: Uri? = null,
    val exportError: String? = null,
    val errorMessage: String? = null,
    val isPremium: Boolean = false,
    // Calendar Sync
    val calendarSyncEnabled: Boolean = false,
    // Reminder Notifications
    val reminderNotificationsEnabled: Boolean = false,
    // Custom Fields
    val customFields: List<PersonaField> = emptyList(),
    val showCustomFieldDialog: Boolean = false,
    val editingCustomField: PersonaField? = null,
    // Data Management
    val isDeletingData: Boolean = false,
    val dataDeleted: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val accountDeleted: Boolean = false,
    // Premium Purchase
    val subscriptionPrice: String? = null,
    val isPurchasing: Boolean = false,
    // Backup & Restore
    val driveBackupEnabled: Boolean = false,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val backupSuccess: Boolean = false,
    val restoreSuccess: Boolean = false,
    val backupError: String? = null,
    val restoreError: String? = null,
    val lastBackupTimestamp: Long? = null,
    val remoteBackupInfo: BackupMetadata? = null,
    val showRestoreConfirmDialog: Boolean = false,
    val isGoogleUser: Boolean = false,
    val needsDriveAuth: Boolean = false,
    val pendingBackupAction: PendingBackupAction? = null
)

enum class PendingBackupAction { BACKUP, RESTORE, ENABLE }
