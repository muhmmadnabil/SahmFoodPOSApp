package com.sahm.pos.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

internal expect fun createCurrentUserDataStore(
    platformContext: PlatformContext,
): DataStore<Preferences>
