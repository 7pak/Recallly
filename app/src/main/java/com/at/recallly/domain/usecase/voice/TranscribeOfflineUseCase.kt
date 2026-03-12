package com.at.recallly.domain.usecase.voice

import com.at.recallly.core.result.Result
import com.at.recallly.domain.repository.WhisperRepository

class TranscribeOfflineUseCase(
    private val whisperRepository: WhisperRepository
) {
    suspend operator fun invoke(audioSamples: FloatArray): Result<String> {
        return whisperRepository.transcribe(audioSamples)
    }
}
