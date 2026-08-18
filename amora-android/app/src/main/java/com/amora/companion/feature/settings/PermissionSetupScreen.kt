package com.amora.companion.feature.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.amora.companion.core.system.permissions.PermissionManager
import com.amora.companion.core.theme.*

@Composable
fun PermissionSetupScreen(
    permissionManager: PermissionManager,
    onBackClick: () -> Unit = {},
    themeName: String = "Midnight Blue (Default Dark)"
) {
    val currentPalette = remember(themeName) { AmoraThemeSystem.getPalette(themeName) }
    var status by remember { mutableStateOf(permissionManager.checkPermissions()) }

    val runtimePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        status = permissionManager.checkPermissions()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            BackButton(onBack = onBackClick)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Permissions Rationale", color = currentPalette.textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("AMORA requires standard & special system permissions to function as your AI companion.", color = currentPalette.subtextColor, fontSize = 12.sp)
        }

        item {
            PermissionCardTile(
                title = "Microphone & Camera",
                description = "Microphone is used for in-memory wake word detection ('Hey Amora'). Camera is used for Face Auth security unlock.",
                isGranted = status.hasAudioPermission && status.hasCameraPermission,
                onRequestClick = {
                    runtimePermissionLauncher.launch(
                        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
                    )
                },
                currentPalette = currentPalette
            )
        }

        item {
            PermissionCardTile(
                title = "Display Over Other Apps (Floating Orb)",
                description = "Required to render the lightweight, draggable floating companion orb above other applications.",
                isGranted = status.hasOverlayPermission,
                onRequestClick = { permissionManager.openOverlaySettings() },
                currentPalette = currentPalette
            )
        }

        item {
            PermissionCardTile(
                title = "Notification Access",
                description = "Allows AMORA to group, filter, and summarize incoming notifications for the Notification Center.",
                isGranted = status.hasNotificationListenerPermission,
                onRequestClick = { permissionManager.openNotificationListenerSettings() },
                currentPalette = currentPalette
            )
        }

        item {
            PermissionCardTile(
                title = "Accessibility Service (Optional)",
                description = "Optional permission to perform deep custom UI actions when specifically requested by the user.",
                isGranted = status.hasAccessibilityPermission,
                onRequestClick = { permissionManager.openAccessibilitySettings() },
                currentPalette = currentPalette
            )
        }
    }
}

@Composable
fun PermissionCardTile(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequestClick: () -> Unit,
    currentPalette: AmoraThemePalette
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        backgroundColor = currentPalette.surfaceColor.copy(alpha = 0.8f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = currentPalette.textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, color = currentPalette.subtextColor, fontSize = 11.sp, lineHeight = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isGranted) "Status: Granted ✓" else "Status: Not Granted",
                    color = if (isGranted) FigmaGreen else FigmaRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRequestClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGranted) currentPalette.subtextColor.copy(alpha = 0.2f) else currentPalette.accentColor
                )
            ) {
                Text(if (isGranted) "Manage" else "Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
