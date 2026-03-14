package com.at.recallly.data.repository

import com.at.recallly.data.local.datastore.PreferencesManager
import com.at.recallly.domain.repository.AdRepository
import com.at.recallly.domain.repository.AdRepository.Companion.FREE_CALENDAR_SYNC_LIMIT
import com.at.recallly.domain.repository.AdRepository.Companion.FREE_NOTES_LIMIT
import com.at.recallly.domain.repository.AdRepository.Companion.FREE_NOTIFICATION_LIMIT
import kotlinx.coroutines.flow.Flow

class AdRepositoryImpl(
    private val preferencesManager: PreferencesManager
) : AdRepository {

    override val freeNotesUsed: Flow<Int> = preferencesManager.freeNotesUsed
    override val freeCalendarSyncsUsed: Flow<Int> = preferencesManager.freeCalendarSyncsUsed
    override val freeNotificationsUsed: Flow<Int> = preferencesManager.freeNotificationsUsed

    override suspend fun incrementFreeNotesUsed() {
        preferencesManager.incrementFreeNotesUsed()
    }

    override suspend fun incrementFreeCalendarSyncsUsed() {
        preferencesManager.incrementFreeCalendarSyncsUsed()
    }

    override suspend fun incrementFreeNotificationsUsed() {
        preferencesManager.incrementFreeNotificationsUsed()
    }

    override fun shouldShowAds(isPremium: Boolean, freeNotesUsed: Int): Boolean {
        return !isPremium && freeNotesUsed >= FREE_NOTES_LIMIT
    }

    override fun canUseCalendarSync(isPremium: Boolean, used: Int): Boolean {
        return isPremium || used < FREE_CALENDAR_SYNC_LIMIT
    }

    override fun canUseNotification(isPremium: Boolean, used: Int): Boolean {
        return isPremium || used < FREE_NOTIFICATION_LIMIT
    }
}
