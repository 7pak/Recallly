package com.at.recallly.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.at.recallly.core.result.Result
import com.at.recallly.core.util.ConnectivityChecker
import com.at.recallly.core.util.LanguageManager
import com.at.recallly.data.local.datastore.PreferencesManager
import com.at.recallly.data.whisper.AudioRecorder
import com.at.recallly.domain.model.PersonaFields
import com.at.recallly.domain.model.VoiceNote
import com.at.recallly.domain.repository.OnboardingRepository
import com.at.recallly.domain.repository.WhisperRepository
import com.at.recallly.domain.usecase.auth.GetCurrentUserUseCase
import com.at.recallly.domain.usecase.voice.ExtractFieldsUseCase
import com.at.recallly.domain.usecase.voice.GetVoiceNotesUseCase
import com.at.recallly.domain.usecase.voice.QueueExtractionUseCase
import com.at.recallly.domain.usecase.voice.SaveVoiceNoteUseCase
import com.at.recallly.domain.repository.VoiceNoteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class HomeViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val onboardingRepository: OnboardingRepository,
    private val extractFieldsUseCase: ExtractFieldsUseCase,
    private val saveVoiceNoteUseCase: SaveVoiceNoteUseCase,
    private val getVoiceNotesUseCase: GetVoiceNotesUseCase,
    private val connectivityChecker: ConnectivityChecker,
    private val whisperRepository: WhisperRepository,
    private val audioRecorder: AudioRecorder,
    private val preferencesManager: PreferencesManager,
    private val queueExtractionUseCase: QueueExtractionUseCase,
    private val voiceNoteRepository: VoiceNoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var whisperTimerJob: Job? = null
    private var whisperAmplitudeJob: Job? = null
    private var whisperSilenceJob: Job? = null
    private var hasDetectedSpeech = false

    companion object {
        private const val SILENCE_TIMEOUT_SECONDS = 4
        private const val SILENCE_AMPLITUDE_THRESHOLD = 0.01f
    }

    init {
        // Observe main data flows
        viewModelScope.launch {
            combine(
                getCurrentUserUseCase().filterNotNull(),
                onboardingRepository.selectedPersona,
                onboardingRepository.selectedFieldIds,
                onboardingRepository.workSchedule,
                getVoiceNotesUseCase()
            ) { user, persona, fieldIds, schedule, voiceNotes ->
                val todayStart = LocalDate.now()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                val noteCountToday = voiceNotes.count { it.createdAt >= todayStart }

                val selectedFields = persona?.let { p ->
                    val allFields = PersonaFields.getFieldsForPersona(p)
                    if (fieldIds.isEmpty()) allFields
                    else allFields.filter { it.id in fieldIds }
                } ?: emptyList()

                HomeUiState(
                    displayName = user.displayName ?: user.email.substringBefore("@"),
                    persona = persona,
                    workSchedule = schedule,
                    noteCountToday = noteCountToday,
                    voiceNotes = voiceNotes,
                    selectedFields = selectedFields,
                    recordingState = _uiState.value.recordingState,
                    liveTranscript = _uiState.value.liveTranscript,
                    extractionState = _uiState.value.extractionState,
                    errorMessage = _uiState.value.errorMessage,
                    whisperModelState = _uiState.value.whisperModelState,
                    showModelDownloadDialog = _uiState.value.showModelDownloadDialog,
                    showInitialModelPrompt = _uiState.value.showInitialModelPrompt,
                    isOfflineExtraction = _uiState.value.isOfflineExtraction,
                    selectedVoiceNote = _uiState.value.selectedVoiceNote,
                    editingVoiceNote = _uiState.value.editingVoiceNote,
                    editingFields = _uiState.value.editingFields
                )
            }.collectLatest { state ->
                _uiState.update { state }
            }
        }

        // Observe whisper model download state
        viewModelScope.launch {
            whisperRepository.downloadState.collect { downloadState ->
                _uiState.update { it.copy(whisperModelState = downloadState) }
            }
        }

        // Migrate from English-only model to multilingual model if needed
        viewModelScope.launch {
            if (whisperRepository.needsModelMigration()) {
                whisperRepository.migrateModel()
            }
        }

        // Show initial model download prompt after onboarding (one-time)
        viewModelScope.launch {
            preferencesManager.hasSeenModelPrompt.collect { hasSeen ->
                if (!hasSeen && !whisperRepository.isModelDownloaded()) {
                    _uiState.update { it.copy(showInitialModelPrompt = true) }
                }
            }
        }
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.MicTapped -> {
                val isOffline = !connectivityChecker.isOnline()
                if (isOffline) {
                    if (whisperRepository.isModelDownloaded()) {
                        startWhisperRecording()
                    } else {
                        _uiState.update { it.copy(showModelDownloadDialog = true) }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            recordingState = RecordingState.Recording(),
                            liveTranscript = "",
                            extractionState = ExtractionState.Idle
                        )
                    }
                }
            }

            is HomeUiEvent.StopRecording -> {
                val transcript = event.transcript.trim()
                if (transcript.isBlank()) {
                    _uiState.update {
                        it.copy(
                            recordingState = RecordingState.Idle,
                            errorMessage = "No speech detected. Please try again."
                        )
                    }
                    return
                }
                _uiState.update { it.copy(recordingState = RecordingState.Processing) }
                extractFields(transcript)
            }

            is HomeUiEvent.DismissRecordingSheet -> {
                _uiState.update {
                    it.copy(recordingState = RecordingState.Idle, liveTranscript = "")
                }
            }

            is HomeUiEvent.StopWhisperRecording -> {
                stopWhisperRecording()
            }

            is HomeUiEvent.CancelWhisperRecording -> {
                cancelWhisperRecording()
            }

            is HomeUiEvent.DownloadWhisperModel -> {
                if (!connectivityChecker.isOnline()) {
                    _uiState.update {
                        it.copy(errorMessage = "You need an internet connection to download the offline voice model.")
                    }
                    return
                }
                _uiState.update {
                    it.copy(showModelDownloadDialog = false, showInitialModelPrompt = false)
                }
                viewModelScope.launch {
                    preferencesManager.setHasSeenModelPrompt()
                    whisperRepository.downloadModel()
                }
            }

            is HomeUiEvent.CancelModelDownload -> {
                viewModelScope.launch {
                    whisperRepository.cancelDownload()
                }
            }

            is HomeUiEvent.DismissModelDownloadDialog -> {
                _uiState.update { it.copy(showModelDownloadDialog = false) }
            }

            is HomeUiEvent.DismissInitialModelPrompt -> {
                _uiState.update { it.copy(showInitialModelPrompt = false) }
                viewModelScope.launch {
                    preferencesManager.setHasSeenModelPrompt()
                }
            }

            is HomeUiEvent.UpdateExtractedField -> {
                val current = _uiState.value.extractionState
                if (current is ExtractionState.Success) {
                    _uiState.update {
                        it.copy(
                            extractionState = current.copy(
                                fields = current.fields + (event.fieldId to event.value)
                            )
                        )
                    }
                }
            }

            is HomeUiEvent.UpdateAdditionalNotes -> {
                val current = _uiState.value.extractionState
                if (current is ExtractionState.Success) {
                    _uiState.update {
                        it.copy(
                            extractionState = current.copy(additionalNotes = event.notes)
                        )
                    }
                }
            }

            is HomeUiEvent.SaveExtractionResult -> {
                val current = _uiState.value.extractionState
                if (current is ExtractionState.Success) {
                    saveVoiceNote(current)
                }
            }

            is HomeUiEvent.DismissExtractionResult -> {
                _uiState.update {
                    it.copy(
                        extractionState = ExtractionState.Idle,
                        recordingState = RecordingState.Idle,
                        isOfflineExtraction = false
                    )
                }
            }

            is HomeUiEvent.DismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }

            is HomeUiEvent.DeleteVoiceNote -> {
                viewModelScope.launch {
                    voiceNoteRepository.deleteVoiceNote(event.id)
                    _uiState.update { state ->
                        state.copy(
                            selectedVoiceNote = if (state.selectedVoiceNote?.id == event.id) null else state.selectedVoiceNote
                        )
                    }
                }
            }

            is HomeUiEvent.SelectVoiceNote -> {
                val note = _uiState.value.voiceNotes.find { it.id == event.id }
                _uiState.update { it.copy(selectedVoiceNote = note) }
            }

            is HomeUiEvent.DismissVoiceNoteDetail -> {
                _uiState.update { it.copy(selectedVoiceNote = null, editingVoiceNote = null, editingFields = emptyMap()) }
            }

            is HomeUiEvent.EditVoiceNote -> {
                val note = _uiState.value.selectedVoiceNote ?: return
                val configuredFields = _uiState.value.selectedFields
                val editFields = mutableMapOf<String, String>()
                configuredFields.forEach { field ->
                    editFields[field.id] = note.extractedFields[field.id] ?: ""
                }
                _uiState.update { it.copy(editingVoiceNote = note, editingFields = editFields) }
            }

            is HomeUiEvent.UpdateNoteField -> {
                _uiState.update { state ->
                    state.copy(editingFields = state.editingFields + (event.fieldId to event.value))
                }
            }

            is HomeUiEvent.SaveNoteEdits -> {
                val editing = _uiState.value.editingVoiceNote ?: return
                val updatedNote = editing.copy(extractedFields = _uiState.value.editingFields)
                viewModelScope.launch {
                    voiceNoteRepository.updateVoiceNote(updatedNote)
                    _uiState.update {
                        it.copy(
                            selectedVoiceNote = updatedNote,
                            editingVoiceNote = null,
                            editingFields = emptyMap()
                        )
                    }
                }
            }

            is HomeUiEvent.CancelNoteEdits -> {
                _uiState.update { it.copy(editingVoiceNote = null, editingFields = emptyMap()) }
            }
        }
    }

    private fun startWhisperRecording() {
        hasDetectedSpeech = false
        audioRecorder.start()
        _uiState.update {
            it.copy(
                recordingState = RecordingState.RecordingWhisper(),
                liveTranscript = "",
                extractionState = ExtractionState.Idle
            )
        }

        // Elapsed time counter
        whisperTimerJob = viewModelScope.launch {
            var seconds = 0
            while (true) {
                delay(1000L)
                seconds++
                val current = _uiState.value.recordingState
                if (current is RecordingState.RecordingWhisper) {
                    _uiState.update {
                        it.copy(recordingState = current.copy(elapsedSeconds = seconds))
                    }
                } else break
            }
        }

        // Amplitude observer + silence detection
        whisperAmplitudeJob = viewModelScope.launch {
            audioRecorder.amplitude.collect { amp ->
                val current = _uiState.value.recordingState
                if (current is RecordingState.RecordingWhisper) {
                    _uiState.update {
                        it.copy(recordingState = current.copy(amplitude = amp))
                    }

                    if (amp > SILENCE_AMPLITUDE_THRESHOLD) {
                        hasDetectedSpeech = true
                        cancelSilenceTimer()
                    } else if (hasDetectedSpeech && whisperSilenceJob?.isActive != true) {
                        startSilenceTimer()
                    }
                }
            }
        }
    }

    private fun startSilenceTimer() {
        whisperSilenceJob = viewModelScope.launch {
            for (i in SILENCE_TIMEOUT_SECONDS downTo 1) {
                val current = _uiState.value.recordingState
                if (current is RecordingState.RecordingWhisper) {
                    _uiState.update {
                        it.copy(recordingState = current.copy(silenceCountdownSeconds = i))
                    }
                }
                delay(1000L)
            }
            // Auto-stop after silence timeout
            stopWhisperRecording()
        }
    }

    private fun cancelSilenceTimer() {
        whisperSilenceJob?.cancel()
        whisperSilenceJob = null
        val current = _uiState.value.recordingState
        if (current is RecordingState.RecordingWhisper && current.silenceCountdownSeconds != null) {
            _uiState.update {
                it.copy(recordingState = current.copy(silenceCountdownSeconds = null))
            }
        }
    }

    private fun stopWhisperRecording() {
        whisperTimerJob?.cancel()
        whisperAmplitudeJob?.cancel()
        whisperSilenceJob?.cancel()
        whisperSilenceJob = null
        val samples = audioRecorder.stop()

        if (samples.size < AudioRecorder.SAMPLE_RATE) {
            _uiState.update {
                it.copy(
                    recordingState = RecordingState.Idle,
                    errorMessage = "Recording too short. Please try again."
                )
            }
            return
        }

        _uiState.update { it.copy(recordingState = RecordingState.Transcribing) }

        viewModelScope.launch {
            val langCode = LanguageManager.getWhisperLanguageCode(
                preferencesManager.appLanguage.first()
            )
            when (val result = whisperRepository.transcribe(samples, langCode)) {
                is Result.Success -> {
                    val transcript = result.data.trim()
                    if (transcript.isBlank()) {
                        _uiState.update {
                            it.copy(
                                recordingState = RecordingState.Idle,
                                errorMessage = "No speech detected. Please try again."
                            )
                        }
                    } else if (connectivityChecker.isOnline()) {
                        // Back online — extract fields via Gemini
                        _uiState.update { it.copy(recordingState = RecordingState.Processing) }
                        extractFields(transcript)
                    } else {
                        // Still offline — show transcript for review, queue extraction on save
                        val fields = _uiState.value.selectedFields
                        val emptyFields = fields.associate { it.id to "" }
                        _uiState.update {
                            it.copy(
                                recordingState = RecordingState.Idle,
                                isOfflineExtraction = true,
                                extractionState = ExtractionState.Success(
                                    transcript = transcript,
                                    fields = emptyFields,
                                    additionalNotes = ""
                                )
                            )
                        }
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            recordingState = RecordingState.Idle,
                            errorMessage = result.exception.message ?: "Transcription failed"
                        )
                    }
                }
            }
        }
    }

    private fun cancelWhisperRecording() {
        whisperTimerJob?.cancel()
        whisperAmplitudeJob?.cancel()
        whisperSilenceJob?.cancel()
        whisperSilenceJob = null
        audioRecorder.cancel()
        _uiState.update {
            it.copy(recordingState = RecordingState.Idle, liveTranscript = "")
        }
    }

    private fun extractFields(transcript: String) {
        viewModelScope.launch {
            val persona = _uiState.value.persona ?: run {
                _uiState.update {
                    it.copy(
                        recordingState = RecordingState.Idle,
                        errorMessage = "No persona selected"
                    )
                }
                return@launch
            }
            val fields = _uiState.value.selectedFields
            val language = preferencesManager.appLanguage.first()

            when (val result = extractFieldsUseCase(transcript, persona, fields, language)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            recordingState = RecordingState.Idle,
                            extractionState = ExtractionState.Success(
                                transcript = transcript,
                                fields = result.data.fields,
                                additionalNotes = result.data.additionalNotes
                            )
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            recordingState = RecordingState.Idle,
                            errorMessage = result.exception.message ?: "Extraction failed"
                        )
                    }
                }
            }
        }
    }

    private fun saveVoiceNote(extraction: ExtractionState.Success) {
        viewModelScope.launch {
            val persona = _uiState.value.persona ?: return@launch
            val isOffline = _uiState.value.isOfflineExtraction
            val noteId = UUID.randomUUID().toString()
            val voiceNote = VoiceNote(
                id = noteId,
                transcript = extraction.transcript,
                extractedFields = extraction.fields,
                additionalNotes = extraction.additionalNotes,
                persona = persona,
                createdAt = System.currentTimeMillis(),
                extractionPending = isOffline
            )
            saveVoiceNoteUseCase(voiceNote)

            if (isOffline) {
                val selectedFieldIds = _uiState.value.selectedFields.map { it.id }.toSet()
                queueExtractionUseCase(noteId, selectedFieldIds)
            }

            _uiState.update {
                it.copy(
                    extractionState = ExtractionState.Idle,
                    isOfflineExtraction = false,
                    errorMessage = if (isOffline) "Voice note saved! Fields will be extracted when you're back online." else null
                )
            }
        }
    }

}
