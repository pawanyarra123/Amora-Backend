package com.amora.companion.core.theme

import androidx.compose.ui.graphics.Color

data class AmoraThemePalette(
    val name: String,
    val isDark: Boolean,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val accentColor: Color,
    val textColor: Color = Color.White,
    val subtextColor: Color = Color(0xFF94A3B8)
)

object AmoraThemeSystem {

    val darkThemes = listOf(
        AmoraThemePalette("Cyberpunk Neon", true, Color(0xFF070410), Color(0xFF160B28), Color(0xFFFF007F), Color(0xFFFFFFFF), Color(0xFFA78BFA)),
        AmoraThemePalette("Midnight Sapphire", true, Color(0xFF040814), Color(0xFF0D162C), Color(0xFF38BDF8), Color(0xFFF0F9FF), Color(0xFF7DD3FC)),
        AmoraThemePalette("Emerald Obsidian", true, Color(0xFF030F08), Color(0xFF0A2214), Color(0xFF10B981), Color(0xFFECFDF5), Color(0xFF6EE7B7)),
        AmoraThemePalette("Crimson Eclipse", true, Color(0xFF0D0303), Color(0xFF240B0B), Color(0xFFEF4444), Color(0xFFFEF2F2), Color(0xFFFCA5A5)),
        AmoraThemePalette("Amethyst Void", true, Color(0xFF0B0414), Color(0xFF1D0E32), Color(0xFFA855F7), Color(0xFFFAF5FF), Color(0xFFD8B4FE)),
        AmoraThemePalette("Sunset Horizon", true, Color(0xFF0F0503), Color(0xFF260E08), Color(0xFFF97316), Color(0xFFFFF7ED), Color(0xFFFDBA74)),
        AmoraThemePalette("Golden Amber", true, Color(0xFF0C0800), Color(0xFF221802), Color(0xFFF59E0B), Color(0xFFFFFBEB), Color(0xFFFCD34D)),
        AmoraThemePalette("Deep Abyss", true, Color(0xFF010B12), Color(0xFF061C2A), Color(0xFF06B6D4), Color(0xFFECFEFF), Color(0xFF67E8F9)),
        AmoraThemePalette("Matrix Terminal", true, Color(0xFF020B04), Color(0xFF081E0C), Color(0xFF22C55E), Color(0xFFF0FDF4), Color(0xFF86EFAC)),
        AmoraThemePalette("Royal Velvet", true, Color(0xFF070618), Color(0xFF141138), Color(0xFF6366F1), Color(0xFFEEF2FF), Color(0xFFA5B4FC)),
        AmoraThemePalette("Rose Gold Dusk", true, Color(0xFF0E050A), Color(0xFF28111E), Color(0xFFFB7185), Color(0xFFFFF1F2), Color(0xFFFDA4AF)),
        AmoraThemePalette("Arctic Aurora", true, Color(0xFF030D14), Color(0xFF092230), Color(0xFF2DD4BF), Color(0xFFF0FDFA), Color(0xFF5EEAD4)),
        AmoraThemePalette("Solar Flare", true, Color(0xFF100400), Color(0xFF2C0E02), Color(0xFFFF5722), Color(0xFFFFF8F6), Color(0xFFFF8A65)),
        AmoraThemePalette("Titanium Stealth", true, Color(0xFF0A0A0E), Color(0xFF181822), Color(0xFF94A3B8), Color(0xFFF8FAFC), Color(0xFFCBD5E1)),
        AmoraThemePalette("Lavender Mist", true, Color(0xFF0A0616), Color(0xFF1A1232), Color(0xFFC084FC), Color(0xFFFAF5FF), Color(0xFFE9D5FF)),
        AmoraThemePalette("Cobalt Pulse", true, Color(0xFF020A1E), Color(0xFF0B1B44), Color(0xFF2563EB), Color(0xFFEFF6FF), Color(0xFF93C5FD)),
        AmoraThemePalette("Blood Moon", true, Color(0xFF120205), Color(0xFF2C070F), Color(0xFFDC2626), Color(0xFFFEF2F2), Color(0xFFF87171)),
        AmoraThemePalette("Neon Synthwave", true, Color(0xFF0C021E), Color(0xFF220947), Color(0xFFF43F5E), Color(0xFFFFF1F2), Color(0xFFFDA4AF)),
        AmoraThemePalette("Phantom Ghost", true, Color(0xFF080B10), Color(0xFF141C28), Color(0xFF38BDF8), Color(0xFFF1F5F9), Color(0xFF94A3B8)),
        AmoraThemePalette("Galactic Nebula", true, Color(0xFF070312), Color(0xFF160B30), Color(0xFF818CF8), Color(0xFFEEF2FF), Color(0xFFC7D2FE))
    )

    val lightThemes = listOf(
        AmoraThemePalette("Ice Crystal", false, Color(0xFFF8FAFF), Color(0xFFE2E8F0), Color(0xFF0284C7), Color(0xFF0F172A), Color(0xFF475569)),
        AmoraThemePalette("Pure Snow", false, Color(0xFFF8FAFC), Color(0xFFFFFFFF), Color(0xFF2563EB), Color(0xFF0F172A), Color(0xFF475569)),
        AmoraThemePalette("Pastel Peach", false, Color(0xFFFFF7ED), Color(0xFFFFEDD5), Color(0xFFEA580C), Color(0xFF0F172A), Color(0xFF475569)),
        AmoraThemePalette("Mint Botanical", false, Color(0xFFF0FDF4), Color(0xFFDCFCE7), Color(0xFF16A34A), Color(0xFF0F172A), Color(0xFF475569)),
        AmoraThemePalette("Champagne Gold", false, Color(0xFFFFFDF5), Color(0xFFFFFBEB), Color(0xFFD97706), Color(0xFF0F172A), Color(0xFF475569))
    )

    val allThemes = darkThemes + lightThemes
    private val themesMap = allThemes.associateBy { it.name }

    fun getPalette(themeName: String): AmoraThemePalette {
        return themesMap[themeName] ?: darkThemes[0]
    }
}
