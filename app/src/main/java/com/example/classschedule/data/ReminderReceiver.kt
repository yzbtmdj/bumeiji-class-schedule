package com.example.classschedule.data

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.classschedule.MainActivity
import com.example.classschedule.R
import com.example.classschedule.domain.Stage
import com.example.classschedule.domain.reminderLine
import com.example.classschedule.domain.resolvedFor
import com.example.classschedule.ScheduleApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext as ScheduleApplication
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val date = LocalDate.ofEpochDay(intent.getLongExtra(EXTRA_DATE, LocalDate.now().toEpochDay()))
                val stage = Stage.fromKey(intent.getStringExtra(EXTRA_STAGE).orEmpty())
                val settings = app.settingsRepository.current()
                if (!settings.notificationsEnabled) return@launch
                if (date.isBefore(settings.term.startDate) || date.isAfter(settings.term.endDate)) return@launch
                if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return@launch
                val phase = settings.reminderConfig.forStage(stage)
                if (!phase.enabled) return@launch
                val courses = app.scheduleRepository.records.first().resolvedFor(date, settings.term, settings.timeSlots)
                    .filter { com.example.classschedule.domain.stageFor(it.startMinutes, settings.reminderConfig) == stage }
                if (courses.isEmpty() && !phase.noClassReminder) return@launch
                NotificationChannels.ensure(context)
                val openToday = Intent(context, MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_OPEN_STAGE, stage.key)
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val contentIntent = PendingIntent.getActivity(
                    context, 9000 + stage.order, openToday,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val builder = NotificationCompat.Builder(context, NotificationChannels.REMINDER_CHANNEL)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("${stage.label}课程 · ${date.monthValue}月${date.dayOfMonth}日")
                    .setContentText(if (courses.isEmpty()) "暂无课程" else "共 ${courses.size} 节课程")
                    .setStyle(NotificationCompat.InboxStyle().also { style ->
                        if (courses.isEmpty()) style.addLine("暂无课程，按时完成今日计划")
                        courses.forEach { item ->
                            style.addLine(reminderLine(item))
                        }
                    })
                    .setAutoCancel(true)
                    .setContentIntent(contentIntent)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                NotificationManagerCompat.from(context).notify(1000 + stage.order, builder.build())
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_DATE = "date_epoch_day"
        const val EXTRA_STAGE = "stage"
    }
}

object NotificationChannels {
    const val REMINDER_CHANNEL = "class_schedule_reminders"

    fun ensure(context: Context) {
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        manager.createNotificationChannel(android.app.NotificationChannel(
            REMINDER_CHANNEL, "课表提醒", android.app.NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "上午、下午、晚上课程提醒" })
    }
}
