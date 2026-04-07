package com.aliyasirnac.overridealarm.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliyasirnac.overridealarm.model.ChallengeType
import com.aliyasirnac.overridealarm.ui.theme.OverrideAlarmTheme
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.sqrt
import kotlin.random.Random

class AlarmActivity : ComponentActivity() {

    private var alarmId: Long = -1L
    private var alarmLabel: String = ""
    private var snoozeEnabled: Boolean = true
    private var snoozeMinutes: Int = 5
    private var challengeTypeStr: String = "NONE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L)
        alarmLabel = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_LABEL) ?: ""
        snoozeEnabled = intent.getBooleanExtra(AlarmReceiver.EXTRA_SNOOZE_ENABLED, true)
        snoozeMinutes = intent.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, 5)
        challengeTypeStr = intent.getStringExtra(AlarmReceiver.EXTRA_CHALLENGE_TYPE) ?: "NONE"

        // Wake up screen and show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(KEYGUARD_SERVICE) as KeyguardManager)
                .requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        enableEdgeToEdge()

        val challengeType = try {
            ChallengeType.valueOf(challengeTypeStr)
        } catch (_: Exception) {
            ChallengeType.NONE
        }

        setContent {
            OverrideAlarmTheme {
                AlarmScreen(
                    label = alarmLabel,
                    snoozeEnabled = snoozeEnabled,
                    snoozeMinutes = snoozeMinutes,
                    challengeType = challengeType,
                    onDismiss = { dismissAlarm() },
                    onSnooze = { snoozeAlarm() }
                )
            }
        }
    }

    private fun dismissAlarm() {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_DISMISS
        }
        startService(stopIntent)
        stopService(Intent(this, AlarmService::class.java))
        finish()
    }

    private fun snoozeAlarm() {
        val snoozeIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_SNOOZE
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }
        startService(snoozeIntent)
        finish()
    }

    override fun onBackPressed() {
        // Prevent dismissing with back button — user must explicitly dismiss
    }
}

