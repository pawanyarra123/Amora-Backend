package com.amora.companion.feature.pcspeaker

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface

// ── Colour palette ─────────────────────────────────────────────────────────────
private val BgColor      = Color(0xFF0A0E1A)
private val SurfaceColor = Color(0xFF111827)
private val AccentGreen  = Color(0xFF10B981)
private val AccentBlue   = Color(0xFF3B82F6)
private val AccentPurple = Color(0xFF8B5CF6)
private val AccentAmber  = Color(0xFFF59E0B)
private val TextColor    = Color(0xFFE2E8F0)
private val SubText      = Color(0xFF94A3B8)

@Composable
fun PcSpeakerScreen() {
    val context  = LocalContext.current

    // State — all initialized to safe defaults (no I/O on main thread)
    var isActive  by remember { mutableStateOf(PcSpeakerService.isRunning) }
    var pairCode  by remember { mutableStateOf(PcSpeakerService.currentPairCode) }
    var phoneIp   by remember { mutableStateOf("Detecting...") }

    // Fetch IP address on IO thread — never on main thread
    LaunchedEffect(Unit) {
        phoneIp = withContext(Dispatchers.IO) { getWifiIpAddress() }
    }

    // Auto-start the pair listener when this screen opens (if not already running)
    LaunchedEffect(Unit) {
        if (!PcSpeakerService.isRunning) {
            try {
                context.startForegroundService(
                    Intent(context, PcSpeakerService::class.java).apply {
                        action = PcSpeakerService.ACTION_START_PAIR
                    }
                )
            } catch (e: Exception) {
                // Service start failed — user will see manual button as fallback
            }
        }
    }

    // Poll isRunning every 500 ms so UI reacts when pairing completes
    LaunchedEffect(Unit) {
        while (true) {
            isActive = PcSpeakerService.isRunning
            delay(500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))

            // ── Header ────────────────────────────────────────────────────────
            Text(
                "🔊 PC Speaker",
                color = TextColor, fontSize = 26.sp, fontWeight = FontWeight.Bold
            )
            Text(
                "Use your phone as a wireless speaker for your PC",
                color = SubText, fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
            )

            // ── Animated orb ─────────────────────────────────────────────────
            SpeakerOrb(isActive = isActive)

            Spacer(Modifier.height(28.dp))

            // ── Status badge ─────────────────────────────────────────────────
            StatusBadge(isActive = isActive)

            Spacer(Modifier.height(24.dp))

            // ── Pair code card (shown when not yet connected) ─────────────────
            if (!isActive) {
                PairCodeCard(
                    pairCode  = pairCode,
                    phoneIp   = phoneIp,
                    onRefresh = {
                        PcSpeakerService.regeneratePairCode()
                        pairCode = PcSpeakerService.currentPairCode
                        // Stop then restart pair listener with the new code
                        context.startService(
                            Intent(context, PcSpeakerService::class.java).apply {
                                action = PcSpeakerService.ACTION_STOP
                            }
                        )
                        try {
                            context.startForegroundService(
                                Intent(context, PcSpeakerService::class.java).apply {
                                    action = PcSpeakerService.ACTION_START_PAIR
                                }
                            )
                        } catch (_: Exception) {}
                    }
                )
                Spacer(Modifier.height(20.dp))
            }

            // ── IP card ───────────────────────────────────────────────────────
            IpAddressCard(phoneIp = phoneIp)

            Spacer(Modifier.height(20.dp))

            // ── Stop button (only when active) ────────────────────────────────
            if (isActive) {
                Button(
                    onClick = {
                        context.startService(
                            Intent(context, PcSpeakerService::class.java).apply {
                                action = PcSpeakerService.ACTION_STOP
                            }
                        )
                        isActive = false
                        PcSpeakerService.regeneratePairCode()
                        pairCode = PcSpeakerService.currentPairCode
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                                ),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "⏹  Disconnect",
                            color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Setup instructions ────────────────────────────────────────────
            SetupInstructions(phoneIp = phoneIp, pairCode = pairCode)

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Pair Code Card ─────────────────────────────────────────────────────────────

@Composable
private fun PairCodeCard(pairCode: String, phoneIp: String, onRefresh: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pairPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(AccentAmber.copy(0.12f), AccentAmber.copy(0.04f))
                ),
                RoundedCornerShape(20.dp)
            )
            .border(1.dp, AccentAmber.copy(alpha * 0.7f), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp).clip(CircleShape)
                        .background(AccentAmber.copy(alpha))
                )
                Text(
                    "PAIR CODE", color = AccentAmber, fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            // Big 4-digit code — like Quick Share
            Text(
                text = pairCode.chunked(2).joinToString("  "),
                color = TextColor,
                fontSize = 52.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 8.sp
            )

            Spacer(Modifier.height(8.dp))
            Text("Run on your PC:", color = SubText, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))

            // Command box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E2A3A), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    "python amora_pc_audio.py $phoneIp $pairCode",
                    color = AccentGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onRefresh) {
                Text("🔄 Refresh Code", color = SubText, fontSize = 12.sp)
            }
        }
    }
}

