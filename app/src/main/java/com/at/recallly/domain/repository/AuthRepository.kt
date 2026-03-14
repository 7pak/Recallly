package com.at.recallly.domain.repository

import android.content.Context
import com.at.recallly.core.result.Result
import com.at.recallly.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun loginWithEmail(email: String, password: String): Result<User>
    suspend fun signUpWithEmail(name: String, email: String, password: String): Result<User>
    suspend fun loginWithGoogle(context: Context): Result<User>
    suspend fun logout()
    suspend fun deleteAccount()
}
