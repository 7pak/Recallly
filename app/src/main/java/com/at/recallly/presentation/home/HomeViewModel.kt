package com.at.recallly.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Intent
import android.provider.CalendarContract
import com.at.recallly.core.result.Result
import com.at.recallly.core.util.ConnectivityChecker
import com.at.recallly.core.util.LanguageManager
import com.at.recallly.data.local.datastore.PreferencesManager
import com.at.recallly.data.whisper.AudioRecorder
import com.at.recallly.domain.model.Persona
import com.at.recallly.domain.model.PersonaFields
import com.at.recallly.domain.model.User
import com.at.recallly.domain.model.VoiceNote
import com.at.recallly.domain.model.WorkSchedule
import com.at.recallly.domain.repository.AdRepository
import com.at.recallly.domain.repository.CustomFieldRepository
import com.at.recallly.domain.repository.OnboardingRepository
import com.at.recallly.domain.repository.ReminderScheduler
import com.at.recallly.domain.repository.WhisperRepository
import com.at.recallly.domain.usecase.auth.GetCurrentUserUseCase
import com.at.recallly.domain.usecase.voice.ExtractFieldsUseCase
import com.at.recallly.domain.usecase.voice.GetVoiceNotesUseCase
import com.at.recallly.domain.usecase.billing.ObservePremiumStatusUseCase
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
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
    private val voiceNoteRepository: VoiceNoteRepository,
    private val observePremiumStatusUseCase: ObservePremiumStatusUseCase,
    private val customFieldRepository: CustomFieldRepository,
    private val reminderScheduler: ReminderScheduler,
    private val adRepository: AdRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var whisperTimerJob: Job? = null
    private var whisperAmplitudeJob: Job? = null
    private var whisperSilenceJob: Job? = null
    private var hasDetectedSpeech = false

    companion object {
        private const val SILENCE_TIMEOUT_SECONDS = 3
        private const val SILENCE_AMPLITUDE_THRESHOLD = 0.01f
    }

    init {
        // Observe main data flows
        viewModelScope.launch {
            // Get custom fields reactively based on persona
            val customFieldsFlow = onboardingRepository.selectedPersona.flatMapLatest { persona ->
                persona?.let { customFieldRepository.getCustomFieldsForPersona(it) } ?: flowOf(emptyList())
            }

            combine(
                getCurrentUserUseCase().filterNotNull(),
                onboardingRepository.selectedPersona,
                onboardingRepository.selectedFieldIds,
                onboardingRepository.workSchedule,
                getVoiceNotesUseCase()
            ) { user, persona, fieldIds, schedule, voiceNotes ->
                Holder(user, persona, fieldIds, schedule, voiceNotes)
            }.combine(customFieldsFlow) { holder, customFields ->
                val todayStart = LocalDate.now()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                val noteCountToday = holder.voiceNotes.count { it.createdAt >= todayStart }

                val selectedFields = holder.persona?.let { p ->
                    val builtInFields = PersonaFields.getFieldsForPersona(p)
                    val allFields = builtInFields + customFields
                    if (holder.fieldIds.isEmpty()) allFields
                    else allFields.filter { it.id in holder.fieldIds }
                } ?: emptyList()

                HomeUiState(
                    displayName = holder.user.displayName ?: holder.user.email.substringBefore("@"),
                    persona = holder.persona,
                    workSchedule = holder.schedule,
                    noteCountToday = noteCountToday,
                    voiceNotes = holder.voiceNotes,
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
                    editingFields = _uiState.value.editingFields,
                    isPremium = _uiState.value.isPremium,
                    calendarDialog = _uiState.value.calendarDialog,
                    calendarSuccess = _uiState.value.calendarSuccess,
                    calendarIntent = _uiState.value.calendarIntent,
                    freeNotesUsed = _uiState.value.freeNotesUsed,
                    adRequired = _uiState.value.adRequired,
                    showAdGateDialog = _uiState.value.showAdGateDialog,
                    showPreRecordAd = _uiState.value.showPreRecordAd,
                    showPostSaveAd = _uiState.value.showPostSaveAd,
                    showReadyToRecordDialog = _uiState.value.showReadyToRecordDialog,
                    canUseCalendarSync = _uiState.value.canUseCalendarSync,
                    canUseNotification = _uiState.value.canUseNotification
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

        // Observe premium status, free notes, calendar syncs, and notifications for gating
        viewModelScope.launch {
            combine(
                observePremiumStatusUseCase(),
                adRepository.freeNotesUsed,
                adRepository.freeCalendarSyncsUsed,
                adRepository.freeNotificationsUsed
            ) { status, notesUsed, calendarUsed, notificationsUsed ->
                val isPremium = status.isPremium
                val adRequired = adRepository.shouldShowAds(isPremium, notesUsed)
                val canCalendar = adRepository.canUseCalendarSync(isPremium, calendarUsed)
                val canNotification = adRepository.canUseNotification(isPremium, notificationsUsed)
                AdGatingState(isPremium, notesUsed, adRequired, canCalendar, canNotification)
            }.collect { state ->
                _uiState.update {
                    it.copy(
                        isPremium = state.isPremium,
                        freeNotesUsed = state.freeNotesUsed,
                        adRequired = state.adRequired,
                        canUseCalendarSync = state.canUseCalendarSync,
                        canUseNotification = state.canUseNotification
                    )
                }
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
                if (_uiState.value.adRequired) {
                    _uiState.update { it.copy(showAdGateDialog = true) }
                } else {
                    proceedWithRecording()
                }
            }

            is HomeUiEvent.AdGateAccepted -> {
                _uiState.update { it.copy(showAdGateDialog = false, showPreRecordAd = true) }
            }

            is HomeUiEvent.AdGateDismissed -> {
                _uiState.update { it.copy(showAdGateDialog = false) }
            }

            is HomeUiEvent.PreRecordAdCompleted -> {
                _uiState.update { it.copy(showPreRecordAd = false, showReadyToRecordDialog = true) }
            }

            is HomeUiEvent.PreRecordAdFailed -> {
                _uiState.update { it.copy(showPreRecordAd = false, showReadyToRecordDialog = true) }
            }

            is HomeUiEvent.ReadyToRecordConfirmed -> {
                _uiState.update { it.copy(showReadyToRecordDialog = false) }
                proceedWithRecording()
            }

            is HomeUiEvent.ReadyToRecordDismissed -> {
                _uiState.update { it.copy(showReadyToRecordDialog = false) }
            }

            is HomeUiEvent.PostSaveAdCompleted -> {
                _uiState.update { it.copy(showPostSaveAd = false) }
            }

            is HomeUiEvent.PostSaveAdFailed -> {
                _uiState.update { it.copy(showPostSaveAd = false) }
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

            is HomeUiEvent.SaveOfflineTranscript -> {
                val current = _uiState.value.extractionState
                if (current is ExtractionState.Success) {
                    saveVoiceNote(current.copy(transcript = event.editedTranscript))
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

            is HomeUiEvent.AddToCalendarTapped -> {
                handleAddToCalendarTapped(event.voiceNoteId, event.fieldId)
            }

            is HomeUiEvent.ConfirmAddToCalendar -> {
                handleConfirmAddToCalendar(event.dateTime)
            }

            is HomeUiEvent.DismissCalendarDialog -> {
                _uiState.update { it.copy(calendarDialog = null) }
            }

            is HomeUiEvent.DismissCalendarSuccess -> {
                _uiState.update { it.copy(calendarSuccess = false) }
            }

            is HomeUiEvent.CalendarIntentLaunched -> {
                _uiState.update { it.copy(calendarIntent = null) }
            }
        }
    }

    private fun proceedWithRecording() {
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

    private fun handleAddToCalendarTapped(voiceNoteId: String, fieldId: String) {
        val state = _uiState.value

        if (!state.canUseCalendarSync) {
            _uiState.update { it.copy(errorMessage = "You've used your free calendar sync. Upgrade to Premium for unlimited syncs.") }
            return
        }

        val voiceNote = state.voiceNotes.find { it.id == voiceNoteId }
            ?: state.selectedVoiceNote
            ?: return

        // Try to find a suggested date from extracted date fields
        val isoDateStr = if (fieldId != "_calendar") {
            voiceNote.extractedFields["${fieldId}_iso"]
        } else {
            // Look for any _iso key in the note
            voiceNote.extractedFields.entries
                .firstOrNull { it.key.endsWith("_iso") }
                ?.value
        }
        val suggestedDate = isoDateStr?.let {
            try { LocalDate.parse(it) } catch (_: Exception) { null }
        }

        // Build event title from contextual info
        val contextInfo = voiceNote.extractedFields.entries
            .firstOrNull { it.key.contains("name") || it.key.contains("site") || it.key.contains("claimant") }
            ?.value?.takeIf { it.isNotBlank() }

        val dateField = state.selectedFields.find { it.id == fieldId }
        val title = when {
            contextInfo != null && dateField != null -> "${dateField.displayName} — $contextInfo"
            contextInfo != null -> contextInfo
            dateField != null -> dateField.displayName
            else -> voiceNote.transcript.take(60).trim()
        }

        _uiState.update {
            it.copy(
                calendarDialog = CalendarDialogState(
                    voiceNoteId = voiceNoteId,
                    fieldId = fieldId,
                    fieldDisplayName = dateField?.displayName ?: "Calendar Event",
                    suggestedDate = suggestedDate,
                    eventTitle = title,
                    eventDescription = voiceNote.transcript.take(500)
                )
            )
        }
    }

    private fun handleConfirmAddToCalendar(dateTime: java.time.LocalDateTime) {
        val dialogState = _uiState.value.calendarDialog ?: return

        val localZone = ZoneId.systemDefault()
        val startMillis = dateTime
            .atZone(localZone)
            .toInstant()
            .toEpochMilli()
        val endMillis = dateTime
            .plusHours(1)
            .atZone(localZone)
            .toInstant()
            .toEpochMilli()

        // Create an Intent that opens Google Calendar with pre-filled event data
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, dialogState.eventTitle)
            putExtra(CalendarContract.Events.DESCRIPTION, dialogState.eventDescription)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            // Target Google Calendar specifically
            setPackage("com.google.android.calendar")
        }

        // Persist synced state optimistically + increment calendar sync counter
        viewModelScope.launch {
            adRepository.incrementFreeCalendarSyncsUsed()
            val voiceNote = _uiState.value.voiceNotes.find { it.id == dialogState.voiceNoteId }
                ?: _uiState.value.selectedVoiceNote
            if (voiceNote != null) {
                val updatedFields = voiceNote.extractedFields.toMutableMap()
                updatedFields["${dialogState.fieldId}_synced"] = "true"
                val updatedNote = voiceNote.copy(extractedFields = updatedFields)
                voiceNoteRepository.updateVoiceNote(updatedNote)
                if (_uiState.value.selectedVoiceNote?.id == voiceNote.id) {
                    _uiState.update { it.copy(selectedVoiceNote = updatedNote) }
                }
            }
        }

        // Schedule local notification reminder if enabled and allowed
        viewModelScope.launch {
            val remindersEnabled = preferencesManager.reminderNotificationsEnabled.first()
            if (remindersEnabled && _uiState.value.canUseNotification) {
                adRepository.incrementFreeNotificationsUsed()
                val reminderId = "${dialogState.voiceNoteId}_${dialogState.fieldId}".hashCode()
                reminderScheduler.schedule(
                    title = dialogState.eventTitle,
                    description = dialogState.eventDescription.take(200),
                    triggerAtMillis = startMillis,
                    reminderId = reminderId
                )
            }
        }

        _uiState.update {
            it.copy(
                calendarDialog = null,
                calendarIntent = intent,
                calendarSuccess = true
            )
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
            val adRequired = _uiState.value.adRequired
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
            adRepository.incrementFreeNotesUsed()

            if (isOffline) {
                val selectedFieldIds = _uiState.value.selectedFields.map { it.id }.toSet()
                queueExtractionUseCase(noteId, selectedFieldIds)
            }

            _uiState.update {
                it.copy(
                    extractionState = ExtractionState.Idle,
                    isOfflineExtraction = false,
                    showPostSaveAd = adRequired,
                    errorMessage = if (isOffline) "Voice note saved! Fields will be extracted when you're back online." else null
                )
            }
        }
    }

    private data class Holder(
        val user: User,
        val persona: Persona?,
        val fieldIds: Set<String>,
        val schedule: WorkSchedule,
        val voiceNotes: List<VoiceNote>
    )

    private data class AdGatingState(
        val isPremium: Boolean,
        val freeNotesUsed: Int,
        val adRequired: Boolean,
        val canUseCalendarSync: Boolean,
        val canUseNotification: Boolean
    )

}
