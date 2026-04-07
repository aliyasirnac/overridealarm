package com.aliyasirnac.overridealarm.alarm

import android.app.KeyguardManager
import android.content.Intent
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliyasirnac.overridealarm.ui.theme.OverrideAlarmTheme
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class AlarmActivity : ComponentActivity() {

    private var alarmId: Long = -1L
    private var alarmLabel: String = ""
    private var snoozeEnabled: Boolean = true
    private var snoozeMinutes: Int = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L)
        alarmLabel = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_LABEL) ?: ""
        snoozeEnabled = intent.getBooleanExtra(AlarmReceiver.EXTRA_SNOOZE_ENABLED, true)
        snoozeMinutes = intent.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, 5)

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

        setContent {
            OverrideAlarmTheme {
                AlarmScreen(
                    label = alarmLabel,
                    snoozeEnabled = snoozeEnabled,
                    snoozeMinutes = snoozeMinutes,
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
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
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

                SwipeToDismissButton(onDismiss = onDismiss)

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SwipeToDismissButton(onDismiss: () -> Unit) {
    var offsetX by remember { mutableStateOf(0f) }
    val maxDrag = 220f // approx dp to px

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
        // Glow ring
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
