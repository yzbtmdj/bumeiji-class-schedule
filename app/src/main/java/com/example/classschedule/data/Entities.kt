package com.example.classschedule.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val teacher: String = "",
    val location: String = "",
    val colorHex: String = "#39766B",
    val note: String = "",
    val source: String = "manual"
)

@Entity(
    tableName = "meetings",
    indices = [Index("courseId"), Index("weekday")]
)
data class MeetingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val weekday: Int,
    val startMinutes: Int? = null,
    val endMinutes: Int? = null,
    val startSlotId: Long? = null,
    val endSlotId: Long? = null,
    val weekRule: String = "EVERY",
    val dateStartEpochDay: Long? = null,
    val dateEndEpochDay: Long? = null
)

data class CourseWithMeetings(
    @Embedded val course: CourseEntity,
    @Relation(parentColumn = "id", entityColumn = "courseId")
    val meetings: List<MeetingEntity>
)
