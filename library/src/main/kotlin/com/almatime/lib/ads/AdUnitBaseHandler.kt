package com.almatime.lib.ads

import android.app.Activity
import com.almatime.lib.ads.data.AdSource
import com.almatime.lib.ads.event.OnAdStateChange
import com.almatime.util.Log

/**
 * @author Alexander Khrapunsky
 * @version 1.0.0, 31.12.21.
 * @since 1.0.0
 */
abstract class AdUnitBaseHandler(val activity: Activity) : OnAdStateChange {

    val adSources = hashMapOf<AdSource, AdSourceBase>()

    // to prevent infinite loop when loading ad source on error
    protected val adSourcesFailedCounter = hashMapOf<AdSource, Int>()
    private val ON_LOAD_FAILED_RETRY = 2

    /**
     * 1. Create specific ad source implementation and add it to [adSources].
     * 2. Call [adSources[adSource].createAdUnit()].
     * 3. Initialize [adSourcesFailedCounter[adSource] = 0].
     */
    abstract fun create(adSource: AdSource)

    abstract fun getSortedAdSources(): List<AdSource>

    abstract fun getIndexOfAdSource(adSource: AdSource): Int

    abstract fun getIndexOfNextAdSource(currAdSource: AdSource): Int

    /** Load an ad source with the highest priority. */
    fun load() {
        loadAdSource(0)
    }

    fun load(adSource: AdSource) {
        Log.i("ads", "LOAD adSource = $adSource = ${adSources[adSource]}")
        adSources[adSource]?.let {
            it.loadIfNotReady()
        }
    }

    private fun loadAdSource(adSourceIndex: Int) {
        val sortedAdSources = getSortedAdSources()
        if (adSourceIndex > sortedAdSources.size - 1) return
        Log.i("ads", "LOAD adSourceIndex = $adSourceIndex = ${sortedAdSources[adSourceIndex]}")

        adSources[sortedAdSources[adSourceIndex]]?.let {
            it.loadIfNotReady()
        }
    }

    /**
     * Shows and ad source with the highest priority.
     * If an ad source has not been loaded, load and iterate again N times to display it again.
     * */
    fun show(onFailedToShow: (() -> Unit)? = null) {
        Log.i("ads", "SHOW ad")
        // todo maybe delay thread? or in coroutine delay
        val RETRY_CYCLE = 6 // retry cycle N times, giving time to load ad sources.
        val sortedAdSources = getSortedAdSources()
        var indexAdSource: Int
        var isStartedToShow = false

        outerloop@
        for (retryCycleInd in 1..RETRY_CYCLE) {
            indexAdSource = 0
            do {
                isStartedToShow = showAvailableAdSourceWithPriority(indexAdSource++)
                if (isStartedToShow) {
                    break@outerloop
                }
            } while (indexAdSource < sortedAdSources.size)
        }

        if (!isStartedToShow && onFailedToShow != null) {
            onFailedToShow()
        }
    }

    /** @return result if started to display an ad. */
    private fun showAvailableAdSourceWithPriority(adSourceIndex: Int): Boolean {
        Log.i("ads", "showAvailableAdSourceWithPriority index = $adSourceIndex")
        val sortedAdSources = getSortedAdSources()
        if (adSourceIndex > adSources.size - 1) return false

        return adSources[sortedAdSources[adSourceIndex]]?.let { adSource ->
            if (adSource.isLoaded()) {
                adSource.show()
                true
            } else {
                adSource.load()
                false
            }
        } ?: false
    }

    private fun showTheFirstNextAvailableAdSourceWithPriority(
            currAdSource: AdSource,
            onFailedToShow: (() -> Unit)? = null
    ) {
        Log.i("ads", "showTheFirstNextAvailableAdSourceWithPriority currAdSource = $currAdSource")
        val sortedAdSources = getSortedAdSources()
        val currAdSourceIndex = getIndexOfAdSource(currAdSource)
        var isStartedToShow = false

        for (i in sortedAdSources.indices) {
            if (i == currAdSourceIndex) {
                continue
            }
            isStartedToShow = showAvailableAdSourceWithPriority(i)
            if (isStartedToShow) {
                break
            }
        }

        if (!isStartedToShow && onFailedToShow != null) {
            onFailedToShow()
        }
    }

    override fun onAdLoaded(adSource: AdSource) {
        Log.i("ads", "onAdLoaded = $adSource")
        adSourcesFailedCounter[adSource] = 0
    }

    override fun onAdShowStarting() {
        Log.i("ads", "onAdShowStarting")
        updateLastActionStateOnStartDisplaying()
    }

    override fun onAdShowEnded() {
        Log.i("ads", "onAdShowEnded")
        updateLastActionStateOnAdDisplayed()
    }

    override fun onAdClosed(action: (() -> Unit)?) {
        Log.i("ads", "onAdClosed")
        action?.run {
            this()
        }
    }

    override fun onAdClicked() {
        Log.i("ads", "onAdClicked")
    }

    override fun onAdEmpty(adSource: AdSource) {
        Log.i("ads", "onAdEmpty currAdSource = $adSource")
        onRetryAction(adSource) {
            loadAdSource(getIndexOfNextAdSource(adSource))
        }
    }

    override fun onFailedToLoad(adSource: AdSource, retryCurrAdSource: Boolean) {
        Log.i("ads", "onFailedToLoad currAdSource = $adSource")
        onRetryAction(adSource) {
            if (retryCurrAdSource) {
                loadAdSource(getIndexOfAdSource(adSource)) // load current ad source
            }
            loadAdSource(getIndexOfNextAdSource(adSource)) // load the next available ad source
        }
    }

    override fun onFailedToShow(adSource: AdSource, onShowError: (() -> Unit)?) {
        Log.i("ads", "onFailedToShow currAdSource = $adSource")
        val isActionProcessed = onRetryAction(adSource) {
            loadAdSource(getIndexOfNextAdSource(adSource))
            showTheFirstNextAvailableAdSourceWithPriority(adSource) {
                onShowError?.run { this() }
            }
        }
        if (!isActionProcessed && onShowError != null) {
            onShowError()
        }
    }

    override fun onError(adSource: AdSource) {
        Log.w("ads", "onError currAdSource = $adSource")
    }

    /** @return the boolean result if #action() processed. */
    private inline fun onRetryAction(adSource: AdSource, action: () -> Unit): Boolean {
        return adSourcesFailedCounter[adSource]?.let { counterFails ->
            if (counterFails >= ON_LOAD_FAILED_RETRY) {
                return false
            }

            adSourcesFailedCounter[adSource] = counterFails + 1
            action()
            true
        } ?: false
    }

    private fun updateLastActionStateOnStartDisplaying() {
        //Settings.GetInstance().userLastAction = Settings.UserLastAction.AD_FULLSCREEN_SHOWING
    }

    private fun updateLastActionStateOnAdDisplayed() {
        //Settings.GetInstance().userLastAction = null
    }

    fun dispose() {
        adSources.values.forEach {
            it.destroy()
        }
    }

}
