package com.whatsup.automation.presentation.screens.auth.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * رسم شعار حرف S السيبراني المحاط بإطار سداسي وماسي دوّار لشاشة الدخول
 */
@Composable
fun CyberSMonogram(
    modifier: Modifier = Modifier,
    animTimeMs: Long = 0L
) {
    val rotHex = ((animTimeMs % 12000L).toFloat() / 12000f) * 360f
    val rotDiamond = -(((animTimeMs % 8000L).toFloat() / 8000f) * 360f)
    val pulseScale = 1.0f + (kotlin.math.sin(animTimeMs * 0.0031) * 0.06f).toFloat()

    Canvas(modifier = modifier.scale(pulseScale)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = w * 0.44f

        // 1. الإطار السداسي الدوار (Hexagon Aura)
        val hexPath = Path()
        for (i in 0 until 6) {
            val angle = Math.toRadians((rotHex + (i * 60)).toDouble())
            val px = cx + (kotlin.math.cos(angle) * r).toFloat()
            val py = cy + (kotlin.math.sin(angle) * r).toFloat()
            if (i == 0) hexPath.moveTo(px, py) else hexPath.lineTo(px, py)
        }
        hexPath.close()
        drawPath(
            path = hexPath,
            color = Color(0xFF00E676).copy(alpha = 0.45f),
            style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
        )

        // 2. الإطار الماسي المعاكس (Diamond Aura)
        val diamondPath = Path()
        for (i in 0 until 4) {
            val angle = Math.toRadians((rotDiamond + (i * 90) + 45).toDouble())
            val px = cx + (kotlin.math.cos(angle) * (r * 0.88f)).toFloat()
            val py = cy + (kotlin.math.sin(angle) * (r * 0.88f)).toFloat()
            if (i == 0) diamondPath.moveTo(px, py) else diamondPath.lineTo(px, py)
        }
        diamondPath.close()
        drawPath(
            path = diamondPath,
            color = Color(0xFF06B6D4).copy(alpha = 0.35f),
            style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
        )

        // 3. رسم حرف S المنحني المتوهج
        val sPath = Path().apply {
            moveTo(w * 0.68f, h * 0.24f)
            cubicTo(w * 0.32f, h * 0.16f, w * 0.22f, h * 0.38f, w * 0.52f, h * 0.50f)
            cubicTo(w * 0.78f, h * 0.62f, w * 0.68f, h * 0.86f, w * 0.30f, h * 0.78f)
        }

        // توهج خلفي ناعم لحرف S
        drawPath(
            path = sPath,
            color = Color(0x6600E676),
            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
        )

        // خط S النيوني الرئيسي
        drawPath(
            path = sPath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF34D399), Color(0xFF00E676), Color(0xFF06B6D4)),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            ),
            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
        )

        // لمعان مركزي أبيض
        drawPath(
            path = sPath,
            color = Color.White.copy(alpha = 0.9f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
