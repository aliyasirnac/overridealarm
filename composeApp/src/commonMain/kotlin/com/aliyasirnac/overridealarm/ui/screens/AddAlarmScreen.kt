package com.aliyasirnac.overridealarm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliyasirnac.overridealarm.model.Alarm
import com.aliyasirnac.overridealarm.model.ALL_DAYS
import com.aliyasirnac.overridealarm.model.ChallengeType
import com.aliyasirnac.overridealarm.model.DAY_LABELS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmScreen(
    existingAlarm: Alarm?,
    onSave: (Alarm) -> Unit,
    onCancel: () -> Unit,
    onPickRingtone: ((uri: String?, name: String?) -> Unit) -> Unit = {}
) {
    val isEditing = existingAlarm != null

    var label by remember { mutableStateOf(existingAlarm?.label ?: "") }
    var repeatDays by remember { mutableStateOf(existingAlarm?.repeatDays ?: emptySet()) }
    var vibrate by remember { mutableStateOf(existingAlarm?.vibrate ?: true) }
    var snoozeEnabled by remember { mutableStateOf(existingAlarm?.snoozeEnabled ?: true) }
    var challengeType by remember { mutableStateOf(existingAlarm?.challengeType ?: ChallengeType.NONE) }
    var ringtoneUri by remember { mutableStateOf(existingAlarm?.ringtoneUri) }
    var ringtoneName by remember { mutableStateOf(existingAlarm?.ringtoneName) }
    var wakeupCheck by remember { mutableStateOf(existingAlarm?.wakeupCheck ?: false) }
    var forceSpeaker by remember { mutableStateOf(existingAlarm?.forceSpeaker ?: true) }
    var flashStrobe by remember { mutableStateOf(existingAlarm?.flashStrobe ?: false) }
    var ttsEnabled by remember { mutableStateOf(existingAlarm?.ttsEnabled ?: false) }
    var ttsMessage by remember { mutableStateOf(existingAlarm?.ttsMessage ?: "") }

    val timePickerState = rememberTimePickerState(
        initialHour = existingAlarm?.hour ?: 7,
        initialMinute = existingAlarm?.minute ?: 0,
        is24Hour = false
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Alarmı Düzenle" else "Yeni Alarm",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onCancel) {
                        Text("İptal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            onSave(
                                Alarm(
                                    id = existingAlarm?.id ?: kotlin.random.Random.nextLong(0, Long.MAX_VALUE),
                                    hour = timePickerState.hour,
                                    minute = timePickerState.minute,
                                    label = label.trim(),
                                    repeatDays = repeatDays,
                                    vibrate = vibrate,
                                    snoozeEnabled = snoozeEnabled,
                                    isEnabled = existingAlarm?.isEnabled ?: true,
                                    challengeType = challengeType,
                                    ringtoneUri = ringtoneUri,
                                    ringtoneName = ringtoneName,
                                    wakeupCheck = wakeupCheck,
                                    forceSpeaker = forceSpeaker,
                                    flashStrobe = flashStrobe,
                                    ttsEnabled = ttsEnabled,
                                    ttsMessage = ttsMessage.ifBlank { null }
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Kaydet", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Time picker area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                        clockDialSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                        clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                        selectorColor = MaterialTheme.colorScheme.primary,
                        timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Label
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Alarm adı (isteğe bağlı)") },
                placeholder = { Text("örn. İşe git, Toplantı") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Alarm Sound selector
            SectionCard(title = "Alarm Sesi") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onPickRingtone { uri, name ->
                                ringtoneUri = uri
                                ringtoneName = name
                            }
                        },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔔",
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ringtoneName ?: "Varsayılan alarm sesi",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Değiştirmek için dokunun",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "▶",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Repeat days
            SectionCard(title = "Tekrar") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ALL_DAYS.forEach { day ->
                        val isSelected = day in repeatDays
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                repeatDays = if (isSelected) repeatDays - day else repeatDays + day
                            },
                            label = {
                                Text(
                                    text = DAY_LABELS[day]?.take(2) ?: "",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (repeatDays.isEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tekrar seçilmedi — bir kez çalacak",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Challenge type selector
            SectionCard(title = "Uyanma Görevi") {
                Text(
                    text = "Alarmı kapatabilmek için bir görev tamamlayın",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChallengeChip("Yok", "😴", challengeType == ChallengeType.NONE, Modifier.weight(1f)) {
                        challengeType = ChallengeType.NONE
                    }
                    ChallengeChip("Matematik", "🧮", challengeType == ChallengeType.MATH, Modifier.weight(1f)) {
                        challengeType = ChallengeType.MATH
                    }
                    ChallengeChip("Sallama", "📱", challengeType == ChallengeType.SHAKE, Modifier.weight(1f)) {
                        challengeType = ChallengeType.SHAKE
                    }
                    ChallengeChip("Yazma", "⌨️", challengeType == ChallengeType.TYPING, Modifier.weight(1f)) {
                        challengeType = ChallengeType.TYPING
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Options
            SectionCard(title = "Seçenekler") {
                OptionRow(
                    label = "Titreşim",
                    description = "Alarm çalarken telefon titresin",
                    checked = vibrate,
                    onCheckedChange = { vibrate = it }
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                OptionRow(
                    label = "Erteleme (5 dk)",
                    description = "Alarmı 5 dakika ertele",
                    checked = snoozeEnabled,
                    onCheckedChange = { snoozeEnabled = it }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Override Features
            SectionCard(title = "Override Özellikleri") {
                OptionRow(
                    label = "🔊 Kulaklık Bypass",
                    description = "Kulaklık takılı olsa bile sesi hoparlörden çal",
                    checked = forceSpeaker,
                    onCheckedChange = { forceSpeaker = it }
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                OptionRow(
                    label = "📸 Flaş Strobe",
                    description = "Alarm çalarken flaş ışığı yanıp sönsün",
                    checked = flashStrobe,
                    onCheckedChange = { flashStrobe = it }
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                OptionRow(
                    label = "🗣️ Sesli Uyarı (TTS)",
                    description = "Alarm saatini ve mesajı yüksek sesle okusun",
                    checked = ttsEnabled,
                    onCheckedChange = { ttsEnabled = it }
                )
                if (ttsEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ttsMessage,
                        onValueChange = { ttsMessage = it },
                        label = { Text("TTS Mesajı (isteğe bağlı)") },
                        placeholder = { Text("örn. Toplantıya geç kalıyorsun!") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                OptionRow(
                    label = "⏰ Uyanıklık Doğrulaması",
                    description = "5 dk sonra \"Uyanık mısın?\" sorar, cevap vermezsen alarm tekrar çalar",
                    checked = wakeupCheck,
                    onCheckedChange = { wakeupCheck = it }
                )
            }

            // Override info banner
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(text = "🔒", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Override Alarm",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Bu alarm DND, sessiz mod ve pil kısıtlamalarını aşarak çalar. Ses seviyesi otomatik olarak max'a alınır.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ChallengeChip(
    label: String,
    emoji: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = emoji, fontSize = 18.sp)
                Text(text = label, style = MaterialTheme.typography.labelSmall)
            }
        },
        modifier = modifier.height(60.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

@Composable
private fun OptionRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}
