package com.at.recallly.presentation.home

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

private const val SILENCE_TIMEOUT_SECONDS = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingBottomSheet(
    recordingState: RecordingState,
    onStopRecording: (transcript: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var accumulatedTranscript by remember { mutableStateOf("") }
    var partialText by remember { mutableStateOf("") }
    var silenceCountdown by remember { mutableStateOf<Int?>(null) }
    var silenceTimerJob by remember { mutableStateOf<Job?>(null) }
    var hasAutoStopped by remember { mutableStateOf(false) }
    var recognizerRef by remember { mutableStateOf<SpeechRecognizer?>(null) }

    fun buildFinalTranscript(): String {
        return if (partialText.isNotBlank()) {
            if (accumulatedTranscript.isBlank()) partialText
            else "$accumulatedTranscript $partialText"
        } else accumulatedTranscript
    }

    fun stopAndSubmit() {
        if (hasAutoStopped) return
        hasAutoStopped = true
        silenceTimerJob?.cancel()
        silenceTimerJob = null
        silenceCountdown = null
        recognizerRef?.stopListening()
        recognizerRef?.destroy()
        recognizerRef = null
        onStopRecording(buildFinalTranscript())
    }

    fun cancelRecording() {
        silenceTimerJob?.cancel()
        silenceTimerJob = null
        silenceCountdown = null
        hasAutoStopped = true
        recognizerRef?.stopListening()
        recognizerRef?.destroy()
        recognizerRef = null
        onDismiss()
    }

    fun startSilenceTimer() {
        if (silenceTimerJob?.isActive == true) return
        silenceTimerJob = scope.launch {
            for (i in SILENCE_TIMEOUT_SECONDS downTo 1) {
                silenceCountdown = i
                delay(1000L)
            }
            silenceCountdown = 0
            stopAndSubmit()
        }
    }

    fun cancelSilenceTimer() {
        silenceTimerJob?.cancel()
        silenceTimerJob = null
        silenceCountdown = null
    }

    ModalBottomSheet(
        onDismissRequest = { cancelRecording() },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (recordingState) {
                is RecordingState.Recording -> {
                    val context = LocalContext.current

                    // SpeechRecognizer lifecycle
                    DisposableEffect(Unit) {
                        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                            Timber.e("SpeechRecognizer not available on this device")
                            onDismiss()
                            return@DisposableEffect onDispose { }
                        }

                        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                        recognizerRef = speechRecognizer

                        fun createIntent(): Intent {
                            return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                                putExtra(
                                    RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                                    60000L
                                )
                                putExtra(
                                    RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                                    30000L
                                )
                                putExtra(
                                    RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                                    30000L
                                )
                            }
                        }

                        val listener = object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) {}

                            override fun onBeginningOfSpeech() {
                                cancelSilenceTimer()
                            }

                            override fun onRmsChanged(rmsdB: Float) {}
                            override fun onBufferReceived(buffer: ByteArray?) {}
                            override fun onEndOfSpeech() {}

                            override fun onError(error: Int) {
                                if (hasAutoStopped) return
                                when (error) {
                                    SpeechRecognizer.ERROR_NO_MATCH,
                                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                                        // No speech in this segment — restart
                                        try {
                                            speechRecognizer.startListening(createIntent())
                                        } catch (e: Exception) {
                                            Timber.e(e, "Failed to restart SpeechRecognizer")
                                        }
                                    }
                                    SpeechRecognizer.ERROR_SERVER,
                                    SpeechRecognizer.ERROR_NETWORK,
                                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                                        // Retry
                                        try {
                                            speechRecognizer.startListening(createIntent())
                                        } catch (e: Exception) {
                                            Timber.e(e, "Failed to restart SpeechRecognizer")
                                        }
                                    }
                                    else -> {
                                        Timber.e("SpeechRecognizer error: $error")
                                        try {
                                            speechRecognizer.startListening(createIntent())
                                        } catch (e: Exception) {
                                            Timber.e(e, "Failed to restart SpeechRecognizer")
                                        }
                                    }
                                }
                            }

                            override fun onResults(results: Bundle?) {
                                if (hasAutoStopped) return
                                val matches = results?.getStringArrayList(
                                    SpeechRecognizer.RESULTS_RECOGNITION
                                )
                                val text = matches?.firstOrNull()?.trim() ?: ""
                                if (text.isNotBlank()) {
                                    accumulatedTranscript = if (accumulatedTranscript.isBlank()) text
                                    else "$accumulatedTranscript $text"
                                    partialText = ""
                                    startSilenceTimer()
                                }
                                // Restart for next utterance
                                if (!hasAutoStopped) {
                                    try {
                                        speechRecognizer.startListening(createIntent())
                                    } catch (e: Exception) {
                                        Timber.e(e, "Failed to restart SpeechRecognizer")
                                    }
                                }
                            }

                            override fun onPartialResults(partialResults: Bundle?) {
                                if (hasAutoStopped) return
                                val matches = partialResults?.getStringArrayList(
                                    SpeechRecognizer.RESULTS_RECOGNITION
                                )
                                val text = matches?.firstOrNull()?.trim() ?: ""
                                if (text.isNotBlank() && text != partialText) {
                                    cancelSilenceTimer()
                                }
                                partialText = text
                            }

                            override fun onEvent(eventType: Int, params: Bundle?) {}
                        }

                        speechRecognizer.setRecognitionListener(listener)
                        speechRecognizer.startListening(createIntent())

                        onDispose {
                            silenceTimerJob?.cancel()
                            try {
                                speechRecognizer.stopListening()
                                speechRecognizer.destroy()
                            } catch (_: Exception) { }
                            recognizerRef = null
                        }
                    }

                    val isSilent = silenceCountdown != null

                    // Pulsing mic animation
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = if (isSilent) 1.01f else 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(if (isSilent) 1 else 800),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_scale"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = when {
                            silenceCountdown != null && silenceCountdown!! > 0 ->
                                "Stopping in ${silenceCountdown}s..."
                            silenceCountdown == 0 -> "Stopping..."
                            else -> "Listening..."
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSilent)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Surface(
                        modifier = Modifier
                            .size(96.dp)
                            .scale(scale),
                        shape = CircleShape,
                        color = if (isSilent)
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        else
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Surface(
                                modifier = Modifier.size(72.dp),
                                shape = CircleShape,
                                color = if (isSilent)
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                else
                                    MaterialTheme.colorScheme.secondary
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Mic,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = if (isSilent)
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        else
                                            MaterialTheme.colorScheme.onSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Live transcript
                    val displayText = if (accumulatedTranscript.isBlank() && partialText.isBlank()) {
                        "Start speaking..."
                    } else {
                        val full = accumulatedTranscript
                        if (partialText.isNotBlank()) {
                            if (full.isBlank()) partialText else "$full $partialText"
                        } else full
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (accumulatedTranscript.isBlank() && partialText.isBlank())
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { cancelRecording() },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Button(
                            onClick = { stopAndSubmit() },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Stop",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                RecordingState.Processing -> {
                    Spacer(modifier = Modifier.height(32.dp))

                    CircularProgressIndicator(
                        modifier = Modifier.size(56.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        strokeWidth = 4.dp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Extracting data...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "AI is analyzing your voice note",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }

                RecordingState.Idle,
                is RecordingState.RecordingWhisper,
                RecordingState.Transcribing -> { /* Handled by other sheets */ }
            }
        }
    }
}
