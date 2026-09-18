package com.example.classschedule.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreTime
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TableView
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.classschedule.data.CourseWithMeetings
import com.example.classschedule.data.ImportConflictStrategy
import com.example.classschedule.data.ImportIssue
import com.example.classschedule.data.ImportPreview
import com.example.classschedule.data.MeetingEntity
import com.example.classschedule.domain.AppSettings
import com.example.classschedule.domain.CourseColorPalette
import com.example.classschedule.domain.PhaseReminder
import com.example.classschedule.domain.ReminderConfig
import com.example.classschedule.domain.ResolvedMeeting
import com.example.classschedule.domain.Stage
import com.example.classschedule.domain.TermSettings
import com.example.classschedule.domain.ThemeMode
import com.example.classschedule.domain.TimeSlot
import com.example.classschedule.domain.WeekRule
import com.example.classschedule.domain.WeekRuleType
import com.example.classschedule.domain.dayLabel
import com.example.classschedule.domain.defaultTerm
import com.example.classschedule.domain.formatMinutes
import com.example.classschedule.domain.resolveMeeting
import com.example.classschedule.domain.resolvedFor
import com.example.classschedule.domain.stageFor
import com.example.classschedule.domain.placeMeetingLanes
import com.example.classschedule.domain.slotSpanFor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.classschedule.R
import com.example.classschedule.BuildConfig

private val PagePadding = 16.dp
private val SmallRadius = RoundedCornerShape(8.dp)
private val DateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val WeekNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
private val CoursePalette = CourseColorPalette

private data class SlotTimeDraft(val start: String = "", val end: String = "")

private enum class DonationMethod(val title: String, val imageRes: Int) {
    ALIPAY("支付宝", R.drawable.donate_alipay),
    WECHAT("微信支付", R.drawable.donate_wechat)
}

