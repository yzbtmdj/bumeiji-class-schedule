package com.example.classschedule

import com.example.classschedule.data.ImportFormat
import com.example.classschedule.data.ScheduleFileParser
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class LegacyXlsAttachmentTest {
    @Test
    fun readsConfiguredLegacyXlsGrid() {
        val path = System.getenv("CLASS_SCHEDULE_XLS_PATH")
        assumeTrue(!path.isNullOrBlank() && File(path).isFile)
        val preview = ScheduleFileParser.parseXls(File(path!!).inputStream(), File(path).name)
        assertEquals(ImportFormat.CAMPUS_GRID, preview.format)
        assertTrue(preview.rows.isNotEmpty())
        assertTrue(preview.rows.any { it.courseName.contains("网络攻防对抗") })
        assertTrue(preview.rows.any { it.startSlotNumber == 6 && it.endSlotNumber == 9 && it.sourceCell == "C8" })
        assertTrue(preview.rows.any { it.location == "致远楼406" && it.teacher == "谭可久" })
        assertTrue(preview.rows.all { it.weekRule.fromWeek == 1 && it.weekRule.toWeek in 11..16 })
    }
}
