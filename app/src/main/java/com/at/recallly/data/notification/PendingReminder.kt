package com.at.recallly.data.notification

import kotlinx.serialization.Serializable

@Serializable
data class PendingReminder(
    val id: Int,
    val title: String,
    val description: String,
    val triggerAtMillis: Long
)