@Composable
fun ScheduleRoot(
    viewModel: MainViewModel,
    initialStage: String?,
    requestNotifications: () -> Unit,
    requestExactAlarm: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val importPreview by viewModel.importPreview.collectAsStateWithLifecycle()
    var pendingRoute by remember { mutableStateOf<String?>(null) }
    var colorsChecked by rememberSaveable { mutableStateOf(false) }
    var exportUri by remember { mutableStateOf<Uri?>(null) }
    val contentResolver = androidx.compose.ui.platform.LocalContext.current.contentResolver
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.parseFile(contentResolver, it, displayName(contentResolver, it) ?: "课表文件") }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> exportUri = uri }
    LaunchedEffect(exportUri) {
        val uri = exportUri ?: return@LaunchedEffect
        runCatching {
            contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(viewModel.exportCsv()) }
                ?: error("无法写入导出文件")
        }.onSuccess { viewModel.clearOperationMessage(); viewModel.selectDate(viewModel.selectedDate.value) }
            .onFailure { viewModel.clearOperationMessage() }
        exportUri = null
    }
    LaunchedEffect(settings.notificationsEnabled, settings.term, settings.timeSlots, settings.reminderConfig) {
        viewModel.rescheduleNow()
    }
    LaunchedEffect(records) {
        if (!colorsChecked && records.size >= 2) {
            colorsChecked = true
            viewModel.ensureDistinctCourseColors()
        }
    }

    if (!settings.onboardingComplete) {
        OnboardingScreen(
            initialTerm = settings.term,
            onChooseImport = { name, monday, start, end ->
                viewModel.completeOnboarding(name, monday, start, end, importMode = true)
                pendingRoute = "import"
            },
            onChooseManual = { name, monday, start, end ->
                viewModel.completeOnboarding(name, monday, start, end, importMode = false)
                pendingRoute = "course_edit/0"
            }
        )
    } else {
        MainShell(
            viewModel = viewModel,
            settings = settings,
            records = records,
            importPreview = importPreview,
            startRoute = pendingRoute ?: "today",
            initialStage = initialStage,
            onPickImport = {
                importLauncher.launch(arrayOf("text/csv", "text/*", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel", "application/octet-stream"))
            },
            onExport = { exportLauncher.launch("课表-${LocalDate.now()}.csv") },
            requestNotifications = requestNotifications,
            requestExactAlarm = requestExactAlarm
        )
    }
}

@Composable
private fun MainShell(
    viewModel: MainViewModel,
    settings: AppSettings,
    records: List<CourseWithMeetings>,
    importPreview: ImportPreview?,
    startRoute: String,
    initialStage: String?,
    onPickImport: () -> Unit,
    onExport: () -> Unit,
    requestNotifications: () -> Unit,
    requestExactAlarm: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route.orEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val operationMessage by viewModel.operationMessage.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    LaunchedEffect(initialStage) {
        if (initialStage != null && navController.currentDestination?.route != "today") {
            navController.navigate("today") {
                launchSingleTop = true
            }
        }
    }
    LaunchedEffect(operationMessage) {
        val message = operationMessage ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = if (message.startsWith("课程已删除")) "撤销" else null,
            duration = if (message.startsWith("课程已删除")) SnackbarDuration.Long else SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
        viewModel.clearOperationMessage()
    }
    val openImport: () -> Unit = {
        navController.navigate("import") { launchSingleTop = true }
        onPickImport()
    }
    val mainRoute = route.substringBefore("?").substringBefore("/")
    val showBottomBar = mainRoute in setOf("today", "week", "courses", "settings")
    Scaffold(
        topBar = {
            ScheduleTopBar(
                route = mainRoute,
                canGoBack = !showBottomBar,
                onBack = { navController.popBackStack() },
                onAdd = if (mainRoute == "courses") ({ navController.navigate("course_edit/0") }) else null,
                onImport = if (mainRoute == "settings") openImport else null
            )
        },
        bottomBar = {
            if (showBottomBar) BottomNavigation(mainRoute) { destination ->
                navController.navigate(destination) {
                    popUpTo("today") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(padding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable("today") {
                TodayScreen(viewModel, settings, records, initialStage, onOpenCourse = { navController.navigate("course_edit/$it") })
            }
            composable("week") {
                WeekScreen(viewModel, settings, records, onOpenCourse = { navController.navigate("course_edit/$it") })
            }
            composable("courses") {
                CoursesScreen(viewModel, settings, records, onOpenCourse = { navController.navigate("course_edit/$it") })
            }
            composable("settings") {
                SettingsScreen(viewModel, settings, openImport, onExport, requestNotifications, requestExactAlarm) {
                    navController.navigate("donation")
                }
            }
            composable("donation") {
                DonationScreen()
            }
            composable("import") {
                ImportScreen(
                    viewModel = viewModel,
                    settings = settings,
                    preview = importPreview,
                    onPickImport = onPickImport,
                    onOpenSlotConfirm = { navController.navigate("slot_confirm") },
                    onDone = { navController.navigate("courses") { popUpTo("import") { inclusive = true } } }
                )
            }
            composable("slot_confirm") {
                SlotConfirmScreen(
                    settings = settings,
                    preview = importPreview,
                    onSave = { values -> viewModel.saveMissingTimeSlots(values) { navController.popBackStack() } }
                )
            }
            composable("course_edit/{courseId}") { entry ->
                val id = entry.arguments?.getString("courseId")?.toLongOrNull() ?: 0L
                CourseEditScreen(viewModel, settings, records.firstOrNull { it.course.id == id }, onSaved = { navController.popBackStack() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleTopBar(
    route: String,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onAdd: (() -> Unit)?,
    onImport: (() -> Unit)?
) {
    val title = when (route) {
        "today" -> "今日"
        "week" -> "周课表"
        "courses" -> "课程"
        "settings" -> "设置"
        "import" -> "导入课表"
        "slot_confirm" -> "确认节次时间"
        "course_edit" -> "课程编辑"
        "donation" -> "打赏作者"
        else -> "不美鸡课表"
    }
    CenterAlignedTopAppBar(
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            if (canGoBack) IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
            }
        },
        actions = {
            onImport?.let {
                IconButton(onClick = it) { Icon(Icons.Outlined.FileUpload, contentDescription = "导入课表") }
            }
            onAdd?.let {
                IconButton(onClick = it) { Icon(Icons.Outlined.Add, contentDescription = "新增课程") }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}

@Composable
private fun BottomNavigation(current: String, onNavigate: (String) -> Unit) {
    val items = listOf(
        Triple("today", "今日", Icons.Outlined.Today),
        Triple("week", "课表", Icons.Outlined.CalendarViewWeek),
        Triple("courses", "课程", Icons.Outlined.School),
        Triple("settings", "设置", Icons.Outlined.Settings)
    )
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        items.forEach { (route, label, icon) ->
            val selected = current == route
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onNavigate(route) }
                    )
                    .semantics { contentDescription = label },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.secondaryContainer
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = label,
                    color = if (selected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun OnboardingScreen(
    initialTerm: TermSettings,
    onChooseImport: (String, String, String, String) -> Unit,
    onChooseManual: (String, String, String, String) -> Unit
) {
    var name by remember(initialTerm) { mutableStateOf(initialTerm.name) }
    var monday by remember(initialTerm) { mutableStateOf(initialTerm.firstMonday.format(DateFormat)) }
    var start by remember(initialTerm) { mutableStateOf(initialTerm.startDate.format(DateFormat)) }
    var end by remember(initialTerm) { mutableStateOf(initialTerm.endDate.format(DateFormat)) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = SmallRadius, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp).size(28.dp))
            }
            Column {
                Text("不美鸡课表", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("先建立这一学期的时间坐标", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("01 / 02  学期信息", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        HorizontalDivider()
        LabeledTextField("学期名称", name, { name = it }, "例如：2026 秋季学期")
        LabeledTextField("第一教学周周一", monday, { monday = it }, "yyyy-MM-dd")
        LabeledTextField("学期开始日期", start, { start = it }, "yyyy-MM-dd")
        LabeledTextField("学期结束日期", end, { end = it }, "yyyy-MM-dd")
        Spacer(Modifier.height(4.dp))
        Text("02 / 02  选择建立方式", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text("提醒权限会在你主动开启提醒时申请。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FilledTonalButton(
            onClick = { onChooseImport(name, monday, start, end) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
        ) {
            Icon(Icons.Outlined.FileUpload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("导入课表文件")
        }
        OutlinedButton(
            onClick = { onChooseManual(name, monday, start, end) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
        ) {
            Icon(Icons.Outlined.Edit, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("手动添加第一门课")
        }
    }
}

@Composable
private fun TodayScreen(
    viewModel: MainViewModel,
    settings: AppSettings,
    records: List<CourseWithMeetings>,
    initialStage: String?,
    onOpenCourse: (Long) -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val resolved = records.resolvedFor(selectedDate, settings.term, settings.timeSlots)
    val today = LocalDate.now()
    val next = resolved.firstOrNull { selectedDate != today || it.endMinutes > LocalTime.now().hour * 60 + LocalTime.now().minute }
    val focusedStage = remember(initialStage) { initialStage?.let(Stage::fromKey) }
    val focusRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(focusedStage, selectedDate) {
        if (focusedStage != null) {
            delay(120)
            focusRequester.bringIntoView()
        }
    }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(PagePadding), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        DateSwitcher(selectedDate, viewModel::selectDate)
        if (settings.showAlmanac) AlmanacCard(selectedDate)
        Text("${settings.term.name} · 第${com.example.classschedule.domain.weekNumber(selectedDate, settings.term) ?: "-"}周", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (next != null) {
            NextLesson(next, focusedStage)
        } else {
            EmptyState("今天没有安排课程", "把时间留给复习、阅读或休息", Icons.Outlined.EventNote)
        }
        Stage.entries.forEach { stage ->
            val stageCourses = resolved.filter { stageFor(it.startMinutes, settings.reminderConfig) == stage }
            StageHeader(
                stage,
                stageCourses.size,
                modifier = if (stage == focusedStage) Modifier.bringIntoViewRequester(focusRequester) else Modifier,
                focused = stage == focusedStage
            )
            if (stageCourses.isEmpty()) {
                Text("暂无课程", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp, bottom = 4.dp))
            } else {
                stageCourses.forEach { item -> CourseLine(item, onClick = { onOpenCourse(item.course.id) }) }
            }
        }
    }
}

@Composable
private fun DateSwitcher(date: LocalDate, onDateChange: (LocalDate) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = { onDateChange(date.minusDays(1)) }) { Icon(Icons.Outlined.ChevronLeft, contentDescription = "前一天") }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (date == LocalDate.now()) "今天" else date.format(DateTimeFormatter.ofPattern("M月d日")), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${dayLabel(date.dayOfWeek.value)}  ${date.format(DateFormat)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = { onDateChange(date.plusDays(1)) }) { Icon(Icons.Outlined.ChevronRight, contentDescription = "后一天") }
    }
}

@Composable
private fun AlmanacCard(date: LocalDate) {
    val info = remember(date) { almanacForDate(date) }
    Surface(
        modifier = Modifier.fillMaxWidth().clip(SmallRadius),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Column(Modifier.padding(start = 10.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(if (date == LocalDate.now()) "今日黄历" else "黄历", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text("${info.lunarDateLabel} · ${info.ganzhiYear}（${info.zodiac}）", style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (info.detailAvailable) {
                AlmanacActivityLine("宜", info.dayYi)
                AlmanacActivityLine("忌", info.dayJi)
            } else {
                Text("本地暂无详细宜忌数据", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AlmanacActivityLine(label: String, activities: List<String>) {
    val tint = if (label == "宜") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = tint, modifier = Modifier.width(28.dp))
        Text(
            activities.take(5).joinToString("、").ifBlank { "暂无" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NextLesson(item: ResolvedMeeting, focusedStage: Stage?) {
    val stripe = parseColor(item.course.colorHex)
    Surface(
        modifier = Modifier.fillMaxWidth().clip(SmallRadius),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Row(modifier = Modifier.heightIn(min = 112.dp)) {
            Box(Modifier.width(6.dp).fillMaxHeight().background(stripe))
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (focusedStage != null) "${focusedStage.label}提醒定位" else "下一节课", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(formatMinutes(item.startMinutes), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("至 ${formatMinutes(item.endMinutes)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
                }
                Text(item.course.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOf(item.course.location, item.course.teacher).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "未填写地点和教师" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun StageHeader(stage: Stage, count: Int, modifier: Modifier = Modifier, focused: Boolean = false) {
    Row(modifier = modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stage.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Text("$count 节", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (focused) {
            Spacer(Modifier.weight(1f))
            Text("通知定位", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun CourseLine(item: ResolvedMeeting, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(SmallRadius).clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(4.dp).height(48.dp).clip(RoundedCornerShape(2.dp)).background(parseColor(item.course.colorHex)))
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(item.course.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${formatMinutes(item.startMinutes)}-${formatMinutes(item.endMinutes)}  ${item.course.location.ifBlank { "地点待定" }}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(dayLabel(item.meeting.weekday), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WeekScreen(viewModel: MainViewModel, settings: AppSettings, records: List<CourseWithMeetings>, onOpenCourse: (Long) -> Unit) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val weekStart = selectedDate.with(DayOfWeek.MONDAY)
    val horizontal = rememberScrollState()
    val vertical = rememberScrollState()
    val zoomControlsScroll = rememberScrollState()
    var zoom by rememberSaveable { mutableFloatStateOf(settings.weekZoom) }
    val slots = settings.timeSlots
    val coursesByDay = remember(records, settings.term, settings.timeSlots, weekStart) {
        (1..7).associateWith { day -> records.resolvedFor(weekStart.plusDays((day - 1).toLong()), settings.term, slots) }
    }
    LaunchedEffect(settings.weekZoom) {
        if ((zoom - settings.weekZoom).absoluteValue > 0.01f) zoom = settings.weekZoom
    }
    LaunchedEffect(zoom) {
        delay(300)
        if ((zoom - settings.weekZoom).absoluteValue > 0.01f) viewModel.setWeekZoom(zoom)
    }
    val transformState = rememberTransformableState { zoomChange, _, _ ->
        zoom = (zoom * zoomChange).coerceIn(0.75f, 2f)
    }
    val cellWidth = 104.dp * zoom
    val cellHeight = 74.dp * zoom
    val axisWidth = 58.dp * zoom
    val headerHeight = 50.dp * zoom
    val boardWidth = axisWidth + cellWidth * 7
    val boardTextStyle = MaterialTheme.typography.labelSmall.copy(fontSize = (12f * zoom).coerceIn(10f, 16f).sp)
    Column(Modifier.fillMaxSize().padding(top = 8.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = PagePadding), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.selectDate(weekStart.minusDays(1)) }) { Icon(Icons.Outlined.ChevronLeft, contentDescription = "上一周") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${weekStart.format(DateTimeFormatter.ofPattern("M月d日"))} - ${weekStart.plusDays(6).format(DateTimeFormatter.ofPattern("M月d日"))}", fontWeight = FontWeight.Bold)
                Text("第${com.example.classschedule.domain.weekNumber(selectedDate, settings.term) ?: "-"}周", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { viewModel.selectDate(weekStart.plusWeeks(1)) }) { Icon(Icons.Outlined.ChevronRight, contentDescription = "下一周") }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(zoomControlsScroll).padding(horizontal = PagePadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = { zoom = (zoom - 0.1f).coerceAtLeast(0.75f) }) { Icon(Icons.Outlined.ZoomOut, contentDescription = "缩小课表") }
            Text("${(zoom * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, modifier = Modifier.semantics { contentDescription = "课表缩放 ${(zoom * 100).roundToInt()}%" })
            IconButton(onClick = { zoom = 1f }) { Icon(Icons.Outlined.Refresh, contentDescription = "恢复课表缩放") }
            IconButton(onClick = { zoom = (zoom + 0.1f).coerceAtMost(2f) }) { Icon(Icons.Outlined.ZoomIn, contentDescription = "放大课表") }
        }
        HorizontalDivider()
        Box(
            Modifier.fillMaxSize()
                .horizontalScroll(horizontal)
                .verticalScroll(vertical)
                .transformable(
                    state = transformState,
                    canPan = { false },
                    lockRotationOnZoomPan = true
                )
        ) {
            WeekBoard(
                weekStart = weekStart,
                slots = slots,
                coursesByDay = coursesByDay,
                cellWidth = cellWidth,
                cellHeight = cellHeight,
                axisWidth = axisWidth,
                headerHeight = headerHeight,
                boardWidth = boardWidth,
                boardTextStyle = boardTextStyle,
                zoom = zoom,
                onOpenCourse = onOpenCourse
            )
        }
    }
}

@Composable
private fun WeekBoard(
    weekStart: LocalDate,
    slots: List<com.example.classschedule.domain.TimeSlot>,
    coursesByDay: Map<Int, List<ResolvedMeeting>>,
    cellWidth: Dp,
    cellHeight: Dp,
    axisWidth: Dp,
    headerHeight: Dp,
    boardWidth: Dp,
    boardTextStyle: androidx.compose.ui.text.TextStyle,
    zoom: Float,
    onOpenCourse: (Long) -> Unit
) {
    val gridHeight = cellHeight * slots.size
    Box(Modifier.width(boardWidth).height(headerHeight + gridHeight)) {
        Column(Modifier.width(boardWidth)) {
            Row(Modifier.height(headerHeight)) {
                Box(Modifier.width(axisWidth).height(headerHeight))
                WeekNames.forEachIndexed { index, name ->
                    val date = weekStart.plusDays(index.toLong())
                    Column(
                        Modifier.width(cellWidth).height(headerHeight).padding(horizontal = 4.dp * zoom),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(name, style = boardTextStyle, fontWeight = FontWeight.Bold)
                        Text(
                            date.dayOfMonth.toString(),
                            style = boardTextStyle,
                            color = if (date == LocalDate.now()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            slots.forEach { slot ->
                Row(Modifier.height(cellHeight)) {
                    Box(Modifier.width(axisWidth).height(cellHeight), contentAlignment = Alignment.TopCenter) {
                        Text(
                            "${slot.number}\n${formatMinutes(slot.startMinutes)}",
                            style = boardTextStyle,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp * zoom)
                        )
                    }
                    (1..7).forEach {
                        Box(
                            Modifier.width(cellWidth).height(cellHeight).padding(2.dp * zoom)
                                .border(0.5.dp * zoom, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), SmallRadius)
                        )
                    }
                }
            }
        }

        coursesByDay.forEach { (day, dayCourses) ->
            val placements = placeMeetingLanes(dayCourses)
            placements.forEach { placement ->
                val item = placement.item
                val span = slotSpanFor(item.startMinutes, item.endMinutes, slots) ?: return@forEach
                val laneGap = 3.dp * zoom
                val laneCount = placement.laneCount.coerceAtLeast(1)
                val laneWidth = ((cellWidth.value - laneGap.value * (laneCount + 1)) / laneCount).coerceAtLeast(24f).dp
                val x = axisWidth + cellWidth * (day - 1) + laneGap + (laneWidth + laneGap) * placement.lane
                val top = headerHeight + cellHeight * span.topPosition
                val bottom = headerHeight + cellHeight * span.bottomPosition
                val cardHeight = (bottom - top - 4.dp * zoom).coerceAtLeast(28.dp * zoom)
                val courseColor = parseColor(item.course.colorHex)
                Box(
                    modifier = Modifier.offset(x = x, y = top)
                        .width(laneWidth)
                        .height(cardHeight)
                        .clip(SmallRadius)
                        .background(courseColor.copy(alpha = 0.16f))
                        .border(1.dp * zoom, courseColor.copy(alpha = 0.65f), SmallRadius)
                        .clickable { onOpenCourse(item.course.id) }
                ) {
                    Row(Modifier.fillMaxSize()) {
                        Box(Modifier.width(4.dp * zoom).fillMaxHeight().background(courseColor))
                        Column(Modifier.fillMaxSize().padding(4.dp * zoom), verticalArrangement = Arrangement.spacedBy(1.dp * zoom)) {
                            Text(
                                item.course.name,
                                style = boardTextStyle,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${formatMinutes(item.startMinutes)}-${formatMinutes(item.endMinutes)}",
                                style = boardTextStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoursesScreen(viewModel: MainViewModel, settings: AppSettings, records: List<CourseWithMeetings>, onOpenCourse: (Long) -> Unit) {
    Column(Modifier.fillMaxSize().padding(PagePadding)) {
        Text("课程目录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("${records.size} 门课程 · 点击进入编辑", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
        if (records.isEmpty()) {
            EmptyState("还没有课程", "从右上角新增，或在设置中导入课表", Icons.Outlined.MenuBook)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(records, key = { it.course.id }) { record ->
                    CourseCatalogRow(
                        record = record,
                        slots = settings.timeSlots,
                        onClick = { onOpenCourse(record.course.id) },
                        onDelete = { viewModel.deleteCourse(record) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CourseCatalogRow(
    record: CourseWithMeetings,
    slots: List<TimeSlot>,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(SmallRadius).clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.42f))
    ) {
        Row(Modifier.heightIn(min = 76.dp).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(5.dp).height(52.dp).clip(RoundedCornerShape(2.dp)).background(parseColor(record.course.colorHex)))
            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(record.course.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(record.meetings.sortedWith(compareBy({ it.weekday }, { resolveMeeting(it, slots)?.first ?: Int.MAX_VALUE })).joinToString("  ") { meeting ->
                    val time = resolveMeeting(meeting, slots)
                    "${dayLabel(meeting.weekday)}${time?.let { " ${formatMinutes(it.first)}" }.orEmpty()}"
                }.ifBlank { "暂无上课安排" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOf(record.course.teacher, record.course.location).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "未填写教师和地点" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Outlined.Edit, contentDescription = "编辑${record.course.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "删除${record.course.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseEditScreen(viewModel: MainViewModel, settings: AppSettings, record: CourseWithMeetings?, onSaved: () -> Unit) {
    var name by remember(record?.course?.id) { mutableStateOf(record?.course?.name.orEmpty()) }
    var teacher by remember(record?.course?.id) { mutableStateOf(record?.course?.teacher.orEmpty()) }
    var location by remember(record?.course?.id) { mutableStateOf(record?.course?.location.orEmpty()) }
    var note by remember(record?.course?.id) { mutableStateOf(record?.course?.note.orEmpty()) }
    var color by remember(record?.course?.id) { mutableStateOf(record?.course?.colorHex.orEmpty()) }
    var showLastMeetingDialog by remember(record?.course?.id) { mutableStateOf(false) }
    var meetings by remember(record?.course?.id, settings.timeSlots) {
        mutableStateOf(
            if (record == null) {
                listOf(MeetingDraft(null, 1, "", "", "每周", ""))
            } else {
                record.meetings
                    .sortedWith(compareBy<MeetingEntity>({ it.weekday }, { resolveMeeting(it, settings.timeSlots)?.first ?: Int.MAX_VALUE }))
                    .map { meeting ->
                        val times = resolveMeeting(meeting, settings.timeSlots)
                        MeetingDraft(
                            id = meeting.id,
                            weekday = meeting.weekday,
                            startText = times?.first?.let(::formatMinutes).orEmpty(),
                            endText = times?.second?.let(::formatMinutes).orEmpty(),
                            weekRuleText = displayWeekRule(WeekRule.decode(meeting.weekRule)),
                            slotText = displaySlotRange(meeting, settings.timeSlots)
                        )
                    }
            }
        )
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(PagePadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (record == null) "新增课程" else "编辑课程", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        LabeledTextField("课程名称", name, { name = it }, "必填")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            LabeledTextField("教师", teacher, { teacher = it }, "可选", Modifier.weight(1f))
            LabeledTextField("地点", location, { location = it }, "可选", Modifier.weight(1f))
        }
        Text("上课安排（${meetings.size}）", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
        meetings.forEachIndexed { index, draft ->
            MeetingEditor(
                index = index,
                draft = draft,
                onChange = { next -> meetings = meetings.mapIndexed { itemIndex, item -> if (itemIndex == index) next else item } },
                onDelete = {
                    if (meetings.size == 1 && record != null) {
                        showLastMeetingDialog = true
                    } else {
                        meetings = meetings.filterIndexed { itemIndex, _ -> itemIndex != index }
                    }
                }
            )
        }
        if (meetings.isEmpty()) {
            Text("当前课程没有上课安排，可点击下方添加。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(
            onClick = { meetings += MeetingDraft(null, 1, "", "", "每周", "") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Icon(Icons.Outlined.AddCircleOutline, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("添加上课安排")
        }
        Text("课程颜色", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            if (color.isBlank()) "未手动指定，保存时自动分配" else "已选择颜色，可在课表中快速区分",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
        ) {
            CoursePalette.forEach { candidate ->
                val selected = candidate.equals(color, true)
                Box(
                    Modifier.size(48.dp)
                        .clip(SmallRadius)
                        .background(parseColor(candidate))
                        .border(if (selected) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, SmallRadius)
                        .clickable { color = candidate }
                        .semantics { contentDescription = "选择课程颜色 ${candidate.removePrefix("#")}" }
                )
            }
        }
        LabeledTextField("备注", note, { note = it }, "可选", minLines = 3)
        Button(
            onClick = {
                viewModel.saveCourse(
                    id = record?.course?.id,
                    name = name,
                    teacher = teacher,
                    location = location,
                    colorHex = color,
                    note = note,
                    meetings = meetings,
                    onSuccess = onSaved
                )
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
        ) { Icon(Icons.Outlined.CheckCircleOutline, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("保存课程") }
    }
    if (showLastMeetingDialog) {
        AlertDialog(
            onDismissRequest = { showLastMeetingDialog = false },
            title = { Text("删除最后一条上课安排？") },
            text = { Text("可以只保留课程信息，也可以同时删除这门课程。") },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = {
                        meetings = emptyList()
                        showLastMeetingDialog = false
                    }, modifier = Modifier.heightIn(min = 48.dp)) { Text("仅删除安排") }
                    TextButton(onClick = {
                        record?.let { existingRecord ->
                            viewModel.deleteCourse(existingRecord) {
                                showLastMeetingDialog = false
                                onSaved()
                            }
                        }
                    }, modifier = Modifier.heightIn(min = 48.dp)) { Text("删除课程") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showLastMeetingDialog = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text("取消") }
            }
        )
    }
}

@Composable
private fun MeetingEditor(
    index: Int,
    draft: MeetingDraft,
    onChange: (MeetingDraft) -> Unit,
    onDelete: () -> Unit
) {
    var weekdayExpanded by remember(draft.id, index) { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().clip(SmallRadius),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("安排 ${index + 1}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "删除第${index + 1}条上课安排")
                }
            }
            Box {
                OutlinedTextField(
                    value = dayLabel(draft.weekday),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("星期") },
                    modifier = Modifier.fillMaxWidth().clickable { weekdayExpanded = true }
                )
                androidx.compose.material3.DropdownMenu(expanded = weekdayExpanded, onDismissRequest = { weekdayExpanded = false }) {
                    (1..7).forEach { day ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(dayLabel(day)) },
                            onClick = { onChange(draft.copy(weekday = day)); weekdayExpanded = false }
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                LabeledTextField("开始时间", draft.startText, { onChange(draft.copy(startText = it)) }, "08:00", Modifier.weight(1f))
                LabeledTextField("结束时间", draft.endText, { onChange(draft.copy(endText = it)) }, "09:40", Modifier.weight(1f))
            }
            LabeledTextField("节次（可选）", draft.slotText, { onChange(draft.copy(slotText = it)) }, "例如：1-2；填写后跟随节次设置")
            LabeledTextField("周次规则", draft.weekRuleText, { onChange(draft.copy(weekRuleText = it)) }, "每周、单周、双周、1-16、1,3,5")
        }
    }
}

@Composable
private fun ImportScreen(
    viewModel: MainViewModel,
    settings: AppSettings,
    preview: ImportPreview?,
    onPickImport: () -> Unit,
    onOpenSlotConfirm: () -> Unit,
    onDone: () -> Unit
) {
    var strategy by remember { mutableStateOf(ImportConflictStrategy.MERGE) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(PagePadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (preview == null) {
            EmptyState("选择一个课表文件", "支持 CSV、标准 XLSX 和旧版 XLS；导入前会先预览并检查", Icons.Outlined.UploadFile)
            FilledTonalButton(onClick = onPickImport, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Icon(Icons.Outlined.FileUpload, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("选择文件") }
            Text("必需字段：课程名、星期、开始时间、结束时间", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("可选字段：地点、教师、周次、颜色、备注", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("教务系统格状课表会读取星期、周次、节次、地点和教师；节次时间未配置时需先到设置确认。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(preview.sourceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("识别格式：${preview.format.label}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                if (preview.format == com.example.classschedule.data.ImportFormat.CAMPUS_GRID) {
                    "已按星期、周次和节次解析单元格内容"
                } else {
                    "已识别字段：${preview.headers.joinToString("、")}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (preview.format == com.example.classschedule.data.ImportFormat.STANDARD_TABLE && preview.fieldMapping != null) {
                val mapping = preview.fieldMapping
                Text("字段映射", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("自动识别的列可以调整；必需字段未映射时不能确认导入。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ImportFieldPicker("课程名", true, mapping.courseName, preview.headers) { value -> viewModel.remapImport(mapping.copy(courseName = value)) }
                ImportFieldPicker("星期", true, mapping.weekday, preview.headers) { value -> viewModel.remapImport(mapping.copy(weekday = value)) }
                ImportFieldPicker("开始时间", true, mapping.startTime, preview.headers) { value -> viewModel.remapImport(mapping.copy(startTime = value)) }
                ImportFieldPicker("结束时间", true, mapping.endTime, preview.headers) { value -> viewModel.remapImport(mapping.copy(endTime = value)) }
                ImportFieldPicker("地点", false, mapping.location, preview.headers) { value -> viewModel.remapImport(mapping.copy(location = value)) }
                ImportFieldPicker("教师", false, mapping.teacher, preview.headers) { value -> viewModel.remapImport(mapping.copy(teacher = value)) }
                ImportFieldPicker("周次", false, mapping.weekRule, preview.headers) { value -> viewModel.remapImport(mapping.copy(weekRule = value)) }
                ImportFieldPicker("颜色", false, mapping.color, preview.headers) { value -> viewModel.remapImport(mapping.copy(color = value)) }
                ImportFieldPicker("备注", false, mapping.note, preview.headers) { value -> viewModel.remapImport(mapping.copy(note = value)) }
            }
            Text("数据预览（${preview.rows.size} 条有效记录）", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            if (preview.rows.isEmpty()) Text("没有可导入的有效记录", color = MaterialTheme.colorScheme.error)
            else {
                Row(Modifier.horizontalScroll(rememberScrollState()).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .5f), SmallRadius)) {
                    Column(Modifier.width(72.dp)) { PreviewCell("来源", true); preview.rows.take(8).forEach { PreviewCell(it.sourceCell.ifBlank { "第${it.rowNumber}行" }) } }
                    Column(Modifier.width(220.dp)) { PreviewCell("原始单元格内容", true); preview.rows.take(8).forEach { PreviewCell(it.sourceValue, maxLines = 3) } }
                    Column(Modifier.width(150.dp)) { PreviewCell("课程名", true); preview.rows.take(8).forEach { PreviewCell(it.courseName) } }
                    Column(Modifier.width(80.dp)) { PreviewCell("星期", true); preview.rows.take(8).forEach { PreviewCell(dayLabel(it.weekday)) } }
                    Column(Modifier.width(132.dp)) {
                        PreviewCell("时间/节次", true)
                        preview.rows.take(8).forEach {
                            PreviewCell(
                                if (it.startSlotNumber != null) "第${it.startSlotNumber}-${it.endSlotNumber ?: it.startSlotNumber}节"
                                else if (it.startMinutes != null && it.endMinutes != null) "${formatMinutes(it.startMinutes)}-${formatMinutes(it.endMinutes)}"
                                else "-"
                            )
                        }
                    }
                    Column(Modifier.width(90.dp)) { PreviewCell("周次", true); preview.rows.take(8).forEach { PreviewCell(displayWeekRule(it.weekRule)) } }
                    Column(Modifier.width(130.dp)) { PreviewCell("地点", true); preview.rows.take(8).forEach { PreviewCell(it.location) } }
                    Column(Modifier.width(120.dp)) { PreviewCell("教师", true); preview.rows.take(8).forEach { PreviewCell(it.teacher) } }
                }
            }
            if (preview.issues.isNotEmpty()) {
                Text("校验结果", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                preview.issues.take(12).forEach { issue -> IssueRow(issue) }
            }
            val requiredSlots = preview.requiredSlotNumbers
            val slotsNeedingConfirmation = if (settings.timeSlotsConfigured) {
                requiredSlots - settings.timeSlots.map { it.number }.toSet()
            } else {
                requiredSlots
            }
            if (slotsNeedingConfirmation.isNotEmpty()) {
                Text("需先配置节次时间", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(
                    "文件使用了第${slotsNeedingConfirmation.sorted().joinToString("、")}节。${if (settings.timeSlotsConfigured) "请补充这些节次的" else "首次导入前请确认这些节次的"}开始和结束时间，再返回导入。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                OutlinedButton(onClick = onOpenSlotConfirm, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icon(Icons.Outlined.AccessTime, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("确认缺失节次时间")
                }
            }
            Text("重复项处理", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ImportConflictStrategy.entries.forEach { option ->
                    FilterChip(selected = strategy == option, onClick = { strategy = option }, label = { Text(when (option) { ImportConflictStrategy.MERGE -> "合并"; ImportConflictStrategy.OVERWRITE -> "覆盖匹配项"; ImportConflictStrategy.CANCEL -> "取消" }) }, modifier = Modifier.heightIn(min = 48.dp))
                }
            }
            Text("冲突默认只警告，不会阻止导入。覆盖只作用于匹配课程，不会直接清空旧课表。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { viewModel.applyImport(strategy, onDone) }, enabled = !preview.hasErrors && preview.rows.isNotEmpty() && slotsNeedingConfirmation.isEmpty() && strategy != ImportConflictStrategy.CANCEL, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("确认导入") }
            OutlinedButton(onClick = viewModel::clearImportPreview, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("重新选择文件") }
        }
    }
}

@Composable
private fun ImportFieldPicker(
    label: String,
    required: Boolean,
    selected: Int?,
    headers: List<String>,
    onSelect: (Int?) -> Unit
) {
    var expanded by remember(label, headers) { mutableStateOf(false) }
    val selectedLabel = selected?.let { headers.getOrNull(it) }.orEmpty().ifBlank { "未选择" }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(if (required) "$label（必需）" else label) },
            modifier = Modifier.fillMaxWidth().clickable { expanded = true }
        )
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("未选择") },
                onClick = { onSelect(null); expanded = false },
                modifier = Modifier.heightIn(min = 48.dp)
            )
            headers.forEachIndexed { index, header ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text("${index + 1}. ${header.ifBlank { "未命名列" }}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = { onSelect(index); expanded = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }
    }
}

@Composable
private fun SlotConfirmScreen(
    settings: AppSettings,
    preview: ImportPreview?,
    onSave: (Map<Int, Pair<String, String>>) -> Unit
) {
    val requiredSlots = preview?.requiredSlotNumbers.orEmpty()
    val missingSlots = (if (settings.timeSlotsConfigured) {
        requiredSlots.filter { number -> settings.timeSlots.none { it.number == number } }
    } else {
        requiredSlots.toList()
    }).sorted()
    var drafts by remember(missingSlots) {
        mutableStateOf(missingSlots.associateWith { SlotTimeDraft() })
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(PagePadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("导入前确认节次时间", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "文件只提供了第几节，没有提供具体钟点。请按学校作息明确填写，应用不会替你猜测时间。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (missingSlots.isEmpty()) {
            Text("没有需要确认的缺失节次。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            missingSlots.forEach { number ->
                val draft = drafts[number] ?: SlotTimeDraft()
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = SmallRadius,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("第${number}节", fontWeight = FontWeight.SemiBold, modifier = Modifier.width(56.dp))
                        LabeledTextField(
                            "开始时间",
                            draft.start,
                            { value -> drafts = drafts + (number to draft.copy(start = value)) },
                            "例如 20:00",
                            Modifier.weight(1f)
                        )
                        LabeledTextField(
                            "结束时间",
                            draft.end,
                            { value -> drafts = drafts + (number to draft.copy(end = value)) },
                            "例如 20:45",
                            Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        Button(
            onClick = { onSave(drafts.mapValues { (_, value) -> value.start to value.end }) },
            enabled = missingSlots.isNotEmpty() && missingSlots.all { number ->
                val draft = drafts[number]
                !draft?.start.isNullOrBlank() && !draft?.end.isNullOrBlank()
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
        ) { Text("保存节次时间并返回导入") }
    }
}

@Composable
private fun PreviewCell(value: String, header: Boolean = false, maxLines: Int = 2) {
    Text(value.ifBlank { "-" }, style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall, fontWeight = if (header) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp).border(.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = .25f)).padding(horizontal = 8.dp, vertical = 6.dp), maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun IssueRow(issue: ImportIssue) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
        Icon(if (issue.severity == com.example.classschedule.data.IssueSeverity.ERROR) Icons.Outlined.WarningAmber else Icons.Outlined.Info, contentDescription = null, tint = if (issue.severity == com.example.classschedule.data.IssueSeverity.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(if (issue.rowNumber > 0) "第${issue.rowNumber}行：${issue.message}" else issue.message, style = MaterialTheme.typography.bodySmall, color = if (issue.severity == com.example.classschedule.data.IssueSeverity.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsScreen(
    viewModel: MainViewModel,
    settings: AppSettings,
    onPickImport: () -> Unit,
    onExport: () -> Unit,
    requestNotifications: () -> Unit,
    requestExactAlarm: () -> Unit,
    onOpenDonation: () -> Unit
) {
    var termName by remember(settings.term) { mutableStateOf(settings.term.name) }
    var monday by remember(settings.term) { mutableStateOf(settings.term.firstMonday.format(DateFormat)) }
    var start by remember(settings.term) { mutableStateOf(settings.term.startDate.format(DateFormat)) }
    var end by remember(settings.term) { mutableStateOf(settings.term.endDate.format(DateFormat)) }
    var reminderConfig by remember(settings.reminderConfig) { mutableStateOf(settings.reminderConfig) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(PagePadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionTitle("学期")
        LabeledTextField("学期名称", termName, { termName = it }, "例如：2026 秋季学期")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            LabeledTextField("第一周周一", monday, { monday = it }, "yyyy-MM-dd", Modifier.weight(1f))
            LabeledTextField("开始日期", start, { start = it }, "yyyy-MM-dd", Modifier.weight(1f))
        }
        LabeledTextField("结束日期", end, { end = it }, "yyyy-MM-dd")
        OutlinedButton(onClick = { viewModel.saveTerm(termName, monday, start, end) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("保存学期") }

        SettingsSectionTitle("通知提醒")
        SettingSwitchRow("开启课程提醒", "每天按上午、下午、晚上发送课程清单", Icons.Outlined.Notifications, settings.notificationsEnabled) {
            viewModel.setNotificationsEnabled(it)
            if (it) requestNotifications()
        }
        if (settings.notificationsEnabled && !viewModel.hasExactAlarmAccess()) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = SmallRadius, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text("可能延迟", fontWeight = FontWeight.SemiBold)
                        Text("系统未授予精确提醒权限，将使用系统允许的非精确闹钟。", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = requestExactAlarm, modifier = Modifier.heightIn(min = 48.dp)) { Text("去开启精确提醒") }
                    }
                }
            }
        }
        reminderConfig.all.forEach { phase ->
            PhaseEditor(phase) { updated -> reminderConfig = reminderConfig.withPhase(updated) }
        }
        Button(onClick = { viewModel.saveReminders(reminderConfig) }, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) { Text("保存提醒设置") }

        SettingsSectionTitle("节次时间")
        Text(
            if (settings.timeSlotsConfigured) {
                "关联节次的课程会随这里的时间变化而更新。"
            } else {
                "当前显示的是内置示例时间。首次导入按节次排列的课表前，请先确认学校作息。"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TimeSlotEditor(settings.timeSlots, viewModel)

        SettingsSectionTitle("外观与数据")
        SettingSwitchRow(
            "显示今日黄历",
            "在今日页日期下方显示农历信息",
            Icons.Outlined.EventNote,
            settings.showAlmanac,
            viewModel::setShowAlmanac
        )
        SettingIconLabel("主题", Icons.Outlined.Palette)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(selected = settings.themeMode == mode, onClick = { viewModel.setTheme(mode) }, label = { Text(when (mode) { ThemeMode.SYSTEM -> "跟随系统"; ThemeMode.LIGHT -> "浅色"; ThemeMode.DARK -> "深色" }) }, leadingIcon = { Icon(when (mode) { ThemeMode.SYSTEM -> Icons.Outlined.Palette; ThemeMode.LIGHT -> Icons.Outlined.LightMode; ThemeMode.DARK -> Icons.Outlined.DarkMode }, contentDescription = null) }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
        SettingLinkRow("导入课表", "CSV / XLSX，先预览后写入", Icons.Outlined.FileUpload, onPickImport)
        SettingLinkRow("导出课表", "导出为可再次导入的 CSV", Icons.Outlined.Download, onExport)
        SettingLinkRow("打赏作者", "支持一只不太美的鸡继续维护", Icons.Outlined.FavoriteBorder, onOpenDonation)
        Spacer(Modifier.height(12.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("不美鸡课表 ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("作者：一只不太美的鸡 · 数据只保存在本机", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DonationScreen() {
    var selectedMethod by remember { mutableStateOf<DonationMethod?>(null) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(PagePadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsSectionTitle("支持作者")
        Text(
            "如果不美鸡课表帮到了你，可以选择一种方式支持作者。收款码默认隐藏，点击对应项目后查看。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SettingLinkRow("支付宝", "点击查看支付宝收款码", Icons.Outlined.QrCode2) {
            selectedMethod = DonationMethod.ALIPAY
        }
        SettingLinkRow("微信支付", "点击查看微信收款码", Icons.Outlined.QrCode2) {
            selectedMethod = DonationMethod.WECHAT
        }
        Text(
            "感谢你的支持。二维码仅用于打赏，不会上传或保存任何账号信息。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    selectedMethod?.let { method ->
        DonationImageDialog(method) { selectedMethod = null }
    }
}

@Composable
private fun DonationImageDialog(method: DonationMethod, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(.92f).heightIn(max = 600.dp),
            shape = SmallRadius,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(method.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "关闭${method.title}收款码")
                    }
                }
                Text("请使用${method.title}扫描下方二维码", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Image(
                    painter = painterResource(method.imageRes),
                    contentDescription = "${method.title}收款二维码",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().clip(SmallRadius)
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun SettingIconLabel(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)); Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
private fun SettingSwitchRow(label: String, supporting: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(label, fontWeight = FontWeight.SemiBold); Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Switch(checked, onCheckedChange)
    }
}

@Composable
private fun SettingLinkRow(label: String, supporting: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(label, fontWeight = FontWeight.SemiBold); Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Icon(Icons.Outlined.ChevronRight, contentDescription = "打开$label", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PhaseEditor(phase: PhaseReminder, onChange: (PhaseReminder) -> Unit) {
    var start by remember(phase) { mutableStateOf(formatMinutes(phase.boundaryStart)) }
    var end by remember(phase) { mutableStateOf(formatMinutes(phase.boundaryEnd)) }
    var notify by remember(phase) { mutableStateOf("%02d:%02d".format(phase.notifyHour, phase.notifyMinute)) }
    fun update(transform: (PhaseReminder) -> PhaseReminder) = onChange(transform(phase))
    Surface(color = MaterialTheme.colorScheme.surface, shape = SmallRadius, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .38f)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingSwitchRow(phase.stage.label, "按课程开始时间归入此阶段", Icons.Outlined.AccessTime, phase.enabled) { enabledValue -> update { old -> old.copy(enabled = enabledValue) } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                LabeledTextField("阶段开始", start, { start = it }, "00:00", Modifier.weight(1f))
                LabeledTextField("阶段结束", end, { end = it }, "12:00", Modifier.weight(1f))
                LabeledTextField("提醒时间", notify, { notify = it }, "07:00", Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = phase.noClassReminder, onCheckedChange = { update { old -> old.copy(noClassReminder = it) } })
                Text("无课时也提醒", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = {
                val startMin = com.example.classschedule.domain.parseClock(start) ?: phase.boundaryStart
                val endMin = com.example.classschedule.domain.parseClock(end) ?: phase.boundaryEnd
                val clock = com.example.classschedule.domain.parseClock(notify) ?: (phase.notifyHour * 60 + phase.notifyMinute)
                onChange(phase.copy(boundaryStart = startMin, boundaryEnd = endMin, notifyHour = clock / 60, notifyMinute = clock % 60))
            }, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) { Text("应用本段时间") }
        }
    }
}

@Composable
private fun TimeSlotEditor(slots: List<TimeSlot>, viewModel: MainViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        slots.forEach { slot ->
            var start by remember(slot) { mutableStateOf(formatMinutes(slot.startMinutes)) }
            var end by remember(slot) { mutableStateOf(formatMinutes(slot.endMinutes)) }
            Row(Modifier.fillMaxWidth().heightIn(min = 58.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${slot.number}", modifier = Modifier.width(24.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                LabeledTextField("开始", start, { start = it }, "08:00", Modifier.weight(1f))
                LabeledTextField("结束", end, { end = it }, "08:45", Modifier.weight(1f))
                IconButton(onClick = { viewModel.updateTimeSlot(slot, start, end) }) { Icon(Icons.Outlined.CheckCircleOutline, contentDescription = "保存第${slot.number}节") }
                IconButton(onClick = { viewModel.deleteTimeSlot(slot) }) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "删除第${slot.number}节") }
            }
        }
        OutlinedButton(onClick = viewModel::addTimeSlot, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Icon(Icons.Outlined.AddCircleOutline, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("新增节次") }
    }
}

@Composable
private fun LabeledTextField(label: String, value: String, onValueChange: (String) -> Unit, supporting: String, modifier: Modifier = Modifier, minLines: Int = 1) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, placeholder = { Text(supporting) }, modifier = modifier.fillMaxWidth(), minLines = minLines, singleLine = minLines == 1)
}

@Composable
private fun EmptyState(title: String, supporting: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f), shape = SmallRadius, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

private fun parseColor(value: String): Color = runCatching { Color(value.toColorInt()) }.getOrDefault(Color(0xFF39766B))

private fun displayWeekRule(rule: WeekRule): String = when (rule.type) {
    WeekRuleType.EVERY -> "每周"
    WeekRuleType.ODD -> "单周"
    WeekRuleType.EVEN -> "双周"
    WeekRuleType.SET -> rule.weeks.sorted().joinToString(",")
    WeekRuleType.RANGE -> "${rule.fromWeek ?: ""}-${rule.toWeek ?: ""}"
    WeekRuleType.DATES -> "${rule.fromDate ?: ""} 至 ${rule.toDate ?: ""}"
}

private fun displaySlotRange(meeting: com.example.classschedule.data.MeetingEntity, slots: List<TimeSlot>): String {
    val start = meeting.startSlotId?.let { id -> slots.firstOrNull { it.id == id }?.number } ?: return ""
    val end = meeting.endSlotId?.let { id -> slots.firstOrNull { it.id == id }?.number } ?: start
    return if (start == end) start.toString() else "$start-$end"
}

private fun displayName(resolver: ContentResolver, uri: Uri): String? = runCatching {
    resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}.getOrNull()
