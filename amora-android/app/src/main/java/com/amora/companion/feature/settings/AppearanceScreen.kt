package com.amora.companion.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.amora.companion.core.theme.AmoraThemePalette
import com.amora.companion.core.theme.AmoraThemeSystem
import com.amora.companion.core.theme.BackButton
import com.amora.companion.core.theme.FigmaRed

@Composable
fun AppearanceScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit = {}
) {
    val selectedThemeName by viewModel.themePreset.collectAsState(initial = "Midnight Blue (Default Dark)")
    val wallpaperUriStr by viewModel.customWallpaperUri.collectAsState(initial = "")
    val currentPalette = remember(selectedThemeName) { AmoraThemeSystem.getPalette(selectedThemeName) }
    var selectedCategoryTab by remember { mutableIntStateOf(0) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setCustomWallpaperUri(it.toString()) }
    }

    val displayedThemes = if (selectedCategoryTab == 0) AmoraThemeSystem.darkThemes else AmoraThemeSystem.lightThemes

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            BackButton(onBack = onBackClick)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "✨ Themes & Appearance",
                color = currentPalette.textColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Upload custom gallery wallpaper & choose from 40 dynamic theme palettes",
                color = currentPalette.subtextColor,
                fontSize = 12.sp
            )
        }

        // Custom Wallpaper Upload Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = currentPalette.surfaceColor.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, currentPalette.accentColor.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🖼️ Custom Gallery Wallpaper",
                        color = currentPalette.textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (wallpaperUriStr.isNotEmpty()) "Custom wallpaper active" else "No custom wallpaper set (using active theme background)",
                        color = currentPalette.subtextColor,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = currentPalette.accentColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Upload Photo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        if (wallpaperUriStr.isNotEmpty()) {
                            Button(
                                onClick = { viewModel.setCustomWallpaperUri("") },
                                colors = ButtonDefaults.buttonColors(containerColor = FigmaRed),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Reset Wallpaper", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Category Tab Switcher (20 Dark / 20 Light)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { selectedCategoryTab = 0 },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedCategoryTab == 0) currentPalette.accentColor else currentPalette.surfaceColor.copy(alpha = 0.7f)
                    )
                ) {
                    Text(
                        "🌙 20 Dark Themes",
                        color = if (selectedCategoryTab == 0) Color.White else currentPalette.textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = { selectedCategoryTab = 1 },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedCategoryTab == 1) currentPalette.accentColor else currentPalette.surfaceColor.copy(alpha = 0.7f)
                    )
                ) {
                    Text(
                        "☀️ 20 Light Themes",
                        color = if (selectedCategoryTab == 1) Color.White else currentPalette.textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        items(displayedThemes) { theme ->
            ThemeCardTile(
                theme = theme,
                isSelected = selectedThemeName == theme.name,
                currentPalette = currentPalette,
                onSelect = { viewModel.setThemePreset(theme.name) }
            )
        }
    }
}

@Composable
fun ThemeCardTile(
    theme: AmoraThemePalette,
    isSelected: Boolean,
    currentPalette: AmoraThemePalette,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) currentPalette.accentColor.copy(alpha = 0.2f) else currentPalette.surfaceColor.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(18.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, theme.accentColor) else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Row(modifier = Modifier.padding(end = 12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(theme.backgroundColor)
                            .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(theme.accentColor)
                            .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                    )
                }

                Column {
                    Text(
                        text = theme.name,
                        color = currentPalette.textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (theme.isDark) "Dark Mode Palette" else "Light Mode Palette",
                        color = currentPalette.subtextColor,
                        fontSize = 11.sp
                    )
                }
            }

            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(selectedColor = currentPalette.accentColor)
            )
        }
    }
}
