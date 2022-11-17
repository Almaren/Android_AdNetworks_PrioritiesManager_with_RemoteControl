package com.almatime.lib.ads.rewarded

import com.almatime.lib.ads.data.AdMobData
import com.almatime.lib.ads.data.AdSource
import com.almatime.lib.ads.data.ServiceResultState
import com.almatime.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * @author Alexander Khrapunsky
 * @version 1.0.0, 02/11/2022.
 * @since 1.0.0
 */
class AdmobRewarded(
    adUnitHandler: AdRewardedHandler,
    adSource: AdSource
) : AdSourceRewarded(adUnitHandler, adSource), AdMobData {

    private var adUnit: RewardedAd? = null

    private var isLoaded = false
    private var isCompleted = false

    private val TEST_ID = "ca-app-pub-3940256099942544/5224354917"

    override fun createAdUnit() {
    }

    override fun isLoaded() = isLoaded

    override fun load() {
        var adRequest = getAdRequest

        RewardedAd.load(
            adUnitHandler.activity,
            idRewarded,
            adRequest,
            object : RewardedAdLoadCallback() {

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    if (Log.DEBUG) {
                        adError?.let {
                            // Gets the domain from which the error came.
                            val errorDomain = it.domain
                            // Gets the error code. See
                            // https://developers.google.com/android/reference/com/google/android/gms/ads/AdRequest#constant-summary
                            // for a list of possible codes.
                            val errorCode = it.code
                            // Gets an error message.
                            // For example "Account not approved yet". See
                            // https://support.google.com/admob/answer/9905175 for explanations of
                            // common errors.
                            val errorMessage = it.message
                            // Gets additional response information about the request. See
                            // https://developers.google.com/admob/android/response-info for more
                            // information.
                            val responseInfo = it.responseInfo
                            // Gets the cause of the error, if available.
                            val cause = it.cause
                            // All of this information is available via the error's toString() method.
                            Log.w("ads", "AdMob failed to load! ${it.toString()}")
                        }
                    }
                    adUnit = null
                    onFailedToLoad(adSource, false)
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    Log.i("ads", "AdMob loaded")
                    adUnit = ad
                    isLoaded = true
                    onAdLoaded(adSource)
                }

            }
        )
    }

    override fun show() {
        // Don't forget to set the ad reference to null so you. Don't show the ad a second time.
        adUnit?.run {
            fullScreenContentCallback = object : FullScreenContentCallback() {

                override fun onAdClicked() {
                    onAdClicked()
                }

                // Called when ad is dismissed. Closed.
                override fun onAdDismissedFullScreenContent() {
                    Log.i("ads", "Ad was dismissed isCompleted = $isCompleted")
                    adUnit = null
                    isLoaded = false
                    onAdShowEnded()

                    (adUnitHandler as AdRewardedHandler).run {
                        load(adSource)
                        onResult?.let {
                            if (isCompleted) {
                                it(ServiceResultState.SUCCESS)
                            } else {
                                it(ServiceResultState.INTERRUPTED)
                            }
                        }
                    }
                    isCompleted = false
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w("ads", "Ad failed to show!")
                    adUnit = null
                    isLoaded = false
                    isCompleted = false
                    onFailedToShow(adSource) {
                        (adUnitHandler as AdRewardedHandler)?.onResult?.run {
                            this(ServiceResultState.ERROR)
                        }
                    }
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                }

                // Called when ad is started shown.
                override fun onAdShowedFullScreenContent() {
                    Log.i("ads", "Ad showed fullscreen content")
                    onAdShowStarting()
                }

            }
            // For Google ads, all onUserEarnedReward() calls occur before onAdDismissedFullScreenContent().
            // For ads served through mediation, the third-party ad network SDK's implementation
            // determines the callback order. For ad network SDKs that provide a single close callback
            // with reward information, the mediation adapter invokes onUserEarnedReward() before onAdDismissedFullScreenContent().
            show(adUnitHandler.activity) {
                Log.i("ads", "AdMob REWARDED!")
                isCompleted = true
            }
        } ?: run {
            onFailedToShow(adSource)
        }
    }

    override fun destroy() {
        adUnit = null
    }

}