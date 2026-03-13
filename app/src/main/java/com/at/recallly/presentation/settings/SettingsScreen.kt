package com.at.recallly.presentation.settings

import android.content.Intent
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
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.at.recallly.R
import com.at.recallly.core.util.LanguageManager
import com.at.recallly.data.whisper.WhisperModelManager
import com.at.recallly.domain.model.ModelDownloadState
import com.at.recallly.presentation.common.LanguageSelectorDialog
import com.at.recallly.presentation.util.localizedDisplayName
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
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
