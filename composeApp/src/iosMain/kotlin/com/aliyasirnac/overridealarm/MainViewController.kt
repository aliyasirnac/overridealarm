package com.aliyasirnac.overridealarm

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import com.aliyasirnac.overridealarm.repository.AlarmRepositoryIos
import com.aliyasirnac.overridealarm.repository.AlarmSchedulerIos

fun MainViewController() = ComposeUIViewController {
    CompositionLocalProvider(
        LocalAlarmRepository provides AlarmRepositoryIos(),
        LocalAlarmScheduler provides AlarmSchedulerIos()
    ) {
        App()
    }
}
