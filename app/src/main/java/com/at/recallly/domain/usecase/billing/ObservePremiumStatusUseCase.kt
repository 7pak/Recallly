package com.at.recallly.domain.usecase.billing

import com.at.recallly.domain.model.SubscriptionStatus
import com.at.recallly.domain.repository.BillingRepository
import kotlinx.coroutines.flow.Flow

class ObservePremiumStatusUseCase(private val billingRepository: BillingRepository) {
    operator fun invoke(): Flow<SubscriptionStatus> {
        return billingRepository.subscriptionStatus
    }
}
