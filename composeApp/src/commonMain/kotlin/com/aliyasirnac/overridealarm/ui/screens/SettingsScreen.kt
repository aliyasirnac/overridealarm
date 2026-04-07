package com.aliyasirnac.overridealarm.ui.screens

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliyasirnac.overridealarm.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    currentTheme: ThemeMode = ThemeMode.DARK,
    onThemeChange: (ThemeMode) -> Unit = {}
) {
    // Persisted settings would use DataStore in a real app
    var defaultVibrate by remember { mutableStateOf(true) }
    var defaultForceSpeaker by remember { mutableStateOf(true) }
    var defaultFlashStrobe by remember { mutableStateOf(false) }
    var defaultTts by remember { mutableStateOf(false) }
    var defaultWakeupCheck by remember { mutableStateOf(false) }
    var use24HourFormat by remember { mutableStateOf(false) }
    var autoSilenceMinutes by remember { mutableStateOf(10) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Ayarlar",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("← Geri", color = MaterialTheme.colorScheme.primary)
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
                .verticalScroll(rememberScrollState())
        ) {
            // ── Theme ──
            SettingsSection(title = "Tema") {
                val options = listOf(
                    Triple(ThemeMode.SYSTEM, "📱", "Sistem"),
                    Triple(ThemeMode.LIGHT, "☀️", "Açık"),
                    Triple(ThemeMode.DARK, "🌙", "Koyu")
                )
                options.forEachIndexed { index, (mode, icon, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeChange(mode) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(icon, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (currentTheme == mode) {
                            Text(
                                "✓",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                    if (index < options.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── General Settings ──
            SettingsSection(title = "Genel") {
                SettingsToggleRow(
                    icon = "🕐",
                    label = "24 Saat Formatı",
                    description = "Saati 24 saat formatında göster",
                    checked = use24HourFormat,
                    onCheckedChange = { use24HourFormat = it }
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                SettingsToggleRow(
                    icon = "📳",
                    label = "Varsayılan Titreşim",
                    description = "Yeni alarmlar titreşimle başlasın",
                    checked = defaultVibrate,
                    onCheckedChange = { defaultVibrate = it }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Override Defaults ──
            SettingsSection(title = "Override Varsayılanlar") {
                SettingsToggleRow(
                    icon = "🔊",
                    label = "Kulaklık Bypass",
                    description = "Varsayılan olarak hoparlörden çalsın",
                    checked = defaultForceSpeaker,
                    onCheckedChange = { defaultForceSpeaker = it }
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                SettingsToggleRow(
                    icon = "📸",
                    label = "Flaş Strobe",
                    description = "Varsayılan olarak flaş yanıp sönsün",
                    checked = defaultFlashStrobe,
                    onCheckedChange = { defaultFlashStrobe = it }
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                SettingsToggleRow(
                    icon = "🗣️",
                    label = "Sesli Uyarı (TTS)",
                    description = "Varsayılan olarak sesli uyarı açık olsun",
                    checked = defaultTts,
                    onCheckedChange = { defaultTts = it }
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                SettingsToggleRow(
                    icon = "⏰",
                    label = "Uyanıklık Doğrulaması",
                    description = "Varsayılan olarak 5dk sonra kontrol",
                    checked = defaultWakeupCheck,
                    onCheckedChange = { defaultWakeupCheck = it }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Auto Silence ──
            SettingsSection(title = "Alarm Davranışı") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔇", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Otomatik Susturma",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "$autoSilenceMinutes dakika sonra alarm sussun",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { if (autoSilenceMinutes > 1) autoSilenceMinutes-- },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("−", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text(
                            "$autoSilenceMinutes dk",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(48.dp),
                            textAlign = TextAlign.Center
                        )
                        FilledTonalIconButton(
                            onClick = { if (autoSilenceMinutes < 30) autoSilenceMinutes++ },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── About ──
            SettingsSection(title = "Hakkında") {
                AboutRow(label = "Uygulama", value = "Override Alarm")
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                AboutRow(label = "Sürüm", value = "1.0.0")
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                AboutRow(label = "Geliştirici", value = "Ali Yasir Naç")
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                AboutRow(label = "Platform", value = "Kotlin Multiplatform")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer
            Text(
                text = "Override Alarm v1.0.0 🔥",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            )
        }
    }
}

@Composable
private fun SettingsSection(
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
private fun SettingsToggleRow(
    icon: String,
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(12.dp))
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

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
