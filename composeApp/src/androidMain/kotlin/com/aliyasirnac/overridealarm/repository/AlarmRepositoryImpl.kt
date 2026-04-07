package com.aliyasirnac.overridealarm.repository

import android.content.Context
import android.content.SharedPreferences
import com.aliyasirnac.overridealarm.model.Alarm
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AlarmRepositoryImpl(context: Context) : AlarmRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("override_alarm_prefs", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    override fun getAlarms(): List<Alarm> {
        val raw = prefs.getString(KEY_ALARMS, "[]") ?: "[]"
        return try {
            json.decodeFromString(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun saveAlarm(alarm: Alarm) {
        val alarms = getAlarms().toMutableList()
        alarms.add(alarm)
        persist(alarms)
    }

    override fun deleteAlarm(id: Long) {
        persist(getAlarms().filter { it.id != id })
    }

    override fun updateAlarm(alarm: Alarm) {
        persist(getAlarms().map { if (it.id == alarm.id) alarm else it })
    }

    private fun persist(alarms: List<Alarm>) {
        prefs.edit().putString(KEY_ALARMS, json.encodeToString(alarms)).apply()
    }

    companion object {
        private const val KEY_ALARMS = "alarm_list"
    }
}
