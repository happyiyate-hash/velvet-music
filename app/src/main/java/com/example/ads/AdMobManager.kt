package com.example.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AdErrorInfo(
    val adType: String,
    val errorCode: Int,
    val errorName: String,
    val errorMessage: String,
    val adUnitId: String,
    val timestamp: String,
    val explanation: String,
    val responseInfo: String
)

object AdMobManager : Application.ActivityLifecycleCallbacks {
    private const val TAG = "AdMobManager"

    // Real production IDs from AdMob account
    const val REAL_APP_ID = "ca-app-pub-5201231396499174~5411308963"
    const val REAL_APP_OPEN_AD_UNIT_ID = "ca-app-pub-5201231396499174/5630052387"
    const val REAL_REWARDED_AD_UNIT_ID = "ca-app-pub-5201231396499174/9210953174"

    // Google Official Sample Test IDs (Always fill 100% of the time)
    const val TEST_APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    // Switch between real and test ads for live verification
    private val _useTestAds = MutableStateFlow(false)
    val useTestAds: StateFlow<Boolean> = _useTestAds.asStateFlow()

    fun setUseTestAds(enableTestAds: Boolean, context: Context) {
        _useTestAds.value = enableTestAds
        logEvent("Switched ad mode to: ${if (enableTestAds) "Google Test Ads (100% Fill)" else "Real AdMob Production IDs"}")
        // Clear cached ads and reload with chosen ad unit IDs
        appOpenAd = null
        rewardedAd = null
        _isRewardedAdLoaded.value = false
        _isAppOpenAdLoaded.value = false
        loadAppOpenAd(context)
        loadRewardedAd(context)
    }

    val activeAppOpenAdUnitId: String
        get() = if (_useTestAds.value) TEST_APP_OPEN_AD_UNIT_ID else REAL_APP_OPEN_AD_UNIT_ID

    val activeRewardedAdUnitId: String
        get() = if (_useTestAds.value) TEST_REWARDED_AD_UNIT_ID else REAL_REWARDED_AD_UNIT_ID

    var currentActivity: Activity? = null
        private set

    private var isInitialized = false
    private var startedActivityCount = 0
    private var showAppOpenWhenReady = false

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

    private val _isAppOpenAdLoaded = MutableStateFlow(false)
    val isAppOpenAdLoaded: StateFlow<Boolean> = _isAppOpenAdLoaded.asStateFlow()

    private val _lastError = MutableStateFlow<AdErrorInfo?>(null)
    val lastError: StateFlow<AdErrorInfo?> = _lastError.asStateFlow()

    private val _recentEvents = MutableStateFlow<List<String>>(emptyList())
    val recentEvents: StateFlow<List<String>> = _recentEvents.asStateFlow()

    fun logEvent(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = "[$time] $msg"
        Log.d(TAG, entry)
        val list = _recentEvents.value.toMutableList()
        list.add(0, entry)
        if (list.size > 50) list.removeAt(list.size - 1)
        _recentEvents.value = list
    }

    private fun translateErrorCode(code: Int): Pair<String, String> {
        return when (code) {
            0 -> Pair(
                "ERROR_CODE_INTERNAL_ERROR (0)",
                "Internal Google AdMob server error. Often caused by temporary network routing or invalid server response."
            )
            1 -> Pair(
                "ERROR_CODE_INVALID_REQUEST (1)",
                "The ad request was invalid. Verify that the Ad Unit ID is correct and belongs to the App ID registered in your AdMob console."
            )
            2 -> Pair(
                "ERROR_CODE_NETWORK_ERROR (2)",
                "Network connection error. Check device internet connection or DNS."
            )
            3 -> Pair(
                "ERROR_CODE_NO_FILL (3)",
                "No Ad Inventory returned from Google. This is the most common reason for new AdMob ad units. Google notes that new ad units may take from 1 hour up to a few days to serve ads, or until tax/payment profiles are verified in AdMob console. You can toggle 'Test Ads' in the Diagnostics panel to see the ad flow work instantly!"
            )
            else -> Pair("UNKNOWN_ERROR ($code)", "Unspecified error code returned from Google AdMob SDK.")
        }
    }

