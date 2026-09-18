package com.example.classschedule.data

import com.example.classschedule.domain.WeekRule
import com.example.classschedule.domain.parseClock
import com.example.classschedule.domain.TimeSlot
import org.xml.sax.InputSource
import javax.xml.parsers.DocumentBuilderFactory
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream
import kotlin.math.roundToInt

data class ImportRow(
    val rowNumber: Int,
    val courseName: String,
    val weekday: Int,
    val startMinutes: Int?,
    val endMinutes: Int?,
    val location: String = "",
    val teacher: String = "",
    val weekRule: WeekRule = WeekRule(),
    val color: String = "",
    val note: String = "",
    val startSlotNumber: Int? = null,
    val endSlotNumber: Int? = null,
    val sourceCell: String = "",
    val sourceValue: String = ""
)

enum class IssueSeverity { ERROR, WARNING }

enum class ImportFormat(val label: String) {
    STANDARD_TABLE("标准字段表"),
    CAMPUS_GRID("教务系统格状课表")
}

data class StandardFieldMapping(
    val courseName: Int? = null,
    val weekday: Int? = null,
    val startTime: Int? = null,
    val endTime: Int? = null,
    val location: Int? = null,
    val teacher: Int? = null,
    val weekRule: Int? = null,
    val color: Int? = null,
    val note: Int? = null
)

data class ImportIssue(val rowNumber: Int, val message: String, val severity: IssueSeverity)

data class ImportPreview(
    val sourceName: String,
    val headers: List<String>,
    val rows: List<ImportRow>,
    val issues: List<ImportIssue>,
    val conflicts: List<String>,
    val format: ImportFormat = ImportFormat.STANDARD_TABLE,
    val fieldMapping: StandardFieldMapping? = null,
    val sourceMatrix: List<List<String>> = emptyList()
) {
    val hasErrors: Boolean get() = issues.any { it.severity == IssueSeverity.ERROR }

    val requiredSlotNumbers: Set<Int>
        get() = rows.flatMap { row ->
            val start = row.startSlotNumber
            val end = row.endSlotNumber ?: start
            if (start != null && end != null && end >= start) (start..end).toList() else emptyList()
        }.toSet()
}

object ScheduleFileParser {
    fun parseCsv(text: String, sourceName: String = "课表.csv"): ImportPreview {
        val matrix = parseDelimited(text.removePrefix("\uFEFF"))
        return mapMatrix(sourceName, matrix)
    }

