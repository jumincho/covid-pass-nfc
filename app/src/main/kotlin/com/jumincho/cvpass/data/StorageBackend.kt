package com.jumincho.cvpass.data

/** Where the venue-side visitor log lives. */
sealed interface StorageBackend {
    /** A Room database on this device, so the whole flow works on one phone. */
    data object Device : StorageBackend

    /** Cloud Firestore in the given project. */
    data class Firestore(val projectId: String) : StorageBackend
}
