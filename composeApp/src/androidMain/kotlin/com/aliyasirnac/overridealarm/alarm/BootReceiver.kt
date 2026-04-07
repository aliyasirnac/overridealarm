package com.aliyasirnac.overridealarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aliyasirnac.overridealarm.repository.AlarmRepositoryImpl

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        // Re-schedule all enabled alarms after device reboot
        val repository = AlarmRepositoryImpl(context)
        val scheduler = AlarmSchedulerImpl(context)

        repository.getAlarms()
            .filter { it.isEnabled }
            .forEach { scheduler.scheduleAlarm(it) }
    }
}
