package com.amora.companion.feature.emergency

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmergencyScreen() {
    var isEmergencyTriggered by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF450A0A), Color(0xFF0F172A))
                )
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text("🆘 Emergency SOS Panic Alert", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Broadcast immediate distress location to emergency contacts", color = Color(0xFFFCA5A5), fontSize = 14.sp)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D).copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Hold to Trigger Panic Alert", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { isEmergencyTriggered = true },
                        modifier = Modifier.size(120.dp),
                        shape = RoundedCornerShape(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text("SOS", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    if (isEmergencyTriggered) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "🚨 SOS Distress signal & GPS coordinates broadcasted!",
                            color = Color(0xFFF87171),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
