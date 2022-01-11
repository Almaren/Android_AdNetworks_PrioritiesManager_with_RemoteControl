package com.almatime.lib.ads.event

import com.almatime.lib.ads.data.AdSource

/**
 * @author Alexander Khrapunsky
 * @version 1.0.0, 01.01.22.
 * @since 1.0.0
 */
interface OnAdStateChange {

    fun onAdLoaded(adSource: AdSource)
    fun onAdShowStarting()
    /** but not closed yet */
    fun onAdShowEnded()
    fun onAdClosed(action: (() -> Unit)? = null)
    fun onAdClicked()
    fun onAdEmpty(adSource: AdSource)
    fun onFailedToLoad(adSource: AdSource, retryCurrAdSource: Boolean = true)
    fun onFailedToShow(adSource: AdSource, onShowError: (() -> Unit)? = null)
    fun onError(adSource: AdSource)

}