package com.almatime.lib.ads

import android.os.Build
import com.almatime.lib.ads.data.*
import com.almatime.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.Exception
import java.lang.IllegalArgumentException
import kotlin.math.max

/**
 * Handles the priority of displaying an ad source for each ad unit type. The first is a highest in a list.
 * Currently detecting only LATAM region.
 * Takes a remote config json with ad sources priorities. Priority = -1 is disabled, higher is better.
 *  {
 *      "AdSource": [ <IntPriorityInterstitials>, <IntPriorityRewarded> ]
 *  }
 *
 * @author Alexander Khrapunsky
 * @version 1.0.1, 05.01.22.
 * @since 1.0.0
 */
object AdSourcePriorityHandler {

    /** Ordered - the first element has a higher priority, don't include disabled sources. */
    lateinit var adSourcesInterstitials: List<AdSource>
    lateinit var adSourcesRewarded: List<AdSource>

    lateinit var adSourcesInterstitialsOrigServer: List<AdSource>
    lateinit var adSourcesRewardedOrigServer: List<AdSource>

    private val adSourceForLatam = AdSource.AdmobMediation

    val latam = setOf("AR", "BO", "BR", "CL", "CO", "EC", "FK", "GF", "GY", "PY", "PE", "SR", "UY", "VE")
    val northAndCentralAmerica = setOf("MX", "GG", "HN", "SV", "BZ", "NI", "CR", "PA")
    val caribbeanAndOthers = setOf("CU", "DO", "DM", "HT", "GP", "MQ", "PR", "BL", "MF") // include also Dominica
    val spanishAndPortuguese = setOf("ES", "PT")
    val latamLanguages = setOf(AppLangCode.PT, AppLangCode.ES)

    private var latestLangCode: AppLangCode? = null

    /** <b>MUST BE CALLED FIRST BEFORE FIRST USE OF THIS SINGLETON!!!</b> */
    fun init(userDeviceData: UserDeviceData) {
        Log.i("ads", "AdSourcePriority.init: data = ${userDeviceData}")
        if (isUserFromLatamRegion(userDeviceData)) {
            adSourcesInterstitials = listOf(adSourceForLatam)
            adSourcesRewarded = listOf(adSourceForLatam)
        } else {
            adSourcesInterstitials = listOf(AdSource.AdmobMediation, AdSource.UnityAds)
            adSourcesRewarded = listOf(AdSource.AdmobMediation, AdSource.UnityAds)
        }
    }

    fun updatePrioritiesAccordingToMinApi() {
        adSourcesInterstitials = filterAdSourcesAccordingToBuildApi(adSourcesInterstitials)
        adSourcesRewarded = filterAdSourcesAccordingToBuildApi(adSourcesRewarded)

        Log.i("ads", "AdSourcePriority updatePrioritiesAccordingToMinApi FILTERED: INTERSTITIALS: $adSourcesInterstitials, " +
            " REWARDED: $adSourcesRewarded")
    }

