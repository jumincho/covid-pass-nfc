package com.jumincho.cvpass.data.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.jumincho.cvpass.FirebaseSettings
import com.jumincho.cvpass.core.business.BusinessNumber
import com.jumincho.cvpass.core.checkin.CheckIn
import com.jumincho.cvpass.core.checkin.CheckInRepository
import com.jumincho.cvpass.core.checkin.RetentionPolicy
import com.jumincho.cvpass.core.visitor.PhoneNumber
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * [CheckInRepository] on Cloud Firestore.
 *
 * Layout: `venues/{businessNumber}/checkins/{checkInId}` with the fields `visitorName`,
 * `phone`, `venueName`, `checkedInAt` (timestamp), `date` (`yyyy-MM-dd` in [zone], for
 * per-day queries) and `expireAt` (timestamp for a Firestore TTL policy that enforces
 * [RetentionPolicy] on the server).
 */
class FirestoreCheckInRepository(
    private val firestore: FirebaseFirestore,
    private val zone: ZoneId,
) : CheckInRepository {

    override suspend fun add(checkIn: CheckIn) {
        val write = checkIns(checkIn.venueId).document(checkIn.id).set(
            mapOf(
                "visitorName" to checkIn.visitorName,
                "phone" to checkIn.phone.digits,
                "venueName" to checkIn.venueName,
                "checkedInAt" to Timestamp(checkIn.checkedInAt),
                "date" to checkIn.checkedInAt.atZone(zone).toLocalDate().toString(),
                "expireAt" to Timestamp(checkIn.checkedInAt.plus(RetentionPolicy.RETENTION)),
            ),
        )
        // Firestore applies the write locally at once and syncs it when online. Waiting briefly
        // surfaces immediate failures (such as a rules rejection) without blocking offline check-ins.
        withTimeoutOrNull(WRITE_ACK_TIMEOUT_MILLIS) { write.await() }
    }

    override suspend fun checkInsOn(venueId: BusinessNumber, date: LocalDate): List<CheckIn> =
        checkIns(venueId).whereEqualTo("date", date.toString()).get().await()
            .documents
            .mapNotNull { it.toCheckIn(venueId) }
            .sortedBy { it.checkedInAt }

    override fun countOn(venueId: BusinessNumber, date: LocalDate): Flow<Int> = callbackFlow {
        val registration = checkIns(venueId).whereEqualTo("date", date.toString())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                } else if (snapshot != null) {
                    trySend(snapshot.size())
                }
            }
        awaitClose { registration.remove() }
    }

    /** Expiry is enforced server-side by a TTL policy on `expireAt`; clients never bulk-delete. */
    override suspend fun deleteOlderThan(cutoff: Instant) = Unit

    private fun checkIns(venueId: BusinessNumber): CollectionReference =
        firestore.collection("venues").document(venueId.digits).collection("checkins")

    private fun DocumentSnapshot.toCheckIn(venueId: BusinessNumber): CheckIn? {
        val phone = getString("phone")?.let(PhoneNumber::parse) ?: return null
        return CheckIn(
            id = id,
            venueId = venueId,
            venueName = getString("venueName").orEmpty(),
            visitorName = getString("visitorName") ?: return null,
            phone = phone,
            checkedInAt = getTimestamp("checkedInAt")?.toInstant() ?: return null,
        )
    }

    private companion object {
        const val WRITE_ACK_TIMEOUT_MILLIS = 5_000L
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
