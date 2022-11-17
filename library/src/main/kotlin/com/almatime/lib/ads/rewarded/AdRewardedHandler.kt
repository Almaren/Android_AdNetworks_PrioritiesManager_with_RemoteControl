package com.almatime.lib.ads.rewarded

import android.app.Activity
import com.almatime.lib.ads.data.AdSource
import com.almatime.lib.ads.AdSourcePriorityHandler
import com.almatime.lib.ads.AdUnitBaseHandler
import com.almatime.lib.ads.data.ServiceResultState
import com.almatime.util.Log

/**
 * @author Alexander Khrapunsky
 * @version 1.0.1, 29.12.21.
 * @since 1.0.0
 */
class AdRewardedHandler(activity: Activity) : AdUnitBaseHandler(activity) {

    override fun getSortedAdSources() = AdSourcePriorityHandler.adSourcesRewarded

    override fun getIndexOfAdSource(adSource: AdSource) =
            AdSourcePriorityHandler.getIndexOfAdSourceRewarded(adSource)

    override fun getIndexOfNextAdSource(currAdSource: AdSource) =
            AdSourcePriorityHandler.getIndexOfNextAvailableAdSourceRewarded(currAdSource)

    var onResult: ((ServiceResultState) -> Unit)? = null

    override fun create(adSource: AdSource, loadAd: Boolean) {
        Log.i("ads", "AdRewardedHandler create: $adSource")
        when (adSource) {
            AdSource.UnityAds -> {
                adSources[AdSource.UnityAds] = UnityRewarded(
                    this,
                    AdSource.UnityAds
                ).apply {
                    createAdUnit()
                    if (loadAd) load()
                }
                adSourcesFailedCounter[AdSource.UnityAds] = 0
            }
            AdSource.AdmobMediation -> {
                adSources[AdSource.AdmobMediation] = AdmobRewarded(
                    this,
                    AdSource.AdmobMediation
                ).apply {
                    createAdUnit()
                    if (loadAd) load()
                }
                adSourcesFailedCounter[AdSource.AdmobMediation] = 0
            }
            AdSource.UnityAdsMediation -> {
                adSources[AdSource.UnityAdsMediation] = UnityMediationRewarded(
                    this,
                    AdSource.UnityAdsMediation
                ).apply {
                    createAdUnit()
                    if (loadAd) load()
                }
                adSourcesFailedCounter[AdSource.UnityAdsMediation] = 0
            }
            else -> {}
        }
    }

    /**
     * @param ServiceResultState for a public lib should use class from lib.data
     */
    fun show(onResult: (ServiceResultState) -> Unit) {
        this.onResult = onResult
        super.show {
            onResult(ServiceResultState.UNAVAILABLE)
        }
    }

}
