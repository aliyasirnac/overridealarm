package com.aliyasirnac.overridealarm

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
import com.aliyasirnac.overridealarm.ui.theme.OverrideAlarmTheme

val LocalAlarmRepository = compositionLocalOf<AlarmRepository> { InMemoryAlarmRepository() }
val LocalAlarmScheduler = compositionLocalOf<AlarmScheduler> { NoOpAlarmScheduler() }

sealed class Screen {
    data object AlarmList : Screen()
    data object AddAlarm : Screen()
    data class EditAlarm(val alarm: Alarm) : Screen()
}

@Composable
fun App(
    permissionBanner: (@Composable () -> Unit)? = null
) {
    val repository = LocalAlarmRepository.current
    val scheduler = LocalAlarmScheduler.current

    val viewModel = viewModel<AlarmViewModel> { AlarmViewModel(repository, scheduler) }
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.AlarmList) }

    OverrideAlarmTheme {
        when (val screen = currentScreen) {
            is Screen.AlarmList -> AlarmListScreen(
                alarms = alarms,
                onAddAlarm = { currentScreen = Screen.AddAlarm },
                onToggleAlarm = { viewModel.toggleAlarm(it) },
                onDeleteAlarm = { viewModel.deleteAlarm(it) },
                onEditAlarm = { currentScreen = Screen.EditAlarm(it) },
                permissionBanner = permissionBanner
            )
            is Screen.AddAlarm -> AddAlarmScreen(
                existingAlarm = null,
                onSave = { alarm ->
                    viewModel.addAlarm(alarm)
                    currentScreen = Screen.AlarmList
                },
                onCancel = { currentScreen = Screen.AlarmList }
            )
            is Screen.EditAlarm -> AddAlarmScreen(
                existingAlarm = screen.alarm,
                onSave = { alarm ->
                    viewModel.updateAlarm(alarm)
                    currentScreen = Screen.AlarmList
                },
                onCancel = { currentScreen = Screen.AlarmList }
            )
        }
    }
}
