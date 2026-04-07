package com.aliyasirnac.overridealarm.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.aliyasirnac.overridealarm.MainActivity

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private var volumeEnforceHandler: android.os.Handler? = null
    private var volumeEnforceRunnable: Runnable? = null

    private var currentAlarmId: Long = -1L
    private var currentAlarmLabel: String = ""
    private var currentSnoozeEnabled: Boolean = true
    private var currentSnoozeMinutes: Int = 5
    private var currentChallengeType: String = "NONE"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SNOOZE -> {
                val minutes = intent.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, 5)
                scheduleSnooze(currentAlarmId, currentAlarmLabel, minutes)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_DISMISS -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> startAlarm(intent)
        }
        return START_NOT_STICKY
    }

    private fun startAlarm(intent: Intent?) {
        currentAlarmId = intent?.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L) ?: -1L
        currentAlarmLabel = intent?.getStringExtra(AlarmReceiver.EXTRA_ALARM_LABEL) ?: ""
        val vibrate = intent?.getBooleanExtra(AlarmReceiver.EXTRA_VIBRATE, true) ?: true
        currentSnoozeEnabled = intent?.getBooleanExtra(AlarmReceiver.EXTRA_SNOOZE_ENABLED, true) ?: true
        currentSnoozeMinutes = intent?.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, 5) ?: 5
        currentChallengeType = intent?.getStringExtra(AlarmReceiver.EXTRA_CHALLENGE_TYPE) ?: "NONE"

        // Acquire CPU wake lock so alarm runs even if screen is off
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "OverrideAlarm::WakeLock"
        ).also { it.acquire(10 * 60 * 1000L) }

        // Force alarm stream to maximum volume — overrides silent/DND for alarm stream
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

        // Request audio focus on ALARM stream
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAcceptsDelayedFocusGain(false)
                .build()
            audioFocusRequest = focusRequest
            audioManager.requestAudioFocus(focusRequest)
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(currentAlarmLabel))

        // Play alarm sound using STREAM_ALARM (bypasses DND alarm exception)
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        // FLAG_AUDIBILITY_ENFORCED can sometimes help bypass restrictions
                        .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()
                )
                setDataSource(applicationContext, alarmUri)
                isLooping = true
                prepare()
                
                // Blast volume right before start
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
                start()
            }
            
            // Continuous volume enforcer to combat aggressive OS volume lowering
            volumeEnforceHandler = android.os.Handler(android.os.Looper.getMainLooper())
            volumeEnforceRunnable = object : Runnable {
                override fun run() {
                    try {
                        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                        if (currentVol < maxVolume) {
                            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
                        }
                        volumeEnforceHandler?.postDelayed(this, 1000L)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            volumeEnforceHandler?.post(volumeEnforceRunnable!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Vibrate
        if (vibrate) startVibration()

        // Launch full-screen AlarmActivity
        val activityIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, currentAlarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, currentAlarmLabel)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_ENABLED, currentSnoozeEnabled)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, currentSnoozeMinutes)
            putExtra(AlarmReceiver.EXTRA_CHALLENGE_TYPE, currentChallengeType)
        }
        startActivity(activityIntent)
    }

    private fun startVibration() {
        val pattern = longArrayOf(0, 800, 400, 800, 400)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun scheduleSnooze(alarmId: Long, label: String, minutes: Int) {
        val snoozeTime = System.currentTimeMillis() + minutes * 60 * 1000L
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, if (label.isBlank()) "Ertelendi" else "$label (Ertelendi)")
            putExtra(AlarmReceiver.EXTRA_VIBRATE, true)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_ENABLED, false) // No nested snooze
            putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, 5)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            (alarmId xor 0xDEAD).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val alarmInfo = android.app.AlarmManager.AlarmClockInfo(snoozeTime, pendingIntent)
        alarmManager.setAlarmClock(alarmInfo, pendingIntent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Bildirimleri",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Override Alarm çalıyor"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(label: String): Notification {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, currentAlarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, currentAlarmLabel)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_ENABLED, currentSnoozeEnabled)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, currentSnoozeMinutes)
        }
        val fullScreenPi = PendingIntent.getActivity(
            this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissPi = PendingIntent.getService(
            this, 1,
            Intent(this, AlarmService::class.java).apply { action = ACTION_DISMISS },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⏰ Alarm")
            .setContentText(label.ifBlank { "Kalkma zamanı!" })
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPi, true)
            .setAutoCancel(false)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Kapat",
                dismissPi
            )
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null

        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null

        vibrator?.cancel()
        vibrator = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                (getSystemService(AUDIO_SERVICE) as AudioManager).abandonAudioFocusRequest(it)
            }
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    companion object {
        const val CHANNEL_ID = "override_alarm_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_SNOOZE = "com.aliyasirnac.overridealarm.ACTION_SNOOZE"
        const val ACTION_DISMISS = "com.aliyasirnac.overridealarm.ACTION_DISMISS"
    }
}
