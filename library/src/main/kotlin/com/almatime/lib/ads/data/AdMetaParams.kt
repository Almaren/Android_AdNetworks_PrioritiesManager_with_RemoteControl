package com.almatime.lib.ads.data

import android.content.Context
import com.almatime.util.Log
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

/**
 * Ad meta data params such as GDPR, COPPA, CCPA, gender, location, age...
 *
 * @author Alexander Khrapunsky
 * @version 1.0.0, 29.12.21.
 * @since 1.0.0
 */
class ConsentUserData(
    val age: Int,
    val privacyLaws: Set<DataPrivacyApplyLaw>?,
    val consentState: ConsentState
) {
    override fun toString() = "{ age: $age, laws: ${privacyLaws}, consentState: ${consentState}}"
}

enum class ConsentState {
    UNKNOWN, NOT_REQUIRED, SHOW_DIALOG, AGREED, DISAGREED
}

/**
 * COPPA - directed to children under age 13 app that collect, use, or disclose personal information from children.
 * LGPD - Brazil.
 * PIPL - Personal Information Protection Law, regarding ad personalization, applicable to users residing in China.
 */
enum class DataPrivacyApplyLaw {
    GDPR, CCPA, COPPA, LGPD, PIPL
}

/**
 * Sets user metadata to ad networks such as GDPR, COPPA...
 * <b>For IronSource setMetaData must be called before SDK initialization!</b>
 * @param privacyLaws null if none
 */
fun initMetaDataParams(context: Context, data: ConsentUserData) {
    val isConsentAgreed = data.consentState != ConsentState.DISAGREED
    val isConsentNotRequired = data.consentState in setOf(ConsentState.NOT_REQUIRED, ConsentState.UNKNOWN)

    // ---------------------------------- UnityMediation ----------------------------------------
    val unityMediationConsent: com.unity3d.mediation.ConsentStatus = when {
        isConsentAgreed || isConsentNotRequired -> com.unity3d.mediation.ConsentStatus.GIVEN
        !isConsentAgreed -> com.unity3d.mediation.ConsentStatus.DENIED
        else -> com.unity3d.mediation.ConsentStatus.NOT_DETERMINED
    }

    if (data.privacyLaws != null) {
        for (law in data.privacyLaws) {
            // ------------------------------------ Unity ----------------------------------------------
            // If you've already implemented the gdpr API to solicit consent, you can also use it for CCPA compliance
            // by extending your implementation to CCPA-affected users. Similarly, the privacy API can apply to GDPR
            // when extended to affected users.
            com.unity3d.ads.metadata.MetaData(context).apply {
                when (law) {
                    DataPrivacyApplyLaw.GDPR -> {
                        set("gdpr.consent", isConsentAgreed || isConsentNotRequired)
                        commit()
                    }
                    DataPrivacyApplyLaw.CCPA -> {
                        set("privacy.consent", isConsentAgreed || isConsentNotRequired)
                        commit()
                    }
                    else -> {}
                }
            }

            // ---------------------------------- UnityMediation ----------------------------------------
            val unityPrivacyLaw: com.unity3d.mediation.DataPrivacyLaw? = when (law) {
                DataPrivacyApplyLaw.GDPR -> com.unity3d.mediation.DataPrivacyLaw.GDPR
                DataPrivacyApplyLaw.CCPA -> com.unity3d.mediation.DataPrivacyLaw.CCPA
                else -> null
            }
            unityPrivacyLaw?.let {
                com.unity3d.mediation.DataPrivacy.userGaveConsent(unityMediationConsent, it, context)
            }
        }
    }

    // Age limit based on my dashboard settings "This app is directed to children under the age of 13."
    // Ad Controls -> age limits -> "Do not show ads rated 13+".
    // set option: "privacy.useroveragelimit" = false if under age limit.
    if (data.age >= 13) {
        com.unity3d.ads.metadata.MetaData(context).apply {
            set("privacy.useroveragelimit", true)
            commit()
        }
    }

    if (Log.DEBUG) {
        Log.i("ads", "received consentData=${data}, isConsentAgreed=$isConsentAgreed, " +
                "isConsentNotRequired=$isConsentNotRequired")
    }
}

// a date of birth in YYYY-MM-DD format
private fun getUserDob(age: Int): String? {
    try {
        val strTodayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val strCurrYear = strTodayDate.substring(0, 4)
        val currYear = Integer.valueOf(strCurrYear)
        val yearOfBirth = currYear - age
        return yearOfBirth.toString() + strTodayDate.substring(4)
    } catch (e: Exception) {
    }
    return null
}