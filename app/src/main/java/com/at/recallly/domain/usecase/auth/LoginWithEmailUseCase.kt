package com.at.recallly.domain.usecase.auth

import com.at.recallly.core.result.Result
import com.at.recallly.domain.model.User
import com.at.recallly.domain.repository.AuthRepository

class LoginWithEmailUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        return authRepository.loginWithEmail(email, password)
    }
}
