package com.jumincho.cvpass.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.IOException

/**
 * Creates the DataStore that [PreferencesVisitorStore] and [PreferencesVenueStore] share.
 * DataStore allows one instance per file, so the app must call this only once.
 */
fun createPreferencesDataStore(context: Context): DataStore<Preferences> =
    PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(FILE_NAME) }

/** DataStore reads that treat an unreadable file as empty instead of crashing the UI. */
internal val DataStore<Preferences>.safeData: Flow<Preferences>
    get() = data.catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }

/** Never rename: existing installs keep the visitor's profile, pass and venue in this file. */
private const val FILE_NAME = "cvpass"
