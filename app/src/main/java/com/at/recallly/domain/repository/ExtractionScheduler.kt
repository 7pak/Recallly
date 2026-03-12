package com.at.recallly.domain.repository

interface ExtractionScheduler {
    fun enqueueExtraction(voiceNoteId: String, selectedFieldIds: Set<String>)
}
