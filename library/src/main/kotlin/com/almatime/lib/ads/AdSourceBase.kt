package com.almatime.lib.ads

import com.almatime.lib.ads.data.AdSource
import com.almatime.lib.ads.event.OnAdStateChange

/**
 * Each ad network should implement the base class with specific ad type.
 *
 * @author Alexander Khrapunsky
 * @version 1.0.0, 30.12.21.
 * @since 1.0.0
 */
abstract class AdSourceBase(
    val adUnitHandler: AdUnitBaseHandler,
    val adSource: AdSource
) : OnAdStateChange by adUnitHandler {

    abstract fun createAdUnit()

    abstract fun isLoaded(): Boolean

    abstract fun load()

    fun loadIfNotReady() {
        if (!isLoaded()) {
            load()
        }
    }

    abstract fun show()

    abstract fun destroy()

}