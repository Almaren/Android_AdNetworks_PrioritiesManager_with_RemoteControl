package com.almatime.lib.ads.rewarded

import com.almatime.lib.ads.data.AdSource
import com.almatime.lib.ads.data.ServiceResultState
import com.almatime.util.Log
import com.unity3d.mediation.*
import com.unity3d.mediation.errors.LoadError
import com.unity3d.mediation.errors.ShowError

/**
 * @author Alexander Khrapunsky
 * @version 1.0.0, 05.01.22.
 * @since 1.0.0
 */
class UnityMediationRewarded(
    adUnitHandler: AdRewardedHandler,
    adSource: AdSource
) : AdSourceRewarded(adUnitHandler, adSource) {

    private val ID_GLOBAL = "your_ad_unit_id" // todo set your id

    private var isLoaded = false

    private var adUnit: RewardedAd? = null

    override fun createAdUnit() {
        adUnit = RewardedAd(adUnitHandler.activity, ID_GLOBAL)
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

    private val loadListener = object : IRewardedAdLoadListener {

        override fun onRewardedLoaded(ad: RewardedAd?) {
            Log.i("ads", "onRewardedLoaded placementId = ${ad?.adUnitId}")
            isLoaded = true
            onAdLoaded(adSource)
        }

        override fun onRewardedFailedLoad(ad: RewardedAd?, error: LoadError?, msg: String?) {
            Log.i("ads", "onRewardedFailedLoad placementId = ${ad?.adUnitId}, $error, $msg")
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

    /**
     * Skip state impossible to determine.
     */
    private val showListener = object : IRewardedAdShowListener {

        // The ad has started to show.
        override fun onRewardedShowed(ad: RewardedAd?) {
            Log.i("ads", "onRewardedShowed placementId = ${ad?.adUnitId}")
            onAdShowStarting()
        }

        override fun onRewardedClicked(ad: RewardedAd?) {
            onAdClicked()
        }

        // Called before onUserRewarded.
        override fun onRewardedClosed(ad: RewardedAd?) {
            Log.i("ads", "onRewardedClosed placementId = ${ad?.adUnitId}")
            onAdShowEnded()
            onAdClosed()
            isLoaded = false
            adUnitHandler.load(adSource)
        }

        override fun onRewardedFailedShow(ad: RewardedAd?, error: ShowError?, msg: String?) {
            Log.i("ads", "onRewardedFailedShow placementId = ${ad?.adUnitId}, $error, $msg")
            isLoaded = false
            onFailedToShow(adSource) {
                adUnitHandler?.onResult?.run {
                    this(ServiceResultState.ERROR)
                }
            }
        }

        // Always called after onRewardedClosed.
        override fun onUserRewarded(ad: RewardedAd?, reward: IReward?) {
            Log.i("ads", "onUserRewarded placementId = ${reward}")
            adUnitHandler?.onResult?.run {
                this(ServiceResultState.SUCCESS)
            }
        }

    }

}