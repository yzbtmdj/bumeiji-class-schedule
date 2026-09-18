package com.example.classschedule.data

import androidx.room.withTransaction
import com.example.classschedule.domain.WeekRule
import com.example.classschedule.domain.parseClock
import com.example.classschedule.domain.TimeSlot
import com.example.classschedule.domain.DefaultCourseColor
import com.example.classschedule.domain.allocateCourseColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

enum class ImportConflictStrategy { MERGE, OVERWRITE, CANCEL }

class ScheduleRepository(private val database: AppDatabase) {
    private val courses = database.courseDao()
    private val meetings = database.meetingDao()

    val records: Flow<List<CourseWithMeetings>> = combine(
        courses.observeAll(),
        meetings.observeAll()
    ) { courseList, meetingList ->
        val grouped = meetingList.groupBy { it.courseId }
        courseList.map { CourseWithMeetings(it, grouped[it.id].orEmpty()) }
    }

    suspend fun saveCourse(
        id: Long?,
        name: String,
        teacher: String,
        location: String,
        colorHex: String,
        note: String,
        meeting: MeetingEntity,
        source: String = "manual"
    ): Long = database.withTransaction {
        val courseId = if (id == null || id == 0L) {
            courses.insert(CourseEntity(name = name, teacher = teacher, location = location, colorHex = colorHex, note = note, source = source))
        } else {
            courses.update(CourseEntity(id, name, teacher, location, colorHex, note, source))
            id
        }
        if (id == null || id == 0L) {
            meetings.insert(meeting.copy(id = 0, courseId = courseId))
        } else {
            val existingMeeting = meetings.forCourse(courseId).firstOrNull()
            if (existingMeeting == null) {
                meetings.insert(meeting.copy(id = 0, courseId = courseId))
            } else {
                meetings.update(meeting.copy(id = existingMeeting.id, courseId = courseId))
            }
        }
        courseId
    }

    suspend fun saveCourseWithMeetings(
        id: Long?,
        name: String,
        teacher: String,
        location: String,
        colorHex: String,
        note: String,
        meetingsToSave: List<MeetingEntity>,
        source: String = "manual"
    ): Long = database.withTransaction {
        val courseId = if (id == null || id == 0L) {
            courses.insert(CourseEntity(name = name, teacher = teacher, location = location, colorHex = colorHex, note = note, source = source))
        } else {
            courses.update(CourseEntity(id, name, teacher, location, colorHex, note, source))
            id
        }
        if (id == null || id == 0L) {
            meetings.insertAll(meetingsToSave.map { it.copy(id = 0, courseId = courseId) })
        } else {
            val existing = meetings.forCourse(courseId)
            val existingIds = existing.map { it.id }.toSet()
            val keptIds = mutableSetOf<Long>()
            meetingsToSave.forEach { meeting ->
                if (meeting.id != 0L && meeting.id in existingIds) {
                    meetings.update(meeting.copy(courseId = courseId))
                    keptIds += meeting.id
                } else {
                    meetings.insert(meeting.copy(id = 0, courseId = courseId))
                }
            }
            existing.filterNot { it.id in keptIds }.forEach { meetings.delete(it) }
        }
        courseId
    }

    suspend fun deleteCourse(record: CourseWithMeetings) = database.withTransaction {
        record.meetings.forEach { meetings.delete(it) }
        courses.delete(record.course)
    }

    suspend fun restoreCourse(record: CourseWithMeetings): Long = database.withTransaction {
        val id = courses.insert(record.course.copy(id = 0))
        meetings.insertAll(record.meetings.map { it.copy(id = 0, courseId = id) })
        id
    }

    /** Recolors only duplicate legacy defaults; explicit non-default colors are untouched. */
    suspend fun ensureDistinctCourseColors(): Int = database.withTransaction {
        val all = courses.getAll()
        val legacy = all.filter { it.colorHex.isBlank() || it.colorHex.equals(DefaultCourseColor, true) }
        if (legacy.size < 2) return@withTransaction 0
        val legacyIds = legacy.map { it.id }.toSet()
        val used = all.filterNot { it.id in legacyIds }.map { it.colorHex }.toMutableSet()
        val updates = legacy.sortedBy { it.id }.map { course ->
            val color = allocateCourseColor("${course.name}:${course.id}", used)
            used += color
            course.copy(colorHex = color)
        }
        courses.updateAll(updates)
        updates.size
    }

    suspend fun importRows(
        rows: List<ImportRow>,
        strategy: ImportConflictStrategy,
        defaultColor: String = "#39766B",
        timeSlots: List<TimeSlot> = emptyList()
    ): Int = database.withTransaction {
        if (strategy == ImportConflictStrategy.CANCEL) return@withTransaction 0
        val existing = courses.getAll().toMutableList()
        val overwrittenIds = mutableSetOf<Long>()
        var count = 0
        rows.forEach { row ->
            val match = existing.firstOrNull {
                it.name.trim().equals(row.courseName.trim(), ignoreCase = true) &&
                    (row.teacher.isBlank() || it.teacher.trim() == row.teacher.trim())
            }
            val courseId = if (match != null) {
                if (strategy == ImportConflictStrategy.OVERWRITE) {
                    if (overwrittenIds.add(match.id)) meetings.deleteForCourse(match.id)
                    courses.update(match.copy(
                        teacher = row.teacher.ifBlank { match.teacher },
                        location = row.location.ifBlank { match.location },
                        colorHex = row.color.ifBlank { match.colorHex },
                        note = row.note.ifBlank { match.note },
                        source = "import"
                    ))
                }
                match.id
            } else {
                val color = row.color.ifBlank {
                    allocateCourseColor(
                        "${row.courseName}:${row.weekday}:${row.startMinutes ?: row.startSlotNumber ?: 0}",
                        existing.map { it.colorHex }
                    )
                }
                val inserted = courses.insert(CourseEntity(
                    name = row.courseName.trim(),
                    teacher = row.teacher.trim(),
                    location = row.location.trim(),
                    colorHex = color.ifBlank { defaultColor },
                    note = row.note.trim(),
                    source = "import"
                ))
                existing += CourseEntity(inserted, row.courseName.trim(), row.teacher.trim(), row.location.trim(), color.ifBlank { defaultColor }, row.note.trim(), "import")
                inserted
            }
            meetings.insert(row.toMeeting(timeSlots).copy(courseId = courseId))
            count += 1
        }
        count
    }

    suspend fun getRecord(id: Long): CourseWithMeetings? {
        return recordsFirst().firstOrNull { it.course.id == id }
    }

    private suspend fun recordsFirst(): List<CourseWithMeetings> {
        val courseList = courses.getAll()
        val meetingList = meetings.getAll().groupBy { it.courseId }
        return courseList.map { CourseWithMeetings(it, meetingList[it.id].orEmpty()) }
    }

    suspend fun allMeetings(): List<MeetingEntity> = meetings.getAll()
}
