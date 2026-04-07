package com.aliyasirnac.overridealarm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliyasirnac.overridealarm.model.Alarm
import com.aliyasirnac.overridealarm.ui.components.AlarmCard
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    alarms: List<Alarm>,
    onAddAlarm: () -> Unit,
    onToggleAlarm: (Alarm) -> Unit,
    onDeleteAlarm: (Alarm) -> Unit,
    onEditAlarm: (Alarm) -> Unit,
    permissionBanner: (@Composable () -> Unit)? = null
) {
    var currentInstant by remember { mutableStateOf(Clock.System.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentInstant = Clock.System.now()
            delay(1000)
        }
    }

    val localNow = currentInstant.toLocalDateTime(TimeZone.currentSystemDefault())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onAddAlarm,
                    icon = {
                        Text(text = "+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    },
                    text = { Text("Alarm Ekle", fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
                )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                ClockHeader(
                    hour = localNow.hour,
                    minute = localNow.minute,
                    dayOfWeek = localNow.dayOfWeek.name,
                    activeAlarmCount = alarms.count { it.isEnabled }
                )
            }

            permissionBanner?.let {
                item { it() }
            }

            if (alarms.isEmpty()) {
                item { EmptyState() }
            } else {
                item {
                    Text(
                        text = "ALARMLARINIZ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onToggle = { onToggleAlarm(alarm) },
                        onDelete = { onDeleteAlarm(alarm) },
                        onEdit = { onEditAlarm(alarm) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClockHeader(
    hour: Int,
    minute: Int,
    dayOfWeek: String,
    activeAlarmCount: Int
) {
    val h = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val amPm = if (hour < 12) "AM" else "PM"
    val dayStr = dayOfWeek.lowercase().replaceFirstChar { it.uppercase() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${h}:${minute.toString().padStart(2, '0')}",
                fontSize = 72.sp,
                fontWeight = FontWeight.Thin,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 72.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = amPm,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        Text(
            text = dayStr,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (activeAlarmCount > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "$activeAlarmCount alarm aktif",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "🔔", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Alarm yok",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = "İlk alarmını eklemek için + butonuna tıkla",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
