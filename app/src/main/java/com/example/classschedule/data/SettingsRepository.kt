package com.example.classschedule.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.classschedule.domain.AppSettings
import com.example.classschedule.domain.PhaseReminder
import com.example.classschedule.domain.ReminderConfig
import com.example.classschedule.domain.Stage
import com.example.classschedule.domain.ThemeMode
import com.example.classschedule.domain.TermSettings
import com.example.classschedule.domain.TimeSlot
import com.example.classschedule.domain.defaultTerm
import com.example.classschedule.domain.defaultTimeSlots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

private val Context.scheduleDataStore by preferencesDataStore(name = "schedule_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val onboarding = booleanPreferencesKey("onboarding_complete")
        val termName = stringPreferencesKey("term_name")
        val firstMonday = stringPreferencesKey("first_monday")
        val startDate = stringPreferencesKey("term_start")
        val endDate = stringPreferencesKey("term_end")
        val slots = stringPreferencesKey("time_slots")
        val slotsConfigured = booleanPreferencesKey("time_slots_configured")
        val reminders = stringPreferencesKey("reminder_config")
        val theme = stringPreferencesKey("theme_mode")
        val notifications = booleanPreferencesKey("notifications_enabled")
        val weekZoom = floatPreferencesKey("week_zoom")
        val showAlmanac = booleanPreferencesKey("show_almanac")
        val schemaVersion = intPreferencesKey("settings_schema")
    }

    val settings: Flow<AppSettings> = context.scheduleDataStore.data.map { prefs ->
        val default = defaultTerm()
        AppSettings(
            onboardingComplete = resolveOnboardingComplete(
                storedValue = prefs[Keys.onboarding],
                schemaVersion = prefs[Keys.schemaVersion],
                termName = prefs[Keys.termName],
                firstMondayText = prefs[Keys.firstMonday],
                startText = prefs[Keys.startDate],
                endText = prefs[Keys.endDate]
            ),
            term = TermSettings(
                name = prefs[Keys.termName] ?: default.name,
                firstMonday = parseDate(prefs[Keys.firstMonday]) ?: default.firstMonday,
                startDate = parseDate(prefs[Keys.startDate]) ?: default.startDate,
                endDate = parseDate(prefs[Keys.endDate]) ?: default.endDate
            ),
            timeSlots = decodeSlots(prefs[Keys.slots]),
            timeSlotsConfigured = prefs[Keys.slotsConfigured] ?: false,
            reminderConfig = decodeReminders(prefs[Keys.reminders]),
            themeMode = runCatching { ThemeMode.valueOf(prefs[Keys.theme] ?: ThemeMode.SYSTEM.name) }
                .getOrDefault(ThemeMode.SYSTEM),
            notificationsEnabled = prefs[Keys.notifications] ?: false,
            weekZoom = (prefs[Keys.weekZoom] ?: 1f).coerceIn(0.75f, 2f),
            showAlmanac = prefs[Keys.showAlmanac] ?: true
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun saveTerm(term: TermSettings, completeOnboarding: Boolean? = null) {
        context.scheduleDataStore.edit { prefs ->
            prefs[Keys.termName] = term.name
            prefs[Keys.firstMonday] = term.firstMonday.toString()
            prefs[Keys.startDate] = term.startDate.toString()
            prefs[Keys.endDate] = term.endDate.toString()
            completeOnboarding?.let { prefs[Keys.onboarding] = it }
            prefs[Keys.schemaVersion] = 1
        }
    }

    suspend fun setOnboardingComplete(value: Boolean) {
        context.scheduleDataStore.edit { it[Keys.onboarding] = value }
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.scheduleDataStore.edit { it[Keys.theme] = mode.name }
    }

    suspend fun setNotificationsEnabled(value: Boolean) {
        context.scheduleDataStore.edit { it[Keys.notifications] = value }
    }

    suspend fun setWeekZoom(value: Float) {
        context.scheduleDataStore.edit { it[Keys.weekZoom] = value.coerceIn(0.75f, 2f) }
    }

    suspend fun setShowAlmanac(value: Boolean) {
        context.scheduleDataStore.edit { it[Keys.showAlmanac] = value }
    }

    suspend fun saveSlots(slots: List<TimeSlot>, markConfigured: Boolean = true) {
        context.scheduleDataStore.edit {
            it[Keys.slots] = encodeSlots(slots)
            if (markConfigured) it[Keys.slotsConfigured] = true
        }
    }

    suspend fun saveReminders(config: ReminderConfig) {
        context.scheduleDataStore.edit { it[Keys.reminders] = encodeReminders(config) }
    }

    private fun parseDate(value: String?): LocalDate? = value?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    private fun decodeSlots(value: String?): List<TimeSlot> {
        if (value.isNullOrBlank()) return defaultTimeSlots()
        return runCatching {
            val array = JSONArray(value)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                TimeSlot(item.getLong("id"), item.getInt("number"), item.getInt("start"), item.getInt("end"))
            }.sortedBy { it.number }
        }.getOrDefault(defaultTimeSlots())
    }

    private fun encodeSlots(slots: List<TimeSlot>): String = JSONArray().apply {
        slots.forEach { slot ->
            put(JSONObject().apply {
                put("id", slot.id)
                put("number", slot.number)
                put("start", slot.startMinutes)
                put("end", slot.endMinutes)
            })
        }
    }.toString()

    private fun decodeReminders(value: String?): ReminderConfig {
        if (value.isNullOrBlank()) return ReminderConfig.defaults()
        return runCatching {
            val root = JSONObject(value)
            fun decode(stage: Stage): PhaseReminder {
                val item = root.optJSONObject(stage.key) ?: error("missing phase")
                return PhaseReminder(
                    stage = stage,
                    enabled = item.optBoolean("enabled", true),
                    boundaryStart = item.optInt("boundaryStart"),
                    boundaryEnd = item.optInt("boundaryEnd"),
                    notifyHour = item.optInt("hour"),
                    notifyMinute = item.optInt("minute"),
                    noClassReminder = item.optBoolean("noClassReminder", false)
                )
            }
            ReminderConfig(decode(Stage.MORNING), decode(Stage.AFTERNOON), decode(Stage.EVENING))
        }.getOrDefault(ReminderConfig.defaults())
    }

    private fun encodeReminders(config: ReminderConfig): String = JSONObject().apply {
        config.all.forEach { phase ->
            put(phase.stage.key, JSONObject().apply {
                put("enabled", phase.enabled)
                put("boundaryStart", phase.boundaryStart)
                put("boundaryEnd", phase.boundaryEnd)
                put("hour", phase.notifyHour)
                put("minute", phase.notifyMinute)
                put("noClassReminder", phase.noClassReminder)
            })
        }
    }.toString()
}
