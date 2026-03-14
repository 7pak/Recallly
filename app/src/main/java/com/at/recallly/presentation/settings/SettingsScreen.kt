package com.at.recallly.presentation.settings

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.at.recallly.R
import com.at.recallly.core.util.LanguageManager
import com.at.recallly.data.whisper.WhisperModelManager
import com.at.recallly.domain.model.ModelDownloadState
import com.at.recallly.presentation.common.LanguageSelectorDialog
import com.at.recallly.presentation.util.localizedDisplayName
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToPersonaSelection: () -> Unit,
    onNavigateToFieldSelection: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDeleteDataDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    val currentLanguageCode = LanguageManager.getCurrentLanguageCode()
    val context = LocalContext.current

    // Launch share sheet when PDF export completes
    LaunchedEffect(uiState.exportedFileUri) {
        uiState.exportedFileUri?.let { uri ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, null))
            onEvent(SettingsUiEvent.ResetExportState)
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.settings_logout),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_logout_confirm),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.settings_logout))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showLanguageDialog) {
        LanguageSelectorDialog(
            currentLanguageCode = currentLanguageCode,
            onLanguageSelected = { code ->
                showLanguageDialog = false
                onEvent(SettingsUiEvent.ChangeLanguage(code))
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    // Navigate out after data deletion or account deletion
    LaunchedEffect(uiState.dataDeleted) {
        if (uiState.dataDeleted) onLogout()
    }
    LaunchedEffect(uiState.accountDeleted) {
        if (uiState.accountDeleted) onLogout()
    }

    if (showDeleteDataDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDataDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.DeleteSweep,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.settings_delete_data),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_delete_data_confirm),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDataDialog = false
                        onEvent(SettingsUiEvent.DeleteAllData)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDataDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.settings_delete_account),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_delete_account_confirm),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        onEvent(SettingsUiEvent.DeleteAccount)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.settings_delete_account))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteAccountDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // Error dialog
    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { onEvent(SettingsUiEvent.DismissError) },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { onEvent(SettingsUiEvent.DismissError) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            }
        )
    }

    // Restore confirmation dialog
    if (uiState.showRestoreConfirmDialog && uiState.remoteBackupInfo != null) {
        val info = uiState.remoteBackupInfo
        val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            .format(Date(info.timestamp))
        AlertDialog(
            onDismissRequest = { onEvent(SettingsUiEvent.DismissRestoreDialog) },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.settings_restore_confirm_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.settings_restore_confirm_message,
                        dateStr,
                        info.voiceNoteCount,
                        info.deviceName
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { onEvent(SettingsUiEvent.ConfirmRestore) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.settings_restore))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { onEvent(SettingsUiEvent.DismissRestoreDialog) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // Backup/restore success feedback
    LaunchedEffect(uiState.backupSuccess) {
        if (uiState.backupSuccess) {
            kotlinx.coroutines.delay(2000)
            onEvent(SettingsUiEvent.ResetBackupSuccess)
        }
    }
    LaunchedEffect(uiState.restoreSuccess) {
        if (uiState.restoreSuccess) {
            kotlinx.coroutines.delay(2000)
            onEvent(SettingsUiEvent.ResetRestoreSuccess)
        }
    }

    // Backup error dialog
    if (uiState.backupError != null) {
        AlertDialog(
            onDismissRequest = { onEvent(SettingsUiEvent.ResetBackupSuccess) },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Text(
                    text = uiState.backupError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { onEvent(SettingsUiEvent.ResetBackupSuccess) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            }
        )
    }

    // Restore error dialog
    if (uiState.restoreError != null) {
        AlertDialog(
            onDismissRequest = { onEvent(SettingsUiEvent.ResetRestoreSuccess) },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Text(
                    text = uiState.restoreError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { onEvent(SettingsUiEvent.ResetRestoreSuccess) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Premium Subscription Card — at the top, like professional apps
            if (!uiState.isPremium) {
                PremiumSubscriptionCard(
                    isPurchasing = uiState.isPurchasing,
                    onSubscribeClick = {
                        val activity = context as? Activity
                        if (activity != null) {
                            onEvent(SettingsUiEvent.LaunchPurchase(activity))
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Professional Type
            SettingsRow(
                icon = Icons.Outlined.Work,
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                title = stringResource(R.string.settings_professional_type),
                subtitle = uiState.currentPersona?.localizedDisplayName() ?: "Not set",
                onClick = onNavigateToPersonaSelection
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Manage Fields
            SettingsRow(
                icon = Icons.Outlined.Tune,
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                title = stringResource(R.string.settings_manage_fields),
                subtitle = stringResource(R.string.settings_fields_selected, uiState.selectedFieldCount, uiState.totalFieldCount),
                onClick = onNavigateToFieldSelection
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Work Schedule
            SettingsRow(
                icon = Icons.Outlined.Schedule,
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                title = stringResource(R.string.settings_work_schedule),
                subtitle = buildScheduleSummary(uiState.workSchedule),
                onClick = onNavigateToSchedule
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Language
            SettingsRow(
                icon = Icons.Outlined.Language,
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                title = stringResource(R.string.settings_language),
                subtitle = LanguageManager.getLanguageDisplayName(currentLanguageCode),
                onClick = { showLanguageDialog = true }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Offline Voice Model
            val modelState = uiState.whisperModelState
            SettingsRow(
                icon = Icons.Outlined.CloudDownload,
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                title = stringResource(R.string.settings_offline_model),
                subtitle = when (modelState) {
                    is ModelDownloadState.Downloaded ->
                        stringResource(R.string.home_model_downloaded, WhisperModelManager.MODEL_SIZE_MB)
                    is ModelDownloadState.Downloading ->
                        stringResource(R.string.home_model_downloading_percent, (modelState.progress * 100).toInt())
                    is ModelDownloadState.Error -> stringResource(R.string.settings_model_download_failed)
                    ModelDownloadState.NotDownloaded ->
                        stringResource(R.string.home_model_not_downloaded, WhisperModelManager.MODEL_SIZE_MB)
                },
                onClick = {
                    if (modelState !is ModelDownloadState.Downloaded &&
                        modelState !is ModelDownloadState.Downloading
                    ) {
                        onEvent(SettingsUiEvent.DownloadWhisperModel)
                    }
                }
            )

            // Download progress bar
            if (modelState is ModelDownloadState.Downloading) {
                LinearProgressIndicator(
                    progress = { modelState.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Export Data
            SettingsRow(
                icon = Icons.Outlined.Description,
                iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                title = stringResource(R.string.settings_export_data),
                subtitle = when {
                    uiState.isExporting -> stringResource(R.string.export_exporting)
                    uiState.exportError == "empty" -> stringResource(R.string.export_empty)
                    uiState.exportError != null -> stringResource(R.string.export_error)
                    else -> stringResource(R.string.settings_export_subtitle)
                },
                onClick = {
                    if (!uiState.isExporting) onEvent(SettingsUiEvent.ExportPdf)
                }
            )

            if (uiState.isExporting) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Backup & Restore Section
            DriveBackupRow(
                driveBackupEnabled = uiState.driveBackupEnabled,
                onToggle = { onEvent(SettingsUiEvent.ToggleDriveBackup) }
            )

            if (uiState.driveBackupEnabled) {
                // Backup Now
                val backupSubtitle = when {
                    uiState.isBackingUp -> stringResource(R.string.settings_backup_in_progress)
                    uiState.backupSuccess -> stringResource(R.string.settings_backup_success)
                    uiState.lastBackupTimestamp != null -> stringResource(
                        R.string.settings_last_backup,
                        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                            .format(Date(uiState.lastBackupTimestamp))
                    )
                    else -> stringResource(R.string.settings_never_backed_up)
                }
                SettingsRow(
                    icon = Icons.Outlined.Backup,
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                    title = stringResource(R.string.settings_backup_now),
                    subtitle = backupSubtitle,
                    onClick = {
                        if (!uiState.isBackingUp) onEvent(SettingsUiEvent.BackupNow)
                    }
                )

                if (uiState.isBackingUp) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Restore
                val restoreSubtitle = when {
                    uiState.isRestoring -> stringResource(R.string.settings_restore_in_progress)
                    uiState.restoreSuccess -> stringResource(R.string.settings_restore_success)
                    else -> stringResource(R.string.settings_restore_subtitle)
                }
                SettingsRow(
                    icon = Icons.Outlined.CloudDownload,
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                    title = stringResource(R.string.settings_restore),
                    subtitle = restoreSubtitle,
                    onClick = {
                        if (!uiState.isRestoring) onEvent(SettingsUiEvent.RestoreFromBackup)
                    }
                )

                if (uiState.isRestoring) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Calendar Sync
            CalendarSyncRow(
                isPremium = uiState.isPremium,
                calendarSyncEnabled = uiState.calendarSyncEnabled,
                onToggle = { onEvent(SettingsUiEvent.ToggleCalendarSync) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Reminder Notifications
            ReminderNotificationsRow(
                isPremium = uiState.isPremium,
                reminderEnabled = uiState.reminderNotificationsEnabled,
                onToggle = { onEvent(SettingsUiEvent.ToggleReminderNotifications) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Delete All Data
            SettingsRow(
                icon = Icons.Outlined.DeleteSweep,
                iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                iconTint = MaterialTheme.colorScheme.onErrorContainer,
                title = stringResource(R.string.settings_delete_data),
                titleColor = MaterialTheme.colorScheme.error,
                subtitle = if (uiState.isDeletingData) stringResource(R.string.settings_deleting_data)
                    else stringResource(R.string.settings_delete_data_subtitle),
                onClick = {
                    if (!uiState.isDeletingData) showDeleteDataDialog = true
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Delete Account
            SettingsRow(
                icon = Icons.Outlined.DeleteForever,
                iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                iconTint = MaterialTheme.colorScheme.onErrorContainer,
                title = stringResource(R.string.settings_delete_account),
                titleColor = MaterialTheme.colorScheme.error,
                subtitle = if (uiState.isDeletingAccount) stringResource(R.string.settings_deleting_account)
                    else stringResource(R.string.settings_delete_account_subtitle),
                onClick = {
                    if (!uiState.isDeletingAccount) showDeleteAccountDialog = true
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Log Out
            SettingsRow(
                icon = Icons.AutoMirrored.Outlined.Logout,
                iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                iconTint = MaterialTheme.colorScheme.onErrorContainer,
                title = stringResource(R.string.settings_logout),
                titleColor = MaterialTheme.colorScheme.error,
                subtitle = stringResource(R.string.settings_logout_subtitle),
                onClick = { showLogoutDialog = true }
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconContainerColor: androidx.compose.ui.graphics.Color,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = iconContainerColor,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = iconTint
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = titleColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CalendarSyncRow(
    isPremium: Boolean,
    calendarSyncEnabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isPremium, onClick = onToggle)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.settings_calendar_sync),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!isPremium) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(20.dp)
                    ) {
                        Text(
                            text = "PRO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = if (isPremium && calendarSyncEnabled)
                    stringResource(R.string.settings_calendar_sync_on)
                else
                    stringResource(R.string.settings_calendar_sync_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = calendarSyncEnabled,
            onCheckedChange = { if (isPremium) onToggle() },
            enabled = isPremium
        )
    }
}

@Composable
private fun ReminderNotificationsRow(
    isPremium: Boolean,
    reminderEnabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isPremium, onClick = onToggle)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.settings_reminder_notifications),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!isPremium) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(20.dp)
                    ) {
                        Text(
                            text = "PRO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = if (isPremium && reminderEnabled)
                    stringResource(R.string.settings_reminder_notifications_on)
                else
                    stringResource(R.string.settings_reminder_notifications_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = reminderEnabled,
            onCheckedChange = { if (isPremium) onToggle() },
            enabled = isPremium
        )
    }
}

@Composable
private fun DriveBackupRow(
    driveBackupEnabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.CloudUpload,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_drive_backup),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.settings_drive_backup_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = driveBackupEnabled,
            onCheckedChange = { onToggle() }
        )
    }
}

private fun formatTime(time: LocalTime): String {
    val hour = if (time.hour == 0) 12
    else if (time.hour > 12) time.hour - 12
    else time.hour
    val minute = String.format("%02d", time.minute)
    val amPm = if (time.hour < 12) "AM" else "PM"
    return "$hour:$minute $amPm"
}

@Composable
private fun buildScheduleSummary(schedule: com.at.recallly.domain.model.WorkSchedule): String {
    if (schedule.workDays.isEmpty()) return stringResource(R.string.schedule_no_days)

    val sortedDays = schedule.workDays.sorted()
    val dayStr = when {
        sortedDays.size == 7 -> stringResource(R.string.schedule_every_day)
        sortedDays.size == 5 && DayOfWeek.SATURDAY !in sortedDays && DayOfWeek.SUNDAY !in sortedDays ->
            stringResource(R.string.schedule_mon_fri)
        else -> sortedDays.joinToString(", ") {
            it.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        }
    }
    return "$dayStr, ${formatTime(schedule.startTime)} \u2013 ${formatTime(schedule.endTime)}"
}

@Composable
private fun PremiumSubscriptionCard(
    isPurchasing: Boolean,
    onSubscribeClick: () -> Unit
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0F172A), // DeepSlate / Slate900
            Color(0xFF1E293B), // Slate800
            Color(0xFF064E3B)  // MintDark
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(gradientBrush)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Header: Star icon + Title + Limited Offer badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = Color(0xFFFBBF24) // Amber/Gold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.premium_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFBBF24)
                ) {
                    Text(
                        text = stringResource(R.string.premium_limited_offer),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Benefits list with check icons
            val benefits = listOf(
                R.string.premium_benefit_notes,
                R.string.premium_benefit_no_ads,
                R.string.premium_benefit_calendar,
                R.string.premium_benefit_notifications
            )
            benefits.forEach { resId ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF10B981) // ElectricMint
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(resId),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE2E8F0) // Slate200
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Price section: crossed-out original + current price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = stringResource(R.string.premium_original_price),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF94A3B8), // Slate400
                    textDecoration = TextDecoration.LineThrough
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.premium_offer_price),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.premium_per_month),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(start = 4.dp, bottom = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subscribe button
            Button(
                onClick = onSubscribeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isPurchasing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981), // ElectricMint
                    contentColor = Color.White
                )
            ) {
                if (isPurchasing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = stringResource(R.string.premium_subscribe_button),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
