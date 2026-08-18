package com.amora.companion.feature.settings

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
import com.amora.companion.core.theme.*

@Composable
fun FilesDocumentsScreen(onBackClick: () -> Unit = {}) {
    var documentContent by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            BackButton(onBack = onBackClick)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Files & Documents Assistant", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Summarize, translate, explain, and organize content", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
        }

        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Text("Paste Document Text", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = documentContent,
                    onValueChange = { documentContent = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    placeholder = { Text("Paste article, report, or document text...", color = Color.White.copy(alpha = 0.3f), fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FigmaViolet,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (documentContent.isNotBlank()) {
                                resultText = "AI Summary: Key document concepts processed for \"${documentContent.take(30)}...\""
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF06B6D4))), RoundedCornerShape(12.dp))
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Summarize", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Button(
                        onClick = {
                            if (documentContent.isNotBlank()) {
                                resultText = "AI Explanation: Simple concept overview generated for \"${documentContent.take(30)}...\""
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FigmaGradientGreen, RoundedCornerShape(12.dp))
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Explain", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                if (resultText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(resultText, color = FigmaCyanText, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
            }
        }
    }
}
