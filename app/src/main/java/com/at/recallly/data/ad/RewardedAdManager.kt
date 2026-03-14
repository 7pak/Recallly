package com.at.recallly.data.ad

import android.app.Activity
import android.content.Context
import com.at.recallly.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import timber.log.Timber

class RewardedAdManager {

    private var preRecordAd: RewardedAd? = null
    private var postSaveAd: RewardedAd? = null

    private val preRecordAdUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_REWARDED_AD_UNIT_ID
        else BuildConfig.ADMOB_REWARDED_PRE_RECORD_ID

    private val postSaveAdUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_REWARDED_AD_UNIT_ID
        else BuildConfig.ADMOB_REWARDED_POST_SAVE_ID

    val isPreRecordAdReady: Boolean get() = preRecordAd != null
    val isPostSaveAdReady: Boolean get() = postSaveAd != null

    fun preloadPreRecordAd(context: Context) {
        if (preRecordAd != null) return
        RewardedAd.load(
            context,
            preRecordAdUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    preRecordAd = ad
                    Timber.d("Pre-record rewarded ad loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    preRecordAd = null
                    Timber.w("Pre-record rewarded ad failed to load: ${error.message}")
                }
            }
        )
    }

    fun preloadPostSaveAd(context: Context) {
        if (postSaveAd != null) return
        RewardedAd.load(
            context,
            postSaveAdUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    postSaveAd = ad
                    Timber.d("Post-save rewarded ad loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    postSaveAd = null
                    Timber.w("Post-save rewarded ad failed to load: ${error.message}")
                }
            }
        )
    }

    fun showPreRecordAd(
        activity: Activity,
        onRewarded: () -> Unit,
        onFailed: () -> Unit
    ) {
        val ad = preRecordAd
        if (ad == null) {
            Timber.w("Pre-record ad not ready, allowing user to proceed")
            onFailed()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                preRecordAd = null
                preloadPreRecordAd(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                preRecordAd = null
                Timber.w("Pre-record ad failed to show: ${error.message}")
                onFailed()
                preloadPreRecordAd(activity)
            }
        }
        ad.show(activity) { onRewarded() }
    }

    fun showPostSaveAd(
        activity: Activity,
        onRewarded: () -> Unit,
        onFailed: () -> Unit
    ) {
        val ad = postSaveAd
        if (ad == null) {
            Timber.w("Post-save ad not ready, skipping")
            onFailed()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                postSaveAd = null
                preloadPostSaveAd(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                postSaveAd = null
                Timber.w("Post-save ad failed to show: ${error.message}")
                onFailed()
                preloadPostSaveAd(activity)
            }
        }
        ad.show(activity) { onRewarded() }
    }

    companion object {
        private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    }
}
