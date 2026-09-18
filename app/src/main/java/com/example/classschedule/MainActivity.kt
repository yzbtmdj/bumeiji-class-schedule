package com.example.classschedule

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.classschedule.ui.MainViewModel
import com.example.classschedule.ui.ScheduleRoot
import com.example.classschedule.ui.theme.ScheduleTheme

class MainActivity : ComponentActivity() {
    private val launchStage = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchStage.value = intent?.getStringExtra(EXTRA_OPEN_STAGE)
        val app = application as ScheduleApplication
        setContent {
            val vm: MainViewModel = viewModel(
                factory = MainViewModelFactory(app)
            )
            ScheduleTheme(vm.settings.collectAsState().value.themeMode) {
                ScheduleRoot(
                    viewModel = vm,
                    initialStage = launchStage.value,
                    requestNotifications = { requestNotificationPermission() },
                    requestExactAlarm = { openExactAlarmSettings() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        launchStage.value = intent.getStringExtra(EXTRA_OPEN_STAGE)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        }
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply { data = "package:$packageName".toUri() })
        }
    }

    companion object {
        const val EXTRA_OPEN_STAGE = "open_stage"
        private const val REQUEST_NOTIFICATIONS = 510
    }
}

class MainViewModelFactory(private val app: ScheduleApplication) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(app.scheduleRepository, app.settingsRepository, app.alarmScheduler) as T
    }
}
