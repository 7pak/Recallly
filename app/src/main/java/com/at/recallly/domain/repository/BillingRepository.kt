package com.at.recallly.domain.repository

import com.at.recallly.domain.model.SubscriptionStatus
import kotlinx.coroutines.flow.Flow

interface BillingRepository {
    val subscriptionStatus: Flow<SubscriptionStatus>
    suspend fun refreshPurchases()
    suspend fun getSubscriptionPrice(): String?
    suspend fun launchPurchaseFlow(activity: Any): Boolean
}
