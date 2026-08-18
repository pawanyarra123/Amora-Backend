package com.amora.companion.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val selectedLang by viewModel.selectedLanguage.collectAsState(initial = "en")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                )
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.8f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👤", fontSize = 28.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("User Profile", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("AMORA Companion Account", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.8f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("🌐 Companion Primary Language", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            modifier = Modifier.clickable { viewModel.selectLanguage("en") },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedLang == "en") Color(0xFF3B82F6) else Color(0xFF334155)
                        ) {
                            Text(
                                "English",
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            modifier = Modifier.clickable { viewModel.selectLanguage("te") },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedLang == "te") Color(0xFF3B82F6) else Color(0xFF334155)
                        ) {
                            Text(
                                "Telugu (తెలుగు)",
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
