package com.example.classschedule.data

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * v1.1 could mark a configured semester as unfinished when it was saved from Settings.
 * A valid persisted semester is sufficient evidence that this is an affected existing user,
 * while a fresh install has neither a schema version nor stored term fields.
 */
internal fun resolveOnboardingComplete(
    storedValue: Boolean?,
    schemaVersion: Int?,
    termName: String?,
    firstMondayText: String?,
    startText: String?,
    endText: String?
): Boolean {
    if (storedValue == true) return true
    if ((schemaVersion ?: 0) < 1 || termName.isNullOrBlank()) return false
    val firstMonday = firstMondayText?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return false
    val start = startText?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return false
    val end = endText?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return false
    return firstMonday.dayOfWeek == DayOfWeek.MONDAY && !firstMonday.isBefore(start) && !end.isBefore(start) && !end.isBefore(firstMonday)
}
