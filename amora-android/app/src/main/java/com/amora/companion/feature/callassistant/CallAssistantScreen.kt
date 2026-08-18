package com.amora.companion.feature.callassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amora.companion.core.theme.*

@Composable
fun CallAssistantScreen(
    viewModel: CallAssistantViewModel,
    themeName: String = "Midnight Void"
) {
    val currentPalette = remember(themeName) { AmoraThemeSystem.getPalette(themeName) }
    val isEnabled by viewModel.isEnabled.collectAsState(initial = true)
    val activeMode by viewModel.activeMode.collectAsState(initial = "Meeting")
    val logs by viewModel.logs.collectAsState()
    val emergencyAlert by viewModel.emergencyAlert.collectAsState()

    // Cleaned modes: ONLY Meeting, College, Sleeping
    val modes = remember { listOf("Meeting", "College", "Sleeping") }

    // Dialog state for testing call screening
    var showSimulateDialog by remember { mutableStateOf(false) }
    var simCaller by remember { mutableStateOf("Alex") }
    var simIsEmergency by remember { mutableStateOf(false) }
    var simReason by remember { mutableStateOf("Need to confirm tomorrow's presentation project.") }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📞", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Call Assistant & Screening", color = currentPalette.textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Automated voice screening & emergency escalation", color = currentPalette.subtextColor, fontSize = 11.sp)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(currentPalette.accentColor.copy(alpha = 0.15f))
                            .border(1.dp, currentPalette.accentColor.copy(alpha = 0.35f), CircleShape)
                            .clickable { showSimulateDialog = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("⚡ Test Call Flow", color = currentPalette.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Call screening setup & default role card
            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 18.dp,
                    backgroundColor = currentPalette.accentColor.copy(alpha = 0.12f)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🛡️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Android OS Call Protection", color = currentPalette.textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Set AMORA as Default Call Screening App so real carrier calls are intercepted", color = currentPalette.subtextColor, fontSize = 10.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                    try {
                                        val roleManager = context.getSystemService(android.content.Context.ROLE_SERVICE) as android.app.role.RoleManager
                                        if (roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_CALL_SCREENING)) {
                                            val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_CALL_SCREENING)
                                            if (context is android.app.Activity) {
                                                context.startActivityForResult(intent, 2002)
                                            } else {
                                                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(intent)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = currentPalette.accentColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Set AMORA as Default Call Handler", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Call screening toggle
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 18.dp,
                    backgroundColor = currentPalette.surfaceColor.copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable Call Screening", color = currentPalette.textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("AMORA answers calls & asks for caller intent", color = currentPalette.subtextColor, fontSize = 10.sp)
                        }
                        CustomToggle(
                            checked = isEnabled,
                            onCheckedChange = { viewModel.toggleEnabled(it) }
                        )
                    }
                }
            }

            // Cleaned Mode Selector (Meeting, College, Sleeping)
            item {
                SectionLabel("Active Companion Mode")
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    modes.forEach { m ->
                        val modeIcon = when (m) {
                            "Meeting" -> "💼"
                            "College" -> "🎓"
                            "Sleeping" -> "😴"
                            else -> "💼"
                        }
                        ModePillTile(
                            mode = "$modeIcon $m",
                            isSelected = activeMode == m,
                            currentPalette = currentPalette,
                            onClick = { viewModel.selectMode(m) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Mode Rationale Card
            item {
                val modeDesc = when (activeMode) {
                    "Meeting" -> "AMORA tells caller: \"User is busy in a meeting.\" Non-important reasons are logged to Home Dashboard."
                    "College" -> "AMORA tells caller: \"User is in college class.\" Emergency calls trigger continuous vibration & full-screen alert."
                    "Sleeping" -> "AMORA tells caller: \"User is sleeping.\" Emergency calls boost volume to 100% and play continuous sound."
                    else -> "AMORA screens incoming callers automatically."
                }
                VioletCard(
                    title = "Mode Active: $activeMode",
                    description = modeDesc,
                    icon = "🎙️"
                )
            }

            // Screening History Header
            item {
                SectionLabel("Screened Calls & Caller Messages")
            }

            // Screened Call Logs List
            if (logs.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        backgroundColor = currentPalette.surfaceColor.copy(alpha = 0.5f)
                    ) {
                        Text("No screened call logs yet", color = currentPalette.subtextColor, fontSize = 12.sp)
                    }
                }
            } else {
                items(logs, key = { it.id }) { log ->
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
                                Text(if (log.isEmergency) "🚨" else "📞", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(log.callerName, color = currentPalette.textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("Mode: ${log.mode} • ${log.timeAgo}", color = currentPalette.subtextColor, fontSize = 10.sp)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (log.isEmergency) FigmaRed.copy(alpha = 0.15f) else FigmaGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (log.isEmergency) "EMERGENCY" else "SAVED REASON",
                                    color = if (log.isEmergency) FigmaRed else FigmaGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "\"${log.reason}\"",
                            color = currentPalette.textColor.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        // Full Screen Emergency Alert Modal (College / Sleeping Mode Trigger)
        emergencyAlert?.let { alert ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissEmergencyAlert() },
                confirmButton = {
                    Button(
                        onClick = { viewModel.dismissEmergencyAlert() },
                        colors = ButtonDefaults.buttonColors(containerColor = FigmaRed)
                    ) {
                        Text("Acknowledge & Lift Call", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🚨", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("IMPORTANT EMERGENCY CALL", color = FigmaRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("${alert.callerName} says this call is URGENT / IMPORTANT!", color = currentPalette.textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Caller Message: \"${alert.reason}\"", color = currentPalette.subtextColor, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (alert.mode == "Sleeping") "🔊 Volume boosted to MAX. Continuous alarm playing..." else "📳 Continuous vibration & full-screen alert active...",
                            color = FigmaAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                containerColor = currentPalette.surfaceColor
            )
        }
    }

    // Call Simulator Dialog
    if (showSimulateDialog) {
        AlertDialog(
            onDismissRequest = { showSimulateDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.simulateCall(simCaller, activeMode, simIsEmergency, simReason)
                        showSimulateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = currentPalette.accentColor)
                ) {
                    Text("Simulate Call", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSimulateDialog = false }) {
                    Text("Cancel", color = currentPalette.subtextColor)
                }
            },
            title = { Text("⚡ Test Call Screening Flow", color = currentPalette.textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = simCaller,
                        onValueChange = { simCaller = it },
                        label = { Text("Caller Name (e.g. Mom, Boss)", color = currentPalette.subtextColor) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = currentPalette.textColor,
                            unfocusedTextColor = currentPalette.textColor,
                            focusedBorderColor = currentPalette.accentColor,
                            unfocusedBorderColor = currentPalette.subtextColor.copy(alpha = 0.4f),
                            cursorColor = currentPalette.accentColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Is this an Emergency / Important call?", color = currentPalette.textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        CustomToggle(checked = simIsEmergency, onCheckedChange = { simIsEmergency = it })
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = simReason,
                        onValueChange = { simReason = it },
                        label = { Text("Caller Reason / Message", color = currentPalette.subtextColor) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = currentPalette.textColor,
                            unfocusedTextColor = currentPalette.textColor,
                            focusedBorderColor = currentPalette.accentColor,
                            unfocusedBorderColor = currentPalette.subtextColor.copy(alpha = 0.4f),
                            cursorColor = currentPalette.accentColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            containerColor = currentPalette.surfaceColor
        )
    }
}

@Composable
private fun ModePillTile(
    mode: String,
    isSelected: Boolean,
    currentPalette: AmoraThemePalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgModifier = if (isSelected) {
        Modifier.background(currentPalette.accentColor, CircleShape)
    } else {
        Modifier
            .background(currentPalette.surfaceColor.copy(alpha = 0.5f), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .then(bgModifier)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = mode,
            color = if (isSelected) Color.White else currentPalette.subtextColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
