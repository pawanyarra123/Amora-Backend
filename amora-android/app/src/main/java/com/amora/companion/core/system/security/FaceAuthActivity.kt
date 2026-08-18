package com.amora.companion.core.system.security

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amora.companion.core.data.local.db.dao.AmoraDao
import com.amora.companion.core.data.local.db.entities.IntruderLogEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FaceAuthActivity : ComponentActivity() {

    @Inject lateinit var amoraDao: AmoraDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var authState by remember { mutableStateOf("Scanning...") }
            var attempts by remember { mutableIntStateOf(0) }
            val scope = rememberCoroutineScope()

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF0F172A)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "AMORA Security Auth",
                        color = Color.White,
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = authState,
                        color = if (authState.contains("Granted")) Color(0xFF10B981) else Color(0xFF94A3B8),
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = {
                                authState = "Access Granted! Welcome."
                                finish()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                        ) {
                            Text("Simulate Match")
                        }

                        Button(
                            onClick = {
                                attempts++
                                authState = "Face not recognized ($attempts/3)"
                                if (attempts >= 3) {
                                    scope.launch {
                                        amoraDao.insertIntruderLog(
                                            IntruderLogEntity(
                                                photoPath = "/storage/emulated/0/Android/data/com.amora.companion/files/intruder_${System.currentTimeMillis()}.jpg",
                                                failureReason = "Multiple failed face auth attempts"
                                            )
                                        )
                                    }
                                    authState = "Locked. Fallback to password."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Text("Simulate Fail")
                        }
                    }
                }
            }
        }
    }
}
