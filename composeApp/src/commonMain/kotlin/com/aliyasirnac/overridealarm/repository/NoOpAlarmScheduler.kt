package com.aliyasirnac.overridealarm.repository

import com.aliyasirnac.overridealarm.model.Alarm

class NoOpAlarmScheduler : AlarmScheduler {
    override fun scheduleAlarm(alarm: Alarm) {}
    override fun cancelAlarm(alarm: Alarm) {}
}
