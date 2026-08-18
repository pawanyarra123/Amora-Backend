package com.amora.companion.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

data class NotificationGroup(
    val category: String,
    val appName: String,
    val summary: String,
    val urgency: String,
    val timeAgo: String
)

@Composable
fun NotificationCenterScreen(
    themeName: String = "Midnight Blue (Default Dark)"
) {
    val currentPalette = remember(themeName) { AmoraThemeSystem.getPalette(themeName) }
    val mockNotifications = remember {
        listOf(
            NotificationGroup("Messages", "WhatsApp", "3 unread messages from Alex: 'Are we still meeting at 5 PM?'", "High", "2m ago"),
            NotificationGroup("Security", "AMORA Guard", "Face Auth triggered: 1 authorized unlock on device.", "Medium", "15m ago"),
            NotificationGroup("System", "Battery Manager", "Battery health optimal: 85% capacity remaining.", "Low", "1h ago")
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔔", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("AI Notification Summaries", color = currentPalette.textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Smart grouping, filtering, and LLM text summarization", color = currentPalette.subtextColor, fontSize = 11.sp)
                }
            }
        }

        items(mockNotifications) { notif ->
            val tagColor = when (notif.category.lowercase()) {
                "messages" -> FigmaGreen
                "security" -> FigmaRed
                "system" -> FigmaAmber
                else -> currentPalette.accentColor
            }

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp,
                backgroundColor = currentPalette.surfaceColor.copy(alpha = 0.75f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(notif.appName, color = currentPalette.textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(tagColor.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = notif.category,
                                color = tagColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(notif.timeAgo, color = currentPalette.subtextColor, fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(notif.summary, color = currentPalette.textColor.copy(alpha = 0.75f), fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}
