package com.whatsup.automation.presentation.screens.auth.components

import android.os.SystemClock
import androidx.compose.animation.core.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

private data class Node3D(
    val x: Float,
    val y: Float,
    val z: Float,
    val pulseOffset: Float
)

private data class ProjectedNode(
    val px: Float,
    val py: Float,
    val pz: Float,
    val scale: Float
)

private data class MatrixStream(
    var x: Float,
    var y: Float,
    val speed: Float,
    val len: Float,
    val alpha: Float
)

/**
 * النواة التفاعلية ثلاثية الأبعاد للذكاء الاصطناعي (3D Holographic AI Core & Neural Energy Matrix)
 * تحاكي كود الـ Canvas ثلاثي الأبعاد في ملف HTML الأصلي والصورة 1:
 * 1. كرة فيبوناتشي الجيوديسية ذات الـ 78 عقدة
 * 2. دوران مستمر في الفضاء الثلاثي الأبعاد مع تفاعل السحب واللمس (Interactive Pointer Drag)
 * 3. روابط عصبية نيونية (Neural Filaments) بين العقد المتقاربة بتظليل عمقي
 * 4. خطوط بيانات المصفوفة الرقمية الصاعدة (Matrix Streams) في الخلفية
 * 5. خاتم طاقة ترددي نيون متذبذب (Audio/Energy Frequency Wave Ring)
 * 6. ثلاث كبسولات HUD زجاجية عائمة بحركة جيبيّة
 */
