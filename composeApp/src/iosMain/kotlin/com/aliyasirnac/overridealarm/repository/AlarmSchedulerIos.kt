package com.aliyasirnac.overridealarm.repository

import com.aliyasirnac.overridealarm.model.Alarm

class AlarmSchedulerIos : AlarmScheduler {
    override fun scheduleAlarm(alarm: Alarm) {
        // TODO: Implement with UNUserNotificationCenter for iOS
    }

    override fun cancelAlarm(alarm: Alarm) {
        // TODO: Implement with UNUserNotificationCenter for iOS
    }
}
