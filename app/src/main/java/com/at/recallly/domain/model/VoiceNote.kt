package com.at.recallly.domain.model

data class VoiceNote(
    val id: String,
    val transcript: String,
    val extractedFields: Map<String, String>,
    val additionalNotes: String,
    val persona: Persona,
    val createdAt: Long,
    val extractionPending: Boolean = false
)
