package com.example.classschedule

import com.example.classschedule.data.CourseEntity
import com.example.classschedule.data.MeetingEntity
import com.example.classschedule.domain.ResolvedMeeting
import com.example.classschedule.domain.TimeSlot
import com.example.classschedule.domain.allocateCourseColor
import com.example.classschedule.domain.placeMeetingLanes
import com.example.classschedule.domain.slotSpanFor
import com.example.classschedule.domain.resolveMeeting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeekLayoutTest {
    private val slots = listOf(
        TimeSlot(1, 1, 8 * 60, 8 * 60 + 45),
        TimeSlot(2, 2, 8 * 60 + 45, 9 * 60 + 50),
        TimeSlot(3, 3, 9 * 60 + 50, 10 * 60 + 35),
        TimeSlot(4, 4, 10 * 60 + 35, 11 * 60 + 20),
        TimeSlot(5, 5, 11 * 60 + 20, 12 * 60)
    )

    @Test
    fun longMeetingCoversEveryIntersectingRow() {
        val span = slotSpanFor(9 * 60 + 50, 12 * 60, slots)!!
        assertEquals(2, span.firstIndex)
        assertEquals(4, span.lastIndex)
        assertEquals(3, span.rowCount)
        assertEquals(2f, span.topPosition)
        assertEquals(5f, span.bottomPosition)
    }

    @Test
    fun slotIdMeetingUsesResolvedCurrentTimes() {
        val meeting = MeetingEntity(courseId = 1, weekday = 1, startSlotId = 3, endSlotId = 5)
        val times = resolveMeeting(meeting, slots)!!
        assertEquals(9 * 60 + 50, times.first)
        assertEquals(12 * 60, times.second)
        assertEquals(3, slotSpanFor(times.first, times.second, slots)!!.rowCount)
    }

    @Test
    fun midRowEndInterpolatesInsteadOfDroppingTheLastRow() {
        val span = slotSpanFor(9 * 60 + 50, 11 * 60 + 30, slots)!!
        assertEquals(4, span.lastIndex)
        assertTrue(span.bottomPosition > 4f)
        assertTrue(span.bottomPosition < 5f)
    }

    @Test
    fun overlappingMeetingsGetSeparateVisibleLanes() {
        fun item(id: Long, start: Int, end: Int) = ResolvedMeeting(
            CourseEntity(id = id, name = "课程$id"),
            MeetingEntity(id = id, courseId = id, weekday = 1, startMinutes = start, endMinutes = end),
            start,
            end
        )
        val placements = placeMeetingLanes(listOf(item(1, 540, 600), item(2, 570, 630), item(3, 630, 680)))
        assertEquals(3, placements.size)
        assertEquals(2, placements.maxOf { it.laneCount })
        assertNotEquals(placements[0].lane, placements[1].lane)
        assertEquals(0, placements[2].lane)
    }

    @Test
    fun automaticColorsAreStableAndAvoidUsedColors() {
        val used = listOf("#2F7D6D", "#4D6F9F")
        val first = allocateCourseColor("网络空间安全", used)
        val second = allocateCourseColor("网络空间安全", used)
        assertEquals(first, second)
        assertTrue(first.uppercase() !in used.map { it.uppercase() })
    }
}
