package com.amora.companion.feature.alarm

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
    themeName: String = "Midnight Blue (Default Dark)"
) {
    val currentPalette = remember(themeName) { AmoraThemeSystem.getPalette(themeName) }
    val alarms by viewModel.alarms.collectAsState()
    val context = LocalContext.current

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
                Text("⏰", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Alarm", color = currentPalette.textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Say \"Amora turn off alarm\" to dismiss", color = currentPalette.subtextColor, fontSize = 11.sp)
                }
            }

            PulsingAddBtn(accentColor = currentPalette.accentColor) {
                showTimePicker(context) { hour, minute ->
                    viewModel.addAlarm(hour, minute)
                }
            }
        }

        // ── Voice Dismiss Info Card ─────────────────────────────────────────────
        VioletCard(
            title = "Voice Dismiss Only",
            description = "This alarm can only be stopped by saying \"Amora turn off alarm\" or \"Amora stop\"",
            icon = "🎙️"
        )

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
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(currentPalette.surfaceColor.copy(alpha = 0.6f))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⏰", fontSize = 36.sp)
                    }
                    Text("No alarms set", color = currentPalette.textColor.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Tap + to add an alarm\nOnly your voice can dismiss it",
                        color = currentPalette.subtextColor,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmItemCard(
                        alarm = alarm,
                        currentPalette = currentPalette,
                        onToggle = { viewModel.toggleAlarm(alarm, it) },
                        onDelete = { viewModel.deleteAlarm(alarm) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlarmItemCard(
    alarm: AlarmEntity,
    currentPalette: AmoraThemePalette,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val amPm = if (alarm.hour < 12) "AM" else "PM"
    val hour12 = when {
        alarm.hour == 0 -> 12
        alarm.hour > 12 -> alarm.hour - 12
        else -> alarm.hour
    }
    val time12 = String.format("%02d:%02d %s", hour12, alarm.minute, amPm)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        backgroundColor = currentPalette.surfaceColor.copy(alpha = 0.75f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⏰", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(time12, color = currentPalette.textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(alarm.label, color = currentPalette.subtextColor, fontSize = 11.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                CustomToggle(checked = alarm.isEnabled, onCheckedChange = onToggle)
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = currentPalette.subtextColor)
                }
            }
        }
    }
}

@Composable
private fun PulsingAddBtn(accentColor: Color, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(accentColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add Alarm", tint = Color.White)
    }
}

private fun showTimePicker(context: Context, onTimeSet: (hour: Int, minute: Int) -> Unit) {
    val cal = Calendar.getInstance()
    TimePickerDialog(
        context,
        { _, hour, minute -> onTimeSet(hour, minute) },
        cal.get(Calendar.HOUR_OF_DAY),
        cal.get(Calendar.MINUTE),
        false
    ).show()
}
