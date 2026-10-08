package com.jumincho.cvpass.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jumincho.cvpass.core.business.BusinessNumber
import com.jumincho.cvpass.core.business.BusinessRegistration
import com.jumincho.cvpass.core.venue.RegisteredVenue
import com.jumincho.cvpass.core.venue.VenueStore
import com.jumincho.cvpass.core.venue.VenueTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDate

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
