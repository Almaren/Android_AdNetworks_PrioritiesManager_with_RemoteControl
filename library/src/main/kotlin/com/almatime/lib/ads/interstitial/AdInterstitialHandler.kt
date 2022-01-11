package com.almatime.lib.ads.interstitial

import android.app.Activity
import com.almatime.lib.ads.data.AdSource
import com.almatime.lib.ads.AdSourcePriorityHandler
import com.almatime.lib.ads.AdUnitBaseHandler
import com.almatime.util.Log

/**
 * @author Alexander Khrapunsky
 * @version 1.0.1, 29.12.21.
 * @since 1.0.0
 */
class AdInterstitialHandler(activity: Activity) : AdUnitBaseHandler(activity) {

    override fun getSortedAdSources() = AdSourcePriorityHandler.adSourcesInterstitials

    override fun getIndexOfAdSource(adSource: AdSource) =
            AdSourcePriorityHandler.getIndexOfAdSourceInterstitial(adSource)

    override fun getIndexOfNextAdSource(currAdSource: AdSource) =
            AdSourcePriorityHandler.getIndexOfNextAvailableAdSourceInterstitial(currAdSource)

    override fun create(adSource: AdSource) {
        Log.i("ads", "AdInterstitialHandler create: $adSource")
        when (adSource) {
            AdSource.UnityAds -> {
                adSources[AdSource.UnityAds] = UnityInterstitial(
                    this,
                    AdSource.UnityAds,
                    setOf(InterstitialAdType.All)
                ).apply {
                    createAdUnit()
                }
                adSourcesFailedCounter[AdSource.UnityAds] = 0
            }
            AdSource.UnityAdsMediation -> {
                adSources[AdSource.UnityAdsMediation] = UnityMediationInterstitial(
                    this,
                    AdSource.UnityAdsMediation,
                    setOf(InterstitialAdType.All)
                ).apply {
                    createAdUnit()
                }
                adSourcesFailedCounter[AdSource.UnityAdsMediation] = 0
            }
        }
    }

    fun show(type: InterstitialAdType) {
        // todo in next version
    }

}