    fun updatePriorities(strJsonAdSourceWithPriority: String, userDeviceData: UserDeviceData) {
        Log.i("ads", "AdSourcePriority updatePriorities start parsing: $strJsonAdSourceWithPriority")
        val mapType = HashMap<String, IntArray>()
        val map: HashMap<String, IntArray>
        try {
            map = Gson().fromJson(strJsonAdSourceWithPriority, object : TypeToken<HashMap<String, IntArray>>() {}.type)

            adSourcesInterstitials = map.filterValues { it[0] != -1 }
                .entries.sortedByDescending { (_, priorities) -> priorities[0] }
                .mapNotNull {
                    try {
                        AdSource.valueOf(it.key)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }

            adSourcesRewarded = map.filterValues { it[1] != -1 }
                .entries.sortedByDescending { (_, priorities) -> priorities[1] }
                .mapNotNull {
                    try {
                        AdSource.valueOf(it.key)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }

            storeAdSourcesServerOriginalFilteredByMinApi()
        } catch (e: Exception) {
            Log.e(e)
        } finally {
            updatePrioritiesRegardingToUserRegion(false, userDeviceData)
        }
        Log.i("ads", "AdSourcePriority updatePriorities data = ${userDeviceData}")
        Log.i("ads", "AdSourcePriority updatePriorities sorted INTERSTITIALS: $adSourcesInterstitials, " +
            " REWARDED: $adSourcesRewarded")
        updatePrioritiesAccordingToMinApi()
    }

    /**
     * todo handle regions for ad sources
     * Regarding on user locale changing updates the ad sources priorities.
     * Call it on language changing!
     */
    fun updatePrioritiesRegardingToUserRegion(updateOnlyOnLocaleChange: Boolean, userDeviceData: UserDeviceData) {
        if (updateOnlyOnLocaleChange && latestLangCode?.let { userDeviceData.appLangCode == it } ?: false) {
            return
        }
        latestLangCode = userDeviceData.appLangCode

        if (isUserFromLatamRegion(userDeviceData)) {
            Log.i("ads", "updatePrioritiesRegardingToUserRegion user from LATAM")
            if (!adSourcesInterstitials.contains(adSourceForLatam)) {
                adSourcesInterstitials = ArrayList<AdSource>(adSourcesInterstitialsOrigServer)
            }
            if (!adSourcesRewarded.contains(adSourceForLatam)) {
                adSourcesRewarded = ArrayList<AdSource>(adSourcesRewardedOrigServer)
            }
            setAdSourceAsHighestPriority(adSourceForLatam)
        } else {
            // rm adSourceForLatam
            adSourcesInterstitials = removeAdSource(adSourceForLatam, adSourcesInterstitials)
            adSourcesRewarded = removeAdSource(adSourceForLatam, adSourcesRewarded)
        }
        Log.i("ads", "updatePrioritiesRegardingToUserRegion updated INTERSTITIALS: $adSourcesInterstitials, " +
            " REWARDED: $adSourcesRewarded")
    }

    private fun setAdSourceAsHighestPriority(upAdSource: AdSource) {
        fun isHighestPriority(list: List<AdSource>, adSource: AdSource) = list[0] == adSource

        fun setToHighestPriority(rearrangeList: List<AdSource>, upAdSource: AdSource): List<AdSource> {
            val newSortedList = ArrayList<AdSource>()
            newSortedList.add(upAdSource)
            for (adSource in rearrangeList) {
                if (adSource == upAdSource) continue
                newSortedList.add(adSource)
            }
            return newSortedList
        }

        if (adSourcesInterstitials.contains(upAdSource) && !isHighestPriority(adSourcesInterstitials, upAdSource)) {
            adSourcesInterstitials = setToHighestPriority(adSourcesInterstitials, upAdSource)
        }

        if (adSourcesRewarded.contains(upAdSource) && !isHighestPriority(adSourcesRewarded, upAdSource)) {
            adSourcesRewarded = setToHighestPriority(adSourcesRewarded, upAdSource)
        }
    }

    private fun storeAdSourcesServerOriginalFilteredByMinApi() {
        adSourcesInterstitialsOrigServer = filterAdSourcesAccordingToBuildApi(adSourcesInterstitials)
        adSourcesRewardedOrigServer = filterAdSourcesAccordingToBuildApi(adSourcesRewarded)

        Log.i("ads", "storeAdSourcesServerOriginalFilteredByMinApi INTERSTITIALS: $adSourcesInterstitialsOrigServer, " +
            " REWARDED: $adSourcesRewardedOrigServer")
    }

    fun rmAdSourceInterstitial(rmAdSource: AdSource) {
        adSourcesInterstitials = removeAdSource(rmAdSource, adSourcesInterstitials)
        adSourcesInterstitialsOrigServer = adSourcesInterstitials
    }

    fun rmAdSourceRewarded(rmAdSource: AdSource) {
        adSourcesRewarded = removeAdSource(rmAdSource, adSourcesRewarded)
        adSourcesRewardedOrigServer = adSourcesRewarded
    }

    private fun removeAdSource(rmAdSource: AdSource, adSources: List<AdSource>): List<AdSource> {
        if (adSources.contains(rmAdSource)) {
            return adSources.filter { it != rmAdSource }
        }
        return adSources
    }

    fun isInitSdk(adSource: AdSource) = isInitAdSource(adSource, AdType.INTERSTITIAL)
        || isInitAdSource(adSource, AdType.REWARDED)

    /** For a public lib AdType from lib.ads.data should be used. */
    fun isInitAdSource(adSource: AdSource, adType: AdType) = if (adType == AdType.INTERSTITIAL) {
        adSourcesInterstitials.contains(adSource)
    } else if (adType == AdType.REWARDED) {
        adSourcesRewarded.contains(adSource)
    } else {
        false
    }

    fun isAdSourcesEmpty() = adSourcesInterstitials.isEmpty() && adSourcesRewarded.isEmpty()

    fun getActiveSourcesCount() = max(adSourcesInterstitials.size, adSourcesRewarded.size)

    fun getIndexOfAdSourceInterstitial(adSource: AdSource) = adSourcesInterstitials.indexOfFirst { it == adSource }

    fun getIndexOfAdSourceRewarded(adSource: AdSource) = adSourcesRewarded.indexOfFirst { it == adSource }

    /**
     * @return If [adSource] the last in list will return the first element in a list
     */
    fun getIndexOfNextAvailableAdSourceInterstitial(currAdSource: AdSource): Int {
        val currIndex = getIndexOfAdSourceInterstitial(currAdSource)
        return getNextIndex(currIndex, adSourcesInterstitials)
    }

    /**
     * @return If [adSource] the last in list will return the first element in a list
     */
    fun getIndexOfNextAvailableAdSourceRewarded(currAdSource: AdSource): Int {
        val currIndex = getIndexOfAdSourceRewarded(currAdSource)
        return getNextIndex(currIndex, adSourcesRewarded)
    }

    private fun filterAdSourcesAccordingToBuildApi(adSources: List<AdSource>): List<AdSource> {
        return adSources.filter { adSource ->
            val minMaxApiForCurrAdSource = minMaxApiAdSources.get(adSource)
            minMaxApiForCurrAdSource?.let {
                if (Build.VERSION.SDK_INT >= it.first) {
                    it.second?.let {
                        Build.VERSION.SDK_INT <= it
                    } ?: true
                } else {
                    false
                }
            } ?: true
        }
    }

    private fun getNextIndex(currIndex: Int, adSources: List<AdSource>)
        = if (currIndex + 1 <= adSources.size - 1) currIndex + 1 else 0

    /**
     * Check if user country part of Latin America.
     */
    private fun isUserFromLatamRegion(userDeviceData: UserDeviceData): Boolean {
        Log.i("ads", "AdSourcePriority isUserFromLatamRegion data = ${userDeviceData}")
        val isLatamRegion = userDeviceData.localeCountryIso2?.let {
            it in latam || it in northAndCentralAmerica || it in caribbeanAndOthers || it in spanishAndPortuguese
                || userDeviceData.appLangCode in latamLanguages
        } ?: run {
            userDeviceData.appLangCode in latamLanguages
        }
        Log.i("ads", "isUserFromLatamRegion country = ${userDeviceData.localeCountryIso2}, " +
                "lang=${userDeviceData.appLangCode}, finalRes=${isLatamRegion}")
        return isLatamRegion
    }

    private fun testPriorities() {
        adSourcesInterstitials = listOf(AdSource.UnityAds)
        adSourcesRewarded = listOf(AdSource.UnityAds)
    }

}