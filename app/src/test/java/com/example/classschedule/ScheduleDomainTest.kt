package com.example.classschedule

import com.example.classschedule.data.CourseEntity
import com.example.classschedule.data.CourseWithMeetings
import com.example.classschedule.data.ImportIssue
import com.example.classschedule.data.ImportRow
import com.example.classschedule.data.IssueSeverity
import com.example.classschedule.data.MeetingEntity
import com.example.classschedule.data.ScheduleFileParser
import com.example.classschedule.data.StandardFieldMapping
import com.example.classschedule.domain.ReminderConfig
import com.example.classschedule.domain.ResolvedMeeting
import com.example.classschedule.domain.Stage
import com.example.classschedule.domain.TermSettings
import com.example.classschedule.domain.WeekRule
import com.example.classschedule.domain.defaultTerm
import com.example.classschedule.domain.defaultTimeSlots
import com.example.classschedule.domain.reminderLine
import com.example.classschedule.domain.resolveMeeting
import com.example.classschedule.domain.stageFor
import com.example.classschedule.domain.weekNumber
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleDomainTest {
    private val term = TermSettings("测试学期", LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 1), LocalDate.of(2027, 1, 31))

    @Test fun weekNumberUsesFirstTeachingMonday() {
        assertEquals(1, weekNumber(LocalDate.of(2026, 9, 7), term))
        assertEquals(2, weekNumber(LocalDate.of(2026, 9, 14), term))
        assertEquals(null, weekNumber(LocalDate.of(2026, 9, 6), term))
    }

    @Test fun weekRulesSupportEveryOddEvenSetRangeAndDates() {
        assertTrue(WeekRule.fromText("每周")!!.matches(LocalDate.of(2026, 9, 9), term))
        assertTrue(WeekRule.fromText("单周")!!.matches(LocalDate.of(2026, 9, 9), term))
        assertFalse(WeekRule.fromText("双周")!!.matches(LocalDate.of(2026, 9, 9), term))
        assertTrue(WeekRule.fromText("1,3,5")!!.matches(LocalDate.of(2026, 9, 9), term))
        assertTrue(WeekRule.fromText("2-3")!!.matches(LocalDate.of(2026, 9, 14), term))
        assertFalse(WeekRule.fromText("2-3")!!.matches(LocalDate.of(2026, 9, 28), term))
        assertTrue(WeekRule.fromText("2026-09-10 至 2026-09-20")!!.matches(LocalDate.of(2026, 9, 12), term))
    }

    @Test fun weekRulesRoundTripAndStopAtTermEnd() {
        val set = WeekRule.fromText("1,3,5")!!
        assertEquals(set, WeekRule.decode(set.encode()))
        val dateRule = WeekRule.fromText("2026-09-10 至 2026-09-20")!!
        assertEquals(dateRule, WeekRule.decode(dateRule.encode()))
        assertFalse(WeekRule.fromText("每周")!!.matches(LocalDate.of(2027, 2, 1), term))
    }

    @Test fun slotMeetingUsesCurrentSlotTimes() {
        val slots = defaultTimeSlots()
        val meeting = MeetingEntity(courseId = 1, weekday = 1, startSlotId = slots[0].id, endSlotId = slots[1].id)
        assertEquals(8 * 60 to (9 * 60 + 40), resolveMeeting(meeting, slots))
        val updated = slots.map { if (it.id == slots[0].id) it.copy(startMinutes = 8 * 60 + 10) else it }
        assertEquals((8 * 60 + 10) to (9 * 60 + 40), resolveMeeting(meeting, updated))
    }

    @Test fun stageClassificationUsesConfiguredBoundaries() {
        val config = ReminderConfig.defaults()
        assertEquals(Stage.MORNING, stageFor(11 * 60 + 59, config))
        assertEquals(Stage.AFTERNOON, stageFor(12 * 60, config))
        assertEquals(Stage.EVENING, stageFor(18 * 60, config))
    }

    @Test fun clockParserAcceptsMidnightAsDayEnd() {
        assertEquals(24 * 60, com.example.classschedule.domain.parseClock("24:00"))
        assertEquals(null, com.example.classschedule.domain.parseClock("24:01"))
    }

    @Test fun reminderLineContainsTimeCourseAndPlace() {
        val course = CourseEntity(name = "高等数学", location = "A201")
        val item = ResolvedMeeting(course, MeetingEntity(courseId = 1, weekday = 1, startMinutes = 8 * 60, endMinutes = 9 * 60 + 40), 8 * 60, 9 * 60 + 40)
        assertEquals("08:00-09:40 高等数学 · A201", reminderLine(item))
    }

    @Test fun csvParserAcceptsAliasesAndReportsConflictsAsWarnings() {
        val csv = "课程名,星期,开始时间,结束时间,地点,周次\n高等数学,周一,08:00,09:40,A201,每周\n英语,1,09:00,10:00,B201,每周"
        val preview = ScheduleFileParser.parseCsv(csv)
        assertEquals(2, preview.rows.size)
        assertTrue(preview.conflicts.any { it.contains("时间冲突") })
        assertTrue(preview.issues.any { it.severity == IssueSeverity.WARNING })
        assertFalse(preview.hasErrors)
    }

    @Test fun csvParserRejectsInvalidWeekdayAndBackwardsTime() {
        val csv = "课程名,星期,开始时间,结束时间\n物理,周八,10:00,09:00"
        val preview = ScheduleFileParser.parseCsv(csv)
        assertTrue(preview.hasErrors)
        assertTrue(preview.issues.any { it.message.contains("星期无效") })
        assertTrue(preview.issues.any { it.message.contains("结束时间") })
    }

    @Test fun standardImportMappingCanBeChangedBeforePreview() {
        val csv = "名称,星期列,起点,终点,教室列\n网络安全,周二,8:00,9:40,B204"
        val initial = ScheduleFileParser.parseCsv(csv)
        assertTrue(initial.hasErrors)
        val remapped = ScheduleFileParser.remapStandard(
            initial,
            StandardFieldMapping(courseName = 0, weekday = 1, startTime = 2, endTime = 3, location = 4)
        )
        assertFalse(remapped.hasErrors)
        assertEquals("网络安全", remapped.rows.single().courseName)
        assertEquals("B204", remapped.rows.single().location)
    }

    @Test fun xlsxParserReadsFirstSheetWithSharedStrings() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun entry(name: String, value: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(value.toByteArray())
                zip.closeEntry()
            }
            entry("xl/sharedStrings.xml", "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><si><t>课程名</t></si><si><t>星期</t></si><si><t>开始时间</t></si><si><t>结束时间</t></si><si><t>高等数学</t></si><si><t>周一</t></si><si><t>08:00</t></si><si><t>09:40</t></si></sst>")
            entry("xl/workbook.xml", "<workbook xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Sheet1\" r:id=\"rId1\"/></sheets></workbook>")
            entry("xl/_rels/workbook.xml.rels", "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Target=\"worksheets/sheet1.xml\"/></Relationships>")
            entry("xl/worksheets/sheet1.xml", "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData><row r=\"1\"><c r=\"A1\" t=\"s\"><v>0</v></c><c r=\"B1\" t=\"s\"><v>1</v></c><c r=\"C1\" t=\"s\"><v>2</v></c><c r=\"D1\" t=\"s\"><v>3</v></c></row><row r=\"2\"><c r=\"A2\" t=\"s\"><v>4</v></c><c r=\"B2\" t=\"s\"><v>5</v></c><c r=\"C2\" t=\"s\"><v>6</v></c><c r=\"D2\" t=\"s\"><v>7</v></c></row></sheetData></worksheet>")
        }
        val preview = ScheduleFileParser.parseXlsx(out.toByteArray().inputStream())
        assertEquals("高等数学", preview.rows.single().courseName)
        assertEquals(1, preview.rows.single().weekday)
    }

    @Test fun xlsxParserRejectsDtdDeclarationsSafely() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            zip.write("<!DOCTYPE sst [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><sst/>".toByteArray())
            zip.closeEntry()
        }
        try {
            ScheduleFileParser.parseXlsx(out.toByteArray().inputStream())
            throw AssertionError("Expected DTD declarations to be rejected")
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message.orEmpty().contains("DTD"))
        }
    }

    @Test fun xlsxParserReadsPrefixedNamespaces() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun entry(name: String, value: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(value.toByteArray())
                zip.closeEntry()
            }
            entry("xl/sharedStrings.xml", "<x:sst xmlns:x=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><x:si><x:t>课程名</x:t></x:si><x:si><x:t>星期</x:t></x:si><x:si><x:t>开始时间</x:t></x:si><x:si><x:t>结束时间</x:t></x:si><x:si><x:t>网络安全</x:t></x:si><x:si><x:t>周二</x:t></x:si><x:si><x:t>08:00</x:t></x:si><x:si><x:t>09:25</x:t></x:si></x:sst>")
            entry("xl/workbook.xml", "<x:workbook xmlns:x=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><x:sheets><x:sheet name=\"Sheet1\" r:id=\"rId1\"/></x:sheets></x:workbook>")
            entry("xl/_rels/workbook.xml.rels", "<p:Relationships xmlns:p=\"http://schemas.openxmlformats.org/package/2006/relationships\"><p:Relationship Id=\"rId1\" Target=\"worksheets/sheet1.xml\"/></p:Relationships>")
            entry("xl/worksheets/sheet1.xml", "<x:worksheet xmlns:x=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><x:sheetData><x:row r=\"1\"><x:c r=\"A1\" t=\"s\"><x:v>0</x:v></x:c><x:c r=\"B1\" t=\"s\"><x:v>1</x:v></x:c><x:c r=\"C1\" t=\"s\"><x:v>2</x:v></x:c><x:c r=\"D1\" t=\"s\"><x:v>3</x:v></x:c></x:row><x:row r=\"2\"><x:c r=\"A2\" t=\"s\"><x:v>4</x:v></x:c><x:c r=\"B2\" t=\"s\"><x:v>5</x:v></x:c><x:c r=\"C2\" t=\"s\"><x:v>6</x:v></x:c><x:c r=\"D2\" t=\"s\"><x:v>7</x:v></x:c></x:row></x:sheetData></x:worksheet>")
        }
        val preview = ScheduleFileParser.parseXlsx(out.toByteArray().inputStream())
        assertEquals("网络安全", preview.rows.single().courseName)
        assertEquals(2, preview.rows.single().weekday)
    }
}
