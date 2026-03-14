package com.at.recallly

import android.app.Application
import com.at.recallly.core.di.appModule
import com.at.recallly.core.util.LanguageManager
import com.at.recallly.data.notification.ReminderNotificationChannel
import com.at.recallly.data.local.datastore.dataStore
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

class RecalllyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@RecalllyApplication)
            modules(appModule)
        }

        MobileAds.initialize(this) {}

        ReminderNotificationChannel.create(this)

        val savedLanguage = runBlocking {
            dataStore.data.map { prefs ->
                prefs[stringPreferencesKey("app_language")] ?: "en"
            }.first()
        }
        if (savedLanguage != "en") {
            LanguageManager.applyLanguage(savedLanguage)
        }

        // Ensure periodic backup is scheduled if user has it enabled
        val driveBackupEnabled = runBlocking {
            dataStore.data.map { prefs ->
                prefs[booleanPreferencesKey("drive_backup_enabled")] == true
            }.first()
        }
        if (driveBackupEnabled) {
            get<com.at.recallly.data.worker.BackupWorkScheduler>().schedulePeriodicBackup()
        }
    }
}
