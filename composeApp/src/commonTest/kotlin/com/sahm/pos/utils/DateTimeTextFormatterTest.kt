package com.sahm.pos.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeTextFormatterTest {

    @Test
    fun formatsUnixEpochAsDateAndTime() {
        assertEquals("1970-01-01 00:00 UTC", 0L.toUtcDateTimeText())
    }

    @Test
    fun formatsEpochMillisAsDateAndTime() {
        assertEquals("1970-01-02 00:00 UTC", 86_400_000L.toUtcDateTimeText())
    }
}
