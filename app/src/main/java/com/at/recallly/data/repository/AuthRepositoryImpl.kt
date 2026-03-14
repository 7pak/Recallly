package com.at.recallly.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.at.recallly.core.result.Result
import com.at.recallly.core.util.Constants
import com.at.recallly.domain.model.User
import com.at.recallly.domain.repository.AuthRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val credentialManager: CredentialManager
) : AuthRepository {

    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toDomainUser())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun loginWithEmail(email: String, password: String): Result<User> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user?.toDomainUser()
            if (user != null) Result.Success(user)
            else Result.Error(Exception("Login failed: user is null"))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun signUpWithEmail(name: String, email: String, password: String): Result<User> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                val profileUpdate = userProfileChangeRequest { displayName = name }
                firebaseUser.updateProfile(profileUpdate).await()
                Result.Success(firebaseUser.toDomainUser())
            } else {
                Result.Error(Exception("Sign up failed: user is null"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun loginWithGoogle(context: Context): Result<User> {
        return try {
            Timber.d("Google Sign-In: WEB_CLIENT_ID = ${Constants.WEB_CLIENT_ID}")

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(Constants.WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            val googleIdToken = GoogleIdTokenCredential.createFrom(result.credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken.idToken, null)
            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()

            val user = authResult.user?.toDomainUser()
            if (user != null) Result.Success(user)
            else Result.Error(Exception("Google sign-in failed: user is null"))
        } catch (e: NoCredentialException) {
            Timber.e(e, "Google Sign-In: No credential available")
            Result.Error(Exception("No Google account found. Please add a Google account to your device and try again."))
        } catch (e: GetCredentialCancellationException) {
            Timber.e(e, "Google Sign-In: Cancelled")
            Result.Error(Exception("Google sign-in was cancelled."))
        } catch (e: Exception) {
            Timber.e(e, "Google Sign-In failed: ${e.javaClass.simpleName}")
            Result.Error(e)
        }
    }

    override suspend fun logout() {
        try {
            credentialManager.clearCredentialState(
                androidx.credentials.ClearCredentialStateRequest()
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear credential state")
        }
        firebaseAuth.signOut()
    }

    override suspend fun deleteAccount() {
        val user = firebaseAuth.currentUser ?: throw Exception("No user signed in")
        try {
            credentialManager.clearCredentialState(
                androidx.credentials.ClearCredentialStateRequest()
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear credential state")
        }
        user.delete().await()
    }

    private fun FirebaseUser.toDomainUser(): User {
        return User(
            id = uid,
            email = email.orEmpty(),
            displayName = displayName
        )
    }
}
