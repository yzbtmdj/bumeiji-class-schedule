package com.example.classschedule.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<CourseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(course: CourseEntity): Long

    @Update
    suspend fun update(course: CourseEntity)

    @Update
    suspend fun updateAll(courses: List<CourseEntity>)

    @Delete
    suspend fun delete(course: CourseEntity)
}

@Dao
interface MeetingDao {
    @Query("SELECT * FROM meetings ORDER BY weekday, startMinutes")
    fun observeAll(): Flow<List<MeetingEntity>>

    @Query("SELECT * FROM meetings ORDER BY weekday, startMinutes")
    suspend fun getAll(): List<MeetingEntity>

    @Query("SELECT * FROM meetings WHERE courseId = :courseId ORDER BY weekday, startMinutes")
    suspend fun forCourse(courseId: Long): List<MeetingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(meetings: List<MeetingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meeting: MeetingEntity): Long

    @Update
    suspend fun update(meeting: MeetingEntity)

    @Query("DELETE FROM meetings WHERE courseId = :courseId")
    suspend fun deleteForCourse(courseId: Long)

    @Query("DELETE FROM meetings")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(meeting: MeetingEntity)
}
