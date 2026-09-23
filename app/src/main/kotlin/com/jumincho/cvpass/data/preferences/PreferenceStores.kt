package com.jumincho.cvpass.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jumincho.cvpass.core.business.BusinessNumber
import com.jumincho.cvpass.core.business.BusinessRegistration
import com.jumincho.cvpass.core.pass.VaccinationRecord
import com.jumincho.cvpass.core.pass.VerifiedPass
import com.jumincho.cvpass.core.venue.RegisteredVenue
import com.jumincho.cvpass.core.venue.VenueStore
import com.jumincho.cvpass.core.venue.VenueTag
import com.jumincho.cvpass.core.visitor.PersonName
import com.jumincho.cvpass.core.visitor.PhoneNumber
import com.jumincho.cvpass.core.visitor.VisitorProfile
import com.jumincho.cvpass.core.visitor.VisitorStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
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

/** [VenueStore] on Jetpack DataStore. */
class PreferencesVenueStore(private val dataStore: DataStore<Preferences>) : VenueStore {

    override val venue: Flow<RegisteredVenue?> =
        dataStore.safeData.map { it.venue() }.distinctUntilChanged()

    override suspend fun save(venue: RegisteredVenue) {
        dataStore.edit {
            it[NUMBER] = venue.tag.businessNumber.digits
            it[NAME] = venue.tag.name
            it[REPRESENTATIVE] = venue.registration.representativeName
            it[OPENING_EPOCH_DAY] = venue.registration.openingDate.toEpochDay()
            it[VERIFIED] = venue.verifiedWithTaxService
        }
    }

    override suspend fun clear() {
        dataStore.edit {
            it.remove(NUMBER)
            it.remove(NAME)
            it.remove(REPRESENTATIVE)
            it.remove(OPENING_EPOCH_DAY)
            it.remove(VERIFIED)
        }
    }

    private fun Preferences.venue(): RegisteredVenue? {
        val number = this[NUMBER]?.let(BusinessNumber::parse) ?: return null
        val name = this[NAME]?.let(VenueTag::normalizeName) ?: return null
        val representative = this[REPRESENTATIVE] ?: return null
        val openingDate = this[OPENING_EPOCH_DAY]?.let(LocalDate::ofEpochDay) ?: return null
        return RegisteredVenue(
            tag = VenueTag(number, name),
            registration = BusinessRegistration(number, representative, openingDate),
            verifiedWithTaxService = this[VERIFIED] ?: false,
        )
    }

    private companion object {
        val NUMBER = stringPreferencesKey("venue_business_number")
        val NAME = stringPreferencesKey("venue_name")
        val REPRESENTATIVE = stringPreferencesKey("venue_representative")
        val OPENING_EPOCH_DAY = longPreferencesKey("venue_opening_epoch_day")
        val VERIFIED = booleanPreferencesKey("venue_verified_with_tax_service")
    }
}

/** DataStore reads that treat an unreadable file as empty instead of crashing the UI. */
private val DataStore<Preferences>.safeData: Flow<Preferences>
    get() = data.catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
