package com.at.recallly.data.whisper

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.sqrt

class AudioRecorder {

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val samples = mutableListOf<Float>()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    @Volatile
    private var isRecording = false

    @SuppressLint("MissingPermission")
    fun start() {
        if (isRecording) return

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
        )
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Timber.e("Invalid buffer size: $bufferSize")
            return
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize * 2
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Timber.e("AudioRecord failed to initialize")
            audioRecord?.release()
            audioRecord = null
            return
        }

        samples.clear()
        isRecording = true
        audioRecord?.startRecording()

        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = FloatArray(bufferSize / 4) // Float = 4 bytes
            while (isActive && isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING) ?: -1
                if (read > 0) {
                    synchronized(samples) {
                        for (i in 0 until read) {
                            samples.add(buffer[i])
                        }
                    }
                    // Calculate RMS amplitude for visual feedback
                    var sum = 0f
                    for (i in 0 until read) {
                        sum += buffer[i] * buffer[i]
                    }
                    _amplitude.value = sqrt(sum / read)
                }
            }
        }
    }

    fun stop(): FloatArray {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        val result: FloatArray
        synchronized(samples) {
            result = samples.toFloatArray()
            samples.clear()
        }
        _amplitude.value = 0f
        Timber.d("Recorded ${result.size} samples (${result.size / SAMPLE_RATE}s)")
        return result
    }

    fun cancel() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        synchronized(samples) { samples.clear() }
        _amplitude.value = 0f
    }

    companion object {
        const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_FLOAT
    }
}
