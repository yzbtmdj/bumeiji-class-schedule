package com.example.classschedule.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.classschedule.domain.AppSettings
import com.example.classschedule.domain.Stage
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AlarmScheduler(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val scheduleRepository: ScheduleRepository
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    suspend fun reschedule(days: Long = 42) = withContext(Dispatchers.IO) {
        cancelAll()
        val settings = settingsRepository.current()
        if (!settings.notificationsEnabled) return@withContext
        val now = LocalDateTime.now()
        var date = LocalDate.now()
        repeat(days.toInt()) {
            if (date.isBefore(settings.term.startDate) || date.isAfter(settings.term.endDate)) {
                date = date.plusDays(1)
                return@repeat
            }
            settings.reminderConfig.all.filter { it.enabled }.forEach { phase ->
                val dateTime = date.atTime(phase.notifyHour, phase.notifyMinute)
                if (dateTime.isAfter(now.minusMinutes(1))) schedule(date, phase.stage, dateTime, settings)
            }
            date = date.plusDays(1)
        }
    }

    fun exactAlarmAccess(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    private fun schedule(date: LocalDate, stage: Stage, dateTime: LocalDateTime, settings: AppSettings) {
        val id = alarmId(date, stage)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_DATE, date.toEpochDay())
            putExtra(ReminderReceiver.EXTRA_STAGE, stage.key)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val trigger = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        try {
            if (exactAlarmAccess()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
            }
        } catch (_: SecurityException) {
            // Permission can change between the access check and scheduling.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        }
    }

    private fun cancelAll() {
        val today = LocalDate.now().minusDays(2)
        repeat(50) { offset ->
            Stage.entries.forEach { stage ->
                val date = today.plusDays(offset.toLong())
                val pending = PendingIntent.getBroadcast(
                    context,
                    alarmId(date, stage),
                    Intent(context, ReminderReceiver::class.java),
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                pending?.let { alarmManager.cancel(it); it.cancel() }
            }
        }
    }

    companion object {
        fun alarmId(date: LocalDate, stage: Stage): Int = (date.toEpochDay() * 10L + stage.order).toInt()
    }
}
