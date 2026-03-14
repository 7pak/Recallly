package com.at.recallly.data.local.file

import kotlinx.serialization.Serializable

@Serializable
data class CustomFieldDto(
    val id: String,
    val displayName: String,
    val description: String,
    val persona: String,
    val fieldType: String = "TEXT"
)

@Serializable
data class CustomFieldListDto(
    val fields: List<CustomFieldDto> = emptyList()
)
