package com.example.classschedule

import com.example.classschedule.ui.almanacForDate
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlmanacTest {
    @Test
    fun providesOfflineYiAndJiForCurrentSemesterDate() {
        val almanac = almanacForDate(LocalDate.of(2026, 9, 18))

        assertEquals("农历八月初八", almanac.lunarDateLabel)
        assertTrue(almanac.detailAvailable)
        assertFalse(almanac.dayYi.isEmpty())
        assertFalse(almanac.dayJi.isEmpty())
    }

    @Test
    fun calculatesBeyondFourYearsWithoutBundledDateTables() {
        val almanac = almanacForDate(LocalDate.of(2036, 9, 18))

        assertTrue(almanac.detailAvailable)
        assertFalse(almanac.dayYi.isEmpty())
        assertFalse(almanac.dayJi.isEmpty())
    }
}
