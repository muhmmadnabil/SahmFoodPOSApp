package com.sahm.pos.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

internal actual fun createCurrentUserDataStore(
    platformContext: PlatformContext,
): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            platformContext.context.filesDir
                .resolve(CURRENT_USER_PREFS_FILE)
                .absolutePath
                .toPath()
        },
    )
