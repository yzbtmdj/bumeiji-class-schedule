package com.example.classschedule.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.classschedule.ScheduleApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_MY_PACKAGE_REPLACED
            )) return
        val pending = goAsync()
        val app = context.applicationContext as ScheduleApplication
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { app.alarmScheduler.reschedule() } finally { pending.finish() }
        }
    }
}
