package com.sahm.pos.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.sahm.pos.data.local.database.SahmPosDatabase

private const val DATABASE_NAME = "sahm_pos.db"

internal actual fun createDatabaseDriver(platformContext: PlatformContext): SqlDriver =
    NativeSqliteDriver(SahmPosDatabase.Schema, DATABASE_NAME)
