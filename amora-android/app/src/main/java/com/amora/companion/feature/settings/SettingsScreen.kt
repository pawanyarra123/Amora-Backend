package com.amora.companion.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amora.companion.core.theme.*
import com.amora.companion.navigation.AmoraScreen

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigate: (String) -> Unit = {}
) {
    val isMasterOn by viewModel.isMasterSwitchOn.collectAsState(initial = true)
    val backendUrl by viewModel.backendUrl.collectAsState()
    val themeName by viewModel.themePreset.collectAsState(initial = "Midnight Void")

    var editableUrl by remember(backendUrl) { mutableStateOf(backendUrl) }
    var showWipeConfirmDialog by remember { mutableStateOf(false) }
    var wipeConfirmed by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚙️", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("AMORA Master Settings", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Manage system switches, companion tools & security preferences", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                    }
                }
            }

            // ── 1. Master Companion Engine Card ────────────────────────────────────
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Master Companion Engine", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isMasterOn) "Active: Wake word & floating orb running" else "Disabled: All background services paused",
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 11.sp
                            )
                        }
                        CustomToggle(
                            checked = isMasterOn,
                            onCheckedChange = { viewModel.toggleMasterSwitch(it) }
                        )
                    }
                }
            }

            // ── 2. Backend Server Address Card ─────────────────────────────────────
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌐", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Backend Server Address", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Enter ngrok, Tailscale, or Railway URL", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editableUrl,
                        onValueChange = { editableUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FigmaViolet,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.updateBackendUrl(editableUrl) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FigmaGradientVioletCyan, RoundedCornerShape(12.dp))
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Save Backend Address", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // ── 3. Tools & Customization Section ──────────────────────────────────
            item {
                SectionLabel("Tools & Customization")
            }

            item {
                SettingsRowTile(
                    icon = "🎨",
                    title = "Themes & Appearance",
                    subtitle = "Active Theme: $themeName",
                    onClick = { onNavigate(AmoraScreen.Appearance.route) }
                )
            }
            item {
                SettingsRowTile(
                    icon = "🔐",
                    title = "Permissions Rationale & Setup",
                    subtitle = "Microphone, Camera, Overlay & Notification access status",
                    onClick = { onNavigate(AmoraScreen.Permissions.route) }
                )
            }
            item {
                SettingsRowTile(
                    icon = "🎙️",
                    title = "Voice Studio (Coqui XTTS-v2)",
                    subtitle = "Enroll 15s voice sample for custom voice cloning",
                    onClick = { onNavigate(AmoraScreen.VoiceStudio.route) }
                )
            }
            item {
                SettingsRowTile(
                    icon = "🧠",
                    title = "AI Engine & Wake Word",
                    subtitle = "Porcupine / Vosk strategy, Groq Llama 3 models",
                    onClick = { onNavigate(AmoraScreen.AISettings.route) }
                )
            }
            item {
                SettingsRowTile(
                    icon = "📱",
                    title = "Device Automation",
                    subtitle = "Brightness, volume sync, DND rules, accessibility fallback",
                    onClick = { onNavigate(AmoraScreen.DeviceControlSettings.route) }
                )
            }
            item {
                SettingsRowTile(
                    icon = "📄",
                    title = "Files & Documents Intelligence",
                    subtitle = "Summarize, translate, and analyze text reports",
                    onClick = { onNavigate(AmoraScreen.FilesDocuments.route) }
                )
            }

            // ── 4. Data Retention & Master Wipe ───────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FigmaRed.copy(alpha = 0.35f)),
                    colors = CardDefaults.cardColors(containerColor = FigmaRed.copy(alpha = 0.10f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🗑️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Data Retention & Master Wipe", color = FigmaRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Automatic TTL purges intruder photos (30d), summaries (90d), logs (7d).", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showWipeConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(FigmaGradientDanger, RoundedCornerShape(12.dp))
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Delete Everything Now", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        if (wipeConfirmed) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("✓ Master Wipe executed successfully! All local data cleared.", color = FigmaGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        var showWipeSuccessDialog by remember { mutableStateOf(false) }

        // Master Wipe Confirmation Dialog
        if (showWipeConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showWipeConfirmDialog = false },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.wipeAllData {
                                wipeConfirmed = true
                                showWipeConfirmDialog = false
                                showWipeSuccessDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FigmaRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Yes, Wipe All Data", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWipeConfirmDialog = false }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                },
                title = { Text("⚠️ Confirm Master Data Wipe", color = FigmaRed, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Are you sure you want to perform a Master Wipe? All saved call logs, custom shortcuts, training memory, and preferences will be permanently wiped.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                },
                containerColor = Color(0xFF1E293B),
                shape = RoundedCornerShape(24.dp)
            )
        }

        if (showWipeSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showWipeSuccessDialog = false },
                confirmButton = {
                    Button(
                        onClick = { showWipeSuccessDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = FigmaGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                title = { Text("✅ Master Wipe Complete", color = FigmaGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "All local logs, shortcuts, and cache have been completely wiped and reset to default.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                },
                containerColor = Color(0xFF1E293B),
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}