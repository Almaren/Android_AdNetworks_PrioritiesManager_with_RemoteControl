package com.almatime.lib.ads.interstitial

import com.almatime.lib.ads.data.AdSource
import com.almatime.lib.ads.AdSourceBase
import com.almatime.lib.ads.AdUnitBaseHandler

/**
 * @author Alexander Khrapunsky
 * @version 1.0.0, 30.12.21.
 * @since 1.0.0
 */
abstract class AdSourceInterstitial(
    adUnitHandler: AdUnitBaseHandler,
    adSource: AdSource,
    val adTypes: Set<InterstitialAdType>
) : AdSourceBase(adUnitHandler, adSource) {

    var countShowedStatic = 0
    var countShowedVideo = 0

    var lastShownAdType: InterstitialAdType? = null

}

enum class InterstitialAdType {
    All, Display, Playable, Video
}