@Composable
private fun AlarmScreen(
    label: String,
    snoozeEnabled: Boolean,
    snoozeMinutes: Int,
    challengeType: ChallengeType,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    var challengeCompleted by remember { mutableStateOf(challengeType == ChallengeType.NONE) }

    var currentInstant by remember { mutableStateOf(Clock.System.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentInstant = Clock.System.now()
            delay(1000)
        }
    }

    val localNow = currentInstant.toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = localNow.hour
    val h = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val amPm = if (hour < 12) "AM" else "PM"
    val timeStr = "${h}:${localNow.minute.toString().padStart(2, '0')}"

    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")
    val color1 by infiniteTransition.animateColor(
        initialValue = Color(0xFF0F172A),
        targetValue = Color(0xFF1E1B4B),
        animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse),
        label = "color1"
    )
    val color2 by infiniteTransition.animateColor(
        initialValue = Color(0xFF020617),
        targetValue = Color(0xFF09090B),
        animationSpec = infiniteRepeatable(tween(5000), RepeatMode.Reverse),
        label = "color2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(color1, color2)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PulsingBell()

                Spacer(modifier = Modifier.height(40.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = timeStr,
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White,
                        lineHeight = 80.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = amPm,
                        fontSize = 24.sp,
                        color = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                if (label.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = label,
                        fontSize = 20.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = localNow.dayOfWeek.name.lowercase()
                        .replaceFirstChar { it.uppercase() },
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.45f)
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Show challenge UI if not completed
                if (!challengeCompleted) {
                    when (challengeType) {
                        ChallengeType.MATH -> MathChallenge { challengeCompleted = true }
                        ChallengeType.SHAKE -> ShakeChallenge { challengeCompleted = true }
                        ChallengeType.TYPING -> TypingChallenge { challengeCompleted = true }
                        ChallengeType.NONE -> { /* already completed */ }
                    }
                }

                if (snoozeEnabled) {
                    TextButton(
                        onClick = onSnooze,
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(54.dp),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.8f))
                    ) {
                        Text(
                            text = "Zz Ertele (${snoozeMinutes} dk)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (challengeCompleted) {
                    SwipeToDismissButton(onDismiss = onDismiss)
                } else {
                    Text(
                        text = "Kapatmak için görevi tamamlayın ⬆️",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ─── Math Challenge ───────────────────────────────────────────────
@Composable
private fun MathChallenge(onSolved: () -> Unit) {
    val a = remember { Random.nextInt(10, 99) }
    val b = remember { Random.nextInt(10, 99) }
    val correctAnswer = remember { a + b }
    var userAnswer by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🧮 Matematik Görevi",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "$a + $b = ?",
                fontSize = 36.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFF93C5FD)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = userAnswer,
                onValueChange = {
                    userAnswer = it.filter { c -> c.isDigit() }
                    showError = false
                },
                placeholder = { Text("Cevabınız", color = Color.White.copy(alpha = 0.4f)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (userAnswer.toIntOrNull() == correctAnswer) {
                            onSolved()
                        } else {
                            showError = true
                            userAnswer = ""
                        }
                    }
                ),
                modifier = Modifier.fillMaxWidth(0.5f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF60A5FA),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF60A5FA)
                ),
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    fontSize = 24.sp
                )
            )
            if (showError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "❌ Yanlış, tekrar deneyin!",
                    color = Color(0xFFFCA5A5),
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (userAnswer.toIntOrNull() == correctAnswer) {
                        onSolved()
                    } else {
                        showError = true
                        userAnswer = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Kontrol Et", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Shake Challenge ──────────────────────────────────────────────
@Composable
private fun ShakeChallenge(onSolved: () -> Unit) {
    val requiredShakes = 15
    var shakeCount by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val progress = (shakeCount.toFloat() / requiredShakes).coerceIn(0f, 1f)

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var lastMagnitude = 0f
        var lastShakeTime = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val delta = magnitude - lastMagnitude
                lastMagnitude = magnitude

                val now = System.currentTimeMillis()
                if (delta > 6f && now - lastShakeTime > 300) {
                    lastShakeTime = now
                    shakeCount++
                    if (shakeCount >= requiredShakes) {
                        onSolved()
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📱 Sallama Görevi",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Telefonunu $requiredShakes kez salla!",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Progress indicator
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(100.dp),
                    color = Color(0xFF34D399),
                    trackColor = Color.White.copy(alpha = 0.15f),
                    strokeWidth = 8.dp
                )
                Text(
                    text = "$shakeCount/$requiredShakes",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF34D399)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (shakeCount > 0) "Devam et! 💪" else "Başla! 🏃",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        }
    }
}

// ─── Typing Challenge ─────────────────────────────────────────────
@Composable
private fun TypingChallenge(onSolved: () -> Unit) {
    val phrases = listOf(
        "günaydın dünya",
        "kahve zamanı",
        "yeni bir gün",
        "haydi kalk artık",
        "bugün güzel olacak"
    )
    val targetPhrase = remember { phrases[Random.nextInt(phrases.size)] }
    var userInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⌨️ Yazma Görevi",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Aşağıdaki ifadeyi yazın:",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // The phrase to type
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "\"$targetPhrase\"",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFFBBF24),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = userInput,
                onValueChange = {
                    userInput = it
                    showError = false
                },
                placeholder = { Text("Yazın...", color = Color.White.copy(alpha = 0.4f)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (userInput.trim().lowercase() == targetPhrase.lowercase()) {
                            onSolved()
                        } else {
                            showError = true
                        }
                    }
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFBBF24),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFFBBF24)
                )
            )
            if (showError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "❌ Eşleşmiyor, tekrar deneyin!",
                    color = Color(0xFFFCA5A5),
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (userInput.trim().lowercase() == targetPhrase.lowercase()) {
                        onSolved()
                    } else {
                        showError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBF24)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Kontrol Et", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}

// ─── Swipe to Dismiss ─────────────────────────────────────────────
@Composable
private fun SwipeToDismissButton(onDismiss: () -> Unit) {
    var offsetX by remember { mutableStateOf(0f) }
    val maxDrag = 220f

    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .height(68.dp)
            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(34.dp))
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "Kapatmak için kaydırın ➔",
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.titleMedium
        )

        Box(
            modifier = Modifier
                .offset(x = (offsetX).dp)
                .size(60.dp)
                .background(Color(0xFF3B82F6), CircleShape)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val newOffset = offsetX + delta / 2.5f
                        if (newOffset in 0f..maxDrag) {
                            offsetX = newOffset
                        }
                    },
                    onDragStopped = {
                        if (offsetX > maxDrag * 0.7f) {
                            onDismiss()
                        } else {
                            offsetX = 0f
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("X", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Pulsing Bell ─────────────────────────────────────────────────
@Composable
private fun PulsingBell() {
    val infiniteTransition = rememberInfiniteTransition(label = "bell_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .background(Color(0xFFA78BFA).copy(alpha = 0.15f), CircleShape)
        )
        Text(
            text = "🔔",
            fontSize = 64.sp,
            modifier = Modifier.scale(scale)
        )
    }
}
