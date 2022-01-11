package com.almatime.lib.ads.interstitial

import com.almatime.lib.ads.data.AdSource
import com.almatime.lib.ads.AdUnitBaseHandler
import com.almatime.util.Log
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds

/**
 * Adapted to unity ads version 4.0.0
 * [Deprecated api](https://docs.unity.com/ads/DeprecatedAPIClasses.htm)
 *
 * @author Alexander Khrapunsky
 * @version 1.0.0, 01.01.22.
 * @since 1.0.0
 */
class UnityInterstitial(
    adUnitHandler: AdInterstitialHandler,
    adSource: AdSource,
    adTypes: Set<InterstitialAdType>
) : AdSourceInterstitial(adUnitHandler, adSource, adTypes) {

    private val ID_ALL_UNIT_TYPES = "your_ad_unit_id" // todo set your id
    private val ID_DISPLAY = "todo" // todo to create
    private val ID_PLAYABLE = "todo" // todo to create
    private val ID_VIDEO = "todo" // todo to create

    private var isLoaded = false

    override fun createAdUnit() {
        // not required for v4.0.0+
    }

    override fun isLoaded() = isLoaded

    override fun load() {
        UnityAds.load(getIdOfCurrAdType(), loadListener)
    }

    override fun show() {
        UnityAds.show(adUnitHandler.activity, getIdOfCurrAdType(), showListener)
    }

    override fun destroy() {
        // not required
    }

    // todo in next version depending on strategy determine which id to load
    private fun getIdOfCurrAdType() = ID_ALL_UNIT_TYPES

    private fun markLastShownAd(placementId: String) {
        lastShownAdType = when (placementId) {
            ID_ALL_UNIT_TYPES -> InterstitialAdType.All
            ID_DISPLAY -> InterstitialAdType.Display
            ID_VIDEO -> InterstitialAdType.Video
            ID_PLAYABLE -> InterstitialAdType.Playable
            else -> null
        }
    }

    private val loadListener = object : IUnityAdsLoadListener {

        override fun onUnityAdsAdLoaded(placementId: String?) {
            Log.i("ads", "LOADED placementId = ${placementId}")
            isLoaded = true
            onAdLoaded(adSource)
        }

        override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, msg: String?) {
            isLoaded = false
            Log.i("ads", "Interstitial onUnityAdsFailedToLoad error=" + error?.name + ", msg=" + msg)
            error?.let {
                when (it) {
                    UnityAds.UnityAdsLoadError.NO_FILL -> onAdEmpty(adSource)
                    UnityAds.UnityAdsLoadError.TIMEOUT -> onFailedToLoad(adSource)
                    else -> {
                        onFailedToLoad(adSource, false)
                    }
                }
            } ?: onFailedToLoad(adSource)
        }

    }

    private val showListener = object : IUnityAdsShowListener {

        override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, msg: String?) {
            Log.i("ads", "Interstitial onUnityAdsShowFailure error=" + error?.name + ", msg=" + msg)
            isLoaded = false
            onFailedToShow(adSource)
        }

        override fun onUnityAdsShowStart(placementId: String?) {
            onAdShowStarting()
        }

        override fun onUnityAdsShowClick(placementId: String?) {
            onAdClicked()
        }

        override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
            Log.i("ads", "placementId = ${placementId}, state=${state}")
            onAdShowEnded()
            isLoaded = false
            placementId?.let { markLastShownAd(it) }
        }

    }

}