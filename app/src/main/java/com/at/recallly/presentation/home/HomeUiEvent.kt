package com.at.recallly.presentation.home

sealed interface HomeUiEvent {
    data object MicTapped : HomeUiEvent
    data class StopRecording(val transcript: String) : HomeUiEvent
    data object DismissRecordingSheet : HomeUiEvent
    data class UpdateExtractedField(val fieldId: String, val value: String) : HomeUiEvent
    data class UpdateAdditionalNotes(val notes: String) : HomeUiEvent
    data object SaveExtractionResult : HomeUiEvent
    data object DismissExtractionResult : HomeUiEvent
    data object DismissError : HomeUiEvent
    data class DeleteVoiceNote(val id: String) : HomeUiEvent
    data object DownloadWhisperModel : HomeUiEvent
    data object CancelModelDownload : HomeUiEvent
    data object DismissModelDownloadDialog : HomeUiEvent
    data object StopWhisperRecording : HomeUiEvent
    data object CancelWhisperRecording : HomeUiEvent
    data object DismissInitialModelPrompt : HomeUiEvent
}
