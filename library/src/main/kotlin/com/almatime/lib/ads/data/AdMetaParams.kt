package com.almatime.lib.ads.data

import android.content.Context
import android.os.Bundle
import com.almatime.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
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
    val isUnderAgeOfConsent: Boolean,
    val privacyLaws: Set<DataPrivacyApplyLaw>?,
    val consentState: ConsentState
) {
    override fun toString() = "{ age: $age, isUnderAgeOfConsent: $isUnderAgeOfConsent, " +
            "laws: ${privacyLaws}, consentState: ${consentState}}"
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

/** Initialized params. Used in ad request before loading ad. */
val adRequestMetaParamsMap = mutableMapOf<AdSource, Any>()

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

    com.unity3d.ads.metadata.MetaData(context).apply {
        set("privacy.mode", "mixed") // This is a mixed audience game.
        commit()

        if (data.privacyLaws?.contains(DataPrivacyApplyLaw.GDPR) == true) {
            // false = If the user opts out of targeted advertising
            set("gdpr.consent", isConsentAgreed || isConsentNotRequired)
            commit()
        }

        if (data.privacyLaws?.contains(DataPrivacyApplyLaw.CCPA) == true) {
            // false = If the user opts out of targeted advertising
            set("privacy.consent", isConsentAgreed || isConsentNotRequired)
            commit()
        }

        // COPPA - childs under age 13 from all regions
        // This field is only required if privacy.mode is set to mixed
        // true - indicates that the user may not receive personalized ads.
        // false - indicates that the user may receive personalized ads.
        if (data.privacyLaws?.contains(DataPrivacyApplyLaw.COPPA) == true) {
            set("user.nonbehavioral", !isConsentAgreed || !isConsentNotRequired)
            commit()
        } else {
            set("user.nonbehavioral", false)
            commit()
        }
    }

    // ------------------------------------ AdMob -----------------------------------------------
    // https://developers.google.com/admob/android/targeting
    var reqConfig = MobileAds.getRequestConfiguration().toBuilder()

    if (data.privacyLaws?.contains(DataPrivacyApplyLaw.COPPA) == true) {

        reqConfig.setTagForChildDirectedTreatment(if (isConsentAgreed || isConsentNotRequired) {
            RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
        } else if (!isConsentAgreed && !isConsentNotRequired) {
            RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
        } else {
            RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        })
    }

    if (data.privacyLaws?.contains(DataPrivacyApplyLaw.GDPR) == true) {
        reqConfig.setTagForUnderAgeOfConsent(if (data.isUnderAgeOfConsent) {
            RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
        } else {
            RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE
        })
    }

    // Content rating filter settings specified through the Google Mobile Ads SDK override any settings configured using the AdMob UI.
    // Designed for Families (mixed audience) => PG
    reqConfig.setMaxAdContentRating(when (data.age) {
        in 3..12 -> RequestConfiguration.MAX_AD_CONTENT_RATING_PG
        in 13..17 -> RequestConfiguration.MAX_AD_CONTENT_RATING_T
        in 18..100 -> RequestConfiguration.MAX_AD_CONTENT_RATING_MA
        else -> { RequestConfiguration.MAX_AD_CONTENT_RATING_UNSPECIFIED }
    })

    // MobileAds.setRequestConfiguration(requestConfiguration) before AdRequest
    MobileAds.setRequestConfiguration(reqConfig.build())

    // The default behavior of the Google Mobile Ads SDK is to serve personalized ads.
    if (!isConsentNotRequired && !isConsentAgreed) {
        // non personalized ads
        val extras = Bundle()
        extras.putString("npa", "1")
        adRequestMetaParamsMap.put(AdSource.AdmobMediation, extras)
    }

    // ---------------------------------- UnityMediation ----------------------------------------
    /*val unityPrivacyLaw: com.unity3d.mediation.DataPrivacyLaw? = when (law) {
        DataPrivacyApplyLaw.GDPR -> com.unity3d.mediation.DataPrivacyLaw.GDPR
        DataPrivacyApplyLaw.CCPA -> com.unity3d.mediation.DataPrivacyLaw.CCPA
        else -> null
    }
    unityPrivacyLaw?.let {
        com.unity3d.mediation.DataPrivacy.userGaveConsent(unityMediationConsent, it, context)
    }*/

    if (Log.DEBUG) {
        Log.i("ads", "received consentData=${data}, isConsentAgreed=$isConsentAgreed, " +
                "isConsentNotRequired=$isConsentNotRequired")
        Log.i("ads", "AdMob content rating: ${MobileAds.getRequestConfiguration().maxAdContentRating}" +
                ", ${MobileAds.getRequestConfiguration().tagForChildDirectedTreatment}, " +
                "${MobileAds.getRequestConfiguration().tagForUnderAgeOfConsent}")
    }

    //setPrivacyMetadataForSuperAwesome(context)
}

/*private fun setPrivacyMetadataForSuperAwesome(context: Context) {
    getUserDob()?.let {
        Log.i("ads", "SuperAwesome sent dob = $it")
        // a date of birth in YYYY-MM-DD format
        AwesomeAds.triggerAgeCheck(context, it) { isMinorModel ->
            if (isMinorModel != null) {
                // relevant values in the model
                val country = isMinorModel.getCountry()
                val consentAge = isMinorModel.getConsentAgeForCountry()
                val userAge = isMinorModel.getAge()
                val isMinor = isMinorModel.isMinor()
                if (Log.DEBUG) {
                    Log.i("ads", "SuperAwesome received minorData: country = " + country
                            + ", consentAge = " + consentAge + ", userAge = " + userAge + ", isMinor = " + isMinor)
                }
            }
        }
    }
}*/

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
