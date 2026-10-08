package com.jumincho.cvpass.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jumincho.cvpass.core.pass.VaccinationRecord
import com.jumincho.cvpass.core.pass.VerifiedPass
import com.jumincho.cvpass.core.visitor.PersonName
import com.jumincho.cvpass.core.visitor.PhoneNumber
import com.jumincho.cvpass.core.visitor.VisitorProfile
import com.jumincho.cvpass.core.visitor.VisitorStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

/**
 * [VisitorStore] on Jetpack DataStore. The pass is kept as the derived facts only (doses,
 * primary series, last dose date and verification time), never as an image or OCR text.
 */
class PreferencesVisitorStore(private val dataStore: DataStore<Preferences>) : VisitorStore {

    override val profile: Flow<VisitorProfile?> =
        dataStore.safeData.map { it.profile() }.distinctUntilChanged()

    override val pass: Flow<VerifiedPass?> =
        dataStore.safeData.map { it.pass() }.distinctUntilChanged()

    override suspend fun saveProfile(profile: VisitorProfile) {
        dataStore.edit {
            it[NAME] = profile.name
            it[PHONE] = profile.phone.digits
        }
    }

    override suspend fun savePass(pass: VerifiedPass) {
        dataStore.edit {
            it[DOSES] = pass.record.doses
            it[PRIMARY_SERIES_DOSES] = pass.record.primarySeriesDoses
            it[LAST_DOSE_EPOCH_DAY] = pass.record.lastDoseDate.toEpochDay()
            it[VERIFIED_AT_MILLIS] = pass.verifiedAt.toEpochMilli()
        }
    }

    override suspend fun clearPass() {
        dataStore.edit {
            it.remove(DOSES)
            it.remove(PRIMARY_SERIES_DOSES)
            it.remove(LAST_DOSE_EPOCH_DAY)
            it.remove(VERIFIED_AT_MILLIS)
        }
    }

    private fun Preferences.profile(): VisitorProfile? {
        val name = this[NAME]?.let(PersonName::normalize) ?: return null
        val phone = this[PHONE]?.let(PhoneNumber::parse) ?: return null
        return VisitorProfile(name, phone)
    }

    private fun Preferences.pass(): VerifiedPass? {
        val doses = this[DOSES] ?: return null
        val primarySeriesDoses = this[PRIMARY_SERIES_DOSES] ?: return null
        val lastDose = this[LAST_DOSE_EPOCH_DAY] ?: return null
        val verifiedAt = this[VERIFIED_AT_MILLIS] ?: return null
        if (doses < 1 || primarySeriesDoses !in 1..2) return null
        return VerifiedPass(
            VaccinationRecord(doses, primarySeriesDoses, LocalDate.ofEpochDay(lastDose)),
            Instant.ofEpochMilli(verifiedAt),
        )
    }

    private companion object {
        val NAME = stringPreferencesKey("visitor_name")
        val PHONE = stringPreferencesKey("visitor_phone")
        val DOSES = intPreferencesKey("pass_doses")
        val PRIMARY_SERIES_DOSES = intPreferencesKey("pass_primary_series_doses")
        val LAST_DOSE_EPOCH_DAY = longPreferencesKey("pass_last_dose_epoch_day")
        val VERIFIED_AT_MILLIS = longPreferencesKey("pass_verified_at_millis")
    }
}
