package com.example.classschedule.domain

import com.example.classschedule.data.CourseEntity
import com.example.classschedule.data.CourseWithMeetings
import com.example.classschedule.data.MeetingEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

enum class Stage(val key: String, val label: String, val order: Int) {
    MORNING("morning", "上午", 0),
    AFTERNOON("afternoon", "下午", 1),
    EVENING("evening", "晚上", 2);

    companion object {
        fun fromKey(key: String): Stage = entries.firstOrNull { it.key == key } ?: MORNING
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class TimeSlot(
    val id: Long,
    val number: Int,
    val startMinutes: Int,
    val endMinutes: Int
)

data class PhaseReminder(
    val stage: Stage,
    val enabled: Boolean,
    val boundaryStart: Int,
    val boundaryEnd: Int,
    val notifyHour: Int,
    val notifyMinute: Int,
    val noClassReminder: Boolean
)

data class ReminderConfig(
    val morning: PhaseReminder,
    val afternoon: PhaseReminder,
    val evening: PhaseReminder
) {
    val all: List<PhaseReminder> get() = listOf(morning, afternoon, evening)

    fun forStage(stage: Stage): PhaseReminder = when (stage) {
        Stage.MORNING -> morning
        Stage.AFTERNOON -> afternoon
        Stage.EVENING -> evening
    }

    fun withPhase(phase: PhaseReminder): ReminderConfig = when (phase.stage) {
        Stage.MORNING -> copy(morning = phase)
        Stage.AFTERNOON -> copy(afternoon = phase)
        Stage.EVENING -> copy(evening = phase)
    }

    companion object {
        fun defaults() = ReminderConfig(
            morning = PhaseReminder(Stage.MORNING, true, 0, 12 * 60, 7, 0, false),
            afternoon = PhaseReminder(Stage.AFTERNOON, true, 12 * 60, 18 * 60, 12, 0, false),
            evening = PhaseReminder(Stage.EVENING, true, 18 * 60, 24 * 60, 18, 0, false)
        )
    }
}

data class TermSettings(
    val name: String,
    val firstMonday: LocalDate,
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class AppSettings(
    val onboardingComplete: Boolean = false,
    val term: TermSettings = defaultTerm(),
    val timeSlots: List<TimeSlot> = defaultTimeSlots(),
    val timeSlotsConfigured: Boolean = false,
    val reminderConfig: ReminderConfig = ReminderConfig.defaults(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = false,
    val weekZoom: Float = 1f,
    val showAlmanac: Boolean = true
)

data class ResolvedMeeting(
    val course: CourseEntity,
    val meeting: MeetingEntity,
    val startMinutes: Int,
    val endMinutes: Int
)

fun defaultTerm(today: LocalDate = LocalDate.now()): TermSettings {
    val monday = today.with(DayOfWeek.MONDAY)
    return TermSettings("2026 秋季学期", monday, monday, monday.plusMonths(5))
}

fun defaultTimeSlots(): List<TimeSlot> = listOf(
    TimeSlot(1, 1, 8 * 60, 8 * 60 + 45),
    TimeSlot(2, 2, 8 * 60 + 55, 9 * 60 + 40),
    TimeSlot(3, 3, 10 * 60, 10 * 60 + 45),
    TimeSlot(4, 4, 10 * 60 + 55, 11 * 60 + 40),
    TimeSlot(5, 5, 14 * 60, 14 * 60 + 45),
    TimeSlot(6, 6, 14 * 60 + 55, 15 * 60 + 40),
    TimeSlot(7, 7, 16 * 60, 16 * 60 + 45),
    TimeSlot(8, 8, 16 * 60 + 55, 17 * 60 + 40),
    TimeSlot(9, 9, 19 * 60, 19 * 60 + 45),
    TimeSlot(10, 10, 19 * 60 + 55, 20 * 60 + 40)
)

fun formatMinutes(value: Int): String = "%02d:%02d".format(value / 60, value % 60)

fun parseClock(value: String): Int? {
    val match = Regex("^(\\d{1,2}):(\\d{2})$").matchEntire(value.trim()) ?: return null
    val hour = match.groupValues[1].toIntOrNull() ?: return null
    val minute = match.groupValues[2].toIntOrNull() ?: return null
    return when {
        hour == 24 && minute == 0 -> 24 * 60
        hour in 0..23 && minute in 0..59 -> hour * 60 + minute
        else -> null
    }
}

fun dayLabel(day: Int): String = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
    .getOrElse(day) { "周?" }

fun stageFor(startMinutes: Int, config: ReminderConfig): Stage {
    return config.all.firstOrNull { startMinutes >= it.boundaryStart && startMinutes < it.boundaryEnd }?.stage
        ?: if (startMinutes < config.afternoon.boundaryStart) Stage.MORNING else Stage.EVENING
}

fun weekNumber(date: LocalDate, term: TermSettings): Int? {
    if (date.isBefore(term.firstMonday)) return null
    val number = ChronoUnit.WEEKS.between(term.firstMonday, date.with(DayOfWeek.MONDAY)).toInt() + 1
    return number.takeIf { it > 0 }
}

enum class WeekRuleType { EVERY, ODD, EVEN, SET, RANGE, DATES }

data class WeekRule(
    val type: WeekRuleType = WeekRuleType.EVERY,
    val weeks: Set<Int> = emptySet(),
    val fromWeek: Int? = null,
    val toWeek: Int? = null,
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null
) {
    fun encode(): String = listOf(
        type.name,
        weeks.sorted().joinToString(","),
        fromWeek?.toString().orEmpty(),
        toWeek?.toString().orEmpty(),
        fromDate?.toString().orEmpty(),
        toDate?.toString().orEmpty()
    ).joinToString("|")

    fun matches(date: LocalDate, term: TermSettings): Boolean {
        if (date.isBefore(term.startDate) || date.isAfter(term.endDate)) return false
        if (fromDate != null && date.isBefore(fromDate)) return false
        if (toDate != null && date.isAfter(toDate)) return false
        val week = weekNumber(date, term) ?: return false
        return when (type) {
            WeekRuleType.EVERY -> true
            WeekRuleType.ODD -> week % 2 == 1
            WeekRuleType.EVEN -> week % 2 == 0
            WeekRuleType.SET -> week in weeks
            WeekRuleType.RANGE -> week in (fromWeek ?: Int.MIN_VALUE)..(toWeek ?: Int.MAX_VALUE)
            WeekRuleType.DATES -> true
        }
    }

    fun intersects(other: WeekRule): Boolean {
        if (type == WeekRuleType.EVERY || other.type == WeekRuleType.EVERY) return true
        if (type == WeekRuleType.ODD && other.type == WeekRuleType.ODD) return true
        if (type == WeekRuleType.EVEN && other.type == WeekRuleType.EVEN) return true
        if (type == WeekRuleType.ODD && other.type == WeekRuleType.EVEN) return false
        if (type == WeekRuleType.EVEN && other.type == WeekRuleType.ODD) return false
        val thisSet = finiteWeekSet()
        val otherSet = other.finiteWeekSet()
        if (thisSet != null && otherSet != null) return thisSet.intersect(otherSet).isNotEmpty()
        return true
    }

    private fun finiteWeekSet(): Set<Int>? = when (type) {
        WeekRuleType.SET -> weeks
        WeekRuleType.RANGE -> if (fromWeek != null && toWeek != null && toWeek - fromWeek <= 52) {
            (fromWeek..toWeek).toSet()
        } else null
        WeekRuleType.ODD -> (1..52).filter { it % 2 == 1 }.toSet()
        WeekRuleType.EVEN -> (1..52).filter { it % 2 == 0 }.toSet()
        else -> null
    }

    companion object {
        fun decode(value: String): WeekRule {
            if (value.isBlank() || value == "EVERY") return WeekRule()
            val parts = value.split("|")
            val type = runCatching { WeekRuleType.valueOf(parts.getOrNull(0).orEmpty()) }
                .getOrDefault(WeekRuleType.EVERY)
            fun intAt(index: Int) = parts.getOrNull(index)?.toIntOrNull()
            fun dateAt(index: Int) = parts.getOrNull(index)?.takeIf { it.isNotBlank() }?.let {
                runCatching { LocalDate.parse(it) }.getOrNull()
            }
            return WeekRule(
                type = type,
                weeks = parts.getOrNull(1).orEmpty().split(",").mapNotNull { it.toIntOrNull() }.toSet(),
                fromWeek = intAt(2),
                toWeek = intAt(3),
                fromDate = dateAt(4),
                toDate = dateAt(5)
            )
        }

        fun fromText(value: String): WeekRule? {
            val raw = value.trim()
            if (raw.isBlank() || raw == "每周") return WeekRule()
            val dateRange = Regex("^(\\d{4}-\\d{1,2}-\\d{1,2})\\s*(?:至|-)\\s*(\\d{4}-\\d{1,2}-\\d{1,2})$")
                .matchEntire(raw)
            if (dateRange != null) {
                val start = runCatching { LocalDate.parse(dateRange.groupValues[1]) }.getOrNull() ?: return null
                val end = runCatching { LocalDate.parse(dateRange.groupValues[2]) }.getOrNull() ?: return null
                return if (!end.isBefore(start)) WeekRule(WeekRuleType.DATES, fromDate = start, toDate = end) else null
            }
            if (raw == "单周" || raw.equals("odd", true)) return WeekRule(type = WeekRuleType.ODD)
            if (raw == "双周" || raw.equals("even", true)) return WeekRule(type = WeekRuleType.EVEN)
            val range = Regex("^(?:第)?(\\d+)\\s*(?:-|至|到)\\s*(\\d+)(?:周)?$").matchEntire(raw)
            if (range != null) {
                val start = range.groupValues[1].toIntOrNull() ?: return null
                val end = range.groupValues[2].toIntOrNull() ?: return null
                return if (start in 1..64 && end in start..64) {
                    WeekRule(WeekRuleType.RANGE, fromWeek = start, toWeek = end)
                } else null
            }
            val set = raw.removePrefix("第").removeSuffix("周").split(Regex("[,，、\\s]+"))
                .mapNotNull { it.toIntOrNull() }
            return if (set.isNotEmpty() && set.all { it in 1..64 }) WeekRule(WeekRuleType.SET, set.toSet()) else null
        }
    }
}

fun resolveMeeting(meeting: MeetingEntity, slots: List<TimeSlot>): Pair<Int, Int>? {
    if (meeting.startMinutes != null && meeting.endMinutes != null) {
        return meeting.startMinutes to meeting.endMinutes
    }
    val start = slots.firstOrNull { it.id == meeting.startSlotId }?.startMinutes
    val end = slots.firstOrNull { it.id == meeting.endSlotId }?.endMinutes
    return if (start != null && end != null) start to end else null
}

fun CourseWithMeetings.resolvedFor(
    date: LocalDate,
    term: TermSettings,
    slots: List<TimeSlot>
): List<ResolvedMeeting> = meetings.mapNotNull { meeting ->
    if (meeting.weekday != date.dayOfWeek.value || !WeekRule.decode(meeting.weekRule).matches(date, term)) return@mapNotNull null
    val times = resolveMeeting(meeting, slots) ?: return@mapNotNull null
    ResolvedMeeting(course, meeting, times.first, times.second)
}

fun List<CourseWithMeetings>.resolvedFor(
    date: LocalDate,
    term: TermSettings,
    slots: List<TimeSlot>
): List<ResolvedMeeting> = flatMap { it.resolvedFor(date, term, slots) }.sortedWith(
    compareBy<ResolvedMeeting> { it.startMinutes }.thenBy { it.endMinutes }.thenBy { it.course.name }
)

fun LocalTime.toMinutes(): Int = hour * 60 + minute
