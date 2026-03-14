package com.at.recallly.data.billing

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.at.recallly.data.local.datastore.dataStore
import com.at.recallly.domain.model.SubscriptionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PremiumPreferences(private val context: Context) {

    private companion object {
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val PREMIUM_PRODUCT_ID = stringPreferencesKey("premium_product_id")
        val PREMIUM_EXPIRY_MILLIS = longPreferencesKey("premium_expiry_millis")
        val PREMIUM_LAST_VERIFIED_MILLIS = longPreferencesKey("premium_last_verified_millis")
        val DEBUG_PREMIUM_OVERRIDE = booleanPreferencesKey("debug_premium_override")
    }

    val subscriptionStatus: Flow<SubscriptionStatus> = context.dataStore.data
        .map { prefs ->
            SubscriptionStatus(
                isPremium = prefs[IS_PREMIUM] ?: false,
                productId = prefs[PREMIUM_PRODUCT_ID],
                expiryTimeMillis = prefs[PREMIUM_EXPIRY_MILLIS],
                isGracePeriod = false
            )
        }

    val debugOverride: Flow<Boolean?> = context.dataStore.data
        .map { prefs ->
            if (prefs.contains(DEBUG_PREMIUM_OVERRIDE)) prefs[DEBUG_PREMIUM_OVERRIDE] else null
        }

    suspend fun savePremiumStatus(
        isPremium: Boolean,
        productId: String?,
        expiryTimeMillis: Long?
    ) {
        context.dataStore.edit { prefs ->
            prefs[IS_PREMIUM] = isPremium
            if (productId != null) {
                prefs[PREMIUM_PRODUCT_ID] = productId
            } else {
                prefs.remove(PREMIUM_PRODUCT_ID)
            }
            if (expiryTimeMillis != null) {
                prefs[PREMIUM_EXPIRY_MILLIS] = expiryTimeMillis
            } else {
                prefs.remove(PREMIUM_EXPIRY_MILLIS)
            }
            prefs[PREMIUM_LAST_VERIFIED_MILLIS] = System.currentTimeMillis()
        }
    }

    suspend fun setDebugOverride(isPremium: Boolean?) {
        context.dataStore.edit { prefs ->
            if (isPremium != null) {
                prefs[DEBUG_PREMIUM_OVERRIDE] = isPremium
            } else {
                prefs.remove(DEBUG_PREMIUM_OVERRIDE)
            }
        }
    }
}
