package com.at.recallly.domain.repository

import kotlinx.coroutines.flow.Flow

interface AdRepository {
    val freeNotesUsed: Flow<Int>
    val freeCalendarSyncsUsed: Flow<Int>
    val freeNotificationsUsed: Flow<Int>

    suspend fun incrementFreeNotesUsed()
    suspend fun incrementFreeCalendarSyncsUsed()
    suspend fun incrementFreeNotificationsUsed()

    fun shouldShowAds(isPremium: Boolean, freeNotesUsed: Int): Boolean
    fun canUseCalendarSync(isPremium: Boolean, used: Int): Boolean
    fun canUseNotification(isPremium: Boolean, used: Int): Boolean

    companion object {
        const val FREE_NOTES_LIMIT = 7
        const val FREE_CALENDAR_SYNC_LIMIT = 1
        const val FREE_NOTIFICATION_LIMIT = 1
    }
}
