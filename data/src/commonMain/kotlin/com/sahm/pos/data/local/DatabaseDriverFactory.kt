package com.sahm.pos.data.local

import app.cash.sqldelight.db.SqlDriver

internal expect fun createDatabaseDriver(platformContext: PlatformContext): SqlDriver
