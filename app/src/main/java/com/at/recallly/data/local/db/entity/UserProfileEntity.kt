package com.at.recallly.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val uid: String,
    val persona: String?,
    val selectedFieldIds: String?,
    val workDays: String?,
    val startTime: String?,
    val endTime: String?,
    val onboardingStep: Int = 0
)