    fun initialize(application: Application) {
        if (isInitialized) return
        isInitialized = true

        application.registerActivityLifecycleCallbacks(this)
        logEvent("Initializing Google Mobile Ads SDK...")

        MobileAds.initialize(application) { initStatus ->
            val adapterStatusMap = initStatus.adapterStatusMap
            val details = adapterStatusMap.entries.joinToString { "${it.key}: ${it.value.initializationState}" }
            logEvent("MobileAds initialized: $details")
            loadAppOpenAd(application)
            loadRewardedAd(application)
        }
    }

    // --- App Open Ad Implementation ---

    fun loadAppOpenAd(context: Context, onComplete: ((Boolean) -> Unit)? = null) {
        if (isLoadingAppOpenAd) return
        if (isAppOpenAdAvailable()) {
            onComplete?.invoke(true)
            return
        }
        isLoadingAppOpenAd = true
        val unitId = activeAppOpenAdUnitId
        logEvent("Requesting App Open Ad with Unit ID: $unitId")

        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            unitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    logEvent("App Open Ad loaded successfully!")
                    appOpenAd = ad
                    isLoadingAppOpenAd = false
                    appOpenLoadTime = Date().time
                    _isAppOpenAdLoaded.value = true
                    onComplete?.invoke(true)

                    if (showAppOpenWhenReady) {
                        showAppOpenWhenReady = false
                        currentActivity?.let { showAppOpenAdIfAvailable(it) }
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    val (errorName, explanation) = translateErrorCode(loadAdError.code)
                    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    val errorInfo = AdErrorInfo(
                        adType = "App Open Ad",
                        errorCode = loadAdError.code,
                        errorName = errorName,
                        errorMessage = loadAdError.message,
                        adUnitId = unitId,
                        timestamp = time,
                        explanation = explanation,
                        responseInfo = loadAdError.responseInfo?.toString() ?: "None"
                    )
                    _lastError.value = errorInfo
                    logEvent("App Open Ad failed to load: [Code ${loadAdError.code} - $errorName]: ${loadAdError.message}")
                    isLoadingAppOpenAd = false
                    appOpenAd = null
                    _isAppOpenAdLoaded.value = false
                    showAppOpenWhenReady = false
                    onComplete?.invoke(false)
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
            logEvent("App Open Ad not ready yet. Scheduling display when loaded.")
            showAppOpenWhenReady = true
            loadAppOpenAd(activity.applicationContext)
            onAdDismissed()
            return
        }

        val ad = appOpenAd
        if (ad == null) {
            onAdDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                logEvent("App Open Ad dismissed.")
                appOpenAd = null
                isShowingAppOpenAd = false
                _isAppOpenAdLoaded.value = false
                loadAppOpenAd(activity.applicationContext)
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                logEvent("App Open Ad failed to show: ${adError.message}")
                appOpenAd = null
                isShowingAppOpenAd = false
                _isAppOpenAdLoaded.value = false
                loadAppOpenAd(activity.applicationContext)
                onAdDismissed()
            }

            override fun onAdShowedFullScreenContent() {
                logEvent("App Open Ad displaying on screen.")
                isShowingAppOpenAd = true
            }
        }

