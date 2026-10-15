package com.whatsup.automation.presentation.screens.auth.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import android.os.SystemClock
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * مكون رادار شاشة البداية (Splash Multi-Ring Radar & Orbiting Nodes)
 * يطابق ملف HTML الأصلي والصورة 2:
 * 1. ماسح الرادار الدوراني (Radar Sweep)
 * 2. موجات السونار النابضة (Sonar Rings)
 * 3. ثلاث حلقات متحدة المركز مع مسارات متقطعة ومتباينة في سرعة الدوران
 * 4. ثلاث عقد مدارية متوهجة: الرسائل (الوسط)، المستخدم (الخارج)، الصاعقة (الداخل)
 * 5. النواة النيونية النابضة في المركز
 * 6. شريط الليزر السفلي المتحرك (Splash Laser Bar)
 */
@Composable
fun SplashRadarView(
    modifier: Modifier = Modifier,
    laserProgress: Float = 1f
) {
    var timeMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = SystemClock.uptimeMillis()
        while (true) {
            timeMs = SystemClock.uptimeMillis() - start
            delay(16)
        }
    }

    // دورات المسار الراداري (دوران كامل كل 3.2 ثانية)
    val sweepAngle = ((timeMs % 3200L).toFloat() / 3200f) * 360f

    // نبض موجات السونار
    val p1 = ((timeMs % 2000L).toFloat() / 2000f)
    val sonarPulse1 = 0.55f + (p1 * 0.65f)
    val sonarAlpha1 = (1f - p1) * 0.5f

    val p2 = (((timeMs + 700L) % 2500L).toFloat() / 2500f)
    val sonarPulse2 = 0.4f + (p2 * 0.7f)
    val sonarAlpha2 = (1f - p2) * 0.4f

    // زوايا دوران المسارات الثلاثة
    val orbitOuterAngle = -(((timeMs % 12000L).toFloat() / 12000f) * 360f)
    val orbitMiddleAngle = (((timeMs % 9000L).toFloat() / 9000f) * 360f)
    val orbitInnerAngle = (((timeMs % 5500L).toFloat() / 5500f) * 360f)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(310.dp),
            contentAlignment = Alignment.Center
        ) {
            // 1. رسم حلقات الرادار وموجات السونار والماسح
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val maxRadius = size.width * 0.46f

                // ماسح الرادار المخروطي الدوار (Radar Sweep Cone)
                val sweepRad = Math.toRadians(sweepAngle.toDouble())
                val sweepX = cx + (cos(sweepRad) * maxRadius).toFloat()
                val sweepY = cy + (sin(sweepRad) * maxRadius).toFloat()
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF00E676), Color.Transparent),
                        start = Offset(cx, cy),
                        end = Offset(sweepX, sweepY)
                    ),
                    start = Offset(cx, cy),
                    end = Offset(sweepX, sweepY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // موجات السونار الدائرية المتلاشية
                drawCircle(
                    color = Color(0xFF00E676).copy(alpha = sonarAlpha1),
                    radius = maxRadius * sonarPulse1,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.2.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF00BFA5).copy(alpha = sonarAlpha2),
                    radius = maxRadius * sonarPulse2,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx())
                )

                // المسار الخارجي (Outer Concentric Track - R: ~140dp)
                val rOuter = maxRadius * 0.94f
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = rOuter,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx())
                )
                drawArc(
                    color = Color(0xFF00E676).copy(alpha = 0.5f),
                    startAngle = orbitOuterAngle,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(cx - rOuter, cy - rOuter),
                    size = Size(rOuter * 2, rOuter * 2),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // المسار الأوسط (Middle Concentric Track - R: ~105dp)
                val rMiddle = maxRadius * 0.68f
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = rMiddle,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx())
                )
                drawArc(
                    color = Color(0xFF34D399).copy(alpha = 0.6f),
                    startAngle = orbitMiddleAngle,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = Offset(cx - rMiddle, cy - rMiddle),
                    size = Size(rMiddle * 2, rMiddle * 2),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // المسار الداخلي (Inner Concentric Track - R: ~68dp)
                val rInner = maxRadius * 0.44f
                drawCircle(
                    color = Color.White.copy(alpha = 0.1f),
                    radius = rInner,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx())
                )
                drawArc(
                    color = Color(0xFF06B6D4).copy(alpha = 0.65f),
                    startAngle = orbitInnerAngle,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(cx - rInner, cy - rInner),
                    size = Size(rInner * 2, rInner * 2),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 2. العقد المدارية الثلاثية المتوهجة (3 Orbiting Floating Icons)
            // العقدة 1: الرسائل (💬) على المسار الأوسط
            OrbitingNode(
                radius = 98.dp,
                angleDeg = orbitMiddleAngle + 45f,
                icon = Icons.Default.Forum,
                glowColor = Color(0xFF00E676)
            )

            // العقدة 2: جهات الاتصال (👤) على المسار الخارجي
            OrbitingNode(
                radius = 134.dp,
                angleDeg = orbitOuterAngle + 190f,
                icon = Icons.Default.Person,
                glowColor = Color(0xFF34D399)
            )

            // العقدة 3: الأتمتة والصاعقة (⚡) على المسار الداخلي
            OrbitingNode(
                radius = 62.dp,
                angleDeg = orbitInnerAngle + 270f,
                icon = Icons.Default.Bolt,
                glowColor = Color(0xFF06B6D4)
            )

            // 3. النواة النيونية المركزية (Center Core Pulse Dot)
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E676))
                    .border(2.dp, Color.White, CircleShape)
                    .shadow(18.dp, CircleShape, spotColor = Color(0xFF00E676))
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 4. شريط الليزر السفلي المضيء (Minimalist Glowing Laser Line)
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = laserProgress.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF00E676),
                                Color.White
                            )
                        )
                    )
                    .shadow(12.dp, spotColor = Color(0xFF00E676))
            )
        }
    }
}

/**
 * أيقونة العقدة المدارية التي تدور حول المركز عند نصف قطر وزاوية معينة
 */
@Composable
private fun OrbitingNode(
    radius: androidx.compose.ui.unit.Dp,
    angleDeg: Float,
    icon: ImageVector,
    glowColor: Color
) {
    val rad = Math.toRadians(angleDeg.toDouble())
    val offsetX = (cos(rad) * radius.value).dp
    val offsetY = (sin(rad) * radius.value).dp

    Box(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .size(44.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.35f),
                        Color(0xFF07140F).copy(alpha = 0.95f)
                    )
                )
            )
            .border(1.5.dp, glowColor.copy(alpha = 0.7f), CircleShape)
            .shadow(16.dp, CircleShape, spotColor = glowColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = glowColor,
            modifier = Modifier.size(20.dp)
        )
    }
}
