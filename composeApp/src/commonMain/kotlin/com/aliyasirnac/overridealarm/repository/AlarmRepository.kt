package com.aliyasirnac.overridealarm.repository

import com.aliyasirnac.overridealarm.model.Alarm

interface AlarmRepository {
    fun getAlarms(): List<Alarm>
    fun saveAlarm(alarm: Alarm)
    fun deleteAlarm(id: Long)
    fun updateAlarm(alarm: Alarm)
}