// ── Animated Orb ───────────────────────────────────────────────────────────────

@Composable
private fun SpeakerOrb(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = if (isActive) 1.15f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation  = tween(if (isActive) 800 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbScale"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(140.dp).scale(scale).clip(CircleShape)
                    .background(AccentGreen.copy(alpha = 0.15f))
            )
        }
        Box(
            modifier = Modifier
                .size(110.dp).clip(CircleShape)
                .background(
                    if (isActive)
                        Brush.radialGradient(listOf(AccentGreen.copy(0.4f), AccentBlue.copy(0.15f)))
                    else
                        Brush.radialGradient(listOf(AccentAmber.copy(0.2f), Color(0xFF0A0E1A)))
                )
                .border(
                    2.dp,
                    if (isActive) AccentGreen.copy(0.6f) else AccentAmber.copy(0.4f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(if (isActive) "🔊" else "⏳", fontSize = 40.sp)
        }
    }
}

// ── Status Badge ──────────────────────────────────────────────────────────────

@Composable
private fun StatusBadge(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "dot")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "dotAlpha"
    )

    val dotColor = if (isActive) AccentGreen else AccentAmber
    val label    = if (isActive) "Connected · Port $PC_SPEAKER_PORT" else "Waiting to pair…"
    val bgBorder = if (isActive) AccentGreen.copy(0.4f) else AccentAmber.copy(0.3f)

    Box(
        modifier = Modifier
            .background(SurfaceColor, RoundedCornerShape(50.dp))
            .border(1.dp, bgBorder, RoundedCornerShape(50.dp))
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp).clip(CircleShape)
                    .background(dotColor.copy(alpha = dotAlpha))
            )
            Text(label, color = dotColor, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        }
    }
}

// ── IP Address Card ───────────────────────────────────────────────────────────

@Composable
private fun IpAddressCard(phoneIp: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(AccentBlue.copy(0.1f), AccentPurple.copy(0.08f))
                ),
                RoundedCornerShape(16.dp)
            )
            .border(1.dp, AccentBlue.copy(0.25f), RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "📡 Your Phone's IP Address",
                color = SubText, fontSize = 12.sp, fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                phoneIp,
                color = TextColor, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Audio port: $PC_SPEAKER_PORT  |  Pair port: $PC_PAIR_PORT",
                color = AccentBlue, fontSize = 12.sp
            )
        }
    }
}

// ── Setup Instructions ────────────────────────────────────────────────────────

@Composable
private fun SetupInstructions(phoneIp: String, pairCode: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor.copy(0.6f), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(16.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("📋 PC Setup", color = SubText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        HorizontalDivider(color = Color.White.copy(0.06f))
        InstructionStep("1", "Install Python on your PC")
        InstructionStep("2", "Run:  pip install pyaudiowpatch numpy pycaw comtypes")
        InstructionStep("3", "Open a terminal and run:")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E2A3A), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                "python amora_pc_audio.py $phoneIp $pairCode",
                color = AccentGreen,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        InstructionStep("4", "Phone auto-connects — no button needed! 🎉")
        InstructionStep("5", "Play audio on your PC — hear it here instantly")
    }
}

@Composable
private fun InstructionStep(number: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp).clip(CircleShape)
                .background(Color(0xFF1E3A5F)),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = Color(0xFF60A5FA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Text(text, color = SubText, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

// ── Utility — run on IO thread via withContext(Dispatchers.IO) ────────────────

private fun getWifiIpAddress(): String {
    return try {
        NetworkInterface.getNetworkInterfaces()
            ?.asSequence()
            ?.flatMap { it.inetAddresses.asSequence() }
            ?.firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
            ?.hostAddress
            ?: "192.168.x.x"
    } catch (_: Exception) {
        "192.168.x.x"
    }
}
