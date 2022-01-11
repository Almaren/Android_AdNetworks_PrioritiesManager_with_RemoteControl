package com.almatime.lib.ads.rewarded

import com.almatime.lib.ads.data.AdSource
import com.almatime.lib.ads.AdSourceBase
import com.almatime.lib.ads.AdUnitBaseHandler

/**
 * @author Alexander Khrapunsky
 * @version 1.0.0, 05.01.22.
 * @since 1.0.0
 */
abstract class AdSourceRewarded(
    adUnitHandler: AdUnitBaseHandler,
    adSource: AdSource
) : AdSourceBase(adUnitHandler, adSource)