package com.at.recallly.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "recallly_prefs")

class PreferencesManager(private val context: Context) {

    private companion object {
        val ONBOARDING_USER_ID = stringPreferencesKey("onboarding_user_id")
        val ONBOARDING_STEP = intPreferencesKey("onboarding_step")
        val SELECTED_PERSONA = stringPreferencesKey("selected_persona")
        val SELECTED_FIELD_IDS = stringSetPreferencesKey("selected_field_ids")
        val WORK_DAYS = stringSetPreferencesKey("work_days")
        val WORK_START_TIME = stringPreferencesKey("work_start_time")
        val WORK_END_TIME = stringPreferencesKey("work_end_time")
        val DATA_CONSENT_ACCEPTED = booleanPreferencesKey("data_consent_accepted")
        val DRIVE_BACKUP_ENABLED = booleanPreferencesKey("drive_backup_enabled")
        val HAS_SEEN_MODEL_PROMPT = booleanPreferencesKey("has_seen_model_prompt")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val CALENDAR_SYNC_ENABLED = booleanPreferencesKey("calendar_sync_enabled")
        val REMINDER_NOTIFICATIONS_ENABLED = booleanPreferencesKey("reminder_notifications_enabled")
        val PENDING_REMINDERS = stringSetPreferencesKey("pending_reminders")
        val FREE_NOTES_USED = intPreferencesKey("free_notes_used")
        val FREE_CALENDAR_SYNCS_USED = intPreferencesKey("free_calendar_syncs_used")
        val FREE_NOTIFICATIONS_USED = intPreferencesKey("free_notifications_used")
    }

    fun getOnboardingStepForUser(uid: String): Flow<Int> = context.dataStore.data
        .map { prefs ->
            val storedUid = prefs[ONBOARDING_USER_ID]
            if (storedUid == uid) prefs[ONBOARDING_STEP] ?: 0 else 0
        }

