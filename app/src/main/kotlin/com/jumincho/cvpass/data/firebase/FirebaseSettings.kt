package com.jumincho.cvpass.data.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore

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

/**
 * Initialises Firebase from [settings] instead of a `google-services.json` file and returns
 * its Firestore instance. The app uses a named [FirebaseApp] so that it never conflicts
 * with Firebase's automatic initialisation.
 */
fun firestoreFor(context: Context, settings: FirebaseSettings): FirebaseFirestore {
    val app = FirebaseApp.getApps(context).firstOrNull { it.name == FIREBASE_APP_NAME }
        ?: FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setProjectId(settings.projectId)
                .setApplicationId(settings.applicationId)
                .setApiKey(settings.apiKey)
                .build(),
            FIREBASE_APP_NAME,
        )
    return FirebaseFirestore.getInstance(app)
}

private const val FIREBASE_APP_NAME = "cvpass"
