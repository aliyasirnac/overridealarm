package com.aliyasirnac.overridealarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.aliyasirnac.overridealarm.model.Alarm
import com.aliyasirnac.overridealarm.repository.AlarmScheduler
import java.util.Calendar

class AlarmSchedulerImpl(private val context: Context) : AlarmScheduler {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun scheduleAlarm(alarm: Alarm) {
        val pendingIntent = buildPendingIntent(alarm) ?: return

        val triggerTime = nextTriggerTime(alarm)
        // AlarmClockInfo: shown in status bar, bypasses DND "alarms" exception
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    }

    override fun cancelAlarm(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }

    private fun buildPendingIntent(alarm: Alarm): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, alarm.label)
            putExtra(AlarmReceiver.EXTRA_VIBRATE, alarm.vibrate)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_ENABLED, alarm.snoozeEnabled)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, alarm.snoozeMinutes)
            putExtra(AlarmReceiver.EXTRA_CHALLENGE_TYPE, alarm.challengeType.name)
            putExtra(AlarmReceiver.EXTRA_RINGTONE_URI, alarm.ringtoneUri)
            putExtra(AlarmReceiver.EXTRA_FORCE_SPEAKER, alarm.forceSpeaker)
            putExtra(AlarmReceiver.EXTRA_FLASH_STROBE, alarm.flashStrobe)
            putExtra(AlarmReceiver.EXTRA_TTS_ENABLED, alarm.ttsEnabled)
            putExtra(AlarmReceiver.EXTRA_TTS_MESSAGE, alarm.ttsMessage)
            putExtra(AlarmReceiver.EXTRA_WAKEUP_CHECK, alarm.wakeupCheck)
        }
        return PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextTriggerTime(alarm: Alarm): Long {
        val now = Calendar.getInstance()
        val candidate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (alarm.repeatDays.isEmpty()) {
            // One-time alarm: schedule for next occurrence (today or tomorrow)
            if (candidate.timeInMillis <= now.timeInMillis) {
                candidate.add(Calendar.DAY_OF_YEAR, 1)
            }
            return candidate.timeInMillis
        }

        // Find the nearest upcoming day in repeatDays
        // repeatDays uses ISO: 1=Mon…7=Sun; Calendar: MONDAY=2…SUNDAY=1
        fun isoToCalendar(iso: Int): Int = if (iso == 7) Calendar.SUNDAY else iso + 1

        val todayCalendarDay = now.get(Calendar.DAY_OF_WEEK)

        for (offset in 0..7) {
            val checkCalendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayOfWeek = checkCalendar.get(Calendar.DAY_OF_WEEK)
            val isoDay = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1

            if (isoDay in alarm.repeatDays && checkCalendar.timeInMillis > now.timeInMillis) {
                return checkCalendar.timeInMillis
            }
        }

        // Fallback: tomorrow
        return candidate.apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
    }
}
