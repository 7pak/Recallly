package com.at.recallly.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

class BillingClientWrapper(context: Context) : PurchasesUpdatedListener {

    var onPurchaseResult: ((BillingResult, List<Purchase>?) -> Unit)? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build()
        )
        .build()

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        Timber.d("Purchases updated: responseCode=${result.responseCode}")
        onPurchaseResult?.invoke(result, purchases)
    }

    suspend fun queryActivePurchases(): List<Purchase> {
        ensureConnected()
        return suspendCancellableCoroutine { cont ->
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
            billingClient.queryPurchasesAsync(params) { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    cont.resume(purchases)
                } else {
                    Timber.w("queryPurchasesAsync failed: ${result.responseCode} - ${result.debugMessage}")
                    cont.resume(emptyList())
                }
            }
        }
    }

    suspend fun acknowledgePurchase(purchaseToken: String) {
        ensureConnected()
        suspendCancellableCoroutine { cont ->
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { result ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Timber.d("Purchase acknowledged successfully")
                } else {
                    Timber.w("Acknowledge failed: ${result.responseCode} - ${result.debugMessage}")
                }
                cont.resume(Unit)
            }
        }
    }

    suspend fun querySubscriptionDetails(productId: String): ProductDetails? {
        ensureConnected()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            ).build()
        val result = billingClient.queryProductDetails(params)
        return result.productDetailsList?.firstOrNull()
    }

    fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String
    ): BillingResult {
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            ).build()
        return billingClient.launchBillingFlow(activity, params)
    }

    private suspend fun ensureConnected() {
        if (billingClient.isReady) return

        var retryCount = 0
        val maxRetries = 3

        while (retryCount < maxRetries) {
            val connected = tryConnect()
            if (connected) return
            retryCount++
            if (retryCount < maxRetries) {
                kotlinx.coroutines.delay(1000L * retryCount)
            }
        }
        Timber.w("BillingClient failed to connect after $maxRetries attempts")
    }

    private suspend fun tryConnect(): Boolean = suspendCancellableCoroutine { cont ->
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
            }

            override fun onBillingServiceDisconnected() {
                Timber.d("BillingClient disconnected")
            }
        })
    }
}
