package com.jumincho.cvpass

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.jumincho.cvpass.core.business.NtsBusinessRegistry
import com.jumincho.cvpass.core.checkin.CheckInRepository
import com.jumincho.cvpass.core.checkin.CheckInService
import com.jumincho.cvpass.core.checkin.VisitHistory
import com.jumincho.cvpass.core.inspector.InspectorGate
import com.jumincho.cvpass.core.venue.VenueRegistrar
import com.jumincho.cvpass.core.venue.VenueStore
import com.jumincho.cvpass.core.visitor.VisitorStore
import com.jumincho.cvpass.data.firebase.FirestoreCheckInRepository
import com.jumincho.cvpass.data.firebase.firestoreFor
import com.jumincho.cvpass.data.local.CvPassDatabase
import com.jumincho.cvpass.data.local.LocalCheckInRepository
import com.jumincho.cvpass.data.local.LocalVisitHistory
import com.jumincho.cvpass.data.preferences.PreferencesVenueStore
import com.jumincho.cvpass.data.preferences.PreferencesVisitorStore
import com.jumincho.cvpass.ocr.CertificateTextReader
import com.jumincho.cvpass.ocr.MlKitCertificateTextReader
import okhttp3.OkHttpClient
import java.time.Clock
import java.util.concurrent.TimeUnit

/** Where the venue-side visitor log lives. */
sealed interface StorageBackend {
    /** A Room database on this device, so the whole flow works on one phone. */
    data object Device : StorageBackend

    /** Cloud Firestore in the given project. */
    data class Firestore(val projectId: String) : StorageBackend
}

/** Manual dependency injection: one instance per process, owned by [CvPassApplication]. */
class AppContainer(context: Context, val config: AppConfig) {

    private val appContext = context.applicationContext

    /** Wall clock in the device's time zone; per-day queries use this zone. */
    val clock: Clock = Clock.systemDefaultZone()

    val storage: StorageBackend =
        config.firebase?.let { StorageBackend.Firestore(it.projectId) } ?: StorageBackend.Device

    private val database: CvPassDatabase by lazy { CvPassDatabase.build(appContext) }

    private val preferences: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create { appContext.preferencesDataStoreFile("cvpass") }
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build()
    }

    val checkIns: CheckInRepository by lazy {
        val firebase = config.firebase
        if (firebase != null) {
            FirestoreCheckInRepository(firestoreFor(appContext, firebase), clock.zone)
        } else {
            LocalCheckInRepository(database.checkInDao(), clock.zone)
        }
    }

    val visits: VisitHistory by lazy { LocalVisitHistory(database.visitDao()) }

    val visitorStore: VisitorStore by lazy { PreferencesVisitorStore(preferences) }

    val venueStore: VenueStore by lazy { PreferencesVenueStore(preferences) }

    val checkInService: CheckInService by lazy { CheckInService(checkIns, visits, clock) }

    val venueRegistrar: VenueRegistrar by lazy {
        VenueRegistrar(config.businessApiKey.takeIf { it.isNotBlank() }?.let { NtsBusinessRegistry(it, httpClient) })
    }

    val inspectorGate: InspectorGate = InspectorGate(config.inspectorPin)

    val certificateReader: CertificateTextReader by lazy { MlKitCertificateTextReader(appContext) }
}
