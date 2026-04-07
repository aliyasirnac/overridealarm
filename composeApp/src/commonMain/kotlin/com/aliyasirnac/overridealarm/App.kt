package com.aliyasirnac.overridealarm

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aliyasirnac.overridealarm.model.Alarm
import com.aliyasirnac.overridealarm.repository.AlarmRepository
import com.aliyasirnac.overridealarm.repository.AlarmScheduler
import com.aliyasirnac.overridealarm.repository.InMemoryAlarmRepository
import com.aliyasirnac.overridealarm.repository.NoOpAlarmScheduler
import com.aliyasirnac.overridealarm.ui.AlarmViewModel
import com.aliyasirnac.overridealarm.ui.screens.AddAlarmScreen
import com.aliyasirnac.overridealarm.ui.screens.AlarmListScreen
import com.aliyasirnac.overridealarm.ui.screens.SettingsScreen
import com.aliyasirnac.overridealarm.ui.theme.OverrideAlarmTheme
import com.aliyasirnac.overridealarm.ui.theme.ThemeMode
import kotlinx.coroutines.launch

val LocalAlarmRepository = compositionLocalOf<AlarmRepository> { InMemoryAlarmRepository() }
val LocalAlarmScheduler = compositionLocalOf<AlarmScheduler> { NoOpAlarmScheduler() }

sealed class Screen {
    data object AlarmList : Screen()
    data object AddAlarm : Screen()
    data class EditAlarm(val alarm: Alarm) : Screen()
    data object Settings : Screen()
}

@Composable
fun App(
    permissionBanner: (@Composable () -> Unit)? = null,
    onPickRingtone: ((callback: (uri: String?, name: String?) -> Unit) -> Unit) = {}
) {
    val repository = LocalAlarmRepository.current
    val scheduler = LocalAlarmScheduler.current

    val viewModel = viewModel<AlarmViewModel> { AlarmViewModel(repository, scheduler) }
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.AlarmList) }
    var themeMode by remember { mutableStateOf(ThemeMode.DARK) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    OverrideAlarmTheme(themeMode = themeMode) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) { _ ->
            when (val screen = currentScreen) {
                is Screen.Settings -> SettingsScreen(
                    onBack = { currentScreen = Screen.AlarmList },
                    currentTheme = themeMode,
                    onThemeChange = { themeMode = it }
                )
                is Screen.AlarmList -> AlarmListScreen(
                    alarms = alarms,
                    onAddAlarm = { currentScreen = Screen.AddAlarm },
                    onToggleAlarm = { viewModel.toggleAlarm(it) },
                    onDeleteAlarm = { viewModel.deleteAlarm(it) },
                    onEditAlarm = { currentScreen = Screen.EditAlarm(it) },
                    permissionBanner = permissionBanner,
                    onOpenSettings = { currentScreen = Screen.Settings }
                )
                is Screen.AddAlarm -> AddAlarmScreen(
                    existingAlarm = null,
                    onSave = { alarm ->
                        viewModel.addAlarm(alarm)
                        currentScreen = Screen.AlarmList
                        scope.launch {
                            val timeStr = String.format("%02d:%02d", alarm.hour, alarm.minute)
                            snackbarHostState.showSnackbar("⏰ Alarm $timeStr için kuruldu!")
                        }
                    },
                    onCancel = { currentScreen = Screen.AlarmList },
                    onPickRingtone = onPickRingtone
                )
                is Screen.EditAlarm -> AddAlarmScreen(
                    existingAlarm = screen.alarm,
                    onSave = { alarm ->
                        viewModel.updateAlarm(alarm)
                        currentScreen = Screen.AlarmList
                        scope.launch {
                            val timeStr = String.format("%02d:%02d", alarm.hour, alarm.minute)
                            snackbarHostState.showSnackbar("✅ Alarm $timeStr güncellendi!")
                        }
                    },
                    onCancel = { currentScreen = Screen.AlarmList },
                    onPickRingtone = onPickRingtone
                )
            }
        }
    }
}
