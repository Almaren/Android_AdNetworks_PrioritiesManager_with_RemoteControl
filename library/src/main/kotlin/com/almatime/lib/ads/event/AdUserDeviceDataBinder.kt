package com.almatime.lib.ads.event

import com.almatime.lib.ads.data.UserDeviceData

/**
 * You must implement this SAM! Passes relevant updated data to AdManager on request.
 *
 * @author Alexander Khrapunsky
 * @version 1.0.0, 04.01.22.
 * @since 1.0.0
 */
fun interface AdUserDeviceDataBinder {

    fun getUserDeviceData(): UserDeviceData

}