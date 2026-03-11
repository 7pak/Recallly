package com.at.recallly.domain.usecase.auth

import com.at.recallly.domain.model.User
import com.at.recallly.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class GetCurrentUserUseCase(private val authRepository: AuthRepository) {
    operator fun invoke(): Flow<User?> {
        return authRepository.currentUser
    }
}
