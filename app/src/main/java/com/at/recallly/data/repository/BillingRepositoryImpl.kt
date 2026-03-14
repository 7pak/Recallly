package com.at.recallly.data.repository

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.at.recallly.BuildConfig
import com.at.recallly.data.billing.BillingClientWrapper
import com.at.recallly.data.billing.PremiumPreferences
import com.at.recallly.domain.model.SubscriptionStatus
import com.at.recallly.domain.repository.BillingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber

class BillingRepositoryImpl(
    private val billingClientWrapper: BillingClientWrapper,
    private val premiumPreferences: PremiumPreferences
) : BillingRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var cachedProductDetails: ProductDetails? = null

    override val subscriptionStatus: Flow<SubscriptionStatus> =
        if (BuildConfig.DEBUG) {
            combine(
                premiumPreferences.subscriptionStatus,
                premiumPreferences.debugOverride
            ) { status, override ->
                if (override != null) status.copy(isPremium = override) else status
            }
        } else {
            premiumPreferences.subscriptionStatus
        }

    init {
        // Handle purchase results from Google Play billing flow
        billingClientWrapper.onPurchaseResult = { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                scope.launch {
                    try {
                        refreshPurchases()
                    } catch (e: Exception) {
                        Timber.w(e, "Post-purchase refresh failed")
                    }
                }
            }
        }

        scope.launch {
            // In debug builds, default to non-premium for ad testing
            if (BuildConfig.DEBUG) {
                premiumPreferences.setDebugOverride(false)
            }

            try {
                refreshPurchases()
            } catch (e: Exception) {
                Timber.w(e, "Initial purchase refresh failed (offline or unavailable)")
            }
        }
    }

    override suspend fun refreshPurchases() {
        val purchases = billingClientWrapper.queryActivePurchases()

        val activePurchase = purchases.firstOrNull { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }

        if (activePurchase != null) {
            // Acknowledge if needed (Google refunds unacknowledged purchases after 3 days)
            if (!activePurchase.isAcknowledged) {
                billingClientWrapper.acknowledgePurchase(activePurchase.purchaseToken)
            }

            premiumPreferences.savePremiumStatus(
                isPremium = true,
                productId = activePurchase.products.firstOrNull(),
                expiryTimeMillis = null // Expiry not available from client-side query
            )
        } else {
            premiumPreferences.savePremiumStatus(
                isPremium = false,
                productId = null,
                expiryTimeMillis = null
            )
        }
    }

    override suspend fun getSubscriptionPrice(): String? {
        return try {
            val details = billingClientWrapper.querySubscriptionDetails(
                BuildConfig.BILLING_SUBSCRIPTION_ID
            )
            cachedProductDetails = details
            details?.subscriptionOfferDetails
                ?.firstOrNull()
                ?.pricingPhases
                ?.pricingPhaseList
                ?.firstOrNull()
                ?.formattedPrice
        } catch (e: Exception) {
            Timber.w(e, "Failed to query subscription details")
            null
        }
    }

    override suspend fun launchPurchaseFlow(activity: Any): Boolean {
        val act = activity as Activity
        val details = cachedProductDetails
            ?: billingClientWrapper.querySubscriptionDetails(BuildConfig.BILLING_SUBSCRIPTION_ID)
            ?: return false
        cachedProductDetails = details

        val offerToken = details.subscriptionOfferDetails
            ?.firstOrNull()
            ?.offerToken
            ?: return false

        val result = billingClientWrapper.launchBillingFlow(act, details, offerToken)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    suspend fun setDebugOverride(isPremium: Boolean?) {
        if (BuildConfig.DEBUG) {
            premiumPreferences.setDebugOverride(isPremium)
        }
    }
}
