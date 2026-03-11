package com.at.recallly.core.di

import androidx.credentials.CredentialManager
import androidx.room.Room
import com.at.recallly.data.local.db.RecalllyDatabase
import com.at.recallly.data.repository.AuthRepositoryImpl
import com.at.recallly.domain.repository.AuthRepository
import com.at.recallly.domain.usecase.auth.GetCurrentUserUseCase
import com.at.recallly.domain.usecase.auth.LoginWithEmailUseCase
import com.at.recallly.domain.usecase.auth.LoginWithGoogleUseCase
import com.at.recallly.domain.usecase.auth.LogoutUseCase
import com.at.recallly.domain.usecase.auth.SignUpWithEmailUseCase
import com.at.recallly.presentation.auth.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
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

    // Repositories
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }

    // Use Cases
    factory { LoginWithEmailUseCase(get()) }
    factory { SignUpWithEmailUseCase(get()) }
    factory { LoginWithGoogleUseCase(get()) }
    factory { GetCurrentUserUseCase(get()) }
    factory { LogoutUseCase(get()) }

    // ViewModels
    viewModel { AuthViewModel(get(), get(), get(), get(), get()) }
}