    fun parseXlsx(input: InputStream, sourceName: String = "课表.xlsx"): ImportPreview {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory) entries[entry.name] = zip.readBytes()
            }
        }
        val sharedStrings = parseSharedStrings(entries["xl/sharedStrings.xml"])
        val sheetPath = findFirstSheetPath(entries).ifBlank { "xl/worksheets/sheet1.xml" }
        val matrix = parseSheet(entries[sheetPath] ?: return ImportPreview(
            sourceName, emptyList(), emptyList(), listOf(ImportIssue(0, "找不到第一个工作表", IssueSeverity.ERROR)), emptyList()
        ), sharedStrings)
        return mapWorkbookMatrix(sourceName, matrix)
    }

    fun parseXls(input: InputStream, sourceName: String = "课表.xls"): ImportPreview {
        return mapWorkbookMatrix(sourceName, LegacyXlsParser.readMatrix(input))
    }

    fun remapStandard(preview: ImportPreview, mapping: StandardFieldMapping): ImportPreview {
        if (preview.format != ImportFormat.STANDARD_TABLE || preview.sourceMatrix.isEmpty()) return preview
        return mapMatrix(preview.sourceName, preview.sourceMatrix, mapping)
    }

    private fun mapWorkbookMatrix(sourceName: String, matrix: List<List<String>>): ImportPreview {
        return if (looksLikeGrid(matrix)) mapGridMatrix(sourceName, matrix) else mapMatrix(sourceName, matrix)
    }

    private fun looksLikeGrid(matrix: List<List<String>>): Boolean {
        return matrix.any { row ->
            row.any { normalizeHeader(it) == "时间段" } && row.count { parseWeekday(it) != null } >= 3
        }
    }

    private fun mapMatrix(
        sourceName: String,
        matrix: List<List<String>>,
        mappingOverride: StandardFieldMapping? = null
    ): ImportPreview {
        if (matrix.isEmpty()) return ImportPreview(
            sourceName = sourceName,
            headers = emptyList(),
            rows = emptyList(),
            issues = listOf(ImportIssue(0, "文件没有可读取的内容", IssueSeverity.ERROR)),
            conflicts = emptyList(),
            fieldMapping = mappingOverride,
            sourceMatrix = matrix
        )
        val headers = matrix.first().map { it.trim() }
        val normalized = headers.map(::normalizeHeader)
        fun index(vararg names: String): Int? = names.firstNotNullOfOrNull { name -> normalized.indexOf(normalizeHeader(name)).takeIf { it >= 0 } }

        val detectedMapping = StandardFieldMapping(
            courseName = index("课程名", "课程", "科目", "name"),
            weekday = index("星期", "周几", "上课星期", "day"),
            startTime = index("开始时间", "开始", "上课开始", "start"),
            endTime = index("结束时间", "结束", "下课时间", "end"),
            location = index("地点", "教室", "location"),
            teacher = index("教师", "老师", "teacher"),
            weekRule = index("周次", "上课周次", "week"),
            color = index("颜色", "color"),
            note = index("备注", "note")
        )
        val selectedMapping = mappingOverride ?: detectedMapping
        fun validIndex(value: Int?): Int? = value?.takeIf { it in headers.indices }
        val courseIndex = validIndex(selectedMapping.courseName)
        val dayIndex = validIndex(selectedMapping.weekday)
        val startIndex = validIndex(selectedMapping.startTime)
        val endIndex = validIndex(selectedMapping.endTime)
        val issues = mutableListOf<ImportIssue>()
        if (courseIndex == null) issues += ImportIssue(1, "缺少必需字段：课程名", IssueSeverity.ERROR)
        if (dayIndex == null) issues += ImportIssue(1, "缺少必需字段：星期", IssueSeverity.ERROR)
        if (startIndex == null) issues += ImportIssue(1, "缺少必需字段：开始时间", IssueSeverity.ERROR)
        if (endIndex == null) issues += ImportIssue(1, "缺少必需字段：结束时间", IssueSeverity.ERROR)
        if (issues.isNotEmpty()) return ImportPreview(
            sourceName = sourceName,
            headers = headers,
            rows = emptyList(),
            issues = issues,
            conflicts = emptyList(),
            fieldMapping = selectedMapping,
            sourceMatrix = matrix
        )

        val locationIndex = validIndex(selectedMapping.location)
        val teacherIndex = validIndex(selectedMapping.teacher)
        val weekIndex = validIndex(selectedMapping.weekRule)
        val colorIndex = validIndex(selectedMapping.color)
        val noteIndex = validIndex(selectedMapping.note)
        val rows = mutableListOf<ImportRow>()
        val rowErrors = mutableListOf<ImportIssue>()
        matrix.drop(1).forEachIndexed { offset, values ->
            val rowNumber = offset + 2
            fun value(index: Int?) = index?.let { values.getOrNull(it).orEmpty().trim() }.orEmpty()
            val name = value(courseIndex)
            val weekday = parseWeekday(value(dayIndex))
            val start = parseImportTime(value(startIndex))
            val end = parseImportTime(value(endIndex))
            val ruleText = value(weekIndex)
            val rule = WeekRule.fromText(ruleText)
            if (name.isBlank()) rowErrors += ImportIssue(rowNumber, "课程名不能为空", IssueSeverity.ERROR)
            if (weekday == null) rowErrors += ImportIssue(rowNumber, "星期无效：${value(dayIndex)}", IssueSeverity.ERROR)
            if (start == null) rowErrors += ImportIssue(rowNumber, "开始时间无效：${value(startIndex)}", IssueSeverity.ERROR)
            if (end == null) rowErrors += ImportIssue(rowNumber, "结束时间无效：${value(endIndex)}", IssueSeverity.ERROR)
            if (start != null && end != null && end <= start) rowErrors += ImportIssue(rowNumber, "结束时间必须晚于开始时间", IssueSeverity.ERROR)
            if (rule == null) rowErrors += ImportIssue(rowNumber, "周次无效：$ruleText", IssueSeverity.ERROR)
            if (name.isNotBlank() && weekday != null && start != null && end != null && end > start && rule != null) {
                rows += ImportRow(
                    rowNumber = rowNumber,
                    courseName = name,
                    weekday = weekday,
                    startMinutes = start,
                    endMinutes = end,
                    location = value(locationIndex),
                    teacher = value(teacherIndex),
                    weekRule = rule,
                    color = value(colorIndex),
                    note = value(noteIndex),
                    sourceCell = "第${rowNumber}行",
                    sourceValue = values.joinToString(",")
                )
            }
        }
        val conflicts = detectConflicts(rows)
        conflicts.forEach { rowErrors += ImportIssue(0, it, IssueSeverity.WARNING) }
        return ImportPreview(
            sourceName = sourceName,
            headers = headers,
            rows = rows,
            issues = issues + rowErrors,
            conflicts = conflicts,
            fieldMapping = selectedMapping,
            sourceMatrix = matrix
        )
    }

    private fun mapGridMatrix(sourceName: String, matrix: List<List<String>>): ImportPreview {
        val headerIndex = matrix.indexOfFirst { row ->
            row.any { normalizeHeader(it) == "时间段" } && row.count { parseWeekday(it) != null } >= 3
        }
        if (headerIndex < 0) {
            return ImportPreview(
                sourceName,
                emptyList(),
                emptyList(),
                listOf(ImportIssue(0, "未识别教务系统格状课表表头", IssueSeverity.ERROR)),
                emptyList(),
                ImportFormat.CAMPUS_GRID
            )
        }

        val header = matrix[headerIndex].map { it.trim() }
        val dayColumns = header.mapIndexedNotNull { index, value ->
            parseWeekday(value)?.let { day -> index to day }
        }.toMap()
        val sectionColumn = header.indexOfFirst { normalizeHeader(it) in setOf("节次", "课次", "上课节次") }
            .takeIf { it >= 0 }
            ?: (header.indexOfFirst { normalizeHeader(it) == "时间段" } + 1)
        val rows = mutableListOf<ImportRow>()
        val rowIssues = mutableListOf<ImportIssue>()

        matrix.drop(headerIndex + 1).forEachIndexed { offset, values ->
            val sourceRow = headerIndex + offset + 2
            val sectionText = values.getOrNull(sectionColumn).orEmpty().trim()
            val sectionNumber = Regex("\\d+").find(sectionText)?.value?.toIntOrNull()
            if (sectionNumber == null) return@forEachIndexed

            dayColumns.forEach { (column, weekday) ->
                val raw = values.getOrNull(column).orEmpty().trim()
                if (raw.isBlank()) return@forEach
                val sourceCell = cellName(sourceRow, column)
                val parsed = parseGridCell(raw)
                if (parsed == null) {
                    rowIssues += ImportIssue(sourceRow, "来源 $sourceCell：无法识别课程内容或周次/节次", IssueSeverity.ERROR)
                    return@forEach
                }
                rows += ImportRow(
                    rowNumber = sourceRow,
                    courseName = parsed.name,
                    weekday = weekday,
                    startMinutes = null,
                    endMinutes = null,
                    location = parsed.location,
                    teacher = parsed.teacher,
                    weekRule = parsed.rule,
                    startSlotNumber = parsed.startSlot,
                    endSlotNumber = parsed.endSlot,
                    sourceCell = sourceCell,
                    sourceValue = raw
                )
            }
        }

        val conflicts = detectConflicts(rows)
        conflicts.forEach { rowIssues += ImportIssue(0, it, IssueSeverity.WARNING) }
        return ImportPreview(
            sourceName = sourceName,
            headers = header,
            rows = rows,
            issues = rowIssues,
            conflicts = conflicts,
            format = ImportFormat.CAMPUS_GRID
        )
    }

    private data class GridCell(
        val name: String,
        val rule: WeekRule,
        val startSlot: Int,
        val endSlot: Int,
        val location: String,
        val teacher: String
    )

    private fun parseGridCell(raw: String): GridCell? {
        val lines = raw
            .replace("&nbsp;", " ", ignoreCase = true)
            .split(Regex("<br\\s*/?>|[◇◆]|\\r?\\n|\\u000B|\\u2028", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val name = lines.firstOrNull()?.removeSuffix("…")?.removeSuffix("...")?.trim().orEmpty()
        if (name.isBlank()) return null
        val weekMatch = Regex("(\\d+)\\s*(?:-|~|至|到)\\s*(\\d+)\\s*周").find(raw)
        val singleWeekMatch = Regex("(?<![\\d-])(\\d+)\\s*周").find(raw)
        val rule = when {
            weekMatch != null -> WeekRule.fromText("${weekMatch.groupValues[1]}-${weekMatch.groupValues[2]}")
            singleWeekMatch != null -> WeekRule.fromText(singleWeekMatch.groupValues[1])
            raw.contains("每周") || raw.contains("全周") -> WeekRule.fromText("每周")
            raw.contains("单周") -> WeekRule.fromText("单周")
            raw.contains("双周") -> WeekRule.fromText("双周")
            else -> null
        } ?: return null
        val slotMatch = Regex("(\\d+)\\s*(?:-|~|至|到)\\s*(\\d+)\\s*节").find(raw)
        val singleSlotMatch = Regex("(?<![\\d-])(\\d+)\\s*节").find(raw)
        val startSlot = slotMatch?.groupValues?.get(1)?.toIntOrNull()
            ?: singleSlotMatch?.groupValues?.get(1)?.toIntOrNull()
            ?: return null
        val endSlot = slotMatch?.groupValues?.get(2)?.toIntOrNull() ?: startSlot
        if (startSlot !in 1..64 || endSlot !in startSlot..64) return null
        val detailStart = lines.indexOfFirst { it.contains("周") && it.contains("节") }.takeIf { it >= 0 } ?: 0
        var location = ""
        var teacher = ""
        lines.drop(detailStart + 1).forEach { line ->
            when {
                line.startsWithAny("地点", "上课地点", "教室") -> location = stripGridLabel(line)
                line.startsWithAny("教师", "任课教师", "老师") -> teacher = stripGridLabel(line)
                line.contains("学分") || line.contains("总学时") || line.contains("学时") -> Unit
                location.isBlank() -> location = line
                teacher.isBlank() -> teacher = line
            }
        }
        return GridCell(name, rule, startSlot, endSlot, location, teacher)
    }

    private fun String.startsWithAny(vararg prefixes: String): Boolean = prefixes.any { startsWith(it) }

    private fun stripGridLabel(value: String): String = value
        .substringAfterFirst("上课地点")
        .substringAfterFirst("地点")
        .substringAfterFirst("教室")
        .substringAfterFirst("任课教师")
        .substringAfterFirst("教师")
        .substringAfterFirst("老师")
        .trimStart(' ', ':', '：')
        .trim()

    private fun String.substringAfterFirst(prefix: String): String =
        if (startsWith(prefix)) removePrefix(prefix) else this

    private fun normalizeHeader(value: String): String = value.trim().lowercase()
        .replace(" ", "").replace("_", "").replace("-", "")

    private fun parseImportTime(value: String): Int? {
        parseClock(value)?.let { return it }
        // Excel commonly stores a time-only cell as a fraction of one day.
        val fraction = value.toDoubleOrNull() ?: return null
        if (fraction < 0.0 || fraction >= 1.0) return null
        return (fraction * 24 * 60).roundToInt().takeIf { it in 0 until 24 * 60 }
    }

    private fun parseWeekday(value: String): Int? {
        val raw = value.trim().removePrefix("星期").removePrefix("周")
        return when (raw.lowercase()) {
            "一", "1", "mon", "monday" -> 1
            "二", "2", "tue", "tuesday" -> 2
            "三", "3", "wed", "wednesday" -> 3
            "四", "4", "thu", "thursday" -> 4
            "五", "5", "fri", "friday" -> 5
            "六", "6", "sat", "saturday" -> 6
            "日", "天", "7", "sun", "sunday" -> 7
            else -> null
        }
    }

    private fun detectConflicts(rows: List<ImportRow>): List<String> {
        val conflicts = mutableListOf<String>()
        rows.forEachIndexed { index, left ->
            rows.drop(index + 1).forEach { right ->
                if (left.weekday == right.weekday && left.weekRule.intersects(right.weekRule) &&
                    timeRangesOverlap(left, right)
                ) {
                    conflicts += "第${left.rowNumber}行与第${right.rowNumber}行存在时间冲突（${left.courseName} / ${right.courseName}）"
                }
                if (left.courseName.equals(right.courseName, true) && left.weekday == right.weekday &&
                    left.startMinutes == right.startMinutes && left.endMinutes == right.endMinutes &&
                    left.weekRule.encode() == right.weekRule.encode()
                ) {
                    conflicts += "第${left.rowNumber}行与第${right.rowNumber}行疑似重复课程"
                }
            }
        }
        return conflicts.distinct()
    }

    private fun timeRangesOverlap(left: ImportRow, right: ImportRow): Boolean {
        if (left.startMinutes != null && left.endMinutes != null && right.startMinutes != null && right.endMinutes != null) {
            return left.startMinutes < right.endMinutes && right.startMinutes < left.endMinutes
        }
        if (left.startSlotNumber != null && right.startSlotNumber != null) {
            val leftEnd = left.endSlotNumber ?: left.startSlotNumber
            val rightEnd = right.endSlotNumber ?: right.startSlotNumber
            return left.startSlotNumber <= rightEnd && right.startSlotNumber <= leftEnd
        }
        return false
    }

    private fun cellName(row: Int, column: Int): String {
        var value = column + 1
        val result = StringBuilder()
        while (value > 0) {
            val remainder = (value - 1) % 26
            result.insert(0, ('A'.code + remainder).toChar())
            value = (value - 1) / 26
        }
        return "$result$row"
    }

    private fun parseDelimited(text: String): List<List<String>> {
        val delimiter = text.lineSequence().firstOrNull()?.let { line ->
            if (line.count { it == '\t' } > line.count { it == ',' }) '\t' else ','
        } ?: ','
        val result = mutableListOf<MutableList<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var index = 0
        while (index < text.length) {
            val char = text[index]
            when {
                char == '"' && quoted && index + 1 < text.length && text[index + 1] == '"' -> {
                    cell.append('"')
                    index++
                }
                char == '"' -> quoted = !quoted
                char == delimiter && !quoted -> {
                    row += cell.toString()
                    cell.clear()
                }
                (char == '\n' || char == '\r') && !quoted -> {
                    if (char == '\r' && index + 1 < text.length && text[index + 1] == '\n') index++
                    row += cell.toString()
                    cell.clear()
                    if (row.any { it.isNotBlank() }) result += row
                    row = mutableListOf()
                }
                else -> cell.append(char)
            }
            index++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row += cell.toString()
            if (row.any { it.isNotBlank() }) result += row
        }
        return result
    }

    private fun parseSharedStrings(bytes: ByteArray?): List<String> {
        if (bytes == null) return emptyList()
        val document = parseXml(bytes)
        val sharedItems = document.elementsByName("si")
        return (0 until sharedItems.length).map { index ->
            val item = sharedItems.item(index) as org.w3c.dom.Element
            val textNodes = item.elementsByName("t")
            (0 until textNodes.length).joinToString("") { textIndex ->
                textNodes.item(textIndex).textContent.orEmpty()
            }
        }
    }

    private fun parseSheet(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val document = parseXml(bytes)
        val rows = mutableListOf<List<String>>()
        val rowNodes = document.elementsByName("row")
        for (rowIndex in 0 until rowNodes.length) {
            val row = rowNodes.item(rowIndex) as org.w3c.dom.Element
            val current = mutableMapOf<Int, String>()
            val cellNodes = row.elementsByName("c")
            for (cellIndex in 0 until cellNodes.length) {
                val cell = cellNodes.item(cellIndex) as org.w3c.dom.Element
                val ref = cell.getAttribute("r")
                val column = ref.takeWhile { it.isLetter() }.fold(0) { acc, c -> acc * 26 + (c.uppercaseChar() - 'A' + 1) } - 1
                val type = cell.getAttribute("t")
                val valueNode = cell.elementsByName("v").item(0)
                var value = valueNode?.textContent.orEmpty()
                if (type == "s") value = sharedStrings.getOrNull(value.toIntOrNull() ?: -1).orEmpty()
                if (type == "inlineStr") value = cell.elementsByName("t").item(0)?.textContent.orEmpty()
                current[column] = value
            }
            val width = (current.keys.maxOrNull() ?: -1) + 1
            rows += (0 until width).map { current[it].orEmpty() }
        }
        return rows
    }

    private fun findFirstSheetPath(entries: Map<String, ByteArray>): String {
        val workbook = entries["xl/workbook.xml"] ?: return ""
        val rels = entries["xl/_rels/workbook.xml.rels"] ?: return ""
        val workbookDocument = parseXml(workbook)
        val sheet = workbookDocument.elementsByName("sheet").item(0) as? org.w3c.dom.Element ?: return ""
        val firstId = sheet.getAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id")
            .ifBlank { sheet.getAttribute("r:id") }
        if (firstId.isBlank()) return ""
        val relDocument = parseXml(rels)
        val relationshipNodes = relDocument.elementsByName("Relationship")
        for (index in 0 until relationshipNodes.length) {
            val relationship = relationshipNodes.item(index) as org.w3c.dom.Element
            if (relationship.getAttribute("Id") == firstId) {
                val target = relationship.getAttribute("Target")
                return if (target.startsWith("/")) target.removePrefix("/") else "xl/${target.removePrefix("xl/")}"
            }
        }
        return ""
    }

    private fun parseXml(bytes: ByteArray): org.w3c.dom.Document {
        // Android's XML implementation does not expose every Xerces feature. Keep
        // the hard security boundary here, then enable optional features when supported.
        val xml = bytes.toString(StandardCharsets.UTF_8)
        if (Regex("<!DOCTYPE|<!ENTITY", RegexOption.IGNORE_CASE).containsMatchIn(xml)) {
            throw IllegalArgumentException("XML DTD declarations are not supported")
        }
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            safeSetFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            safeSetFeature("http://xml.org/sax/features/external-general-entities", false)
            safeSetFeature("http://xml.org/sax/features/external-parameter-entities", false)
            safeSetFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            runCatching { isXIncludeAware = false }
            runCatching { isExpandEntityReferences = false }
            runCatching { setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "") }
            runCatching { setAttribute("http://javax.xml.XMLConstants/property/accessExternalSchema", "") }
        }
        return factory.newDocumentBuilder().apply {
            // Defense in depth for parsers that ignore the optional feature setters.
            setEntityResolver { _, _ -> InputSource(StringReader("")) }
        }.parse(ByteArrayInputStream(bytes))
    }

    private fun DocumentBuilderFactory.safeSetFeature(name: String, value: Boolean) {
        runCatching { setFeature(name, value) }
    }

    private fun org.w3c.dom.Document.elementsByName(localName: String): org.w3c.dom.NodeList =
        getElementsByTagName(localName).takeIf { it.length > 0 } ?: getElementsByTagNameNS("*", localName)

    private fun org.w3c.dom.Element.elementsByName(localName: String): org.w3c.dom.NodeList =
        getElementsByTagName(localName).takeIf { it.length > 0 } ?: getElementsByTagNameNS("*", localName)
}

fun ImportRow.toMeeting(timeSlots: List<TimeSlot> = emptyList()): MeetingEntity {
    val startSlot = startSlotNumber?.let { number -> timeSlots.firstOrNull { it.number == number } }
    val endSlot = (endSlotNumber ?: startSlotNumber)?.let { number -> timeSlots.firstOrNull { it.number == number } }
    return MeetingEntity(
        courseId = 0,
        weekday = weekday,
        startMinutes = if (startSlot != null && endSlot != null) null else startMinutes,
        endMinutes = if (startSlot != null && endSlot != null) null else endMinutes,
        startSlotId = startSlot?.id,
        endSlotId = endSlot?.id,
        weekRule = weekRule.encode()
    )
}
