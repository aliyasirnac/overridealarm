package com.aliyasirnac.overridealarm.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
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
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aliyasirnac.overridealarm.MainActivity
import java.util.Calendar
import java.util.Locale

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private var volumeEnforceHandler: android.os.Handler? = null
    private var volumeEnforceRunnable: Runnable? = null

    // Flash strobe
    private var flashHandler: android.os.Handler? = null
    private var flashRunnable: Runnable? = null
    private var isFlashOn = false

    // TTS
    private var textToSpeech: TextToSpeech? = null
    private var ttsHandler: android.os.Handler? = null
    private var ttsRunnable: Runnable? = null

    // Wakeup check
    private var wakeupCheckHandler: android.os.Handler? = null

    // State
    private var currentAlarmId: Long = -1L
    private var currentAlarmLabel: String = ""
    private var currentSnoozeEnabled: Boolean = true
    private var currentSnoozeMinutes: Int = 5
    private var currentChallengeType: String = "NONE"
    private var currentRingtoneUri: String? = null
    private var currentForceSpeaker: Boolean = true
    private var currentFlashStrobe: Boolean = false
    private var currentTtsEnabled: Boolean = false
    private var currentTtsMessage: String? = null
    private var currentWakeupCheck: Boolean = false

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
                // If wakeup check is on, schedule a verification instead of clean dismiss
                if (currentWakeupCheck) {
                    scheduleWakeupCheck()
                }
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_WAKEUP_CONFIRMED -> {
                // User confirmed wakeup — cancel any pending re-alarm
                wakeupCheckHandler?.removeCallbacksAndMessages(null)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_WAKEUP_RE_ALARM -> {
                // User didn't confirm — re-trigger alarm at full blast
                startAlarm(intent, isRetrigger = true)
                return START_NOT_STICKY
            }
            else -> startAlarm(intent, isRetrigger = false)
        }
        return START_NOT_STICKY
    }

    private fun startAlarm(intent: Intent?, isRetrigger: Boolean = false) {
        currentAlarmId = intent?.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L) ?: -1L
        currentAlarmLabel = intent?.getStringExtra(AlarmReceiver.EXTRA_ALARM_LABEL) ?: ""
        val vibrate = intent?.getBooleanExtra(AlarmReceiver.EXTRA_VIBRATE, true) ?: true
        currentSnoozeEnabled = if (isRetrigger) false else (intent?.getBooleanExtra(AlarmReceiver.EXTRA_SNOOZE_ENABLED, true) ?: true)
        currentSnoozeMinutes = intent?.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, 5) ?: 5
        currentChallengeType = intent?.getStringExtra(AlarmReceiver.EXTRA_CHALLENGE_TYPE) ?: "NONE"
        currentRingtoneUri = intent?.getStringExtra(AlarmReceiver.EXTRA_RINGTONE_URI)
        currentForceSpeaker = intent?.getBooleanExtra(AlarmReceiver.EXTRA_FORCE_SPEAKER, true) ?: true
        currentFlashStrobe = intent?.getBooleanExtra(AlarmReceiver.EXTRA_FLASH_STROBE, false) ?: false
        currentTtsEnabled = intent?.getBooleanExtra(AlarmReceiver.EXTRA_TTS_ENABLED, false) ?: false
        currentTtsMessage = intent?.getStringExtra(AlarmReceiver.EXTRA_TTS_MESSAGE)
        currentWakeupCheck = if (isRetrigger) false else (intent?.getBooleanExtra(AlarmReceiver.EXTRA_WAKEUP_CHECK, false) ?: false)

        // Acquire CPU wake lock
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "OverrideAlarm::WakeLock"
        ).also { it.acquire(10 * 60 * 1000L) }

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        // ── Feature 1: Headphone Bypass ─────────────────────────
        if (currentForceSpeaker) {
            forceToSpeaker(audioManager)
        }

        // Force alarm stream to maximum volume
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

        // Request audio focus
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

        // Play alarm sound
        try {
            val alarmUri = if (!currentRingtoneUri.isNullOrBlank()) {
                android.net.Uri.parse(currentRingtoneUri)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()
                )
                setDataSource(applicationContext, alarmUri)
                isLooping = true
                prepare()
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
                start()
            }

            // Continuous volume enforcer
            volumeEnforceHandler = android.os.Handler(android.os.Looper.getMainLooper())
            volumeEnforceRunnable = object : Runnable {
                override fun run() {
                    try {
                        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                        if (currentVol < maxVolume) {
                            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
                        }
                        // Re-force speaker if headphones re-connected
                        if (currentForceSpeaker) {
                            forceToSpeaker(audioManager)
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

        // ── Feature 2: Flash Strobe ─────────────────────────
        if (currentFlashStrobe) startFlashStrobe()

        // ── Feature 3: TTS Announcements ────────────────────
        if (currentTtsEnabled) startTTS()

        // Launch full-screen AlarmActivity
        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  Feature 1: Headphone Bypass — Force Speaker Output
    // ═══════════════════════════════════════════════════════════
    private fun forceToSpeaker(audioManager: AudioManager) {
        try {
            // Check if wired/bluetooth headset is connected
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                val hasHeadphones = devices.any { device ->
                    device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    device.type == AudioDeviceInfo.TYPE_USB_HEADSET
                }
                if (hasHeadphones) {
                    // Force audio through speaker
                    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                    audioManager.isSpeakerphoneOn = true
                    Log.d(TAG, "Headphones detected — forced audio to speaker")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error forcing speaker", e)
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  Feature 2: Flash Strobe (Disko Etkisi)
    // ═══════════════════════════════════════════════════════════
    private fun startFlashStrobe() {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return

            flashHandler = android.os.Handler(android.os.Looper.getMainLooper())
            flashRunnable = object : Runnable {
                override fun run() {
                    try {
                        isFlashOn = !isFlashOn
                        cameraManager.setTorchMode(cameraId, isFlashOn)
                        // Toggle every ~250ms → ~4 flashes/second
                        flashHandler?.postDelayed(this, 250L)
                    } catch (e: Exception) {
                        Log.e(TAG, "Flash strobe error", e)
                    }
                }
            }
            flashHandler?.post(flashRunnable!!)
            Log.d(TAG, "Flash strobe started")
        } catch (e: Exception) {
            Log.e(TAG, "Cannot start flash strobe", e)
        }
    }

    private fun stopFlashStrobe() {
        flashHandler?.removeCallbacksAndMessages(null)
        flashHandler = null
        flashRunnable = null
        try {
            if (isFlashOn) {
                val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val cameraId = cameraManager.cameraIdList.firstOrNull()
                if (cameraId != null) {
                    cameraManager.setTorchMode(cameraId, false)
                }
                isFlashOn = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping flash", e)
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  Feature 3: TTS (Text-To-Speech) Announcements
    // ═══════════════════════════════════════════════════════════
    private fun startTTS() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale("tr", "TR")
                // If Turkish not available, fall back to default
                val result = textToSpeech?.isLanguageAvailable(Locale("tr", "TR"))
                if (result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    textToSpeech?.language = Locale.getDefault()
                }

                // Set TTS to use alarm stream
                val params = android.os.Bundle()
                params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
                params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)

                // Speak every 15 seconds
                ttsHandler = android.os.Handler(android.os.Looper.getMainLooper())
                ttsRunnable = object : Runnable {
                    override fun run() {
                        try {
                            val message = buildTTSMessage()
                            textToSpeech?.speak(message, TextToSpeech.QUEUE_ADD, params, "alarm_tts")
                            Log.d(TAG, "TTS speaking: $message")
                            ttsHandler?.postDelayed(this, 15_000L) // Repeat every 15s
                        } catch (e: Exception) {
                            Log.e(TAG, "TTS error", e)
                        }
                    }
                }
                // Start first announcement after 3 seconds
                ttsHandler?.postDelayed(ttsRunnable!!, 3_000L)
            }
        }
    }

    private fun buildTTSMessage(): String {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val timeStr = String.format("%02d:%02d", hour, minute)

        val customMessage = currentTtsMessage
        return if (!customMessage.isNullOrBlank()) {
            "Saat $timeStr! $customMessage"
        } else if (currentAlarmLabel.isNotBlank()) {
            "Saat $timeStr oldu! ${currentAlarmLabel}! Hemen kalk!"
        } else {
            "Saat $timeStr oldu! Kalkma zamanı! Hemen uyan!"
        }
    }

    private fun stopTTS() {
        ttsHandler?.removeCallbacksAndMessages(null)
        ttsHandler = null
        ttsRunnable = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    // ═══════════════════════════════════════════════════════════
    //  Feature 4: Wakeup Verification (5-min post-dismiss check)
    // ═══════════════════════════════════════════════════════════
    private fun scheduleWakeupCheck() {
        Log.d(TAG, "Wakeup check scheduled — will verify in 5 minutes")

        // Create a notification asking "Are you awake?" after 5 minutes
        wakeupCheckHandler = android.os.Handler(android.os.Looper.getMainLooper())
        wakeupCheckHandler?.postDelayed({
            showWakeupCheckNotification()

            // If user doesn't confirm within 30 seconds, re-trigger alarm
            wakeupCheckHandler?.postDelayed({
                Log.d(TAG, "No wakeup confirmation — re-triggering alarm!")
                retriggerAlarm()
            }, 30_000L) // 30 seconds to respond
        }, 5 * 60 * 1000L) // 5 minutes
    }

    private fun showWakeupCheckNotification() {
        val confirmPi = PendingIntent.getService(
            this, 10,
            Intent(this, AlarmService::class.java).apply { action = ACTION_WAKEUP_CONFIRMED },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⏰ Gerçekten Uyanık mısın?")
            .setContentText("30 saniye içinde dokunmazsan alarm tekrar çalacak!")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_menu_agenda,
                "✅ Evet, uyanığım!",
                confirmPi
            )
            .setTimeoutAfter(30_000L)
            .build()

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(WAKEUP_CHECK_NOTIFICATION_ID, notification)
    }

    private fun retriggerAlarm() {
        // Cancel wakeup check notification
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(WAKEUP_CHECK_NOTIFICATION_ID)

        // Re-trigger alarm as a new startCommand with no snooze, no wakeup check
        val reIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_WAKEUP_RE_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, currentAlarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, "KALK! " + currentAlarmLabel)
            putExtra(AlarmReceiver.EXTRA_VIBRATE, true)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_ENABLED, false)
            putExtra(AlarmReceiver.EXTRA_CHALLENGE_TYPE, currentChallengeType)
            putExtra(AlarmReceiver.EXTRA_RINGTONE_URI, currentRingtoneUri)
            putExtra(AlarmReceiver.EXTRA_FORCE_SPEAKER, true)
            putExtra(AlarmReceiver.EXTRA_FLASH_STROBE, true) // Force flash on retrigger
            putExtra(AlarmReceiver.EXTRA_TTS_ENABLED, true) // Force TTS on retrigger
            putExtra(AlarmReceiver.EXTRA_TTS_MESSAGE, "Tekrar uyudun! Hemen kalk! Bu son uyarı!")
            putExtra(AlarmReceiver.EXTRA_WAKEUP_CHECK, false) // No more checks
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(reIntent)
        } else {
            startService(reIntent)
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  Existing Helper Methods
    // ═══════════════════════════════════════════════════════════

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
            putExtra(AlarmReceiver.EXTRA_SNOOZE_ENABLED, false)
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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, currentAlarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, currentAlarmLabel)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_ENABLED, currentSnoozeEnabled)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, currentSnoozeMinutes)
            putExtra(AlarmReceiver.EXTRA_CHALLENGE_TYPE, currentChallengeType)
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

        val snoozePi = PendingIntent.getService(
            this, 2,
            Intent(this, AlarmService::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, currentAlarmId)
                putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, currentSnoozeMinutes)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayLabel = if (label.isBlank()) "Kalkma zamanı!" else label

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⏰ Alarm")
            .setContentText(displayLabel)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPi, true)
            .setAutoCancel(false)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_lock_idle_alarm,
                "Ertele ($currentSnoozeMinutes dk)",
                snoozePi
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Kapat",
                dismissPi
            )
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Stop media
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null

        // Release wake lock
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null

        // Stop vibration
        vibrator?.cancel()
        vibrator = null

        // Release audio focus
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                (getSystemService(AUDIO_SERVICE) as AudioManager).abandonAudioFocusRequest(it)
            }
        }

        // Reset speaker mode
        try {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            audioManager.isSpeakerphoneOn = false
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (e: Exception) { /* ignore */ }

        // Stop volume enforcer
        volumeEnforceHandler?.removeCallbacksAndMessages(null)
        volumeEnforceHandler = null
        volumeEnforceRunnable = null

        // Stop flash strobe
        stopFlashStrobe()

        // Stop TTS
        stopTTS()

        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    companion object {
        private const val TAG = "AlarmService"
        const val CHANNEL_ID = "override_alarm_channel"
        const val NOTIFICATION_ID = 1001
        const val WAKEUP_CHECK_NOTIFICATION_ID = 1002
        const val ACTION_SNOOZE = "com.aliyasirnac.overridealarm.ACTION_SNOOZE"
        const val ACTION_DISMISS = "com.aliyasirnac.overridealarm.ACTION_DISMISS"
        const val ACTION_WAKEUP_CONFIRMED = "com.aliyasirnac.overridealarm.ACTION_WAKEUP_CONFIRMED"
        const val ACTION_WAKEUP_RE_ALARM = "com.aliyasirnac.overridealarm.ACTION_WAKEUP_RE_ALARM"
    }
}
