package com.at.recallly

import android.app.Application
import com.at.recallly.core.di.appModule
import com.at.recallly.core.util.LanguageManager
import com.at.recallly.data.local.datastore.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import androidx.datastore.preferences.core.stringPreferencesKey

class RecalllyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@RecalllyApplication)
            modules(appModule)
        }

        val savedLanguage = runBlocking {
            dataStore.data.map { prefs ->
                prefs[stringPreferencesKey("app_language")] ?: "en"
            }.first()
        }
        if (savedLanguage != "en") {
            LanguageManager.applyLanguage(savedLanguage)
        }
    }
}
