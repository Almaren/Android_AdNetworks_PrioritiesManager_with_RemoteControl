package com.almatime.lib.ads.rewarded

import com.almatime.lib.ads.data.AdSource
import com.almatime.lib.ads.data.ServiceResultState
import com.almatime.util.Log
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds

/**
 * Adapted to unity ads version 4.0.0
 * [Deprecated api](https://docs.unity.com/ads/DeprecatedAPIClasses.htm)
 *
 * @author Alexander Khrapunsky
 * @version 1.0.0, 07.01.22.
 * @since 1.0.0
 */
class UnityRewarded(
    adUnitHandler: AdRewardedHandler,
    adSource: AdSource
) : AdSourceRewarded(adUnitHandler, adSource) {

    private val ID_GLOBAL = "your_ad_unit_id" // todo set your id

    private var isLoaded = false

    override fun createAdUnit() {
        // not required for v4.0.0+
    }

    override fun isLoaded() = isLoaded

    override fun load() {
        UnityAds.load(ID_GLOBAL, loadListener)
    }

    override fun show() {
        UnityAds.show(adUnitHandler.activity, ID_GLOBAL, showListener)
    }

    override fun destroy() {
        // not required
    }

    private val loadListener = object : IUnityAdsLoadListener {

        override fun onUnityAdsAdLoaded(placementId: String?) {
            Log.i("ads", "LOADED placementId = $placementId")
            isLoaded = true
            onAdLoaded(adSource)
        }

        override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, msg: String?) {
            isLoaded = false
            Log.i("ads", "Rewarded onUnityAdsFailedToLoad error=" + error?.name + ", msg=" + msg)
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
            Log.i("ads", "Rewarded onUnityAdsShowFailure error=" + error?.name + ", msg=" + msg)
            isLoaded = false
            onFailedToShow(adSource) {
                adUnitHandler?.onResult?.run {
                    this(ServiceResultState.ERROR)
                }
            }
        }

        override fun onUnityAdsShowStart(placementId: String?) {
            onAdShowStarting()
        }

        override fun onUnityAdsShowClick(placementId: String?) {
            onAdClicked()
        }

        override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
            Log.i("ads", "placementId = $placementId, state=$state")
            onAdShowEnded()
            adUnitHandler.load(adSource)
            isLoaded = false
            state?.let {
                when (it) {
                    UnityAds.UnityAdsShowCompletionState.COMPLETED -> adUnitHandler?.onResult?.run {
                        this(ServiceResultState.SUCCESS)
                    }
                    UnityAds.UnityAdsShowCompletionState.SKIPPED -> adUnitHandler?.onResult?.run {
                        this(ServiceResultState.INTERRUPTED)
                    }
                }
            } ?: kotlin.run {
                adUnitHandler?.onResult?.run {
                    this(ServiceResultState.ERROR)
                }
            }
        }

    }

}