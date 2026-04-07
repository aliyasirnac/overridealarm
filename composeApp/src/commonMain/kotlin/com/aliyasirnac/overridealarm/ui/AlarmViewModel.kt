package com.aliyasirnac.overridealarm.ui

import androidx.lifecycle.ViewModel
import com.aliyasirnac.overridealarm.model.Alarm
import com.aliyasirnac.overridealarm.repository.AlarmRepository
import com.aliyasirnac.overridealarm.repository.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AlarmViewModel(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler
) : ViewModel() {

    private val _alarms = MutableStateFlow<List<Alarm>>(emptyList())
    val alarms: StateFlow<List<Alarm>> = _alarms.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _alarms.value = repository.getAlarms()
            .sortedWith(compareBy({ it.hour }, { it.minute }))
    }

    fun addAlarm(alarm: Alarm) {
        repository.saveAlarm(alarm)
        if (alarm.isEnabled) {
            scheduler.scheduleAlarm(alarm)
        }
        refresh()
    }

    fun deleteAlarm(alarm: Alarm) {
        scheduler.cancelAlarm(alarm)
        repository.deleteAlarm(alarm.id)
        refresh()
    }

    fun toggleAlarm(alarm: Alarm) {
        val updated = alarm.copy(isEnabled = !alarm.isEnabled)
        repository.updateAlarm(updated)
        if (updated.isEnabled) {
            scheduler.scheduleAlarm(updated)
        } else {
            scheduler.cancelAlarm(updated)
        }
        refresh()
    }

    fun updateAlarm(alarm: Alarm) {
        repository.updateAlarm(alarm)
        if (alarm.isEnabled) {
            scheduler.cancelAlarm(alarm)
            scheduler.scheduleAlarm(alarm)
        }
        refresh()
    }
}
