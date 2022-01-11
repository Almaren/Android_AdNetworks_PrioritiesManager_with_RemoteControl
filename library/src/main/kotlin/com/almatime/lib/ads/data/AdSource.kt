package com.almatime.lib.ads.data

import android.os.Build

/**
 * Use the exact ad sources' remote config names.
 */
enum class AdSource {
    IronSrc,
    UnityAds,
    UnityAdsMediation,
    Kidoz,
    Awesome
}

/**
 * Minimum and maximum supported API by each AdSource. Pass null for maximum if not determined.
 */
val minMaxApiAdSources = mapOf(
    AdSource.Awesome to Pair(Build.VERSION_CODES.KITKAT, null),
    AdSource.UnityAds to Pair(Build.VERSION_CODES.JELLY_BEAN_MR2, null),
    AdSource.Kidoz to Pair(Build.VERSION_CODES.JELLY_BEAN_MR1, null),
    AdSource.UnityAdsMediation to Pair(Build.VERSION_CODES.LOLLIPOP, Build.VERSION_CODES.R)
)