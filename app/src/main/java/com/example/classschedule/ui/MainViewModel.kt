package com.example.classschedule.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.classschedule.data.CourseWithMeetings
import com.example.classschedule.data.ImportConflictStrategy
import com.example.classschedule.data.ImportPreview
import com.example.classschedule.data.ImportRow
import com.example.classschedule.data.MeetingEntity
import com.example.classschedule.data.ScheduleFileParser
import com.example.classschedule.data.ScheduleRepository
import com.example.classschedule.data.SettingsRepository
import com.example.classschedule.data.StandardFieldMapping
import com.example.classschedule.domain.AppSettings
import com.example.classschedule.domain.PhaseReminder
import com.example.classschedule.domain.ReminderConfig
import com.example.classschedule.domain.TermSettings
import com.example.classschedule.domain.ThemeMode
import com.example.classschedule.domain.TimeSlot
import com.example.classschedule.domain.WeekRule
import com.example.classschedule.domain.DefaultCourseColor
import com.example.classschedule.domain.allocateCourseColor
import com.example.classschedule.domain.formatMinutes
import com.example.classschedule.domain.parseClock
import com.example.classschedule.data.AlarmScheduler
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.nio.charset.StandardCharsets
import java.nio.charset.Charset
import java.io.ByteArrayInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MeetingDraft(
    val id: Long?,
    val weekday: Int,
    val startText: String,
    val endText: String,
    val weekRuleText: String,
    val slotText: String
)

class MainViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {
    val records: StateFlow<List<CourseWithMeetings>> = scheduleRepository.records.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings()
    )

    private val _importPreview = MutableStateFlow<ImportPreview?>(null)
    val importPreview: StateFlow<ImportPreview?> = _importPreview.asStateFlow()
    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    fun clearOperationMessage() { _operationMessage.value = null }

    fun selectDate(date: LocalDate) { _selectedDate.value = date }

    fun saveTerm(name: String, firstMondayText: String, startText: String, endText: String, complete: Boolean? = null) {
        val first = parseDate(firstMondayText) ?: return message("第一教学周周一日期无效")
        val start = parseDate(startText) ?: return message("学期开始日期无效")
        val end = parseDate(endText) ?: return message("学期结束日期无效")
        if (name.isBlank()) return message("学期名称不能为空")
        if (first.dayOfWeek != java.time.DayOfWeek.MONDAY) return message("第一教学周周一必须是周一")
        if (first.isBefore(start) || end.isBefore(start) || end.isBefore(first)) return message("学期日期范围无效")
        viewModelScope.launch {
            settingsRepository.saveTerm(TermSettings(name.trim(), first, start, end), complete)
            reschedule()
            message(if (complete == true) "学期已保存" else "学期设置已更新")
        }
    }

    fun completeOnboarding(name: String, firstMondayText: String, startText: String, endText: String, importMode: Boolean) {
        val first = parseDate(firstMondayText)
        val start = parseDate(startText)
        val end = parseDate(endText)
        if (name.isBlank() || first == null || start == null || end == null || first.dayOfWeek != java.time.DayOfWeek.MONDAY || first.isBefore(start) || end.isBefore(start) || end.isBefore(first)) {
            message("请完整填写有效的学期信息")
            return
        }
        viewModelScope.launch {
            settingsRepository.saveTerm(TermSettings(name.trim(), first, start, end), completeOnboarding = true)
            message(if (importMode) "学期已保存，请选择课表文件" else "学期已保存，请添加第一门课")
        }
    }

    fun saveCourse(
        id: Long?,
        name: String,
        teacher: String,
        location: String,
        colorHex: String,
        note: String,
        weekday: Int,
        startText: String,
        endText: String,
        weekRuleText: String,
        slotText: String,
        onSuccess: () -> Unit = {}
    ) {
        saveCourse(
            id = id,
            name = name,
            teacher = teacher,
            location = location,
            colorHex = colorHex,
            note = note,
            meetings = listOf(MeetingDraft(null, weekday, startText, endText, weekRuleText, slotText)),
            onSuccess = onSuccess
        )
    }

    fun saveCourse(
        id: Long?,
        name: String,
        teacher: String,
        location: String,
        colorHex: String,
        note: String,
        meetings: List<MeetingDraft>,
        onSuccess: () -> Unit = {}
    ) {
        if (name.isBlank()) return message("课程名不能为空")
        if (id == null && meetings.isEmpty()) return message("新增课程至少需要一条上课安排")
        val slots = settings.value.timeSlots
        val entities = meetings.mapIndexed { index, draft ->
            parseMeetingDraft(draft, slots, index + 1)
        }
        if (entities.any { it == null }) return
        viewModelScope.launch {
            val effectiveColor = colorHex.trim().ifBlank {
                if (id == null) {
                    allocateCourseColor(name, records.value.map { it.course.colorHex })
                } else {
                    DefaultCourseColor
                }
            }
            scheduleRepository.saveCourseWithMeetings(
                id = id,
                name = name.trim(),
                teacher = teacher.trim(),
                location = location.trim(),
                colorHex = effectiveColor,
                note = note.trim(),
                meetingsToSave = entities.filterNotNull()
            )
            reschedule()
            message("课程已保存")
            onSuccess()
        }
    }

    fun deleteCourse(record: CourseWithMeetings, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            scheduleRepository.deleteCourse(record)
            reschedule()
            message("课程已删除，可在 5 秒内撤销")
            deleted = record
            onSuccess()
        }
    }

    fun undoDelete() {
        val snapshot = deleted ?: return
        deleted = null
        viewModelScope.launch {
            scheduleRepository.restoreCourse(snapshot)
            reschedule()
            message("已撤销删除")
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setTheme(mode) }
    }

    fun setWeekZoom(value: Float) {
        viewModelScope.launch { settingsRepository.setWeekZoom(value.coerceIn(0.75f, 2f)) }
    }

    fun setShowAlmanac(value: Boolean) {
        viewModelScope.launch { settingsRepository.setShowAlmanac(value) }
    }

    fun ensureDistinctCourseColors() {
        viewModelScope.launch(Dispatchers.IO) { scheduleRepository.ensureDistinctCourseColors() }
    }

    fun setNotificationsEnabled(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(value)
            reschedule()
            message(if (value) "提醒已开启，请确认系统通知权限" else "提醒已关闭")
        }
    }

    fun saveReminders(config: ReminderConfig) {
        val phases = config.all
        val ordered = phases.sortedBy { it.stage.order }
        if (
            phases.any {
                it.boundaryStart !in 0 until 24 * 60 ||
                    it.boundaryEnd !in 1..(24 * 60) ||
                    it.boundaryStart >= it.boundaryEnd ||
                    it.notifyHour !in 0..23 ||
                    it.notifyMinute !in 0..59
            } || ordered.zipWithNext().any { (left, right) -> left.boundaryEnd > right.boundaryStart }
        ) {
            message("阶段边界或提醒时间无效")
            return
        }
        viewModelScope.launch {
            settingsRepository.saveReminders(config)
            reschedule()
            message("提醒设置已保存")
        }
    }

    fun addTimeSlot() {
        val old = settings.value.timeSlots
        val number = (old.maxOfOrNull { it.number } ?: 0) + 1
        val start = old.lastOrNull()?.endMinutes?.plus(10) ?: 8 * 60
        val slot = TimeSlot(number.toLong(), number, start, (start + 45).coerceAtMost(24 * 60))
        viewModelScope.launch { settingsRepository.saveSlots(old + slot) }
    }

    fun saveMissingTimeSlots(
        slotsToConfigure: Map<Int, Pair<String, String>>,
        onSuccess: () -> Unit = {}
    ) {
        val existing = settings.value.timeSlots
        if (slotsToConfigure.isEmpty()) return onSuccess()
        var nextId = existing.maxOfOrNull { it.id } ?: 0L
        val configured = slotsToConfigure.keys.sorted().map { number ->
            val (startText, endText) = slotsToConfigure.getValue(number)
            val start = parseClock(startText)
            val end = parseClock(endText)
            if (start == null || end == null || end <= start) {
                message("第${number}节的开始和结束时间无效")
                return
            }
            val current = existing.firstOrNull { it.number == number }
            current?.copy(startMinutes = start, endMinutes = end)
                ?: TimeSlot(++nextId, number, start, end)
        }
        viewModelScope.launch {
            val configuredNumbers = configured.map { it.number }.toSet()
            val next = (existing.filterNot { it.number in configuredNumbers } + configured).sortedBy { it.number }
            settingsRepository.saveSlots(next)
            reschedule()
            message("节次时间已确认")
            onSuccess()
        }
    }

    fun updateTimeSlot(slot: TimeSlot, startText: String, endText: String) {
        val start = parseClock(startText) ?: return message("节次开始时间无效")
        val end = parseClock(endText) ?: return message("节次结束时间无效")
        if (end <= start) return message("节次结束时间必须晚于开始时间")
        val next = settings.value.timeSlots.map { if (it.id == slot.id) slot.copy(startMinutes = start, endMinutes = end) else it }
        viewModelScope.launch { settingsRepository.saveSlots(next); reschedule() }
    }

    fun deleteTimeSlot(slot: TimeSlot) {
        viewModelScope.launch { settingsRepository.saveSlots(settings.value.timeSlots.filterNot { it.id == slot.id }); reschedule() }
    }

    fun parseFile(contentResolver: ContentResolver, uri: Uri, sourceName: String) {
        _importPreview.value = null
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                contentResolver.openInputStream(uri)?.use { input ->
                    val mime = contentResolver.getType(uri).orEmpty()
                    val bytes = input.readBytes()
                    when {
                        bytes.startsWith(byteArrayOf(0x50, 0x4B, 0x03, 0x04)) ||
                            sourceName.endsWith(".xlsx", true) && mime.contains("spreadsheetml", true) ->
                            ScheduleFileParser.parseXlsx(ByteArrayInputStream(bytes), sourceName)
                        bytes.startsWith(byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte())) ||
                            sourceName.endsWith(".xls", true) && mime.equals("application/vnd.ms-excel", true) ->
                            ScheduleFileParser.parseXls(ByteArrayInputStream(bytes), sourceName)
                        bytes.isLikelyText() ->
                            ScheduleFileParser.parseCsv(decodeCsv(bytes), sourceName)
                        else -> error("无法识别文件格式，请选择 CSV、XLSX 或 XLS 文件")
                    }
                } ?: error("无法打开文件")
            }.onSuccess { _importPreview.value = it }
                .onFailure { _importPreview.value = ImportPreview(sourceName, emptyList(), emptyList(), listOf(com.example.classschedule.data.ImportIssue(0, it.message ?: "读取文件失败", com.example.classschedule.data.IssueSeverity.ERROR)), emptyList()) }
        }
    }

    fun clearImportPreview() { _importPreview.value = null }

    fun remapImport(mapping: StandardFieldMapping) {
        val preview = _importPreview.value ?: return
        _importPreview.value = ScheduleFileParser.remapStandard(preview, mapping)
    }

    fun applyImport(strategy: ImportConflictStrategy, onSuccess: () -> Unit = {}) {
        val preview = _importPreview.value ?: return
        if (preview.hasErrors) return message("请先修正导入错误")
        val missingSlots = if (settings.value.timeSlotsConfigured) {
            preview.requiredSlotNumbers - settings.value.timeSlots.map { it.number }.toSet()
        } else {
            preview.requiredSlotNumbers
        }
        if (missingSlots.isNotEmpty()) return message("请先配置第${missingSlots.sorted().joinToString("、")}节的时间")
        viewModelScope.launch {
            val count = scheduleRepository.importRows(preview.rows, strategy, timeSlots = settings.value.timeSlots)
            clearImportPreview()
            reschedule()
            message("已导入 $count 条上课记录")
            onSuccess()
        }
    }

    suspend fun exportCsv(): String = withContext(Dispatchers.IO) {
        val rows = records.value.flatMap { record ->
            record.meetings.map { meeting ->
                val times = com.example.classschedule.domain.resolveMeeting(meeting, settings.value.timeSlots)
                listOf(
                    record.course.name, meeting.weekday.toString(),
                    times?.first?.let { formatMinutes(it) }.orEmpty(),
                    times?.second?.let { formatMinutes(it) }.orEmpty(),
                    record.course.location, record.course.teacher,
                    when (WeekRule.decode(meeting.weekRule).type) {
                        com.example.classschedule.domain.WeekRuleType.EVERY -> "每周"
                        com.example.classschedule.domain.WeekRuleType.ODD -> "单周"
                        com.example.classschedule.domain.WeekRuleType.EVEN -> "双周"
                        else -> meeting.weekRule
                    }, record.course.colorHex, record.course.note
                )
            }
        }
        buildString {
            appendLine("课程名,星期,开始时间,结束时间,地点,教师,周次,颜色,备注")
            rows.forEach { row -> appendLine(row.joinToString(",") { csvEscape(it) }) }
        }
    }

    fun hasExactAlarmAccess(): Boolean = alarmScheduler.exactAlarmAccess()

    fun rescheduleNow() = reschedule()

    private var deleted: CourseWithMeetings? = null

    private fun reschedule() { viewModelScope.launch(Dispatchers.IO) { alarmScheduler.reschedule() } }
    private fun message(value: String) { _operationMessage.value = value }
    private fun parseDate(value: String): LocalDate? = runCatching { LocalDate.parse(value.trim(), DATE_FORMAT) }.getOrNull()

    private fun parseMeetingDraft(draft: MeetingDraft, slots: List<TimeSlot>, index: Int): MeetingEntity? {
        if (draft.weekday !in 1..7) {
            message("第${index}条上课安排的星期无效")
            return null
        }
        val rule = WeekRule.fromText(draft.weekRuleText) ?: run {
            message("第${index}条上课安排的周次规则无法识别")
            return null
        }
        val slotNumbers = draft.slotText.trim().split(Regex("[-~至到\\s]+"), limit = 2).mapNotNull { it.toIntOrNull() }
        if (slotNumbers.isNotEmpty() && !settings.value.timeSlotsConfigured) {
            message("请先在设置中确认节次时间，再使用节次编号")
            return null
        }
        val startSlot = slotNumbers.firstOrNull()?.let { number -> slots.firstOrNull { it.number == number } }
        val endSlot = slotNumbers.getOrNull(1)?.let { number -> slots.firstOrNull { it.number == number } } ?: startSlot
        if (slotNumbers.isNotEmpty() && (startSlot == null || endSlot == null)) {
            message("第${index}条上课安排的节次未配置")
            return null
        }
        val directStart = parseClock(draft.startText)
        val directEnd = parseClock(draft.endText)
        val usesSlots = startSlot != null && endSlot != null
        if (!usesSlots && (directStart == null || directEnd == null || directEnd <= directStart)) {
            message("第${index}条上课安排的开始和结束时间无效")
            return null
        }
        return MeetingEntity(
            id = draft.id ?: 0,
            courseId = 0,
            weekday = draft.weekday,
            startMinutes = if (usesSlots) null else directStart,
            endMinutes = if (usesSlots) null else directEnd,
            startSlotId = startSlot?.id,
            endSlotId = endSlot?.id,
            weekRule = rule.encode()
        )
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean = size >= prefix.size &&
        prefix.indices.all { this[it] == prefix[it] }

    private fun ByteArray.isLikelyText(): Boolean = take(4096).none { it == 0.toByte() }

    private fun decodeCsv(bytes: ByteArray): String {
        val utf8 = bytes.toString(StandardCharsets.UTF_8)
        return if (utf8.contains('\uFFFD')) String(bytes, Charset.forName("GB18030")) else utf8
    }

    companion object {
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        private fun csvEscape(value: String): String = if (value.any { it == ',' || it == '"' || it == '\n' }) "\"${value.replace("\"", "\"\"")}\"" else value
    }
}
