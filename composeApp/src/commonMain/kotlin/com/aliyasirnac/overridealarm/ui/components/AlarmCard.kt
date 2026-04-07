package com.aliyasirnac.overridealarm.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliyasirnac.overridealarm.model.Alarm
import com.aliyasirnac.overridealarm.model.ALL_DAYS
import com.aliyasirnac.overridealarm.model.DAY_LABELS
import com.aliyasirnac.overridealarm.model.formattedTimeParts

@Composable
fun AlarmCard(
    alarm: Alarm,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (timeStr, amPm) = alarm.formattedTimeParts()
    val contentAlpha = if (alarm.isEnabled) 1f else 0.45f

    val animatedContainerColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (alarm.isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = androidx.compose.animation.core.tween(300)
    )
    val animatedElevation by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (alarm.isEnabled) 8.dp else 0.dp,
        animationSpec = androidx.compose.animation.core.tween(300)
    )

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = animatedContainerColor
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = animatedElevation
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = timeStr,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = contentAlpha),
                            lineHeight = 42.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = amPm,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = contentAlpha * 0.75f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    if (alarm.label.isNotBlank()) {
                        Text(
                            text = alarm.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = contentAlpha * 0.7f)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Switch(
                        checked = alarm.isEnabled,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    TextButton(
                        onClick = onDelete,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Sil",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            if (alarm.repeatDays.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ALL_DAYS.forEach { day ->
                        DayBadge(
                            label = DAY_LABELS[day] ?: "",
                            isActive = day in alarm.repeatDays,
                            isAlarmEnabled = alarm.isEnabled
                        )
                    }
                }
            } else if (!alarm.isEnabled) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Kapalı",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun DayBadge(
    label: String,
    isActive: Boolean,
    isAlarmEnabled: Boolean
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = when {
            isActive && isAlarmEnabled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            isActive -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
        }
    ) {
        Text(
            text = label.take(2),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isActive && isAlarmEnabled -> MaterialTheme.colorScheme.primary
                isActive -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            },
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}
