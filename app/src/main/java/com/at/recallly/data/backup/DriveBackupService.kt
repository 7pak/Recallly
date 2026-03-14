package com.at.recallly.data.backup

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream

class DriveBackupService(
    private val context: Context,
    private val firebaseAuth: FirebaseAuth
) {
    companion object {
        private const val BACKUP_FILE_NAME = "recallly_backup.json"
        private const val BACKUP_MIME_TYPE = "application/json"
        val DRIVE_APPDATA_SCOPE = Scope(DriveScopes.DRIVE_APPDATA)
    }

    private fun buildDriveService(): Drive {
        val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
            ?: throw DriveAuthRequiredException("Google Sign-In required for backup")

        if (!googleAccount.grantedScopes.contains(DRIVE_APPDATA_SCOPE)) {
            throw DriveAuthRequiredException("Drive backup permission required")
        }

        val account = googleAccount.account
            ?: throw DriveAuthRequiredException("No Google account available")

        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Recallly")
            .build()
    }

    fun hasRequiredScopes(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return false
        return account.grantedScopes.contains(DRIVE_APPDATA_SCOPE)
    }

    fun getAuthorizationIntent(webClientId: String): Intent {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(webClientId)
            .requestScopes(DRIVE_APPDATA_SCOPE)
            .build()
        return GoogleSignIn.getClient(context, signInOptions).signInIntent
    }

    fun isGoogleUser(): Boolean {
        val user = firebaseAuth.currentUser ?: return false
        return user.providerData.any { it.providerId == "google.com" }
    }

    suspend fun uploadBackup(jsonContent: String): String = withContext(Dispatchers.IO) {
        val drive = buildDriveService()

        val existingFileId = findBackupFileId(drive)

        val content = ByteArrayContent.fromString(BACKUP_MIME_TYPE, jsonContent)

        if (existingFileId != null) {
            drive.files().update(existingFileId, null, content).execute()
            Timber.d("Updated existing backup file: $existingFileId")
            existingFileId
        } else {
            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = BACKUP_FILE_NAME
                parents = listOf("appDataFolder")
            }
            val file = drive.files().create(fileMetadata, content)
                .setFields("id")
                .execute()
            Timber.d("Created new backup file: ${file.id}")
            file.id
        }
    }

    suspend fun downloadBackup(): String? = withContext(Dispatchers.IO) {
        val drive = buildDriveService()
        val fileId = findBackupFileId(drive) ?: return@withContext null

        val outputStream = ByteArrayOutputStream()
        drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
        outputStream.toString("UTF-8")
    }

    private fun findBackupFileId(drive: Drive): String? {
        val result = drive.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = '$BACKUP_FILE_NAME'")
            .setFields("files(id)")
            .setPageSize(1)
            .execute()
        return result.files?.firstOrNull()?.id
    }
}

class DriveAuthRequiredException(message: String) : Exception(message)
