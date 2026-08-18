package com.amora.companion.feature.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amora.companion.core.theme.*
import com.amora.companion.navigation.AmoraScreen
import java.util.Calendar

data class AppShortcut(
    val name: String,
    val icon: String,
    val urlOrPackage: String,
    val isWebShortcut: Boolean = true
)

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigate: (String) -> Unit = {},
    themeName: String = "Midnight Void"
) {
    val state by viewModel.uiState.collectAsState()
    val currentPalette = remember(themeName) { AmoraThemeSystem.getPalette(themeName) }
    val context = LocalContext.current

    val pinnedShortcuts = remember { mutableStateListOf<AppShortcut>() }

    LaunchedEffect(state.shortcutsLoaded, state.persistentShortcuts) {
        if (state.shortcutsLoaded) {
            pinnedShortcuts.clear()
            pinnedShortcuts.addAll(state.persistentShortcuts)
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var newAppName by remember { mutableStateOf("") }
    var newAppUrl by remember { mutableStateOf("") }

    var shortcutToDelete by remember { mutableStateOf<AppShortcut?>(null) }
    var showDeleteShortcutConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteShortcutSuccessDialog by remember { mutableStateOf(false) }
    var deletedShortcutName by remember { mutableStateOf("") }
    var showAddShortcutSuccessDialog by remember { mutableStateOf(false) }
    var addedShortcutName by remember { mutableStateOf("") }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greetingText = when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── 1. Greeting & Header Glass Card ────────────────────────────────────
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
            backgroundColor = currentPalette.surfaceColor.copy(alpha = 0.85f),
            borderColor = currentPalette.accentColor.copy(alpha = 0.25f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = greetingText,
                        color = currentPalette.textColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "AMORA Companion Engine",
                        color = currentPalette.accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (state.masterSwitchOn) FigmaGreen else FigmaRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.masterSwitchOn) "🎙️ Listening for 'Amora'" else "🎙️ Mic Gate Inactive",
                            color = if (state.masterSwitchOn) FigmaGreen else FigmaRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isConnected = state.backendStatus == "Connected"
                    val statusBg = if (isConnected) FigmaGreen.copy(alpha = 0.12f) else FigmaRed.copy(alpha = 0.12f)
                    val statusBorder = if (isConnected) FigmaGreen.copy(alpha = 0.28f) else FigmaRed.copy(alpha = 0.28f)
                    val statusColor = if (isConnected) FigmaGreen else FigmaRed

                    Box(
                        modifier = Modifier
                            .background(statusBg, CircleShape)
                            .border(1.dp, statusBorder, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isConnected) "Connected" else "Offline",
                                color = statusColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(currentPalette.surfaceColor)
                            .border(1.dp, currentPalette.accentColor.copy(alpha = 0.3f), CircleShape)
                            .clickable { onNavigate(AmoraScreen.Settings.route) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚙️", fontSize = 15.sp)
                    }
                }
            }
        }

        // ── Voice Input & Mic Diagnostic Card ─────────────────────────────────
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
            backgroundColor = currentPalette.surfaceColor.copy(alpha = 0.88f),
            borderColor = if (state.isTestingVoice) FigmaCyan else currentPalette.accentColor.copy(alpha = 0.35f)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎙️ Live Voice Diagnostic",
                        color = currentPalette.textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .background(if (state.isTestingVoice) FigmaGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .border(1.dp, if (state.isTestingVoice) FigmaGreen else Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (state.isTestingVoice) "RECORDING ACTIVE" else "TAP TO TEST",
                            color = if (state.isTestingVoice) FigmaGreen else currentPalette.subtextColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = state.voiceDiagnosticStatus,
                    color = if (state.voiceDiagnosticStatus.startsWith("❌")) FigmaRed else if (state.voiceDiagnosticStatus.startsWith("✅")) FigmaGreen else currentPalette.subtextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                if (state.lastRecognizedCommand.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Captured: \"${state.lastRecognizedCommand}\"",
                        color = FigmaCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (state.isTestingVoice) {
                    LinearProgressIndicator(
                        progress = { state.micRmsLevel },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = FigmaCyan,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Button(
                    onClick = {
                        if (state.isTestingVoice) {
                            viewModel.stopVoiceDiagnosticTest()
                        } else {
                            viewModel.startVoiceDiagnosticTest()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isTestingVoice) FigmaRed else currentPalette.accentColor
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (state.isTestingVoice) "⏹️ Stop Mic Test" else "🎙️ Test Microphone & Voice Input",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // ── Missed / Screened Call Notification Banner ─────────────────────────
        if (state.latestScreenedCall.isNotEmpty()) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(AmoraScreen.CallAssistant.route) },
                cornerRadius = 18.dp,
                backgroundColor = currentPalette.accentColor.copy(alpha = 0.15f),
                borderColor = currentPalette.accentColor.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text("🔔", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = state.latestScreenedCall,
                            color = currentPalette.textColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text("View →", color = currentPalette.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── 2. Phone Performance Details Glass Card ───────────────────────────
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
            backgroundColor = currentPalette.surfaceColor.copy(alpha = 0.75f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📊", fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PHONE PERFORMANCE DETAILS",
                    color = currentPalette.subtextColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                CircularPerformanceMetric(
                    label = "Battery",
                    value = "${state.batteryPercent}%",
                    color = FigmaGreen
                )
                CircularPerformanceMetric(
                    label = "RAM",
                    value = if (state.ramUsageGb.isNotEmpty()) state.ramUsageGb else "4.0 / 5.5 GB",
                    color = FigmaCyan
                )
                CircularPerformanceMetric(
                    label = "Storage",
                    value = if (state.storageUsageGb.isNotEmpty()) state.storageUsageGb else "83 / 101 GB",
                    color = currentPalette.accentColor
                )
            }
        }

        // ── 3. App Launcher & Web Shortcuts Space ─────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🚀", fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "App Launcher & Web Shortcuts Space",
                    color = currentPalette.textColor.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(currentPalette.accentColor.copy(alpha = 0.15f))
                    .border(1.dp, currentPalette.accentColor.copy(alpha = 0.35f), CircleShape)
                    .clickable { showAddDialog = true }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "+ Add Shortcut",
                    color = currentPalette.accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(pinnedShortcuts, key = { it.name + it.urlOrPackage }) { shortcut ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 18.dp,
                        backgroundColor = currentPalette.surfaceColor.copy(alpha = 0.7f),
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(shortcut.urlOrPackage))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(shortcut.icon, fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                shortcut.name,
                                color = currentPalette.textColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Delete button in top corner (with confirmation)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(FigmaRed)
                            .clickable {
                                shortcutToDelete = shortcut
                                showDeleteShortcutConfirmDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "×",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Add Shortcut Dialog with matching theme input text & high contrast surface
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAppName.isNotEmpty() && newAppUrl.isNotEmpty()) {
                            var formattedUrl = newAppUrl.trim()
                            if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                                formattedUrl = "https://$formattedUrl"
                            }
                            val newSc = AppShortcut(newAppName, "📌", formattedUrl)
                            pinnedShortcuts.add(newSc)
                            viewModel.saveShortcuts(pinnedShortcuts)
                            addedShortcutName = newAppName
                            newAppName = ""
                            newAppUrl = ""
                            showAddDialog = false
                            showAddShortcutSuccessDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = currentPalette.accentColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add Shortcut", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = currentPalette.subtextColor)
                }
            },
            title = { Text("Add Web / App Shortcut", color = currentPalette.textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newAppName,
                        onValueChange = { newAppName = it },
                        label = { Text("App / Website Name", color = currentPalette.subtextColor) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = currentPalette.textColor,
                            unfocusedTextColor = currentPalette.textColor,
                            focusedBorderColor = currentPalette.accentColor,
                            unfocusedBorderColor = currentPalette.accentColor.copy(alpha = 0.4f),
                            cursorColor = currentPalette.accentColor,
                            focusedContainerColor = currentPalette.backgroundColor,
                            unfocusedContainerColor = currentPalette.backgroundColor
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newAppUrl,
                        onValueChange = { newAppUrl = it },
                        label = { Text("URL (e.g. google.com)", color = currentPalette.subtextColor) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = currentPalette.textColor,
                            unfocusedTextColor = currentPalette.textColor,
                            focusedBorderColor = currentPalette.accentColor,
                            unfocusedBorderColor = currentPalette.accentColor.copy(alpha = 0.4f),
                            cursorColor = currentPalette.accentColor,
                            focusedContainerColor = currentPalette.backgroundColor,
                            unfocusedContainerColor = currentPalette.backgroundColor
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            containerColor = currentPalette.surfaceColor,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ── Delete Shortcut Confirmation Dialog ──────────────────────────────────
    if (showDeleteShortcutConfirmDialog && shortcutToDelete != null) {
        val target = shortcutToDelete!!
        AlertDialog(
            onDismissRequest = {
                showDeleteShortcutConfirmDialog = false
                shortcutToDelete = null
            },
            confirmButton = {
                Button(
                    onClick = {
                        pinnedShortcuts.remove(target)
                        viewModel.saveShortcuts(pinnedShortcuts)
                        deletedShortcutName = target.name
                        showDeleteShortcutConfirmDialog = false
                        shortcutToDelete = null
                        showDeleteShortcutSuccessDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FigmaRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Yes, Delete Shortcut", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteShortcutConfirmDialog = false
                    shortcutToDelete = null
                }) {
                    Text("Cancel", color = currentPalette.subtextColor)
                }
            },
            title = { Text("Delete Shortcut?", color = FigmaRed, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to remove '${target.name}' from your app launcher shortcuts?",
                    color = currentPalette.textColor,
                    fontSize = 13.sp
                )
            },
            containerColor = currentPalette.surfaceColor,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ── Delete Shortcut Success Dialog ───────────────────────────────────────
    if (showDeleteShortcutSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteShortcutSuccessDialog = false },
            confirmButton = {
                Button(
                    onClick = { showDeleteShortcutSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = FigmaGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            title = { Text("✅ Shortcut Deleted", color = FigmaGreen, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "'$deletedShortcutName' was deleted successfully from your shortcuts.",
                    color = currentPalette.textColor,
                    fontSize = 13.sp
                )
            },
            containerColor = currentPalette.surfaceColor,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ── Add Shortcut Success Dialog ──────────────────────────────────────────
    if (showAddShortcutSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showAddShortcutSuccessDialog = false },
            confirmButton = {
                Button(
                    onClick = { showAddShortcutSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = FigmaGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            title = { Text("✅ Shortcut Added", color = FigmaGreen, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "'$addedShortcutName' was added successfully to your shortcuts space.",
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
fun CircularPerformanceMetric(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f))
                .border(1.5.dp, color.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value.split(" ")[0],
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}
