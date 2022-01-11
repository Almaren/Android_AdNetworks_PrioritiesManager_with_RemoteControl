package com.almatime.remote

import com.almatime.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig

/**
 * The default minimum fetch interval for Remote Config is 12 hours.
 *
 * @author Alexander Khrapunsky
 * @version 1.0.0, 19-Nov-20.
 * @since 1.0.0
 */
class RemoteConfig {

    private lateinit var remoteConfig: FirebaseRemoteConfig
    var listener: RemoteConfigListener? = null

    init {
        Log.info { "RemoteConfig INIT" }
        try {
            remoteConfig = Firebase.remoteConfig
        } catch (e: Exception) {
            Log.e(e)
        }
        // for test: avoid throttling (more than 5 requests per 60m)
        /*val configSettings = remoteConfigSettings {
            this.minimumFetchIntervalInSeconds = 60
        }
        remoteConfig.setConfigSettingsAsync(configSettings)*/
    }

    fun fetchAllData() {
        Log.info { "RemoteConfig FETCH DATA" }
        if (!::remoteConfig.isInitialized) return
        try {
            remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val jsonAdSources = remoteConfig.getString("adSourcesPriority")

                    listener?.run {
                        onAdSourcesPriority(jsonAdSources)
                    }
                } else {
                    Log.warn { "RemoteConfig: fetching adSourcesPriority failed task." }
                }
            }
        } catch (e: Exception) {
            Log.e(e)
        }
    }

    // for test
    /*fun fetchAllData() {
        val jsonAdSources = "{\"IronSrc\":[-1,-1],\"Kidoz\":[-1,-1],\"UnityAds\":[1,1],\"Awesome\":[2,2],\"NewTestNotInclude\":[4,4]}"
        listener?.run {
            onAdSourcesPriority(jsonAdSources)
        }
    }*/

}
