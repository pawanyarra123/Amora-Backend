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
import kotlinx.coroutines.launch

@Composable
fun BackendConfigScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit = {},
    themeName: String = "Cyberpunk Neon"
) {
    val currentPalette = remember(themeName) { AmoraThemeSystem.getPalette(themeName) }
    val savedBackendUrl by viewModel.backendUrl.collectAsState()

    var backendAddress by remember(savedBackendUrl) { mutableStateOf(savedBackendUrl) }
    var connectionResult by remember { mutableStateOf("") }
    var connectionIsError by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var showConfirmSaveDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            BackButton(onBack = onBackClick)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Backend Address Setup", color = currentPalette.textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Configure Stage 1 (Local / Tailscale / ngrok) or Stage 2 (Railway) endpoint", color = currentPalette.subtextColor, fontSize = 12.sp)
        }

        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                backgroundColor = currentPalette.surfaceColor,
                borderColor = currentPalette.accentColor.copy(alpha = 0.25f)
            ) {
                Text("Configurable Server Endpoint", color = currentPalette.textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = backendAddress,
                    onValueChange = { backendAddress = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("https://amora-backend-production-74b4.up.railway.app", color = currentPalette.subtextColor.copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = currentPalette.accentColor,
                        unfocusedBorderColor = currentPalette.accentColor.copy(alpha = 0.3f),
                        focusedTextColor = currentPalette.textColor,
                        unfocusedTextColor = currentPalette.textColor,
                        focusedContainerColor = currentPalette.backgroundColor,
                        unfocusedContainerColor = currentPalette.backgroundColor,
                        cursorColor = currentPalette.accentColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        showConfirmSaveDialog = true
                    },
                    enabled = !isTesting && backendAddress.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = currentPalette.accentColor)
                ) {
                    Text(
                        if (isTesting) "Testing Connection..." else "Save Backend Address",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                if (connectionResult.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                (if (connectionIsError) FigmaRed else FigmaGreen).copy(alpha = 0.15f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Text(
                            connectionResult,
                            color = if (connectionIsError) Color(0xFFFF6B6B) else FigmaGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // ── 1. Confirm Change Dialog ──────────────────────────────────────────────
    if (showConfirmSaveDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmSaveDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmSaveDialog = false
                        viewModel.updateBackendUrl(backendAddress)
                        showSuccessDialog = true

                        // Test connection in background
                        isTesting = true
                        connectionResult = ""
                        scope.launch {
                            val (ok, message) = viewModel.testBackendConnection()
                            connectionIsError = !ok
                            connectionResult = message
                            isTesting = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = currentPalette.accentColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Yes, Change Address", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmSaveDialog = false }) {
                    Text("Cancel", color = currentPalette.subtextColor)
                }
            },
            title = { Text("Change Backend Address?", color = currentPalette.textColor, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Do you want to change the backend server address to:",
                        color = currentPalette.subtextColor,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        backendAddress,
                        color = currentPalette.accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            containerColor = currentPalette.surfaceColor,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ── 2. Success Confirmation Dialog ────────────────────────────────────────
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            confirmButton = {
                Button(
                    onClick = { showSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = FigmaGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            title = { Text("✅ Changed Successfully", color = FigmaGreen, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "The backend address was changed successfully to $backendAddress! Now testing connection...",
                    color = currentPalette.textColor,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            },
            containerColor = currentPalette.surfaceColor,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
