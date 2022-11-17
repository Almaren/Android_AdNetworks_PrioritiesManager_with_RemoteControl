package com.almatime.lib.ads.data

import android.os.Bundle
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.ads.mediation.unity.UnityMediationAdapter
import com.google.android.gms.ads.AdRequest

/**
 * @author Alexander Khrapunsky
 * @version 1.0.0, 03/11/2022.
 * @since 1.0.0
 */
interface AdMobData {

    val getAdRequest: AdRequest
        get() {
            return adRequestMetaParamsMap[AdSource.AdmobMediation]?.run {
                if (this is Bundle) {
                    AdRequest.Builder()
                        .addNetworkExtrasBundle(AdMobAdapter::class.java, this)
                        .addNetworkExtrasBundle(UnityMediationAdapter::class.java, this)
                        .build()
                } else {
                    AdRequest.Builder().build()
                }
            } ?: AdRequest.Builder().build()
        }

    val idInterstitial: String
        get() = "set your id" // todo set id

    val idRewarded: String
        get() = "set your id" // todo set id

}
