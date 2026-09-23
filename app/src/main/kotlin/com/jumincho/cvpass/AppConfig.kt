package com.jumincho.cvpass

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

/** The three values Firebase needs when it is initialised without `google-services.json`. */
data class FirebaseSettings(val projectId: String, val applicationId: String, val apiKey: String) {
    companion object {
        /** Returns settings if all three values are present, otherwise `null`. */
        fun of(projectId: String, applicationId: String, apiKey: String): FirebaseSettings? =
            if (projectId.isNotBlank() && applicationId.isNotBlank() && apiKey.isNotBlank()) {
                FirebaseSettings(projectId.trim(), applicationId.trim(), apiKey.trim())
            } else {
                null
            }
    }
}