        ad.show(activity)
    }

    // --- Rewarded Ad Implementation ---

    fun loadRewardedAd(context: Context, onComplete: ((Boolean) -> Unit)? = null) {
        if (isLoadingRewardedAd) return
        if (rewardedAd != null) {
            onComplete?.invoke(true)
            return
        }
        isLoadingRewardedAd = true
        val unitId = activeRewardedAdUnitId
        logEvent("Requesting Rewarded Ad with Unit ID: $unitId")

        val request = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            unitId,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    logEvent("Rewarded Ad loaded successfully!")
                    rewardedAd = ad
                    isLoadingRewardedAd = false
                    _isRewardedAdLoaded.value = true
                    onComplete?.invoke(true)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    val (errorName, explanation) = translateErrorCode(loadAdError.code)
                    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    val errorInfo = AdErrorInfo(
                        adType = "Rewarded Ad",
                        errorCode = loadAdError.code,
                        errorName = errorName,
                        errorMessage = loadAdError.message,
                        adUnitId = unitId,
                        timestamp = time,
                        explanation = explanation,
                        responseInfo = loadAdError.responseInfo?.toString() ?: "None"
                    )
                    _lastError.value = errorInfo
                    logEvent("Rewarded Ad failed to load: [Code ${loadAdError.code} - $errorName]: ${loadAdError.message}")
                    rewardedAd = null
                    isLoadingRewardedAd = false
                    _isRewardedAdLoaded.value = false
                    onComplete?.invoke(false)
                }
            }
        )
    }

    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: ((RewardItem) -> Unit)? = null,
        onAdFailedToShow: ((AdErrorInfo) -> Unit)? = null,
        onAdDismissed: () -> Unit = {}
    ) {
        val ad = rewardedAd
        if (ad == null) {
            logEvent("Rewarded Ad not yet cached. Requesting and attempting to wait up to 3s...")
            CoroutineScope(Dispatchers.Main).launch {
                loadRewardedAd(activity.applicationContext)
                var waited = 0
                while (waited < 30 && rewardedAd == null && isLoadingRewardedAd) {
                    delay(100)
                    waited++
                }
                val newlyLoadedAd = rewardedAd
                if (newlyLoadedAd != null) {
                    executeShowRewardedAd(activity, newlyLoadedAd, onRewardEarned, onAdFailedToShow, onAdDismissed)
                } else {
                    val err = _lastError.value ?: AdErrorInfo(
                        adType = "Rewarded Ad",
                        errorCode = 3,
                        errorName = "ERROR_CODE_NO_FILL (3) / Ad Not Ready",
                        errorMessage = "Rewarded ad was not ready to display. If using a new AdMob unit, AdMob may still be generating inventory.",
                        adUnitId = activeRewardedAdUnitId,
                        timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                        explanation = "Google AdMob returned NO_FILL or the ad unit was not ready. Switch to 'Test Ads' in AdMob Diagnostics to preview real ad rendering.",
                        responseInfo = "No cached response"
                    )
                    logEvent("Rewarded Ad could not show: ${err.errorName}")
                    onAdFailedToShow?.invoke(err)
                    onAdDismissed()
                }
            }
            return
        }

        executeShowRewardedAd(activity, ad, onRewardEarned, onAdFailedToShow, onAdDismissed)
    }

    private fun executeShowRewardedAd(
        activity: Activity,
        ad: RewardedAd,
        onRewardEarned: ((RewardItem) -> Unit)?,
        onAdFailedToShow: ((AdErrorInfo) -> Unit)?,
        onAdDismissed: () -> Unit
    ) {
        if (isShowingRewardedAd || isShowingAppOpenAd) {
            onAdDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                logEvent("Rewarded Ad dismissed by user.")
                rewardedAd = null
                isShowingRewardedAd = false
                _isRewardedAdLoaded.value = false
                loadRewardedAd(activity.applicationContext)
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                logEvent("Rewarded Ad failed to show: ${adError.message}")
                val err = AdErrorInfo(
                    adType = "Rewarded Ad",
                    errorCode = adError.code,
                    errorName = "Ad Failed To Show (${adError.code})",
                    errorMessage = adError.message,
                    adUnitId = activeRewardedAdUnitId,
                    timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                    explanation = "AdMob could not display the fullscreen ad. Cause: ${adError.cause?.message ?: "Unknown"}",
                    responseInfo = ad.responseInfo.toString()
                )
                _lastError.value = err
                rewardedAd = null
                isShowingRewardedAd = false
                _isRewardedAdLoaded.value = false
                loadRewardedAd(activity.applicationContext)
                onAdFailedToShow?.invoke(err)
                onAdDismissed()
            }

            override fun onAdShowedFullScreenContent() {
                logEvent("Rewarded Ad is showing on screen.")
                isShowingRewardedAd = true
            }
        }

        ad.show(activity) { rewardItem ->
            logEvent("User earned reward: ${rewardItem.amount} ${rewardItem.type}")
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
