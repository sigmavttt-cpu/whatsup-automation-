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

// خلفيات كربونية داكنة فائقة الفخامة مطابقة لشاشة الدخول (Deep Obsidian & Carbon Surfaces)
val DarkBgPrimary = Color(0xFF060D12) // أسود كربوني نيلي فائق العمق مطابق للسبلاش والدخول
val DarkBgSecondary = Color(0xFF0B141B) // أردوازي ليلي عميق
val DarkBgTertiary = Color(0xFF111E27)
val DarkSurface = Color(0xFF0F1A22) // بطاقات كربونية زجاجية فاخرة
val DarkBgCard = DarkSurface
val DarkSurfaceHover = Color(0xFF162530)
val DarkSurfaceElevated = Color(0xFF14212C)

// زجاج وأغشية بحدود نيون عصرية (Cyber Glass Borders)
val GlassFill = Color(0x1F0F1A22)
val GlassBorder = Color(0x3300E676) // حدود نيون زمردية متألقة
val GlassBorderSubtle = Color(0x1A00E676)
val GlassFillSubtle = Color(0x0F00E676)
val GlassBorderHighlight = Color(0x6600E676)

// نصوص مضيئة عالية التباين والوضوح التام (Ultra-Legible Luminous Typography)
val TextPrimary = Color(0xFFFFFFFF) // أبيض ناصع مضيء فائق الوضوح 100%
val TextSecondary = Color(0xFFCBD5E1) // رمادي فضي ساطع مقروء بوضوح تام ضد الخلفية المظلمة
val TextMuted = Color(0xFF94A3B8) // رمادي متوسط واضح
val TextHint = Color(0xFF64748B)

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
    colors = listOf(Color(0xFF060D12), Color(0xFF0B141B), Color(0xFF060D12))
)

val GlassCardGradient = Brush.verticalGradient(
    colors = listOf(Color(0xCC0F1A22), Color(0xCC0B141B))
)

object ObsidianGlowBorder {
    val Emerald: Brush = Brush.linearGradient(listOf(Color(0xFF00C853), Color(0x8000E676), Color(0x3000C853)))
    val Cyan: Brush = Brush.linearGradient(listOf(Color(0xFF00B0FF), Color(0x8038BDF8), Color(0x3000B0FF)))
    val Purple: Brush = Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0x80A78BFA), Color(0x308B5CF6)))
    val Amber: Brush = Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0x80FBBF24), Color(0x30F59E0B)))
    val Rose: Brush = Brush.linearGradient(listOf(Color(0xFFF43F5E), Color(0x80FB7185), Color(0x30F43F5E)))
    val None: Brush = Brush.linearGradient(listOf(Color(0x4000B0FF), Color(0x1800B0FF)))
}
