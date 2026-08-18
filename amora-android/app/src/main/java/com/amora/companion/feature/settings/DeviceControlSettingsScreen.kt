package com.amora.companion.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amora.companion.core.theme.*

@Composable
fun DeviceControlSettingsScreen(onBackClick: () -> Unit = {}) {
    var accessibilityEnabled by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            BackButton(onBack = onBackClick)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Device Control Settings", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Permissions & standard Android automation actions", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
        }

        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Accessibility Service Fallback", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Standard Android Intents/App Actions are used by default. Enable Accessibility only for deep custom actions.",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 11.sp,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Accessibility Automation", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    CustomToggle(
                        checked = accessibilityEnabled,
                        onCheckedChange = { accessibilityEnabled = it }
                    )
                }
            }
        }
    }
}