@Composable
fun LoginAiCoreCanvas(
    modifier: Modifier = Modifier
) {
    // 1. توليد العقد الثلاثية الأبعاد (78 Fibonacci Nodes)
    val nodes = remember {
        val list = mutableListOf<Node3D>()
        val totalNodes = 78
        val goldenAngle = Math.PI * (3.0 - sqrt(5.0))
        for (i in 0 until totalNodes) {
            val y = 1f - (i.toFloat() / (totalNodes - 1).toFloat()) * 2f
            val radius = sqrt((1f - y * y).coerceAtLeast(0f))
            val theta = goldenAngle * i
            list.add(
                Node3D(
                    x = (cos(theta) * radius).toFloat(),
                    y = y,
                    z = (sin(theta) * radius).toFloat(),
                    pulseOffset = (i * 0.12f) % (2f * Math.PI.toFloat())
                )
            )
        }
        list
    }

    // 2. خطوط مصفوفة البيانات الصاعدة في الخلفية
    val matrixStreams = remember {
        val list = mutableListOf<MatrixStream>()
        for (i in 0 until 22) {
            list.add(
                MatrixStream(
                    x = (i * 0.045f + 0.02f) % 1f,
                    y = (i * 0.08f) % 1f,
                    speed = 0.003f + ((i % 5) * 0.0015f),
                    len = 20f + ((i % 7) * 8f),
                    alpha = 0.15f + ((i % 4) * 0.1f)
                )
            )
        }
        list
    }

    // زوايا الدوران التفاعلية
    var userRotY by remember { mutableStateOf(0f) }
    var userRotX by remember { mutableStateOf(0.25f) }
    var isDragging by remember { mutableStateOf(false) }

    var timeMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = SystemClock.uptimeMillis()
        while (true) {
            timeMs = SystemClock.uptimeMillis() - start
            delay(16)
        }
    }

    val autoTime = (timeMs % 10000000L).toFloat()
    val floatY1 = (sin(timeMs * 0.0028) * 6.0).toFloat()
    val floatY2 = (cos(timeMs * 0.0023) * 6.0).toFloat()
    val floatY3 = (sin(timeMs * 0.0020) * 5.0).toFloat()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(290.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        userRotY += dragAmount.x * 0.012f
                        userRotX += dragAmount.y * 0.012f
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // رسم النواة ثلاثية الأبعاد والمصفوفة والخاتم الترددي
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cw = size.width
            val ch = size.height
            val cx = cw / 2f
            val cy = ch / 2f
            val sphereRadius = min(cw, ch) * 0.35f

            // تحديث زاوية الدوران التلقائي في حال عدم اللمس (دوران سينمائي انسيابي)
            val currentRotY = if (isDragging) userRotY else userRotY + (autoTime * 0.0012f)
            val currentRotX = if (isDragging) userRotX else 0.28f + (sin(autoTime * 0.0008f) * 0.12f)

            // 1. رسم خطوط المصفوفة الرقمية الصاعدة (Matrix Data Streams)
            matrixStreams.forEach { st ->
                st.y -= st.speed
                if (st.y < -0.1f) st.y = 1.1f
                val sx = st.x * cw
                val sy = st.y * ch

                drawLine(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF00E676).copy(alpha = st.alpha),
                            Color(0xFF00E676).copy(alpha = 0f)
                        ),
                        startY = sy,
                        endY = sy + st.len
                    ),
                    start = Offset(sx, sy),
                    end = Offset(sx, sy + st.len),
                    strokeWidth = 1.4.dp.toPx()
                )
            }

            // 2. خاتم الطاقة الترددي المتذبذب حول الكرة (Dynamic Audio/Frequency Wave Ring)
            val wavePath = Path()
            val wavePoints = 64
            for (i in 0..wavePoints) {
                val ang = (i.toFloat() / wavePoints) * (2f * Math.PI.toFloat())
                val waveAmp = (sin(ang * 6f + autoTime * 0.004f) * 5f) + (cos(ang * 4f - autoTime * 0.003f) * 4f)
                val r = (sphereRadius * 1.18f) + waveAmp
                val px = cx + (cos(ang) * r)
                val py = cy + (sin(ang) * r * 0.42f) // ميلان منظوري 3D
                if (i == 0) wavePath.moveTo(px, py) else wavePath.lineTo(px, py)
            }
            drawPath(
                path = wavePath,
                color = Color(0xFF06B6D4).copy(alpha = 0.45f),
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // 3. الإسقاط المنظوري ثلاثي الأبعاد للعقد (3D Projection)
            val cosY = cos(currentRotY)
            val sinY = sin(currentRotY)
            val cosX = cos(currentRotX)
            val sinX = sin(currentRotX)

            val projected = nodes.map { n ->
                // دوران حول محور Y
                val x1 = n.x * cosY - n.z * sinY
                val z1 = n.z * cosY + n.x * sinY
                // دوران حول محور X
                val y2 = n.y * cosX - z1 * sinX
                val z2 = z1 * cosX + n.y * sinX

                val perspective = 2.6f / (2.6f - (z2 * 0.75f))
                ProjectedNode(
                    px = cx + (x1 * sphereRadius * perspective),
                    py = cy + (y2 * sphereRadius * perspective),
                    pz = z2,
                    scale = perspective
                )
            }

            // 4. رسم الروابط العصبية النيونية بين العقد المتقاربة (Neural Filaments)
            val maxDist = sphereRadius * 0.44f
            val count = projected.size
            for (i in 0 until count) {
                val p1 = projected[i]
                for (j in i + 1 until count) {
                    val p2 = projected[j]
                    val dx = p1.px - p2.px
                    val dy = p1.py - p2.py
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist < maxDist) {
                        val depthAlpha = (((p1.pz + p2.pz + 2f) / 4f) * 0.38f).coerceIn(0f, 0.45f)
                        if (depthAlpha > 0.06f) {
                            drawLine(
                                color = Color(0xFF00E676).copy(alpha = depthAlpha),
                                start = Offset(p1.px, p1.py),
                                end = Offset(p2.px, p2.py),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }
                }
            }

            // 5. رسم نقاط العقد النيونية المتوهجة بحسب عمقها المنظوري
            projected.forEach { p ->
                val depthAlpha = (((p.pz + 1.2f) / 2.4f) * 0.85f).coerceIn(0.15f, 1f)
                val nodeRadius = (2.2.dp.toPx() * p.scale).coerceIn(1.2.dp.toPx(), 4.dp.toPx())

                // توهج خافت حول النقطة الأمامية
                if (p.pz > 0.2f) {
                    drawCircle(
                        color = Color(0xFF00E676).copy(alpha = depthAlpha * 0.35f),
                        radius = nodeRadius * 2.4f,
                        center = Offset(p.px, p.py)
                    )
                }

                drawCircle(
                    color = if (p.pz > 0f) Color.White.copy(alpha = depthAlpha) else Color(0xFF00E676).copy(alpha = depthAlpha),
                    radius = nodeRadius,
                    center = Offset(p.px, p.py)
                )
            }
        }

        // 6. ثلاث كبسولات HUD زجاجية عائمة مطابقة للصورة 1
        // كبسولة 1: ⚡ محرك ذكي 24/7 (أعلى اليمين)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-12).dp, y = (16 + floatY1).dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xD90F1A22))
                .border(1.dp, Color(0x6600E676), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E676))
                        .shadow(6.dp, spotColor = Color(0xFF00E676))
                )
                Text(
                    text = "محرك ذكي 24/7",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = Color(0xFFFFA000),
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        // كبسولة 2: 🛡️ تشفير محلي 100% (أعلى اليسار)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 12.dp, y = (42 + floatY2).dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xD90F1A22))
                .border(1.dp, Color(0x6606B6D4), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color(0xFF06B6D4),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "تشفير محلي 100%",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // كبسولة 3: 🔗 بوابة الربط الفوري الآمن نشطة (أسفل الوسط)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-8 + floatY3).dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xD90F1A22))
                .border(1.dp, Color(0x6600E676), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "بوابة الربط الفوري الآمن نشطة",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
