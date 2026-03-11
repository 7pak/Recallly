package com.at.recallly.domain.usecase.auth

import android.content.Context
import com.at.recallly.core.result.Result
import com.at.recallly.domain.model.User
import com.at.recallly.domain.repository.AuthRepository

class LoginWithGoogleUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(context: Context): Result<User> {
        return authRepository.loginWithGoogle(context)
    }
}
