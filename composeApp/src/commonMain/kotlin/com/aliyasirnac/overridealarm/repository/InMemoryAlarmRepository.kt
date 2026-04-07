package com.aliyasirnac.overridealarm.repository

import com.aliyasirnac.overridealarm.model.Alarm

class InMemoryAlarmRepository : AlarmRepository {
    private val alarms = mutableListOf<Alarm>()

    override fun getAlarms(): List<Alarm> = alarms.toList()

    override fun saveAlarm(alarm: Alarm) {
        alarms.add(alarm)
    }

    override fun deleteAlarm(id: Long) {
        alarms.removeAll { it.id == id }
    }

    override fun updateAlarm(alarm: Alarm) {
        val index = alarms.indexOfFirst { it.id == alarm.id }
        if (index >= 0) alarms[index] = alarm
    }
}
