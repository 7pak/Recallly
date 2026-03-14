package com.at.recallly.domain.model

data class SubscriptionStatus(
    val isPremium: Boolean = false,
    val productId: String? = null,
    val expiryTimeMillis: Long? = null,
    val isGracePeriod: Boolean = false
)
