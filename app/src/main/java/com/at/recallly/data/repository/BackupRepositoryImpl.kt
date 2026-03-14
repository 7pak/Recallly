package com.at.recallly.data.repository

import android.os.Build
import com.at.recallly.BuildConfig
import com.at.recallly.core.result.Result
import com.at.recallly.data.backup.BackupDto
import com.at.recallly.data.backup.BackupPreferencesDto
import com.at.recallly.data.backup.DriveBackupService
import com.at.recallly.data.local.datastore.PreferencesManager
import com.at.recallly.data.local.file.CustomFieldFileStorage
import com.at.recallly.data.local.file.VoiceNoteFileStorage
import com.at.recallly.domain.model.BackupMetadata
import com.at.recallly.domain.repository.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber

class BackupRepositoryImpl(
    private val driveBackupService: DriveBackupService,
    private val voiceNoteFileStorage: VoiceNoteFileStorage,
    private val customFieldFileStorage: CustomFieldFileStorage,
    private val preferencesManager: PreferencesManager,
    private val voiceNoteRepository: VoiceNoteRepositoryImpl,
    private val customFieldRepository: CustomFieldRepositoryImpl
) : BackupRepository {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    override suspend fun backup(): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        try {
            val voiceNotes = voiceNoteFileStorage.readAll()
            val customFields = customFieldFileStorage.readAll()
            val prefsSnapshot = preferencesManager.getPreferencesSnapshot()

            val timestamp = System.currentTimeMillis()
            val backupDto = BackupDto(
                version = 1,
                timestamp = timestamp,
                appVersionName = BuildConfig.VERSION_NAME,
                deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
                voiceNotes = voiceNotes,
                customFields = customFields,
                preferences = BackupPreferencesDto(
                    selectedPersona = prefsSnapshot["selectedPersona"] as? String,
                    selectedFieldIds = @Suppress("UNCHECKED_CAST") (prefsSnapshot["selectedFieldIds"] as? Set<String> ?: emptySet()),
                    workDays = @Suppress("UNCHECKED_CAST") (prefsSnapshot["workDays"] as? Set<String> ?: emptySet()),
                    workStartTime = prefsSnapshot["workStartTime"] as? String,
                    workEndTime = prefsSnapshot["workEndTime"] as? String,
                    appLanguage = prefsSnapshot["appLanguage"] as? String ?: "en",
                    calendarSyncEnabled = prefsSnapshot["calendarSyncEnabled"] as? Boolean ?: false,
                    reminderNotificationsEnabled = prefsSnapshot["reminderNotificationsEnabled"] as? Boolean ?: false,
                    hasSeenModelPrompt = prefsSnapshot["hasSeenModelPrompt"] as? Boolean ?: false,
                    driveBackupEnabled = prefsSnapshot["driveBackupEnabled"] as? Boolean ?: true
                )
            )

            val jsonString = json.encodeToString(BackupDto.serializer(), backupDto)
            driveBackupService.uploadBackup(jsonString)
            preferencesManager.setLastBackupTimestamp(timestamp)

            val metadata = BackupMetadata(
                timestamp = timestamp,
                appVersionName = backupDto.appVersionName,
                deviceName = backupDto.deviceName,
                voiceNoteCount = voiceNotes.size,
                customFieldCount = customFields.size
            )
            Timber.d("Backup completed: ${voiceNotes.size} notes, ${customFields.size} fields")
            Result.Success(metadata)
        } catch (e: Exception) {
            Timber.e(e, "Backup failed")
            Result.Error(e as? Exception ?: Exception(e))
        }
    }

    override suspend fun restore(): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        try {
            val jsonString = driveBackupService.downloadBackup()
                ?: return@withContext Result.Error(Exception("No backup found"))

            val backupDto = json.decodeFromString<BackupDto>(jsonString)

            // Write voice notes to disk and refresh in-memory cache
            voiceNoteFileStorage.writeAll(backupDto.voiceNotes)
            voiceNoteRepository.loadFromDisk()

            // Write custom fields to disk and refresh in-memory cache
            customFieldFileStorage.writeAll(backupDto.customFields)
            customFieldRepository.loadFromDisk()

            // Restore preferences
            val prefs = backupDto.preferences
            preferencesManager.restorePreferences(
                persona = prefs.selectedPersona,
                fieldIds = prefs.selectedFieldIds,
                workDays = prefs.workDays,
                workStartTime = prefs.workStartTime,
                workEndTime = prefs.workEndTime,
                appLanguage = prefs.appLanguage,
                calendarSyncEnabled = prefs.calendarSyncEnabled,
                reminderNotificationsEnabled = prefs.reminderNotificationsEnabled,
                hasSeenModelPrompt = prefs.hasSeenModelPrompt,
                driveBackupEnabled = prefs.driveBackupEnabled
            )

            val metadata = BackupMetadata(
                timestamp = backupDto.timestamp,
                appVersionName = backupDto.appVersionName,
                deviceName = backupDto.deviceName,
                voiceNoteCount = backupDto.voiceNotes.size,
                customFieldCount = backupDto.customFields.size
            )
            Timber.d("Restore completed: ${backupDto.voiceNotes.size} notes, ${backupDto.customFields.size} fields")
            Result.Success(metadata)
        } catch (e: Exception) {
            Timber.e(e, "Restore failed")
            Result.Error(e as? Exception ?: Exception(e))
        }
    }

    override suspend fun getRemoteBackupMetadata(): Result<BackupMetadata?> = withContext(Dispatchers.IO) {
        try {
            val jsonString = driveBackupService.downloadBackup()
                ?: return@withContext Result.Success(null)

            val backupDto = json.decodeFromString<BackupDto>(jsonString)
            val metadata = BackupMetadata(
                timestamp = backupDto.timestamp,
                appVersionName = backupDto.appVersionName,
                deviceName = backupDto.deviceName,
                voiceNoteCount = backupDto.voiceNotes.size,
                customFieldCount = backupDto.customFields.size
            )
            Result.Success(metadata)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch remote backup metadata")
            Result.Error(e as? Exception ?: Exception(e))
        }
    }

    override val lastBackupTimestamp: Flow<Long?>
        get() = preferencesManager.lastBackupTimestamp
}