    val selectedPersona: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[SELECTED_PERSONA] }

    val selectedFieldIds: Flow<Set<String>> = context.dataStore.data
        .map { prefs -> prefs[SELECTED_FIELD_IDS] ?: emptySet() }

    val workDays: Flow<Set<String>> = context.dataStore.data
        .map { prefs -> prefs[WORK_DAYS] ?: emptySet() }

    val workStartTime: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[WORK_START_TIME] }

    val workEndTime: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[WORK_END_TIME] }

    suspend fun savePersonaStep(uid: String, personaName: String) {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_USER_ID] = uid
            prefs[SELECTED_PERSONA] = personaName
            prefs[ONBOARDING_STEP] = 1
        }
    }

    suspend fun saveFieldsStep(fieldIds: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_FIELD_IDS] = fieldIds
            prefs[ONBOARDING_STEP] = 2
        }
    }

    suspend fun saveScheduleStep(
        workDays: Set<String>,
        workStartTime: String,
        workEndTime: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[WORK_DAYS] = workDays
            prefs[WORK_START_TIME] = workStartTime
            prefs[WORK_END_TIME] = workEndTime
            prefs[ONBOARDING_STEP] = 3
        }
    }

    suspend fun savePersonaOnly(personaName: String) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_PERSONA] = personaName
        }
    }

    suspend fun saveFieldsOnly(fieldIds: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_FIELD_IDS] = fieldIds
        }
    }

    suspend fun saveScheduleOnly(
        workDays: Set<String>,
        workStartTime: String,
        workEndTime: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[WORK_DAYS] = workDays
            prefs[WORK_START_TIME] = workStartTime
            prefs[WORK_END_TIME] = workEndTime
        }
    }

    val hasSeenModelPrompt: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[HAS_SEEN_MODEL_PROMPT] == true }

    suspend fun setHasSeenModelPrompt() {
        context.dataStore.edit { prefs ->
            prefs[HAS_SEEN_MODEL_PROMPT] = true
        }
    }

    val dataConsentAccepted: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[DATA_CONSENT_ACCEPTED] == true }

    val driveBackupEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[DRIVE_BACKUP_ENABLED] == true }

    val appLanguage: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[APP_LANGUAGE] ?: "en" }

    val calendarSyncEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[CALENDAR_SYNC_ENABLED] == true }

    suspend fun setCalendarSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[CALENDAR_SYNC_ENABLED] = enabled
        }
    }

    val reminderNotificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[REMINDER_NOTIFICATIONS_ENABLED] == true }

    suspend fun setReminderNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[REMINDER_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun savePendingReminder(reminder: com.at.recallly.data.notification.PendingReminder) {
        val json = kotlinx.serialization.json.Json.encodeToString(reminder)
        context.dataStore.edit { prefs ->
            val current = prefs[PENDING_REMINDERS] ?: emptySet()
            // Remove existing entry with same id, then add new one
            val filtered = current.filter { entry ->
                try {
                    kotlinx.serialization.json.Json.decodeFromString<com.at.recallly.data.notification.PendingReminder>(entry).id != reminder.id
                } catch (_: Exception) { true }
            }.toSet()
            prefs[PENDING_REMINDERS] = filtered + json
        }
    }

    suspend fun removePendingReminder(reminderId: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[PENDING_REMINDERS] ?: emptySet()
            val filtered = current.filter { entry ->
                try {
                    kotlinx.serialization.json.Json.decodeFromString<com.at.recallly.data.notification.PendingReminder>(entry).id != reminderId
                } catch (_: Exception) { true }
            }.toSet()
            prefs[PENDING_REMINDERS] = filtered
        }
    }

    fun getAllPendingReminders(): Flow<List<com.at.recallly.data.notification.PendingReminder>> =
        context.dataStore.data.map { prefs ->
            (prefs[PENDING_REMINDERS] ?: emptySet()).mapNotNull { entry ->
                try {
                    kotlinx.serialization.json.Json.decodeFromString<com.at.recallly.data.notification.PendingReminder>(entry)
                } catch (_: Exception) { null }
            }
        }

    suspend fun setAppLanguage(code: String) {
        context.dataStore.edit { prefs ->
            prefs[APP_LANGUAGE] = code
        }
    }

    suspend fun saveDataConsent(driveBackupEnabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DATA_CONSENT_ACCEPTED] = true
            prefs[DRIVE_BACKUP_ENABLED] = driveBackupEnabled
        }
    }

    suspend fun clearOnboarding() {
        context.dataStore.edit { prefs ->
            prefs.remove(ONBOARDING_USER_ID)
            prefs.remove(ONBOARDING_STEP)
            prefs.remove(SELECTED_PERSONA)
            prefs.remove(SELECTED_FIELD_IDS)
            prefs.remove(WORK_DAYS)
            prefs.remove(WORK_START_TIME)
            prefs.remove(WORK_END_TIME)
            prefs.remove(DATA_CONSENT_ACCEPTED)
            prefs.remove(DRIVE_BACKUP_ENABLED)
        }
    }

    val freeNotesUsed: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[FREE_NOTES_USED] ?: 0 }

    suspend fun incrementFreeNotesUsed() {
        context.dataStore.edit { prefs ->
            val current = prefs[FREE_NOTES_USED] ?: 0
            prefs[FREE_NOTES_USED] = current + 1
        }
    }

    val freeCalendarSyncsUsed: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[FREE_CALENDAR_SYNCS_USED] ?: 0 }

    suspend fun incrementFreeCalendarSyncsUsed() {
        context.dataStore.edit { prefs ->
            val current = prefs[FREE_CALENDAR_SYNCS_USED] ?: 0
            prefs[FREE_CALENDAR_SYNCS_USED] = current + 1
        }
    }

    val freeNotificationsUsed: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[FREE_NOTIFICATIONS_USED] ?: 0 }

    suspend fun incrementFreeNotificationsUsed() {
        context.dataStore.edit { prefs ->
            val current = prefs[FREE_NOTIFICATIONS_USED] ?: 0
            prefs[FREE_NOTIFICATIONS_USED] = current + 1
        }
    }

    suspend fun clearAllData() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
