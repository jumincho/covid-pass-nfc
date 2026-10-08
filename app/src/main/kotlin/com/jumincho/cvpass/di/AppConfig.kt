package com.jumincho.cvpass.di

import com.jumincho.cvpass.BuildConfig
import com.jumincho.cvpass.data.firebase.FirebaseSettings

/**
 * Optional integrations, configured through `local.properties` at build time. Every one of
 * them has an honest fallback so the app runs without any keys.
 */
data class AppConfig(
    /** data.go.kr service key for the National Tax Service API; blank means checksum-only checks. */
    val businessApiKey: String,
    /** Code that unlocks inspector mode; blank disables it. */
    val inspectorPin: String,
    /** Firestore backend, or `null` to keep check-ins on the device. */
    val firebase: FirebaseSettings?,
) {
    companion object {
        /** The configuration compiled into this build. */
        fun fromBuildConfig(): AppConfig = AppConfig(
            businessApiKey = BuildConfig.BUSINESS_API_KEY,
            inspectorPin = BuildConfig.INSPECTOR_PIN,
            firebase = FirebaseSettings.of(
                projectId = BuildConfig.FIREBASE_PROJECT_ID,
                applicationId = BuildConfig.FIREBASE_APPLICATION_ID,
                apiKey = BuildConfig.FIREBASE_API_KEY,
            ),
        )
    }
}
