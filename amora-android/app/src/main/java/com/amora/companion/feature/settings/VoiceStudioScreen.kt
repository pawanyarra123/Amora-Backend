package com.amora.companion.feature.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import kotlinx.coroutines.delay

@Composable
fun VoiceStudioScreen(
    onBackClick: () -> Unit = {},
    themeName: String = "Midnight Blue (Default Dark)"
) {
    val currentPalette = remember(themeName) { AmoraThemeSystem.getPalette(themeName) }
    var recording by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(recording) {
        if (recording) {
            progress = 0f
            while (progress < 100f && recording) {
                delay(100)
                progress += (100f / 150f)
            }
            if (progress >= 100f) {
                recording = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            BackButton(onBack = onBackClick)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Voice Studio", color = currentPalette.textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Enroll & configure cloned user voice profile", color = currentPalette.subtextColor, fontSize = 12.sp)
        }

        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                backgroundColor = currentPalette.surfaceColor.copy(alpha = 0.8f)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Voice Sample Enrollment", color = currentPalette.textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Record a 15-second sample speaking naturally to generate your custom cloned voice profile.",
                        color = currentPalette.subtextColor,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Orb
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                if (recording) FigmaRed.copy(alpha = 0.15f) else currentPalette.accentColor.copy(alpha = 0.15f)
                            )
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (recording) "🔴" else "🎙️", fontSize = 32.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (recording || progress >= 100f) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    if (recording) "Recording..." else "Complete!",
                                    color = currentPalette.subtextColor,
                                    fontSize = 10.sp
                                )
                                Text(
                                    "${progress.toInt()}%",
                                    color = currentPalette.accentColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { progress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = if (progress >= 100f) FigmaGreen else currentPalette.accentColor,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Button(
                        onClick = { recording = !recording },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (recording) FigmaRed else currentPalette.accentColor
                        )
                    ) {
                        Text(
                            if (recording) "⏹ Stop Recording" else "▶ Start Voice Recording",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
