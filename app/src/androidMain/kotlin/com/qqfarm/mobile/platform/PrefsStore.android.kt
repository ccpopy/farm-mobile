package com.qqfarm.mobile.platform

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile

actual fun createPreferencesDataStore(): DataStore<Preferences> {
    val context = PlatformGlobals.appContext as? Context
        ?: throw IllegalStateException("Application context not initialized")
    return androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("qq_farm_mobile_prefs") },
    )
}
