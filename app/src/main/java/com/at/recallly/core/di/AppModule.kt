package com.at.recallly.core.di

import androidx.credentials.CredentialManager
import com.at.recallly.BuildConfig
import com.at.recallly.data.export.PdfExportService
import com.at.recallly.data.local.datastore.PreferencesManager
import com.at.recallly.data.local.file.VoiceNoteFileStorage
import com.at.recallly.core.util.ConnectivityChecker
import com.at.recallly.data.notification.AlarmReminderScheduler
import com.at.recallly.data.remote.GeminiExtractionService
import com.at.recallly.data.ad.RewardedAdManager
import com.at.recallly.data.billing.BillingClientWrapper
import com.at.recallly.data.billing.PremiumPreferences
import com.at.recallly.data.local.file.CustomFieldFileStorage
import com.at.recallly.data.backup.DriveBackupService
import com.at.recallly.data.repository.AuthRepositoryImpl
import com.at.recallly.data.repository.AdRepositoryImpl
import com.at.recallly.data.repository.BackupRepositoryImpl
import com.at.recallly.data.repository.BillingRepositoryImpl
import com.at.recallly.data.repository.CustomFieldRepositoryImpl
import com.at.recallly.data.repository.OnboardingRepositoryImpl
import com.at.recallly.data.repository.VoiceNoteRepositoryImpl
import com.at.recallly.domain.repository.AdRepository
import com.at.recallly.domain.repository.AuthRepository
import com.at.recallly.domain.repository.BillingRepository
import com.at.recallly.domain.repository.CustomFieldRepository
import com.at.recallly.domain.repository.ExtractionService
import com.at.recallly.domain.repository.OnboardingRepository
import com.at.recallly.domain.repository.VoiceNoteRepository
import com.at.recallly.domain.usecase.auth.GetCurrentUserUseCase
import com.at.recallly.domain.usecase.auth.LoginWithEmailUseCase
import com.at.recallly.domain.usecase.auth.LoginWithGoogleUseCase
import com.at.recallly.domain.usecase.auth.DeleteAccountUseCase
import com.at.recallly.domain.usecase.auth.DeleteAllDataUseCase
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
import com.at.recallly.data.worker.BackupWorkScheduler
import com.at.recallly.domain.repository.BackupRepository
import com.at.recallly.domain.repository.ExtractionScheduler
import com.at.recallly.domain.repository.ReminderScheduler
import com.at.recallly.domain.repository.WhisperRepository
import com.at.recallly.domain.usecase.backup.BackupDataUseCase
import com.at.recallly.domain.usecase.backup.GetBackupInfoUseCase
import com.at.recallly.domain.usecase.backup.RestoreDataUseCase
import com.at.recallly.domain.usecase.billing.ObservePremiumStatusUseCase
import com.at.recallly.domain.usecase.fields.AddCustomFieldUseCase
import com.at.recallly.domain.usecase.fields.DeleteCustomFieldUseCase
import com.at.recallly.domain.usecase.fields.UpdateCustomFieldUseCase
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

    // ── Core / Infrastructure ────────────────────────────────────────────

    single { FirebaseAuth.getInstance() }
    single { CredentialManager.create(get()) }
    single { PreferencesManager(get()) }
    single { ConnectivityChecker(get()) }

    single {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    // Database — uncomment when KSP is enabled (AGP 9.0+)
    // single {
    //     Room.databaseBuilder(get(), RecalllyDatabase::class.java, "recallly.db")
    //         .build()
    // }
    // single { get<RecalllyDatabase>().userDao() }

    // ── Storage ──────────────────────────────────────────────────────────

    single { VoiceNoteFileStorage(get()) }
    single {
        VoiceNoteRepositoryImpl(get()).also {
            runBlocking { it.loadFromDisk() }
        }
    }
    single<VoiceNoteRepository> { get<VoiceNoteRepositoryImpl>() }

    single { CustomFieldFileStorage(get()) }
    single {
        CustomFieldRepositoryImpl(get()).also {
            runBlocking { it.loadFromDisk() }
        }
    }
    single<CustomFieldRepository> { get<CustomFieldRepositoryImpl>() }

    // ── Whisper (offline speech recognition) ─────────────────────────────

    single { WhisperModelManager(get()) }
    single<WhisperRepository> { WhisperRepositoryImpl(get()) }
    factory { AudioRecorder() }

    // ── AI Extraction ────────────────────────────────────────────────────

    single<ExtractionService> { GeminiExtractionService(get()) }
    single<ExtractionScheduler> { ExtractionWorkScheduler(get()) }

    // ── Notifications ──────────────────────────────────────────────────

    single<ReminderScheduler> { AlarmReminderScheduler(get(), get()) }

    // ── Billing ──────────────────────────────────────────────────────────

    single { BillingClientWrapper(get()) }
    single { PremiumPreferences(get()) }
    single<BillingRepository> { BillingRepositoryImpl(get(), get()) }

    // ── Ads ───────────────────────────────────────────────────────────────

    single { RewardedAdManager() }
    single<AdRepository> { AdRepositoryImpl(get()) }

    // ── Backup ─────────────────────────────────────────────────────────

    single { DriveBackupService(get(), get()) }
    single { BackupWorkScheduler(get()) }
    single<BackupRepository> {
        BackupRepositoryImpl(get(), get(), get(), get(), get<VoiceNoteRepositoryImpl>(), get<CustomFieldRepositoryImpl>())
    }

    // ── Export ───────────────────────────────────────────────────────────

    single { PdfExportService(get()) }

    // ── Repositories ─────────────────────────────────────────────────────

    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<OnboardingRepository> { OnboardingRepositoryImpl(get()) }

    // ── Use Cases: Auth ──────────────────────────────────────────────────

    factory { LoginWithEmailUseCase(get()) }
    factory { SignUpWithEmailUseCase(get()) }
    factory { LoginWithGoogleUseCase(get()) }
    factory { GetCurrentUserUseCase(get()) }
    factory { LogoutUseCase(get()) }
    factory { DeleteAllDataUseCase(get(), get(), get()) }
    factory { DeleteAccountUseCase(get(), get(), get(), get()) }

    // ── Use Cases: Onboarding ────────────────────────────────────────────

    factory { GetFieldsForPersonaUseCase() }
    factory { SavePersonaUseCase(get()) }
    factory { SaveFieldsUseCase(get()) }
    factory { SaveScheduleUseCase(get()) }

    // ── Use Cases: Voice ─────────────────────────────────────────────────

    factory { ExtractFieldsUseCase(get()) }
    factory { SaveVoiceNoteUseCase(get()) }
    factory { GetVoiceNotesUseCase(get()) }
    factory { TranscribeOfflineUseCase(get()) }
    factory { QueueExtractionUseCase(get()) }

    // ── Use Cases: Custom Fields ─────────────────────────────────────────

    factory { AddCustomFieldUseCase(get(), get()) }
    factory { UpdateCustomFieldUseCase(get()) }
    factory { DeleteCustomFieldUseCase(get(), get()) }

    // ── Use Cases: Billing ───────────────────────────────────────────────

    factory { ObservePremiumStatusUseCase(get()) }

    // ── Use Cases: Backup ──────────────────────────────────────────────

    factory { BackupDataUseCase(get()) }
    factory { RestoreDataUseCase(get()) }
    factory { GetBackupInfoUseCase(get()) }

    // ── Use Cases: Export ────────────────────────────────────────────────

    factory { ExportVoiceNotesPdfUseCase(get(), get(), get(), get(), get(), get()) }

    // ── ViewModels ───────────────────────────────────────────────────────

    viewModel {
        AuthViewModel(
            loginWithEmailUseCase = get(),
            signUpWithEmailUseCase = get(),
            loginWithGoogleUseCase = get(),
            logoutUseCase = get(),
            getCurrentUserUseCase = get()
        )
    }

    viewModel {
        OnboardingViewModel(
            getCurrentUserUseCase = get(),
            getFieldsForPersonaUseCase = get(),
            savePersonaUseCase = get(),
            saveFieldsUseCase = get(),
            saveScheduleUseCase = get(),
            onboardingRepository = get(),
            backupWorkScheduler = get()
        )
    }

    viewModel {
        HomeViewModel(
            getCurrentUserUseCase = get(),
            onboardingRepository = get(),
            extractFieldsUseCase = get(),
            saveVoiceNoteUseCase = get(),
            getVoiceNotesUseCase = get(),
            connectivityChecker = get(),
            whisperRepository = get(),
            audioRecorder = get(),
            preferencesManager = get(),
            queueExtractionUseCase = get(),
            voiceNoteRepository = get(),
            observePremiumStatusUseCase = get(),
            customFieldRepository = get(),
            reminderScheduler = get(),
            adRepository = get()
        )
    }

    viewModel {
        SettingsViewModel(
            getCurrentUserUseCase = get(),
            onboardingRepository = get(),
            getFieldsForPersonaUseCase = get(),
            logoutUseCase = get(),
            whisperRepository = get(),
            connectivityChecker = get(),
            preferencesManager = get(),
            exportVoiceNotesPdfUseCase = get(),
            observePremiumStatusUseCase = get(),
            customFieldRepository = get(),
            addCustomFieldUseCase = get(),
            updateCustomFieldUseCase = get(),
            deleteCustomFieldUseCase = get(),
            deleteAllDataUseCase = get(),
            deleteAccountUseCase = get(),
            billingRepository = get(),
            backupDataUseCase = get(),
            restoreDataUseCase = get(),
            getBackupInfoUseCase = get(),
            backupWorkScheduler = get(),
            backupRepository = get(),
            driveBackupService = get()
        )
    }
}
