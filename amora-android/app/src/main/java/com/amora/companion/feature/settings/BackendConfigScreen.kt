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
    themeName: String = "Midnight Blue (Default Dark)"
) {
    val currentPalette = remember(themeName) { AmoraThemeSystem.getPalette(themeName) }
    val savedBackendUrl by viewModel.backendUrl.collectAsState()

    // Seed the editable field from the real saved value instead of a hardcoded
    // emulator-only default, and keep it in sync if it changes elsewhere.
    var backendAddress by remember(savedBackendUrl) { mutableStateOf(savedBackendUrl) }
    var connectionResult by remember { mutableStateOf("") }
    var connectionIsError by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
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
                backgroundColor = currentPalette.surfaceColor.copy(alpha = 0.8f)
            ) {
                Text("Configurable Server Endpoint", color = currentPalette.textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = backendAddress,
                    onValueChange = { backendAddress = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("http://<your-pc-lan-ip>:8000", color = currentPalette.subtextColor.copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = currentPalette.accentColor,
                        unfocusedBorderColor = currentPalette.subtextColor.copy(alpha = 0.3f),
                        focusedTextColor = currentPalette.textColor,
                        unfocusedTextColor = currentPalette.textColor,
                        cursorColor = currentPalette.accentColor
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        // Persist first — this used to only live in local Compose state and
                        // was silently lost on navigating away, so the app kept using the
                        // emulator-only default even after "saving" a real address here.
                        viewModel.updateBackendUrl(backendAddress)

                        isTesting = true
                        connectionResult = ""
                        scope.launch {
                            val (ok, message) = viewModel.testBackendConnection()
                            connectionIsError = !ok
                            connectionResult = message
                            isTesting = false
                        }
                    },
                    enabled = !isTesting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = currentPalette.accentColor)
                ) {
                    Text(
                        if (isTesting) "Testing..." else "Save & Test Connection",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                if (connectionResult.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
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
