package com.sahm.pos.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import platform.Foundation.NSHomeDirectory

internal actual fun createCurrentUserDataStore(
    platformContext: PlatformContext,
): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            "${NSHomeDirectory()}/Documents/$CURRENT_USER_PREFS_FILE".toPath()
        },
    )
