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
        AmoraThemePalette("Midnight Void", true, Color(0xFF000000), Color(0x0AFFFFFF), Color(0xFFA78BFA), Color(0xFFF8FAFC), Color(0x80F8FAFC)),
        AmoraThemePalette("Aurora", true, Color(0xFF020E0A), Color(0x0A00FF96), Color(0xFF34D399), Color(0xFFE8FFF4), Color(0x80E8FFF4)),
        AmoraThemePalette("Solar Flare", true, Color(0xFF0A0400), Color(0x0DFF6400), Color(0xFFFB923C), Color(0xFFFFF8F0), Color(0x80FFF8F0)),
        AmoraThemePalette("Deep Ocean", true, Color(0xFF000A14), Color(0x0D0064FF), Color(0xFF38BDF8), Color(0xFFEFF8FF), Color(0x80EFF8FF)),
        AmoraThemePalette("Rose Noir", true, Color(0xFF0A0007), Color(0x0DFF0080), Color(0xFFF472B6), Color(0xFFFFF0F5), Color(0x80FFF0F5)),
        AmoraThemePalette("Emerald", true, Color(0xFF00100A), Color(0x0D00C850), Color(0xFF34D399), Color(0xFFF0FFF4), Color(0x80F0FFF4)),
        AmoraThemePalette("Cosmic", true, Color(0xFF050010), Color(0x0F8B00FF), Color(0xFFC4B5FD), Color(0xFFF5F0FF), Color(0x80F5F0FF)),
        AmoraThemePalette("Arctic", true, Color(0xFF020812), Color(0x0DC8E6FF), Color(0xFF7DD3FC), Color(0xFFEDF6FF), Color(0x80EDF6FF)),
        AmoraThemePalette("Sunset", true, Color(0xFF080010), Color(0x0DC832C8), Color(0xFFE879F9), Color(0xFFFFF5FF), Color(0x80FFF5FF)),
        AmoraThemePalette("Crimson", true, Color(0xFF0A0000), Color(0x0DDC0000), Color(0xFFF87171), Color(0xFFFFF5F5), Color(0x80FFF5F5)),
        AmoraThemePalette("Matrix", true, Color(0xFF000A00), Color(0x0A00FF00), Color(0xFF4ADE80), Color(0xFFECFDF5), Color(0xA600FF64)),
        AmoraThemePalette("Royal Gold", true, Color(0xFF060400), Color(0x0FD4AF37), Color(0xFFF59E0B), Color(0xFFFFFBEB), Color(0x80FFFBEB)),
        AmoraThemePalette("Lava", true, Color(0xFF0A0200), Color(0x0DFF5000), Color(0xFFFB923C), Color(0xFFFFF8F5), Color(0x80FFF8F5)),
        AmoraThemePalette("Moonlight", true, Color(0xFF07080C), Color(0x0DB4B4DC), Color(0xFFCBD5E1), Color(0xFFF0F0FF), Color(0x80F0F0FF)),
        AmoraThemePalette("Sakura Night", true, Color(0xFF08010A), Color(0x0DFF64C8), Color(0xFFF0ABFC), Color(0xFFFFF5FD), Color(0x80FFF5FD)),
        AmoraThemePalette("Midnight Blue", true, Color(0xFF020818), Color(0x1F1E3A8A), Color(0xFF60A5FA), Color(0xFFEFF6FF), Color(0x80EFF6FF)),
        AmoraThemePalette("Obsidian", true, Color(0xFF090909), Color(0x12787878), Color(0xFF9CA3AF), Color(0xFFFAFAFA), Color(0x73FAFAFA)),
        AmoraThemePalette("Neon Cyber", true, Color(0xFF000008), Color(0x0A00FFFF), Color(0xFF00FFFF), Color(0xFFF0FFFF), Color(0x9900FFFF)),
        AmoraThemePalette("Deep Forest", true, Color(0xFF010A04), Color(0x0F008032), Color(0xFF86EFAC), Color(0xFFF0FFF0), Color(0x80F0FFF0)),
        AmoraThemePalette("Cyber Violet (Cyberpunk)", true, Color(0xFF17062E), Color(0xFF2D0B5A), Color(0xFFFF007F), Color.White, Color(0xFF94A3B8))
    )

    val lightThemes = listOf(
        AmoraThemePalette("Ice Crystal", false, Color(0xFFF8FAFF), Color(0x0F1E3C78), Color(0xFF0284C7), Color(0xFF0F172A), Color(0x8C0F172A)),
        AmoraThemePalette("Pure Snow (Minimal Light)", false, Color(0xFFF8FAFC), Color(0xFFFFFFFF), Color(0xFF2563EB), Color(0xFF0F172A), Color(0xFF475569)),
        AmoraThemePalette("Pastel Peach (Warm Sunrise)", false, Color(0xFFFFF7ED), Color(0xFFFFEDD5), Color(0xFFEA580C), Color(0xFF0F172A), Color(0xFF475569)),
        AmoraThemePalette("Soft Lavender (Serene Violet)", false, Color(0xFFFAF5FF), Color(0xFFF3E8FF), Color(0xFF9333EA), Color(0xFF0F172A), Color(0xFF475569)),
        AmoraThemePalette("Mint Fresh (Cool Botanical)", false, Color(0xFFF0FDF4), Color(0xFFDCFCE7), Color(0xFF16A34A), Color(0xFF0F172A), Color(0xFF475569)),
        AmoraThemePalette("Sky Cyan (Breeze Air)", false, Color(0xFFF0F9FF), Color(0xFFE0F2FE), Color(0xFF0284C7), Color(0xFF0F172A), Color(0xFF475569)),
        AmoraThemePalette("Sakura Blossom (Japanese Cherry)", false, Color(0xFFFFF1F2), Color(0xFFFFE4E6), Color(0xFFE11D48), Color(0xFF0F172A), Color(0xFF475569)),
        AmoraThemePalette("Lemon Sunshine (Vibrant Citrus)", false, Color(0xFFFEFCE8), Color(0xFFFEF9C3), Color(0xFFCA8A04), Color(0xFF0F172A), Color(0xFF475569)),
        AmoraThemePalette("Solarized Light (Cream Classic)", false, Color(0xFFFDF6E3), Color(0xFFEEE8D5), Color(0xFFB58900), Color(0xFF0F172A), Color(0xFF475569)),
        AmoraThemePalette("Coral Reef (Warm Summer)", false, Color(0xFFFFF1F2), Color(0xFFFFE4E6), Color(0xFFF43F5E), Color(0xFF0F172A), Color(0xFF475569)),
        AmoraThemePalette("Nordic Snow (Crisp Ice)", false, Color(0xFFF1F5F9), Color(0xFFE2E8F0), Color(0xFF0EA5E9), Color(0xFF0F172A), Color(0xFF475569)),
        AmoraThemePalette("Matcha Green (Zen Tea)", false, Color(0xFFF4FBF7), Color(0xFFE6F7ED), Color(0xFF059669), Color(0xFF0F172A), Color(0xFF475569)),
        AmoraThemePalette("Bubblegum Pink (Playful Neon)", false, Color(0xFFFDF2F8), Color(0xFFFCE7F3), Color(0xFFDB2777), Color(0xFF0F172A), Color(0xFF475569)),
        AmoraThemePalette("Ivory Silk (Luxury Pearl)", false, Color(0xFFFAF9F6), Color(0xFFF5F5F0), Color(0xFFD97706), Color(0xFF0F172A), Color(0xFF475569)),
        AmoraThemePalette("Glacier Cyan (Alpine Fresh)", false, Color(0xFFECFEFF), Color(0xFFCFFAFE), Color(0xFF0891B2), Color(0xFF0F172A), Color(0xFF475569)),
        AmoraThemePalette("Vanilla Latte (Warm Coffee)", false, Color(0xFFFDFBF7), Color(0xFFF5EFE6), Color(0xFF8B5E34), Color(0xFF0F172A), Color(0xFF475569)),
        AmoraThemePalette("Lilac Mist (Gentle Purple)", false, Color(0xFFF8F5FF), Color(0xFFEDE9FE), Color(0xFF7C3AED), Color(0xFF0F172A), Color(0xFF475569)),
        AmoraThemePalette("Pistachio Cream (Soft Herb)", false, Color(0xFFF7FCF8), Color(0xFFE8F5EB), Color(0xFF15803D), Color(0xFF0F172A), Color(0xFF475569)),
        AmoraThemePalette("Champagne Gold (Elegant Sparkle)", false, Color(0xFFFFFDF5), Color(0xFFFFFBEB), Color(0xFFD97706), Color(0xFF0F172A), Color(0xFF475569)),
        AmoraThemePalette("Ocean Breeze (Coastal Aqua)", false, Color(0xFFF0FDFA), Color(0xFFCCFBF1), Color(0xFF0D9488), Color(0xFF0F172A), Color(0xFF475569))
    )

    val allThemes = darkThemes + lightThemes
    private val themesMap = allThemes.associateBy { it.name }

    fun getPalette(themeName: String): AmoraThemePalette {
        return themesMap[themeName] ?: darkThemes[0]
    }
}
