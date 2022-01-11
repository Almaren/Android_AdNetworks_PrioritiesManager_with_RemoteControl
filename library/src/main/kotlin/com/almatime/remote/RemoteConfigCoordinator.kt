package com.almatime.remote

import com.almatime.lib.ads.AdManager
import com.almatime.lib.ads.AdSourcePriorityHandler
import com.almatime.lib.ads.event.AdUserDeviceDataBinder

/**
 * @author Alexander Khrapunsky
 * @version 1.0.0, 11.01.22.
 * @since 1.0.0
 */
class RemoteConfigCoordinator(val adManager: AdManager) : RemoteConfigNetActions {

    private val remoteConfig: RemoteConfig
    private lateinit var remoteConfigListener: RemoteConfigListener

    init {
        remoteConfig = RemoteConfig() // connects to firebase remote config
        setListener()
    }

    private fun setListener() {
        remoteConfigListener = object : RemoteConfigListener {

            override fun onAdSourcesPriority(jsonStrAdSourcesWithPriority: String) {
                AdSourcePriorityHandler.updatePriorities(jsonStrAdSourcesWithPriority,
                    (adManager.activity as AdUserDeviceDataBinder).getUserDeviceData())
                // init SDKs if required
                adManager.initAdSources()
            }

        }
        remoteConfig.listener = remoteConfigListener
    }

    override fun fetchAllData() {
        remoteConfig.fetchAllData()
    }

}