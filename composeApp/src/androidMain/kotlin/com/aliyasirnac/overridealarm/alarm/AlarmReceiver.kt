package com.aliyasirnac.overridealarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        val alarmLabel = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: ""
        val vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true)
        val snoozeEnabled = intent.getBooleanExtra(EXTRA_SNOOZE_ENABLED, true)
        val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 5)
        val challengeType = intent.getStringExtra(EXTRA_CHALLENGE_TYPE) ?: "NONE"
        val ringtoneUri = intent.getStringExtra(EXTRA_RINGTONE_URI)

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_LABEL, alarmLabel)
            putExtra(EXTRA_VIBRATE, vibrate)
            putExtra(EXTRA_SNOOZE_ENABLED, snoozeEnabled)
            putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            putExtra(EXTRA_CHALLENGE_TYPE, challengeType)
            putExtra(EXTRA_RINGTONE_URI, ringtoneUri)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_LABEL = "alarm_label"
        const val EXTRA_VIBRATE = "alarm_vibrate"
        const val EXTRA_SNOOZE_ENABLED = "alarm_snooze_enabled"
        const val EXTRA_SNOOZE_MINUTES = "alarm_snooze_minutes"
        const val EXTRA_CHALLENGE_TYPE = "alarm_challenge_type"
        const val EXTRA_RINGTONE_URI = "alarm_ringtone_uri"
    }
}
