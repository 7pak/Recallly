package com.at.recallly.data.backup

import com.at.recallly.data.local.file.CustomFieldDto
import com.at.recallly.data.local.file.VoiceNoteDto
import kotlinx.serialization.Serializable

@Serializable
data class BackupDto(
    val version: Int = 1,
    val timestamp: Long,
    val appVersionName: String,
    val deviceName: String,
    val voiceNotes: List<VoiceNoteDto>,
    val customFields: List<CustomFieldDto>,
    val preferences: BackupPreferencesDto
)

@Serializable
data class BackupPreferencesDto(
    val selectedPersona: String? = null,
    val selectedFieldIds: Set<String> = emptySet(),
    val workDays: Set<String> = emptySet(),
    val workStartTime: String? = null,
    val workEndTime: String? = null,
    val appLanguage: String = "en",
    val calendarSyncEnabled: Boolean = false,
    val reminderNotificationsEnabled: Boolean = false,
    val hasSeenModelPrompt: Boolean = false,
    val driveBackupEnabled: Boolean = true
)
