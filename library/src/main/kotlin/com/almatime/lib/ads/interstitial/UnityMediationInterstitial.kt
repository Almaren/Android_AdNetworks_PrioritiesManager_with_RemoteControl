package com.almatime.lib.ads.interstitial

import com.almatime.lib.ads.data.AdSource
import com.almatime.util.Log
import com.unity3d.mediation.AdState
import com.unity3d.mediation.IInterstitialAdLoadListener
import com.unity3d.mediation.IInterstitialAdShowListener
import com.unity3d.mediation.InterstitialAd
import com.unity3d.mediation.errors.LoadError
import com.unity3d.mediation.errors.ShowError

/**
 * Mediation SDK 0.3.0
 * Adapted to unity ads version 3.0.0
 *
 * @author Alexander Khrapunsky
 * @version 1.0.0, 04.01.22.
 * @since 1.0.0
 */
class UnityMediationInterstitial(
    adUnitHandler: AdInterstitialHandler,
    adSource: AdSource,
    adTypes: Set<InterstitialAdType>
) : AdSourceInterstitial(adUnitHandler, adSource, adTypes) {

    private val ID_ALL_UNIT_TYPES = "your_ad_unit_id" // todo set your id
    private val ID_DISPLAY = "todo" // todo to create
    private val ID_PLAYABLE = "todo" // todo to create
    private val ID_VIDEO = "todo" // todo to create

    private var isLoaded = false

    private var adUnit: InterstitialAd? = null

    override fun createAdUnit() {
        adUnit = InterstitialAd(adUnitHandler.activity, getIdOfCurrAdType())
    }

    override fun isLoaded() = isLoaded && adUnit?.run {
        adState == AdState.LOADED
    } ?: false

    override fun load() {
        adUnit?.run {
            if (adState == AdState.UNLOADED) {
                load(loadListener)
            }
        }
    }

    override fun show() {
        adUnit?.run {
            show(showListener)
        }
    }

    override fun destroy() {
        adUnit = null
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

    private val loadListener = object : IInterstitialAdLoadListener {

        override fun onInterstitialLoaded(ad: InterstitialAd?) {
            Log.i("ads", "onInterstitialLoaded placementId = ${ad?.adUnitId}")
            isLoaded = true
            onAdLoaded(adSource)
        }

        override fun onInterstitialFailedLoad(ad: InterstitialAd?, error: LoadError?, msg: String?) {
            Log.i("ads", "onInterstitialFailedLoad placementId = ${ad?.adUnitId}, $error, $msg")
            isLoaded = false
            error?.let {
                when (it) {
                    LoadError.NO_FILL -> onAdEmpty(adSource)
                    LoadError.NETWORK_ERROR -> onFailedToLoad(adSource)
                    else -> {
                        onFailedToLoad(adSource, false)
                    }
                }
            } ?: onFailedToLoad(adSource)
        }

    }

    private val showListener = object : IInterstitialAdShowListener {

        // The ad has started to show.
        override fun onInterstitialShowed(ad: InterstitialAd?) {
            Log.i("ads", "onInterstitialShowed placementId = ${ad?.adUnitId}")
            onAdShowStarting()
        }

        override fun onInterstitialClicked(ad: InterstitialAd?) {
            onAdClicked()
        }

        // The ad has finished showing.
        override fun onInterstitialClosed(ad: InterstitialAd?) {
            Log.i("ads", "onInterstitialClosed placementId = ${ad?.adUnitId}")
            onAdShowEnded()
            onAdClosed()
            isLoaded = false
            ad?.let {
                markLastShownAd(it.adUnitId)
            }
        }

        override fun onInterstitialFailedShow(ad: InterstitialAd?, error: ShowError?, msg: String?) {
            Log.i("ads", "onInterstitialFailedShow placementId = ${ad?.adUnitId}, $error, $msg")
            isLoaded = false
            onFailedToShow(adSource)
        }

    }

}