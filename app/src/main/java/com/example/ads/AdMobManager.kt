package com.example.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AdMobManager - Google Ads functionality is disabled per user request.
 * The play-services-ads library dependency is commented out in build.gradle.kts.
 * The app does not make any ad network calls or trigger ads anywhere.
 *
 * All user credentials and original implementation logic are preserved below for future reference.
 */
data class AdErrorInfo(
    val adType: String = "",
    val errorCode: Int = 0,
    val errorName: String = "",
    val errorMessage: String = "",
    val adUnitId: String = "",
    val timestamp: String = "",
    val explanation: String = "",
    val responseInfo: String = ""
)

object AdMobManager {
    // Real production IDs from AdMob account (Preserved for future re-enabling)
    const val REAL_APP_ID = "ca-app-pub-5201231396499174~5411308963"
    const val REAL_APP_OPEN_AD_UNIT_ID = "ca-app-pub-5201231396499174/5630052387"
    const val REAL_REWARDED_AD_UNIT_ID = "ca-app-pub-5201231396499174/9210953174"

    // Google Official Sample Test IDs
    const val TEST_APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private val _useTestAds = MutableStateFlow(false)
    val useTestAds: StateFlow<Boolean> = _useTestAds.asStateFlow()

    fun setUseTestAds(enableTestAds: Boolean, context: Context) {
        _useTestAds.value = enableTestAds
    }

    val activeAppOpenAdUnitId: String = REAL_APP_OPEN_AD_UNIT_ID
    val activeRewardedAdUnitId: String = REAL_REWARDED_AD_UNIT_ID

    var currentActivity: Activity? = null

    private val _rewardsCount = MutableStateFlow(0)
    val rewardsCount: StateFlow<Int> = _rewardsCount.asStateFlow()

    private val _isRewardedAdLoaded = MutableStateFlow(false)
    val isRewardedAdLoaded: StateFlow<Boolean> = _isRewardedAdLoaded.asStateFlow()

    private val _isAppOpenAdLoaded = MutableStateFlow(false)
    val isAppOpenAdLoaded: StateFlow<Boolean> = _isAppOpenAdLoaded.asStateFlow()

    private val _lastError = MutableStateFlow<AdErrorInfo?>(null)
    val lastError: StateFlow<AdErrorInfo?> = _lastError.asStateFlow()

    private val _recentEvents = MutableStateFlow<List<String>>(emptyList())
    val recentEvents: StateFlow<List<String>> = _recentEvents.asStateFlow()

    // No-op stub methods - all ad network activity is disabled
    fun initialize(application: Application) {}
    fun loadAppOpenAd(context: Context, onComplete: ((Boolean) -> Unit)? = null) { onComplete?.invoke(false) }
    fun isAppOpenAdAvailable(): Boolean = false
    fun showAppOpenAdIfAvailable(activity: Activity, onAdDismissed: () -> Unit = {}) { onAdDismissed() }
    fun loadRewardedAd(context: Context, onComplete: ((Boolean) -> Unit)? = null) { onComplete?.invoke(false) }
    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: ((Any) -> Unit)? = null,
        onAdFailedToShow: ((AdErrorInfo) -> Unit)? = null,
        onAdDismissed: () -> Unit = {}
    ) {
        onAdDismissed()
    }
}

/*
========================================================================================
FULL ORIGINAL ADMOB IMPLEMENTATION (PRESERVED FOR FUTURE RE-ENABLING):
========================================================================================

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

To restore:
1. Uncomment `implementation(libs.play.services.ads)` in app/build.gradle.kts
2. Uncomment the meta-data in app/src/main/AndroidManifest.xml
3. Restore this implementation logic in AdMobManager
========================================================================================
*/
