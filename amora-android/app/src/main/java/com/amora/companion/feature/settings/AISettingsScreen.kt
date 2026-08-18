package com.amora.companion.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
fun AISettingsScreen(onBackClick: () -> Unit = {}) {
    var engineChoice by remember { mutableStateOf("Porcupine (Standard)") }
    var sensitivity by remember { mutableStateOf(0.7f) }

    val engines = remember { listOf("Porcupine (Standard)", "Vosk (Offline Open-Source)") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            BackButton(onBack = onBackClick)
            Spacer(modifier = Modifier.height(4.dp))
            Text("AI & Wake Word Settings", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Configure in-memory keyword spotter & response preferences", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
        }

        // Engine choice card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Text("Wake-Word Engine Strategy", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                engines.forEachIndexed { index, engine ->
                    val isSelected = engineChoice == engine
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { engineChoice = engine }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = engine,
                            color = if (isSelected) FigmaPurpleText else Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )

                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) FigmaGradientVioletCyan else androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                                .then(if (!isSelected) Modifier.background(Color.Transparent, CircleShape) else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }

                    if (index < engines.size - 1) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                    }
                }
            }
        }

        // Sensitivity slider card
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
                    Text("Detection Sensitivity", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format("%.1f", sensitivity),
                        color = FigmaPurpleText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Slider(
                    value = sensitivity,
                    onValueChange = { sensitivity = it },
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = FigmaViolet,
                        activeTrackColor = FigmaViolet,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0.0", color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp)
                    Text("1.0", color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp)
                }
            }
        }
    }
}
