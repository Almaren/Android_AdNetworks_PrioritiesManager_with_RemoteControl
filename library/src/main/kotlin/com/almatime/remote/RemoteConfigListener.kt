package com.almatime.remote

/**
 * @author Alexander Khrapunsky
 * @version 1.0.0, 11.01.22.
 * @since 1.0.0
 */
interface RemoteConfigListener {

    fun onAdSourcesPriority(jsonStrAdSourcesWithPriority: String)

}