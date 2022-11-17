package com.almatime.lib.ads.interstitial

import com.almatime.lib.ads.data.AdMobData
import com.almatime.lib.ads.data.AdSource
import com.almatime.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * https://developers.google.com/android/reference/com/google/android/gms/ads/MobileAds
 *
 * @author Alexander Khrapunsky
 * @version 1.0.0, 02/11/2022.
 * @since 1.0.0
 */
class AdmobInterstitial(
    adUnitHandler: AdInterstitialHandler,
    adSource: AdSource,
    adTypes: Set<InterstitialAdType>
) : AdSourceInterstitial(adUnitHandler, adSource, adTypes), AdMobData {

    private var adUnit: InterstitialAd? = null

    private var isLoaded = false

    private val TEST_ID_FULSCREEN = "ca-app-pub-3940256099942544/1033173712"
    private val TEST_ID_VIDEO = "ca-app-pub-3940256099942544/8691691433"

    override fun createAdUnit() {
    }

    override fun isLoaded() = isLoaded

    override fun load() {
        var adRequest = getAdRequest

        InterstitialAd.load(
            adUnitHandler.activity,
            idInterstitial,
            adRequest,
            object : InterstitialAdLoadCallback() {

                // https://support.google.com/admob/answer/9905175
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

                override fun onAdLoaded(ad: InterstitialAd) {
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
                    Log.i("ads", "Ad was dismissed")
                    adUnit = null
                    isLoaded = false
                    onAdShowEnded()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w("ads", "Ad failed to show!")
                    adUnit = null
                    isLoaded = false
                    onFailedToShow(adSource)
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                }

                // Called when ad is started and shown.
                override fun onAdShowedFullScreenContent() {
                    Log.i("ads", "Ad showed fullscreen content")
                    onAdShowStarting()
                }

            }
            show(adUnitHandler.activity)
        } ?: run {
            onFailedToShow(adSource)
        }
    }

    override fun destroy() {
        adUnit = null
    }

}