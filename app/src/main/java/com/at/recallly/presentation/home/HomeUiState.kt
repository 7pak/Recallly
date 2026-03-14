package com.at.recallly.presentation.home

import android.content.Intent
import com.at.recallly.domain.model.ModelDownloadState
import com.at.recallly.domain.model.Persona
import com.at.recallly.domain.model.PersonaField
import com.at.recallly.domain.model.VoiceNote
import com.at.recallly.domain.model.WorkSchedule
import com.at.recallly.domain.repository.AdRepository
import java.time.LocalDate

data class HomeUiState(
    val displayName: String = "",
    val persona: Persona? = null,
    val workSchedule: WorkSchedule = WorkSchedule(),
    val noteCountToday: Int = 0,
    val voiceNotes: List<VoiceNote> = emptyList(),
    val recordingState: RecordingState = RecordingState.Idle,
    val liveTranscript: String = "",
    val extractionState: ExtractionState = ExtractionState.Idle,
    val selectedFields: List<PersonaField> = emptyList(),
    val errorMessage: String? = null,
    val whisperModelState: ModelDownloadState = ModelDownloadState.NotDownloaded,
    val showModelDownloadDialog: Boolean = false,
    val showInitialModelPrompt: Boolean = false,
    val isOfflineExtraction: Boolean = false,
    val selectedVoiceNote: VoiceNote? = null,
    val editingVoiceNote: VoiceNote? = null,
    val editingFields: Map<String, String> = emptyMap(),
    val isPremium: Boolean = false,
    val calendarDialog: CalendarDialogState? = null,
    val calendarSuccess: Boolean = false,
    val calendarIntent: Intent? = null,
    val freeNotesUsed: Int = 0,
    val freeNotesLimit: Int = AdRepository.FREE_NOTES_LIMIT,
    val adRequired: Boolean = false,
    val showAdGateDialog: Boolean = false,
    val showPreRecordAd: Boolean = false,
    val showPostSaveAd: Boolean = false,
    val showReadyToRecordDialog: Boolean = false,
    val canUseCalendarSync: Boolean = true,
    val canUseNotification: Boolean = true
)

data class CalendarDialogState(
    val voiceNoteId: String,
    val fieldId: String,
    val fieldDisplayName: String,
    val suggestedDate: LocalDate?,
    val eventTitle: String,
    val eventDescription: String
)

sealed interface RecordingState {
    data object Idle : RecordingState
    data class Recording(val silenceCountdownSeconds: Int? = null) : RecordingState
    data class RecordingWhisper(
        val elapsedSeconds: Int = 0,
        val amplitude: Float = 0f,
        val silenceCountdownSeconds: Int? = null
    ) : RecordingState
    data object Transcribing : RecordingState
    data object Processing : RecordingState
}

sealed interface ExtractionState {
    data object Idle : ExtractionState
    data class Success(
        val transcript: String,
        val fields: Map<String, String>,
        val additionalNotes: String
    ) : ExtractionState
}
