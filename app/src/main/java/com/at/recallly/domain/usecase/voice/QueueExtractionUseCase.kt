package com.at.recallly.domain.usecase.voice

import com.at.recallly.domain.repository.ExtractionScheduler

class QueueExtractionUseCase(
    private val scheduler: ExtractionScheduler
) {
    operator fun invoke(voiceNoteId: String, selectedFieldIds: Set<String>) {
        scheduler.enqueueExtraction(voiceNoteId, selectedFieldIds)
    }
}
