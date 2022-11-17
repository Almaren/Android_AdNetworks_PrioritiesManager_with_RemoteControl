package com.almatime.lib.ads.data

/**
 * @author Alexander Khrapunsky
 * @version 1.0.0, 04.01.22.
 * @since 1.0.0
 */
enum class AppLangCode {
    EN,
    PT,
    ES,
    UK,
    DE,
    FR,
    IT,
    AR
}

enum class Gender {
    MALE, FEMALE, UNKNOWN
}

data class UserDeviceData(
    val age: Int,
    val gender: Gender,
    val appLangCode: AppLangCode,
    val localeCountryIso2: String?,
    val isDeviceTablet: Boolean
) {
    override fun toString() = "{ age: $age, appLangCode: $appLangCode, " +
            "country=$localeCountryIso2, tablet=$isDeviceTablet }"
}
