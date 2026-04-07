package com.aliyasirnac.overridealarm.repository

import com.aliyasirnac.overridealarm.model.Alarm

interface AlarmScheduler {
    fun scheduleAlarm(alarm: Alarm)
    fun cancelAlarm(alarm: Alarm)
}
