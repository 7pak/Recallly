package com.at.recallly.presentation.home

import java.time.LocalDateTime

sealed interface HomeUiEvent {
    data object MicTapped : HomeUiEvent
    data class StopRecording(val transcript: String) : HomeUiEvent
    data object DismissRecordingSheet : HomeUiEvent
    data class UpdateExtractedField(val fieldId: String, val value: String) : HomeUiEvent
    data class UpdateAdditionalNotes(val notes: String) : HomeUiEvent
    data object SaveExtractionResult : HomeUiEvent
    data class SaveOfflineTranscript(val editedTranscript: String) : HomeUiEvent
    data object DismissExtractionResult : HomeUiEvent
    data object DismissError : HomeUiEvent
    data class DeleteVoiceNote(val id: String) : HomeUiEvent
    data object DownloadWhisperModel : HomeUiEvent
    data object CancelModelDownload : HomeUiEvent
    data object DismissModelDownloadDialog : HomeUiEvent
    data object StopWhisperRecording : HomeUiEvent
    data object CancelWhisperRecording : HomeUiEvent
    data object DismissInitialModelPrompt : HomeUiEvent
    data class SelectVoiceNote(val id: String) : HomeUiEvent
    data object DismissVoiceNoteDetail : HomeUiEvent
    data class EditVoiceNote(val id: String) : HomeUiEvent
    data class UpdateNoteField(val fieldId: String, val value: String) : HomeUiEvent
    data object SaveNoteEdits : HomeUiEvent
    data object CancelNoteEdits : HomeUiEvent
    data class AddToCalendarTapped(val voiceNoteId: String, val fieldId: String) : HomeUiEvent
    data class ConfirmAddToCalendar(val dateTime: LocalDateTime) : HomeUiEvent
    data object DismissCalendarDialog : HomeUiEvent
    data object DismissCalendarSuccess : HomeUiEvent
    data object CalendarIntentLaunched : HomeUiEvent
    data object AdGateAccepted : HomeUiEvent
    data object AdGateDismissed : HomeUiEvent
    data object PreRecordAdCompleted : HomeUiEvent
    data object PreRecordAdFailed : HomeUiEvent
    data object PostSaveAdCompleted : HomeUiEvent
    data object PostSaveAdFailed : HomeUiEvent
    data object ReadyToRecordConfirmed : HomeUiEvent
    data object ReadyToRecordDismissed : HomeUiEvent
}
