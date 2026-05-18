package com.sahm.pos.utils

fun Long.toUtcDateTimeText(): String {
    val totalSeconds = floorDiv(1000L)
    val days = totalSeconds.floorDiv(SecondsPerDay)
    val secondsOfDay = totalSeconds.floorMod(SecondsPerDay)
    val date = civilFromDays(days)
    val hour = secondsOfDay / SecondsPerHour
    val minute = (secondsOfDay % SecondsPerHour) / SecondsPerMinute

    return buildString {
        append(date.year.toString().padStart(4, '0'))
        append('-')
        append(date.month.toString().padStart(2, '0'))
        append('-')
        append(date.day.toString().padStart(2, '0'))
        append(' ')
        append(hour.toString().padStart(2, '0'))
        append(':')
        append(minute.toString().padStart(2, '0'))
        append(" UTC")
    }
}

private const val SecondsPerMinute = 60L
private const val SecondsPerHour = 60L * SecondsPerMinute
private const val SecondsPerDay = 24L * SecondsPerHour

private data class CivilDate(
    val year: Int,
    val month: Int,
    val day: Int,
)

private fun civilFromDays(daysSinceUnixEpoch: Long): CivilDate {
    val shiftedDays = daysSinceUnixEpoch + 719468
    val era = if (shiftedDays >= 0) shiftedDays / 146097 else (shiftedDays - 146096) / 146097
    val dayOfEra = shiftedDays - era * 146097
    val yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 - dayOfEra / 146096) / 365
    val year = yearOfEra + era * 400
    val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
    val monthPrime = (5 * dayOfYear + 2) / 153
    val day = dayOfYear - (153 * monthPrime + 2) / 5 + 1
    val month = monthPrime + if (monthPrime < 10) 3 else -9
    val adjustedYear = year + if (month <= 2) 1 else 0

    return CivilDate(
        year = adjustedYear.toInt(),
        month = month.toInt(),
        day = day.toInt(),
    )
}

private fun Long.floorDiv(other: Long): Long {
    val quotient = this / other
    val remainder = this % other
    return if (remainder != 0L && ((this xor other) < 0)) quotient - 1 else quotient
}

private fun Long.floorMod(other: Long): Long =
    this - floorDiv(other) * other
