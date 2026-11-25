package com.qqfarm.mobile.platform

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
private fun prefsFile(): okio.Path {
    val dir = NSFileManager.defaultManager.URLForDirectory(
        NSDocumentDirectory, NSUserDomainMask, null, true, null,
    ) ?: throw IllegalStateException("no documents dir")
    return (dir.path + "/qq_farm_mobile.preferences_pb").toPath()
}

actual fun createPreferencesDataStore(): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { prefsFile() })
