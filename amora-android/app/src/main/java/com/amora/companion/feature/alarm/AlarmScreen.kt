package com.amora.companion.feature.alarm

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amora.companion.core.data.local.db.entities.AlarmEntity
import com.amora.companion.core.theme.*
import java.util.Calendar

@Composable
fun AlarmScreen(
    viewModel: AlarmViewModel = hiltViewModel(),
    themeName: String = "Cyberpunk Neon"
) {
    val currentPalette = remember(themeName) { AmoraThemeSystem.getPalette(themeName) }
    val alarms by viewModel.alarms.collectAsState()
    val context = LocalContext.current

    var selectedRingtoneTitle by remember { mutableStateOf("Default Alarm Tone") }
    var selectedRingtoneUri by remember { mutableStateOf("") }
    var showSetDialog by remember { mutableStateOf(false) }
    var alarmHour by remember { mutableIntStateOf(7) }
    var alarmMinute by remember { mutableIntStateOf(0) }
    var alarmLabel by remember { mutableStateOf("Wake Up") }

    // Dialog state management
    var alarmToDelete by remember { mutableStateOf<AlarmEntity?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteSuccessDialog by remember { mutableStateOf(false) }
    var showSaveSuccessDialog by remember { mutableStateOf(false) }
    var savedAlarmTimeString by remember { mutableStateOf("") }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri: Uri? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
        if (uri != null) {
            selectedRingtoneUri = uri.toString()
            val ringtone = RingtoneManager.getRingtone(context, uri)
            selectedRingtoneTitle = ringtone?.getTitle(context) ?: "Custom Sound"
        } else {
            selectedRingtoneTitle = "Silent / Default"
            selectedRingtoneUri = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Header ─────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(currentPalette.accentColor.copy(alpha = 0.2f))
                        .border(1.5.dp, currentPalette.accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⏰", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Neon Cyber Alarm", color = currentPalette.textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Voice-dismiss & custom ringtone system", color = currentPalette.subtextColor, fontSize = 11.sp)
                }
            }

            Button(
                onClick = {
                    showTimePicker(context) { h, m ->
                        alarmHour = h
                        alarmMinute = m
                        showSetDialog = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = currentPalette.accentColor),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Alarm", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // ── Voice Dismiss Glowing Info Card ─────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, currentPalette.accentColor.copy(alpha = 0.4f)),
            colors = CardDefaults.cardColors(containerColor = currentPalette.surfaceColor)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(currentPalette.accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎙️", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Voice-Dismiss Lock Active", color = currentPalette.textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Alarm will ring continuously until you say \"Amora stop\" or \"Amora turn off alarm\".", color = currentPalette.subtextColor, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
        }

        // ── Alarms List / Empty State ──────────────────────────────────────────
        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(currentPalette.surfaceColor)
                            .border(1.5.dp, currentPalette.accentColor.copy(alpha = 0.3f), CircleShape)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⏰", fontSize = 36.sp)
                    }
                    Text("No alarms scheduled", color = currentPalette.textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Tap + New Alarm to configure time\nand choose your custom ringtone",
                        color = currentPalette.subtextColor,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmItemCard(
                        alarm = alarm,
                        currentPalette = currentPalette,
                        onToggle = { viewModel.toggleAlarm(alarm, it) },
                        onDelete = {
                            alarmToDelete = alarm
                            showDeleteConfirmDialog = true
                        }
                    )
                }
            }
        }
    }

    // ── 1. Save Alarm Configuration Dialog ───────────────────────────────────
    if (showSetDialog) {
        val timeString = formatTime(alarmHour, alarmMinute)
        AlertDialog(
            onDismissRequest = { showSetDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addAlarm(alarmHour, alarmMinute, alarmLabel)
                        savedAlarmTimeString = timeString
                        showSetDialog = false
                        showSaveSuccessDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = currentPalette.accentColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Alarm", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetDialog = false }) {
                    Text("Cancel", color = currentPalette.subtextColor)
                }
            },
            title = { Text("Set Alarm: $timeString", color = currentPalette.textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = alarmLabel,
                        onValueChange = { alarmLabel = it },
                        label = { Text("Alarm Label", color = currentPalette.subtextColor) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = currentPalette.textColor,
                            unfocusedTextColor = currentPalette.textColor,
                            focusedBorderColor = currentPalette.accentColor,
                            unfocusedBorderColor = currentPalette.accentColor.copy(alpha = 0.4f),
                            focusedContainerColor = currentPalette.backgroundColor,
                            unfocusedContainerColor = currentPalette.backgroundColor
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("🎵 Alarm Ringtone:", color = currentPalette.textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Custom Alarm Sound")
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                    if (selectedRingtoneUri.isNotEmpty()) {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(selectedRingtoneUri))
                                    }
                                }
                                ringtonePickerLauncher.launch(intent)
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = currentPalette.backgroundColor),
                        border = BorderStroke(1.dp, currentPalette.accentColor.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🎵", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(selectedRingtoneTitle, color = currentPalette.textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Text("Change →", color = currentPalette.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            containerColor = currentPalette.surfaceColor,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ── 2. Save Success Dialog ────────────────────────────────────────────────
    if (showSaveSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSaveSuccessDialog = false },
            confirmButton = {
                Button(
                    onClick = { showSaveSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = FigmaGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            title = { Text("✅ Alarm Scheduled", color = FigmaGreen, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Your alarm has been scheduled successfully for $savedAlarmTimeString.",
                    color = currentPalette.textColor,
                    fontSize = 13.sp
                )
            },
            containerColor = currentPalette.surfaceColor,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ── 3. Delete Confirmation Dialog ─────────────────────────────────────────
    if (showDeleteConfirmDialog && alarmToDelete != null) {
        val target = alarmToDelete!!
        val timeString = formatTime(target.hour, target.minute)
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                alarmToDelete = null
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAlarm(target)
                        showDeleteConfirmDialog = false
                        alarmToDelete = null
                        showDeleteSuccessDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FigmaRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Yes, Delete Alarm", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    alarmToDelete = null
                }) {
                    Text("Cancel", color = currentPalette.subtextColor)
                }
            },
            title = { Text("Delete Alarm?", color = FigmaRed, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete the alarm scheduled for $timeString (${target.label})?",
                    color = currentPalette.textColor,
                    fontSize = 13.sp
                )
            },
            containerColor = currentPalette.surfaceColor,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ── 4. Delete Success Dialog ──────────────────────────────────────────────
    if (showDeleteSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSuccessDialog = false },
            confirmButton = {
                Button(
                    onClick = { showDeleteSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = FigmaGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            title = { Text("✅ Alarm Deleted", color = FigmaGreen, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "The alarm was deleted successfully.",
                    color = currentPalette.textColor,
                    fontSize = 13.sp
                )
            },
            containerColor = currentPalette.surfaceColor,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun AlarmItemCard(
    alarm: AlarmEntity,
    currentPalette: AmoraThemePalette,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (alarm.isEnabled) currentPalette.accentColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)),
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.isEnabled) currentPalette.surfaceColor else currentPalette.surfaceColor.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = formatTime(alarm.hour, alarm.minute),
                    color = if (alarm.isEnabled) currentPalette.textColor else currentPalette.subtextColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (alarm.label.isNotBlank()) alarm.label else "Alarm",
                    color = if (alarm.isEnabled) currentPalette.accentColor else currentPalette.subtextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = currentPalette.accentColor,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                    )
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete alarm",
                        tint = FigmaRed.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val h12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    return String.format("%02d:%02d %s", h12, minute, amPm)
}

private fun showTimePicker(context: Context, onTimeSet: (Int, Int) -> Unit) {
    val cal = Calendar.getInstance()
    TimePickerDialog(
        context,
        { _, hourOfDay, minute -> onTimeSet(hourOfDay, minute) },
        cal.get(Calendar.HOUR_OF_DAY),
        cal.get(Calendar.MINUTE),
        false
    ).show()
}
