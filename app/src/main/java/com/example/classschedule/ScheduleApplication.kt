package com.example.classschedule

import android.app.Application
import com.example.classschedule.data.AlarmScheduler
import com.example.classschedule.data.AppDatabase
import com.example.classschedule.data.ScheduleRepository
import com.example.classschedule.data.SettingsRepository

class ScheduleApplication : Application() {
    val database by lazy { AppDatabase.create(this) }
    val settingsRepository by lazy { SettingsRepository(this) }
    val scheduleRepository by lazy { ScheduleRepository(database) }
    val alarmScheduler by lazy { AlarmScheduler(this, settingsRepository, scheduleRepository) }
}
