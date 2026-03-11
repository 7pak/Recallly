package com.at.recallly

import android.app.Application
import com.at.recallly.core.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class RecalllyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@RecalllyApplication)
            modules(appModule)
        }
    }
}
