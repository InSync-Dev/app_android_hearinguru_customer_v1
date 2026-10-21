package com.hearinguru.app.data

import android.content.Context
import android.content.SharedPreferences


object UserProfileStore {


   private const val PREFS_NAME        = "hearinguru_user_profile"
   private const val KEY_AGE_GROUP     = "age_group"
   private const val KEY_IS_CONSENTED  = "is_consented"
   private const val KEY_CONSENT_DATE  = "consent_date"


   private fun prefs(context: Context): SharedPreferences =
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)


   fun getAgeGroup(context: Context): String =
      prefs(context).getString(KEY_AGE_GROUP, "") ?: ""

   fun isConsented(context: Context): Boolean =
      prefs(context).getBoolean(KEY_IS_CONSENTED, false)

   fun getConsentDate(context: Context): String =
      prefs(context).getString(KEY_CONSENT_DATE, "") ?: ""


   fun saveConsent(context: Context, ageGroup: String) {
      val consentDate = java.time.LocalDate.now().toString()

      prefs(context).edit()
         .putBoolean(KEY_IS_CONSENTED, true)
         .putString(KEY_AGE_GROUP,    ageGroup)
         .putString(KEY_CONSENT_DATE, consentDate)
         .apply()
   }

   fun updateAgeGroup(context: Context, ageGroup: String) {
      prefs(context).edit()
         .putString(KEY_AGE_GROUP, ageGroup)
         .apply()
   }

   fun clearConsent(context: Context) {
      prefs(context).edit()
         .putBoolean(KEY_IS_CONSENTED, false)
         .putString(KEY_AGE_GROUP,    "")
         .putString(KEY_CONSENT_DATE, "")
         .apply()
   }
}
