package com.at.recallly.core.di

import androidx.credentials.CredentialManager
import com.at.recallly.BuildConfig
import com.at.recallly.data.export.PdfExportService
import com.at.recallly.data.local.datastore.PreferencesManager
import com.at.recallly.data.local.file.VoiceNoteFileStorage
import com.at.recallly.core.util.ConnectivityChecker
import com.at.recallly.data.remote.GeminiExtractionService
import com.at.recallly.data.repository.AuthRepositoryImpl
import com.at.recallly.data.repository.OnboardingRepositoryImpl
import com.at.recallly.data.repository.VoiceNoteRepositoryImpl
import com.at.recallly.domain.repository.AuthRepository
import com.at.recallly.domain.repository.ExtractionService
import com.at.recallly.domain.repository.OnboardingRepository
import com.at.recallly.domain.repository.VoiceNoteRepository
import com.at.recallly.domain.usecase.auth.GetCurrentUserUseCase
import com.at.recallly.domain.usecase.auth.LoginWithEmailUseCase
import com.at.recallly.domain.usecase.auth.LoginWithGoogleUseCase
import com.at.recallly.domain.usecase.auth.LogoutUseCase
import com.at.recallly.domain.usecase.auth.SignUpWithEmailUseCase
import com.at.recallly.domain.usecase.export.ExportVoiceNotesPdfUseCase
import com.at.recallly.domain.usecase.onboarding.GetFieldsForPersonaUseCase
import com.at.recallly.domain.usecase.onboarding.SaveFieldsUseCase
import com.at.recallly.domain.usecase.onboarding.SavePersonaUseCase
import com.at.recallly.domain.usecase.onboarding.SaveScheduleUseCase
import com.at.recallly.data.repository.WhisperRepositoryImpl
import com.at.recallly.data.whisper.AudioRecorder
import com.at.recallly.data.whisper.WhisperModelManager
import com.at.recallly.data.worker.ExtractionWorkScheduler
import com.at.recallly.domain.repository.ExtractionScheduler
import com.at.recallly.domain.repository.WhisperRepository
import com.at.recallly.domain.usecase.voice.ExtractFieldsUseCase
import com.at.recallly.domain.usecase.voice.GetVoiceNotesUseCase
import com.at.recallly.domain.usecase.voice.QueueExtractionUseCase
import com.at.recallly.domain.usecase.voice.SaveVoiceNoteUseCase
import com.at.recallly.domain.usecase.voice.TranscribeOfflineUseCase
import com.at.recallly.presentation.auth.AuthViewModel
import com.at.recallly.presentation.home.HomeViewModel
import com.at.recallly.presentation.onboarding.OnboardingViewModel
import com.at.recallly.presentation.settings.SettingsViewModel
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // Firebase
    single { FirebaseAuth.getInstance() }
    single { CredentialManager.create(get()) }

    // Database — uncomment when KSP is enabled (AGP 9.0+)
    // single {
    //     Room.databaseBuilder(get(), RecalllyDatabase::class.java, "recallly.db")
    //         .build()
    // }
    // single { get<RecalllyDatabase>().userDao() }

    // DataStore
    single { PreferencesManager(get()) }

    // Gemini AI
    single {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    // Network
    single { ConnectivityChecker(get()) }

    // Voice Note Storage
    single { VoiceNoteFileStorage(get()) }
    single {
        VoiceNoteRepositoryImpl(get()).also {
            runBlocking { it.loadFromDisk() }
        }
    }
    single<VoiceNoteRepository> { get<VoiceNoteRepositoryImpl>() }
    single<ExtractionService> { GeminiExtractionService(get()) }
    single<ExtractionScheduler> { ExtractionWorkScheduler(get()) }

    // Whisper (offline speech recognition)
    single { WhisperModelManager(get()) }
    single<WhisperRepository> { WhisperRepositoryImpl(get()) }
    factory { AudioRecorder() }

    // Repositories
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<OnboardingRepository> { OnboardingRepositoryImpl(get()) }

    // Use Cases — Auth
    factory { LoginWithEmailUseCase(get()) }
    factory { SignUpWithEmailUseCase(get()) }
    factory { LoginWithGoogleUseCase(get()) }
    factory { GetCurrentUserUseCase(get()) }
    factory { LogoutUseCase(get()) }

    // Use Cases — Onboarding
    factory { GetFieldsForPersonaUseCase() }
    factory { SavePersonaUseCase(get()) }
    factory { SaveFieldsUseCase(get()) }
    factory { SaveScheduleUseCase(get()) }

    // Export
    single { PdfExportService(get()) }
    factory { ExportVoiceNotesPdfUseCase(get(), get(), get(), get(), get()) }

    // Use Cases — Voice
    factory { ExtractFieldsUseCase(get()) }
    factory { SaveVoiceNoteUseCase(get()) }
    factory { GetVoiceNotesUseCase(get()) }
    factory { TranscribeOfflineUseCase(get()) }
    factory { QueueExtractionUseCase(get()) }

    // ViewModels
    viewModel { AuthViewModel(get(), get(), get(), get(), get()) }
    viewModel { OnboardingViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
}
