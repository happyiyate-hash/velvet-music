package com.example.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

/**
 * AdMobManager provides unified Google AdMob integration:
 * - App Open Ad on launch and foreground return
 * - Rewarded Ads triggered when music recognition completes or from the Main Page reward actions
 */
object AdMobManager : Application.ActivityLifecycleCallbacks {
    private const val TAG = "AdMobManager"

    private const val appOpenAdUnitId: String = "ca-app-pub-5201231396499174/5630052387"
    private const val rewardedAdUnitId: String = "ca-app-pub-5201231396499174/9210953174"

    var currentActivity: Activity? = null
        private set

    private var isInitialized = false
    private var startedActivityCount = 0

    // App Open Ad State
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAppOpenAd = false
    private var isShowingAppOpenAd = false
    private var appOpenLoadTime: Long = 0

    // Rewarded Ad State
    private var rewardedAd: RewardedAd? = null
    private var isLoadingRewardedAd = false
    private var isShowingRewardedAd = false

    // Observables for Compose UI
    private val _rewardsCount = MutableStateFlow(0)
    val rewardsCount: StateFlow<Int> = _rewardsCount.asStateFlow()

    private val _isRewardedAdLoaded = MutableStateFlow(false)
    val isRewardedAdLoaded: StateFlow<Boolean> = _isRewardedAdLoaded.asStateFlow()

    fun initialize(application: Application) {
        if (isInitialized) return
        isInitialized = true

        application.registerActivityLifecycleCallbacks(this)

        MobileAds.initialize(application) { initStatus ->
            Log.d(TAG, "Google AdMob MobileAds initialized: $initStatus")
            loadAppOpenAd(application)
            loadRewardedAd(application)
        }
    }

    // --- App Open Ad Implementation ---

    fun loadAppOpenAd(context: Context) {
        if (isLoadingAppOpenAd || isAppOpenAdAvailable()) return
        isLoadingAppOpenAd = true

        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            appOpenAdUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "App Open Ad loaded successfully.")
                    appOpenAd = ad
                    isLoadingAppOpenAd = false
                    appOpenLoadTime = Date().time
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "App Open Ad failed to load: ${loadAdError.message}")
                    isLoadingAppOpenAd = false
                    appOpenAd = null
                }
            }
        )
    }

    fun isAppOpenAdAvailable(): Boolean {
        val ad = appOpenAd ?: return false
        val dateDifference = Date().time - appOpenLoadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < (numMilliSecondsPerHour * 4)
    }

    fun showAppOpenAdIfAvailable(activity: Activity, onAdDismissed: () -> Unit = {}) {
        if (isShowingAppOpenAd || isShowingRewardedAd) {
            onAdDismissed()
            return
        }

        if (!isAppOpenAdAvailable()) {
            loadAppOpenAd(activity.applicationContext)
            onAdDismissed()
            return
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "App Open Ad dismissed.")
                appOpenAd = null
                isShowingAppOpenAd = false
                loadAppOpenAd(activity.applicationContext)
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "App Open Ad failed to show: ${adError.message}")
                appOpenAd = null
                isShowingAppOpenAd = false
                loadAppOpenAd(activity.applicationContext)
                onAdDismissed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "App Open Ad showed.")
                isShowingAppOpenAd = true
            }
        }

        appOpenAd?.show(activity)
    }

    // --- Rewarded Ad Implementation ---

    fun loadRewardedAd(context: Context) {
        if (isLoadingRewardedAd || rewardedAd != null) return
        isLoadingRewardedAd = true

        val request = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            rewardedAdUnitId,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded Ad loaded successfully.")
                    rewardedAd = ad
                    isLoadingRewardedAd = false
                    _isRewardedAdLoaded.value = true
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Rewarded Ad failed to load: ${loadAdError.message}")
                    rewardedAd = null
                    isLoadingRewardedAd = false
                    _isRewardedAdLoaded.value = false
                }
            }
        )
    }

    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: ((RewardItem) -> Unit)? = null,
        onAdDismissed: () -> Unit = {}
    ) {
        val ad = rewardedAd
        if (ad == null) {
            Log.d(TAG, "Rewarded Ad not ready, loading new one...")
            loadRewardedAd(activity.applicationContext)
            onAdDismissed()
            return
        }

        if (isShowingRewardedAd || isShowingAppOpenAd) {
            onAdDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded Ad dismissed.")
                rewardedAd = null
                isShowingRewardedAd = false
                _isRewardedAdLoaded.value = false
                loadRewardedAd(activity.applicationContext)
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Rewarded Ad failed to show: ${adError.message}")
                rewardedAd = null
                isShowingRewardedAd = false
                _isRewardedAdLoaded.value = false
                loadRewardedAd(activity.applicationContext)
                onAdDismissed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded Ad is showing.")
                isShowingRewardedAd = true
            }
        }

        ad.show(activity) { rewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            _rewardsCount.value += 1
            onRewardEarned?.invoke(rewardItem)
        }
    }

    // --- Activity Lifecycle Listeners ---

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
        if (startedActivityCount == 0) {
            // App transitioned to foreground
            showAppOpenAdIfAvailable(activity)
        }
        startedActivityCount++
    }
    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {
        startedActivityCount--
        if (startedActivityCount < 0) startedActivityCount = 0
    }
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }
}
