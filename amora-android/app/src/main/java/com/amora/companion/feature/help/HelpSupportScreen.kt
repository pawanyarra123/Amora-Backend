package com.amora.companion.feature.help

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HelpSupportScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Help & Support", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Guides, FAQs, and Privacy Information", color = Color(0xFF94A3B8), fontSize = 14.sp)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Getting Started Guide", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("1. Say 'Hey Amora' to activate in-memory wake word detection.\n2. The floating orb animates through listening -> thinking -> speaking.\n3. Configure your local/Tailscale backend URL in Settings.", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Privacy & Retention Policy", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• No audio is written to disk or sent over network before wake phrase detection.\n• Intruder photos auto-purged after 30 days.\n• Call summaries auto-purged after 90 days.\n• Logs auto-purged after 7 days.", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                }
            }
        }
    }
}
