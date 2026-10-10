package com.whatsup.automation.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// لوحة ألوان واتساب أوتوميشن العصرية — أبيض ناصع مع خطوط ولمسات زمردية (Emerald White & Mint Luxury)
val WhatsAppGreen = Color(0xFF00A884) // زمردي واتساب الأصلي المعتمد
val WhatsAppGreenLight = Color(0xFF25D366) // أخضر واتساب الناصع
val WhatsAppGreenDark = Color(0xFF075E54) // أخضر داكن فخم
val WhatsAppTeal = Color(0xFF0D9488)
val WhatsAppDarkTeal = Color(0xFF042F2E)
val WhatsAppNeon = Color(0xFF00C853)

val CyberCyan = Color(0xFF0284C7)
val CyanGlow = Color(0xFF0EA5E9)
val ElectricBlue = Color(0xFF2563EB)
val NeonPurple = Color(0xFF7C3AED)
val VioletNebula = Color(0xFF9333EA)
val AmberWarning = Color(0xFFD97706)
val RoseError = Color(0xFFE11D48)
val CoralError = Color(0xFFF43F5E)

// خلفيات بيضاء نقية وعصرية (Crisp White & Light Surfaces)
val DarkBgPrimary = Color(0xFFF8FAFC) // أبيض عاجي ناعم ومريح للعين
val DarkBgSecondary = Color(0xFFF1F5F9)
val DarkBgTertiary = Color(0xFFE2E8F0)
val DarkSurface = Color(0xFFFFFFFF) // بطاقات بيضاء ناصعة
val DarkBgCard = DarkSurface
val DarkSurfaceHover = Color(0xFFF8FAFC)
val DarkSurfaceElevated = Color(0xFFFFFFFF)

// زجاج وأغشية بخطوط زمردية خضراء أنيقة
val GlassFill = Color(0x0A00A884)
val GlassBorder = Color(0x3300A884) // خطوط خضراء واضحة وأنيقة
val GlassBorderSubtle = Color(0x1F00A884)
val GlassFillSubtle = Color(0x0500A884)
val GlassBorderHighlight = Color(0x8000A884)

// نصوص عالية التباين والوضوح على الخلفيات البيضاء
val TextPrimary = Color(0xFF0F172A) // أسود كحلي عميق وواضح جداً
val TextSecondary = Color(0xFF475569) // رمادي أردوازي أنيق
val TextMuted = Color(0xFF64748B)
val TextHint = Color(0xFF94A3B8)

// تدرجات لونية زمردية وبيضاء
val EmeraldGradient = Brush.horizontalGradient(
    colors = listOf(WhatsAppGreen, WhatsAppGreenLight)
)

val LiquidEmeraldGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF075E54), Color(0xFF00A884), Color(0xFF25D366))
)

val NeonPurpleGradient = Brush.linearGradient(
    colors = listOf(NeonPurple, CyberCyan)
)

val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC), Color(0xFFF1F5F9))
)

val GlassCardGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC))
)

object ObsidianGlowBorder {
    val Emerald: Brush = Brush.linearGradient(listOf(Color(0x6600A884), Color(0x3325D366), Color(0x1500A884)))
    val Cyan: Brush = Brush.linearGradient(listOf(Color(0x660284C7), Color(0x330EA5E9), Color(0x150284C7)))
    val Purple: Brush = Brush.linearGradient(listOf(Color(0x667C3AED), Color(0x339333EA), Color(0x157C3AED)))
    val Amber: Brush = Brush.linearGradient(listOf(Color(0x66D97706), Color(0x33F59E0B), Color(0x15D97706)))
    val Rose: Brush = Brush.linearGradient(listOf(Color(0x66E11D48), Color(0x33F43F5E), Color(0x15E11D48)))
    val None: Brush = Brush.linearGradient(listOf(Color(0x2500A884), Color(0x1000A884)))
}
