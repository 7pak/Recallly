package com.at.recallly.domain.model

data class BackupMetadata(
    val timestamp: Long,
    val appVersionName: String,
    val deviceName: String,
    val voiceNoteCount: Int,
    val customFieldCount: Int
)
