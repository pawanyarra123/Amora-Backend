package com.amora.companion.feature.messaging

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun MessageCorrectionDialog(
    recipient: String,
    originalText: String,
    correctedText: String,
    onConfirmSend: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Confirm Message Grammar", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Recipient: $recipient", color = Color(0xFF60A5FA), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Text("Original Draft:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Text(originalText, color = Color(0xFFCBD5E1), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Text("AI Polished Version:", color = Color(0xFF34D399), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(correctedText, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirmSend(correctedText) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Confirm & Send")
                    }
                }
            }
        }
    }
}
