package com.example.classschedule

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.classschedule.data.AppDatabase
import java.io.IOException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        helper.createDatabase("migration-test", 1).apply {
            execSQL("CREATE TABLE IF NOT EXISTS courses (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, teacher TEXT NOT NULL, location TEXT NOT NULL, colorHex TEXT NOT NULL, note TEXT NOT NULL)")
            execSQL("CREATE TABLE IF NOT EXISTS meetings (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, courseId INTEGER NOT NULL, weekday INTEGER NOT NULL, startMinutes INTEGER, endMinutes INTEGER, startSlotId INTEGER, endSlotId INTEGER, weekRule TEXT NOT NULL, dateStartEpochDay INTEGER, dateEndEpochDay INTEGER)")
            execSQL("CREATE INDEX IF NOT EXISTS index_meetings_courseId ON meetings (courseId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_meetings_weekday ON meetings (weekday)")
            close()
        }
        helper.runMigrationsAndValidate("migration-test", 2, true, AppDatabase.MIGRATION_1_2)
    }
}
