package com.example.classschedule

import com.example.classschedule.data.resolveOnboardingComplete
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryCompatibilityTest {
    @Test
    fun restoresAffectedV11SemesterWithValidPersistedFields() {
        assertTrue(
            resolveOnboardingComplete(
                storedValue = false,
                schemaVersion = 1,
                termName = "2026 秋季学期",
                firstMondayText = "2026-09-14",
                startText = "2026-09-14",
                endText = "2027-02-14"
            )
        )
    }

    @Test
    fun doesNotRestoreFreshInstallation() {
        assertFalse(
            resolveOnboardingComplete(
                storedValue = null,
                schemaVersion = null,
                termName = null,
                firstMondayText = null,
                startText = null,
                endText = null
            )
        )
    }

    @Test
    fun doesNotRestoreInvalidLegacyTerm() {
        assertFalse(
            resolveOnboardingComplete(
                storedValue = false,
                schemaVersion = 1,
                termName = "2026 秋季学期",
                firstMondayText = "2026-09-15",
                startText = "2026-09-14",
                endText = "2027-02-14"
            )
        )
    }
}
