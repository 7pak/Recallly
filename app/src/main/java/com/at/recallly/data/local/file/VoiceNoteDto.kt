package com.at.recallly.data.local.file

import kotlinx.serialization.Serializable

@Serializable
data class VoiceNoteDto(
    val id: String,
    val transcript: String,
    val extractedFields: Map<String, String>,
    val additionalNotes: String,
    val persona: String,
    val createdAt: Long,
    val extractionPending: Boolean = false
)

@Serializable
data class VoiceNoteListDto(
    val notes: List<VoiceNoteDto> = emptyList()
)
