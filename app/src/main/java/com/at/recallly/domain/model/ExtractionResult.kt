package com.at.recallly.domain.model

data class ExtractionResult(
    val fields: Map<String, String>,
    val additionalNotes: String
)
