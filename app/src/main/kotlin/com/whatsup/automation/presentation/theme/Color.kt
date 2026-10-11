package com.whatsup.automation.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// لوحة ألوان واتساب أوتوميشن العصرية الإصدار 8.3.0 — نمط Cyber Jade & Electric Slate Luxury
val WhatsAppGreen = Color(0xFF00C853) // أخضر نيون زمردي ساطع وحيوي
val WhatsAppGreenLight = Color(0xFF00E676) // أخضر فسفوري متألق
val WhatsAppGreenDark = Color(0xFF004D40) // زمردي ملكي داكن فخم
val WhatsAppTeal = Color(0xFF00BFA5) // زبرجد نقي
val WhatsAppDarkTeal = Color(0xFF00363A)
val WhatsAppNeon = Color(0xFF00E676)

val CyberCyan = Color(0xFF00B0FF) // أزرق سماوي نيون متوهج
val CyanGlow = Color(0xFF38BDF8) // توهج سيان ناعم
val ElectricBlue = Color(0xFF2563EB)
val NeonPurple = Color(0xFF8B5CF6)
val VioletNebula = Color(0xFFA855F7)
val AmberWarning = Color(0xFFF59E0B)
val RoseError = Color(0xFFF43F5E)
val CoralError = Color(0xFFFF5252)

// علامات الصح والشارات التفاعلية (Checkmarks & Badges)
val CheckmarkGreen = Color(0xFF00C853)
val CheckmarkGlow = Color(0x3300C853)
val CheckmarkCyan = Color(0xFF00B0FF)

// خلفيات نقية وعصرية عالية التباين (High-Contrast Slate & Crisp White Surfaces)
val DarkBgPrimary = Color(0xFFF1F5F9) // رمادي جليدي أنيق وفائق التباين
val DarkBgSecondary = Color(0xFFE2E8F0)
val DarkBgTertiary = Color(0xFFCBD5E1)
val DarkSurface = Color(0xFFFFFFFF) // بطاقات بيضاء ناصعة
val DarkBgCard = DarkSurface
val DarkSurfaceHover = Color(0xFFF8FAFC)
val DarkSurfaceElevated = Color(0xFFFFFFFF)

// زجاج وأغشية بحدود نيون عصرية (Cyber Glass Borders)
val GlassFill = Color(0x0C00B0FF)
val GlassBorder = Color(0x4D00B0FF) // حدود سيان متألقة واضحة جداً
val GlassBorderSubtle = Color(0x2600C853)
val GlassFillSubtle = Color(0x0600C853)
val GlassBorderHighlight = Color(0x9900B0FF)

// نصوص عالية التباين والوضوح التام (Ultra-Sharp Carbon Typography)
val TextPrimary = Color(0xFF020617) // أسود كربوني نقي 100% للخطوط والعناوين
val TextSecondary = Color(0xFF334155) // أردوازي داكن مقروء بوضوح فائق
val TextMuted = Color(0xFF64748B)
val TextHint = Color(0xFF94A3B8)

// تدرجات لونية زمردية وبيضاء وسيبرانية
val EmeraldGradient = Brush.horizontalGradient(
    colors = listOf(WhatsAppGreen, WhatsAppGreenLight)
)

val LiquidEmeraldGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF004D40), Color(0xFF00C853), Color(0xFF00E676))
)

val NeonPurpleGradient = Brush.linearGradient(
    colors = listOf(NeonPurple, CyberCyan)
)

val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9), Color(0xFFE2E8F0))
)

val GlassCardGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9))
)

object ObsidianGlowBorder {
    val Emerald: Brush = Brush.linearGradient(listOf(Color(0xFF00C853), Color(0x8000E676), Color(0x3000C853)))
    val Cyan: Brush = Brush.linearGradient(listOf(Color(0xFF00B0FF), Color(0x8038BDF8), Color(0x3000B0FF)))
    val Purple: Brush = Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0x80A78BFA), Color(0x308B5CF6)))
    val Amber: Brush = Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0x80FBBF24), Color(0x30F59E0B)))
    val Rose: Brush = Brush.linearGradient(listOf(Color(0xFFF43F5E), Color(0x80FB7185), Color(0x30F43F5E)))
    val None: Brush = Brush.linearGradient(listOf(Color(0x4000B0FF), Color(0x1800B0FF)))
}
