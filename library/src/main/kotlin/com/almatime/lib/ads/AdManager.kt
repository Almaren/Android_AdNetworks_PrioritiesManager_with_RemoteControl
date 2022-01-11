package com.almatime.lib.ads

import android.app.Activity
import android.widget.RelativeLayout
import com.almatime.lib.ads.data.*
import com.almatime.lib.ads.event.AdUserDeviceDataBinder
import com.almatime.lib.ads.interstitial.AdInterstitialHandler
import com.almatime.lib.ads.rewarded.AdRewardedHandler
import com.almatime.util.Log
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import com.unity3d.mediation.IInitializationListener
import com.unity3d.mediation.InitializationConfiguration
import com.unity3d.mediation.UnityMediation
import com.unity3d.mediation.errors.SdkInitializationError
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import kotlin.collections.ArrayList

/**
 * SDKs initialization firstly done whenever entering to MainMenuScene, secondly whenever receiving
 * remote config in whatever time, even before entering to MainMenuScene.
 * SDK init must be done via Main UI thread.
 *
 * @param layoutContainer the main container of the whole activity, used to dynamically add banner.
 *
 * @author Alexander Khrapunsky
 * @see https://github.com/Almaren/Android_AdNetworks_PrioritiesManager_with_RemoteControl
 * @version 1.0.1, 29.12.21.
 * @since 1.0.0
 */
class AdManager(val activity: Activity, val layoutContainer: RelativeLayout) {

    //private val adBanner = AdBanner(activity)
    private val adInterstitial = AdInterstitialHandler(activity)
    private val adRewarded = AdRewardedHandler(activity)

    private val sdksInitialized = Collections.synchronizedList(ArrayList<AdSource>())
    private val sdksInitInProcessState = ConcurrentHashMap<AdSource, AtomicBoolean>()

    private val APP_ID_UNITY_ADS = "your_app_id_stored_in_resources_or_in_properties" // todo set your id
    private val APP_ID_UNITY_MEDIATION = "your_app_id_stored_in_resources_or_in_properties" // todo set your id

    /**
     * Should be initialized after Consent Scene.
     */
    fun initAds() {
        Log.i("ads", "initAds initAds thread = " + Thread.currentThread().name)

        AdSourcePriorityHandler.init((activity as AdUserDeviceDataBinder).getUserDeviceData())
        AdSourcePriorityHandler.updatePrioritiesAccordingToMinApi()
        setPrivacyMetaData() // init ad networks metadata
        initAdSources()
    }

    /**
     * Called twice. Once from Core. Second from RemoteConfigManager on receiving remote data.
     */
    @Synchronized fun initAdSources() {
        if (Log.DEBUG) {
            Log.i("ads", "initAdSources Curr Thread = " + Thread.currentThread().name
                    + ", sdksInitialized = " + sdksInitialized.toString())
        }

        if (!Thread.currentThread().name.contains("main")) {
            activity.runOnUiThread {
                createAdSources()
            }
        } else {
            // already running in main thread
            createAdSources()
        }
    }

