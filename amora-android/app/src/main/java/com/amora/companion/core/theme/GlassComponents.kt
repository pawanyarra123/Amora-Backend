package com.amora.companion.core.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Color Constants from Figma App UI ──────────────────────────────────────────
val FigmaBgDark       = Color(0xFF0A0E1A)
val FigmaSurface      = Color(0xFF111827)
val FigmaViolet       = Color(0xFF7C3AED)
val FigmaPurpleText   = Color(0xFFA78BFA)
val FigmaCyan         = Color(0xFF06B6D4)
val FigmaCyanText     = Color(0xFF22D3EE)
val FigmaGreen        = Color(0xFF22C55E)
val FigmaAmber        = Color(0xFFF59E0B)
val FigmaRed          = Color(0xFFEF4444)
val FigmaSubtext      = Color(0x66FFFFFF)

val FigmaGradientVioletCyan = Brush.linearGradient(listOf(FigmaViolet, FigmaCyan))
val FigmaGradientDanger     = Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFB91C1C)))
val FigmaGradientGreen      = Brush.linearGradient(listOf(Color(0xFF059669), Color(0xFF22C55E)))

/**
 * Ambient background with top-left purple glow and bottom-right cyan glow.
 */
@Composable
fun AmbientBg(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FigmaBgDark)
    ) {
        // Top-left purple radial glow
        Box(
            modifier = Modifier
                .offset(x = (-60).dp, y = (-60).dp)
                .size(320.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(FigmaViolet.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
        )
        // Bottom-right cyan radial glow
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .size(280.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(FigmaCyan.copy(alpha = 0.10f), Color.Transparent)
                    )
                )
        )
    }
}

/**
 * GlassCard - Frosted glass effect container with subtle 1px border.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    borderColor: Color = Color.White.copy(alpha = 0.08f),
    backgroundColor: Color = Color.White.copy(alpha = 0.04f),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) modifier.clickable { onClick() } else modifier

    Card(
        modifier = clickableModifier,
        shape = RoundedCornerShape(cornerRadius),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

/**
 * SectionLabel - Uppercase 11.sp letter-spaced tracking label.
 */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = Color.White.copy(alpha = 0.35f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = modifier.padding(vertical = 4.dp)
    )
}

/**
 * CustomToggle - Violet to Cyan gradient toggle switch.
 */
@Composable
fun CustomToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = FigmaViolet,
            uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
            uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
            uncheckedBorderColor = Color.White.copy(alpha = 0.15f)
        )
    )
}

/**
 * BackButton - Violet text back button with arrow.
 */
@Composable
fun BackButton(
    onBack: () -> Unit,
    label: String = "Back to Settings",
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onBack,
        modifier = modifier
    ) {
        Text("← $label", color = FigmaPurpleText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * SettingsRow - Glass tile for navigation & settings options.
 */
@Composable
fun SettingsRowTile(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        subtitle,
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text("›", color = Color.White.copy(alpha = 0.3f), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * VioletCard - Info card with subtle violet theme.
 */
@Composable
fun VioletCard(
    title: String,
    description: String,
    icon: String = "🎙️",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, FigmaViolet.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = FigmaViolet.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = FigmaPurpleText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

/**
 * AmberCard - Info card with amber theme (used for platform rationale & warnings).
 */
@Composable
fun AmberCard(
    title: String,
    description: String,
    icon: String = "📋",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, FigmaAmber.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = FigmaAmber.copy(alpha = 0.10f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = FigmaAmber, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}
