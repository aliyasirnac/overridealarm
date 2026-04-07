package com.aliyasirnac.overridealarm

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aliyasirnac.overridealarm.alarm.AlarmSchedulerImpl
import com.aliyasirnac.overridealarm.repository.AlarmRepositoryImpl
import com.aliyasirnac.overridealarm.ui.theme.OverrideAlarmTheme
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    // Callback to return ringtone selection to the Compose UI
    private var ringtoneCallback: ((uri: String?, name: String?) -> Unit)? = null

    private lateinit var ringtonePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var customFilePickerLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Register ringtone picker launcher (System sounds)
        ringtonePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val ringtoneUri: Uri? = result.data
                ?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)

            if (ringtoneUri != null) {
                val ringtone = RingtoneManager.getRingtone(this, ringtoneUri)
                val name = ringtone?.getTitle(this) ?: "Bilinmeyen ses"
                ringtoneCallback?.invoke(ringtoneUri.toString(), name)
            } else {
                // User selected "Silent" or cancelled
                ringtoneCallback?.invoke(null, null)
            }
            ringtoneCallback = null
        }

        // Register custom file picker launcher
        customFilePickerLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            if (uri != null) {
                try {
                    // Copy file to internal storage to ensure persistent access
                    val fileName = getFileName(uri) ?: "ozel_ses_${System.currentTimeMillis()}"
                    val internalUri = copyFileToInternalStorage(uri, fileName)
                    
                    if (internalUri != null) {
                        ringtoneCallback?.invoke(internalUri.toString(), fileName)
                    } else {
                        // Fallback if copy fails
                        ringtoneCallback?.invoke(uri.toString(), fileName)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Fallback to original URI if copy fails
                    ringtoneCallback?.invoke(uri.toString(), "Özel Ses")
                }
            } else {
                ringtoneCallback?.invoke(null, null)
            }
            ringtoneCallback = null
        }

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }

        val repository = AlarmRepositoryImpl(this)
        val scheduler = AlarmSchedulerImpl(this)

        setContent {
            CompositionLocalProvider(
                LocalAlarmRepository provides repository,
                LocalAlarmScheduler provides scheduler
            ) {
                OverrideAlarmTheme {
                    val context = LocalContext.current
                    var showExactAlarmDialog by remember { mutableStateOf(false) }
                    var showBatteryDialog by remember { mutableStateOf(false) }
                    var showSoundPickerDialog by remember { mutableStateOf(false) }
                    var currentCallback by remember { mutableStateOf<((String?, String?) -> Unit)?>(null) }

                    LaunchedEffect(Unit) {
                        // Check SCHEDULE_EXACT_ALARM permission (Android 12+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val alarmMgr = getSystemService(ALARM_SERVICE) as AlarmManager
                            if (!alarmMgr.canScheduleExactAlarms()) {
                                showExactAlarmDialog = true
                            }
                        }
                        // Check battery optimization exemption
                        val pm = getSystemService(POWER_SERVICE) as PowerManager
                        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                            showBatteryDialog = true
                        }
                    }

                    if (showExactAlarmDialog) {
                        PermissionDialog(
                            title = "Kesin Alarm İzni Gerekli",
                            message = "Alarmın tam zamanında çalması için Android 12+ cihazlarda kesin alarm izni gereklidir. Ayarlara git ve bu uygulamaya izin ver.",
                            confirmText = "Ayarlara Git",
                            onConfirm = {
                                showExactAlarmDialog = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    startActivity(
                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                            data = Uri.parse("package:$packageName")
                                        }
                                    )
                                }
                            },
                            onDismiss = { showExactAlarmDialog = false }
                        )
                    }

                    if (showBatteryDialog && !showExactAlarmDialog) {
                        PermissionDialog(
                            title = "Pil Optimizasyonunu Kapat",
                            message = "Arka planda pil optimizasyonu alarmı engelleyebilir. Güvenilir alarmlar için bu uygulamayı optimizasyon dışında bırak.",
                            confirmText = "Ayarlara Git",
                            onConfirm = {
                                showBatteryDialog = false
                                try {
                                    startActivity(
                                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                            data = Uri.parse("package:$packageName")
                                        }
                                    )
                                } catch (e: Exception) {
                                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                }
                            },
                            onDismiss = { showBatteryDialog = false }
                        )
                    }

                    if (showSoundPickerDialog) {
                        SoundSourceDialog(
                            onSelectSystem = {
                                showSoundPickerDialog = false
                                ringtoneCallback = currentCallback
                                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_RINGTONE)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Alarm Sesi Seçin")
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    putExtra(
                                        RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                                    )
                                }
                                ringtonePickerLauncher.launch(intent)
                            },
                            onSelectFile = {
                                showSoundPickerDialog = false
                                ringtoneCallback = currentCallback
                                customFilePickerLauncher.launch(arrayOf("audio/*"))
                            },
                            onDismiss = {
                                showSoundPickerDialog = false
                                currentCallback = null
                            }
                        )
                    }

                    App(
                        onPickRingtone = { callback ->
                            currentCallback = callback
                            showSoundPickerDialog = true
                        }
                    )
                }
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    private fun copyFileToInternalStorage(uri: Uri, fileName: String): Uri? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val dir = File(filesDir, "custom_sounds")
            if (!dir.exists()) dir.mkdirs()
            val outputFile = File(dir, fileName)
            val outputStream = FileOutputStream(outputFile)
            
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 100
    }
}

@Composable
private fun SoundSourceDialog(
    onSelectSystem: () -> Unit,
    onSelectFile: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ses Kaynağı Seçin", fontWeight = FontWeight.Bold) },
        text = { Text("Alarm sesini nereden seçmek istersiniz?") },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSelectSystem,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Sistem Sesleri")
                }
                Button(
                    onClick = onSelectFile,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Dosyalarımdan Seç")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun PermissionDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Sonra")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}