    /**
     * IronSource listeners should be initialized before the sdk init.
     */
    @Synchronized private fun createAdSources() {
        Log.i("ads", "createAdSources started sdksInitialized = $sdksInitialized, thread=${Thread.currentThread().id}")
        // --------------------------------- Unity Standalone --------------------------------------
        if (isInitSdk(AdSource.UnityAds)) {
            // todo disable test mode
            UnityAds.initialize(activity.applicationContext, APP_ID_UNITY_ADS,
                    true, true, object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    Log.i("ads", "UnityAds is successfully initialized.")
                    initAdUnit(AdSource.UnityAds, AdType.INTERSTITIAL)
                    initAdUnit(AdSource.UnityAds, AdType.REWARDED)
                    setInitSdkState(AdSource.UnityAds, true)
                }

                override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, msg: String?) {
                    Log.w("ads", "UnityAds Failed to Initialize : $msg")
                    setInitSdkState(AdSource.UnityAds, false)
                }
            })
            UnityAds.setDebugMode(true) // todo disable test mode
        }

        // --------------------------------- UnityMediation ----------------------------------------
        if (isInitSdk(AdSource.UnityAdsMediation)) {
            Log.i("ads", "createAdSources CREATE UnityMediation Thread = " + Thread.currentThread().id)
            val configuration = InitializationConfiguration.builder()
                    .setGameId(APP_ID_UNITY_MEDIATION)
                    .setInitializationListener(object : IInitializationListener {
                        override fun onInitializationComplete() {
                            // Unity Mediation is initialized. Try loading an ad.
                            Log.i("ads", "UnityMediation is successfully initialized.")
                            initAdUnit(AdSource.UnityAdsMediation, AdType.INTERSTITIAL)
                            initAdUnit(AdSource.UnityAdsMediation, AdType.REWARDED)
                            setInitSdkState(AdSource.UnityAdsMediation, true)
                        }

                        override fun onInitializationFailed(errorCode: SdkInitializationError, msg: String) {
                            // Unity Mediation failed to initialize. Printing failure reason...
                            Log.w("ads", "UnityMediation Failed to Initialize : $msg")
                            setInitSdkState(AdSource.UnityAdsMediation, false)
                        }
                    }).build()
            UnityMediation.initialize(configuration)
            UnityMediation.setLogLevel(Level.WARNING)
            UnityAds.setDebugMode(true) // todo disable test mode
        }

        Log.i("ads", "createAdSources finished sdksInitialized = $sdksInitialized, thread=${Thread.currentThread().id}")
    }

    /** Thread safe. */
    @Synchronized private fun isInitSdk(adSource: AdSource): Boolean {
        val initInProcessState = sdksInitInProcessState[adSource]?.get() ?: false
        return if (!initInProcessState && !sdksInitialized.contains(adSource)
                && AdSourcePriorityHandler.isInitSdk(adSource)) {
            sdksInitInProcessState[adSource] = AtomicBoolean(true)
            Log.i("ads", "isInitSdk: $adSource=true")
            true
        } else {
            Log.i("ads", "isInitSdk: $adSource=false")
            false
        }
    }

    /** Thread safe. */
    @Synchronized private fun setInitSdkState(adSource: AdSource, isSucceed: Boolean) {
        sdksInitInProcessState[adSource]?.set(false)
        if (!isSucceed) {
            AdSourcePriorityHandler.rmAdSourceInterstitial(adSource)
            AdSourcePriorityHandler.rmAdSourceRewarded(adSource)
            Log.i("ads", "setInitSdkState: $adSource, FAILED. REMOVE")
        } else {
            Log.i("ads", "setInitSdkState: $adSource, SUCCEED")
            sdksInitialized.add(adSource)
        }
    }

    /** Thread safe. For a public lib AdType from lib.ads.data should be used. */
    @Synchronized private fun initAdUnit(adSource: AdSource, type: AdType) {
        if (AdSourcePriorityHandler.isInitAdSource(adSource, type)) {
            when (type) {
                AdType.INTERSTITIAL -> adInterstitial.create(adSource)
                AdType.REWARDED -> adRewarded.create(adSource)
                else -> {}
            }
        }
    }

    /**
     * Called after #initAds.
     * Should be called when those data are known and also after user profile data changes.
     */
    fun setPrivacyMetaData() {
        initMetaDataParams(activity.applicationContext, getConsentUserData())
    }

    fun loadAdInterstitial() {
        Log.i("ads", "AdManager.loadAdInterstitial")
        val userDeviceData = (activity as AdUserDeviceDataBinder).getUserDeviceData()
        AdSourcePriorityHandler.updatePrioritiesRegardingToUserRegion(true, userDeviceData)
        try {
            adInterstitial.load()
        } catch (e: Exception) {
            Log.e(e)
        }
    }

    fun loadAdRewarded() {
        Log.i("ads", "AdManager.loadAdRewarded")
        val userDeviceData = (activity as AdUserDeviceDataBinder).getUserDeviceData()
        AdSourcePriorityHandler.updatePrioritiesRegardingToUserRegion(true, userDeviceData)
        try {
            adRewarded.load()
        } catch (e: Exception) {
            Log.e(e)
        }
    }

    fun loadBanner() {

    }

    fun showAdInterstitial() {
        Log.i("ads", "AdManager.showAdInterstitial")
        try {
            adInterstitial.show()
        } catch (e: Exception) {
            Log.e(e)
        }
    }

    /**
     * @param ServiceResultState The result callback. Will be hold for the rewarded lifecycle.
     */
    fun showAdRewarded(onResult: (ServiceResultState) -> Unit) {
        Log.i("ads", "AdManager.showAdRewarded")
        try {
            adRewarded.show(onResult)
        } catch (e: Exception) {
            Log.e(e)
        }
    }

    fun showBanner() {

    }

    fun dispose() {
        adInterstitial.dispose()
        adRewarded.dispose()
        sdksInitialized.clear()
        sdksInitInProcessState.clear()
    }

    /** todo Implement you own logic here! */
    private fun getConsentUserData(): ConsentUserData {
        /*val age = Settings.GetInstance().age
        val consentState = adaptConsentStateToAds(Settings.GetInstance().consentStatus)

        val isConsentNotRequired = Settings.GetInstance().consentStatus == Settings.ConsentStatus.NOT_REQUIRED
                || Settings.GetInstance().consentStatus == Settings.ConsentStatus.UNKNOWN
        val dataPrivacyLaws = hashSetOf<DataPrivacyApplyLaw>()

        // detect consent given for GDPR or CCPA or LGPD
        // currently can't determine California State location
        if (Settings.GetInstance().isUserFromUSA && (age < 13 || (isConsentNotRequired ||
                Settings.GetInstance().consentStatus == Settings.ConsentStatus.DISAGREED))) {
            dataPrivacyLaws.add(DataPrivacyApplyLaw.CCPA)
        } else if (Settings.GetInstance().isUserFromRegionGDPR) {
            dataPrivacyLaws.add(DataPrivacyApplyLaw.GDPR)
        } else if (Settings.GetInstance().isUserFromRegionLGPD) {
            dataPrivacyLaws.add(DataPrivacyApplyLaw.LGPD)
        }*/

        val age = 0
        val dataPrivacyLaws = hashSetOf<DataPrivacyApplyLaw>()
        val consentState = ConsentState.NOT_REQUIRED

        return ConsentUserData(
            age,
            dataPrivacyLaws,
            consentState
        )
    }

}