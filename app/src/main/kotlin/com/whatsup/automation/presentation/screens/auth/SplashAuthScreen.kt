package com.whatsup.automation.presentation.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.presentation.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * شاشة سبلاش سكرين + تسجيل الدخول السينمائية المشتركة (Splash & Login Harmonic Experience).
 * تجمع بين:
 * 1) السبلاش سكرين (الصورة 2): شعار Sigma، مدارات نيون، 3 كبسولات عائمة (رسائل، مستخدم، صاعقة).
 * 2) شاشة تسجيل الدخول (الصورة 1): أزرار «تسجيل الدخول» و «ربط حساب جديد»، تذييل الصفحة وتوهج الشعار.
 * 3) حركة انسيابية فائقة السلاسة (Shared Animation & Fluid Transition) تربط الاثنين بروعة بصرية 60/120fps.
 */
enum class SplashPhase {
    SPLASH_ORBITAL, // المرحلة الأولى: مدارات السبلاش الأيقونية العائمة
    LOGIN_INTERACTIVE // المرحلة الثانية: شاشة الدخول وأزرار الربط التفاعلية
}

@Composable
fun SplashAuthScreen(
    onLoginSuccess: () -> Unit,
    onLinkNewAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // حالة المرحلة الحالية (تبدأ بالسبلاش وتتحول تلقائياً أو بالنقر إلى الدخول)
    var currentPhase by remember { mutableStateOf(SplashPhase.SPLASH_ORBITAL) }

    // مؤقت التحول السلس التلقائي من السبلاش إلى الدخول بعد 2.4 ثانية
    LaunchedEffect(Unit) {
        delay(2400)
        if (currentPhase == SplashPhase.SPLASH_ORBITAL) {
            currentPhase = SplashPhase.LOGIN_INTERACTIVE
        }
    }

    // حركة حية مستمرة للمدارات والأيقونات العائمة
    val infiniteTransition = rememberInfiniteTransition(label = "OrbitalFloating")
    val orbitPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    val floatAnim1 by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Float1"
    )

    val floatAnim2 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Float2"
    )

    // تحولات الانتقال بين السبلاش والدخول
    val isLogin = currentPhase == SplashPhase.LOGIN_INTERACTIVE

    val orbitalAlpha by animateFloatAsState(
        targetValue = if (isLogin) 0.08f else 1f,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "OrbitalAlpha"
    )

    val orbitalScale by animateFloatAsState(
        targetValue = if (isLogin) 1.25f else 1f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "OrbitalScale"
    )

    val logoScale by animateFloatAsState(
        targetValue = if (isLogin) 0.92f else 1.08f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "LogoScale"
    )

    val buttonsOffset by animateFloatAsState(
        targetValue = if (isLogin) 0f else 120f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "ButtonsOffset"
    )

    val buttonsAlpha by animateFloatAsState(
        targetValue = if (isLogin) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 200, easing = LinearOutSlowInEasing),
        label = "ButtonsAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF060D12)) // أسود كربوني نيلي فائق العمق مطابق للصور
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // إمكانية تخطي السبلاش فورا بالنقر
                if (currentPhase == SplashPhase.SPLASH_ORBITAL) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    currentPhase = SplashPhase.LOGIN_INTERACTIVE
                }
            }
    ) {
        // 1. هالة النيون الخلفية الخافتة (Aurora Ambient Glow)
        Box(
            modifier = Modifier
                .size(450.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-60).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x2800E676),
                            Color(0x0F00C853),
                            Color.Transparent
                        )
                    )
                )
        )

        // 2. المدارات والكبسولات العائمة التفاعلية (Orbital Graphics)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(orbitalScale)
                .alpha(orbitalAlpha)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = Offset(size.width * 0.5f, size.height * 0.65f)
                val baseRadius = size.width * 0.42f * orbitPulse

                // رسم أقواس المدارات النيونية المنحنية
                drawArc(
                    color = Color(0x2200E676),
                    startAngle = 140f,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - baseRadius, centerOffset.y - baseRadius),
                    size = androidx.compose.ui.geometry.Size(baseRadius * 2, baseRadius * 2),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )

                val innerRadius = baseRadius * 0.72f
                drawArc(
                    color = Color(0x1800E676),
                    startAngle = 100f,
                    sweepAngle = 240f,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - innerRadius, centerOffset.y - innerRadius),
                    size = androidx.compose.ui.geometry.Size(innerRadius * 2, innerRadius * 2),
                    style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                )

                val outerRadius = baseRadius * 1.25f
                drawArc(
                    color = Color(0x1200E676),
                    startAngle = 180f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - outerRadius, centerOffset.y - outerRadius),
                    size = androidx.compose.ui.geometry.Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // الكبسولة 1: كبسولة الرسائل (أعلى اليمين)
            OrbitalBadge(
                icon = Icons.Default.Forum,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = 100.dp, y = (40 + floatAnim1).dp)
            )

            // الكبسولة 2: كبسولة المستخدم/الملف الشخصي (اليسار)
            OrbitalBadge(
                icon = Icons.Default.Person,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = (-100).dp, y = (100 + floatAnim2).dp)
            )

            // الكبسولة 3: كبسولة الصاعقة/الأتمتة (أسفل اليمين)
            OrbitalBadge(
                icon = Icons.Default.Bolt,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = 90.dp, y = (160 + floatAnim1).dp)
            )
        }

        // 3. المحتوى الرأسي الأساسي (الشعار، العنوان، الأزرار)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // الجزء العلوي: الشعار والعناوين
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                // الشعار الأيقوني المتوهج (Sigma / Infinity Glowing Ribbon)
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(logoScale),
                    contentAlignment = Alignment.Center
                ) {
                    // توهج خلفي ناعم للشعار
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0x4400E676), Color.Transparent)
                                )
                            )
                    )

                    // رسم شعار Sigma / Infinity Loop المنحني المتوهج
                    SigmaInfinityIcon(
                        modifier = Modifier.size(72.dp),
                        isInfinity = isLogin
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // اسم التطبيق: Sigma
                Text(
                    text = "Sigma",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )

                // السطر الفرعي المميز: AutoFlow 2.0 (أخضر نيون + أبيض)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "AutoFlow",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhatsAppGreenLight
                    )
                    Text(
                        text = "2.0",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // العبارة الوصفية الترحيبية (تتحول بنعومة بين السبلاش والدخول)
                Crossfade(
                    targetState = isLogin,
                    animationSpec = tween(400),
                    label = "TaglineCrossfade"
                ) { showLoginText ->
                    Text(
                        text = if (showLoginText) {
                            "مرحباً بك في تجربة الأتمتة الذكية"
                        } else {
                            "أتمتة ذكية .. تواصل باحتراف"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // الجزء السفلي: أزرار الدخول وتذييل الصفحة (المرحلة 3)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = buttonsOffset.dp)
                    .alpha(buttonsAlpha)
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // الزر الأساسي: «تسجيل الدخول» كبسولي زمردي نيون فخم مطابق للصورة 1
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onLoginSuccess()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(16.dp, RoundedCornerShape(27.dp), spotColor = Color(0x8800E676)),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00C853)
                        )
                    ) {
                        Text(
                            text = "تسجيل الدخول",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // الزر الثانوي: «ربط حساب جديد» كبسولي زجاجي مع أيقونة الباركود
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onLinkNewAccount()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(27.dp))
                            .background(Color(0xCC0F1A22)),
                        shape = RoundedCornerShape(27.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300E676)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ربط حساب جديد",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = WhatsAppGreenLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // عبارة التذييل الأنيقة: تجربة مختلفة.. تبدأ من هنا ⭐
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "⭐ تجربة محترفة.. تبدأ من هنا",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        // زر إعادة تشغيل حركة السبلاش في الزاوية العلوية لمنح المستخدم تجربة بصرية ممتعة
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                currentPhase = if (currentPhase == SplashPhase.SPLASH_ORBITAL) {
                    SplashPhase.LOGIN_INTERACTIVE
                } else {
                    SplashPhase.SPLASH_ORBITAL
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x1F00E676))
        ) {
            Icon(
                imageVector = if (isLogin) Icons.Default.PlayArrow else Icons.Default.Check,
                contentDescription = "تبديل المعاينة",
                tint = WhatsAppGreenLight,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * الكبسولة المدارية العائمة (Orbital Floating Badge).
 * دائرة زجاجية عائمة تحمل أيقونة خضراء نيونية بتوهج خفيف.
 */
@Composable
fun OrbitalBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0x3300E676),
                        Color(0xCC0F1A22)
                    )
                )
            )
            .border(1.2.dp, Color(0x4D00E676), CircleShape)
            .shadow(12.dp, CircleShape, spotColor = Color(0x6600E676)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = WhatsAppGreenLight,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * رسم شعار Sigma المتوهج المنحني أو رمز Infinity بواسطة Canvas بدقة فائقة.
 */
@Composable
fun SigmaInfinityIcon(
    modifier: Modifier = Modifier,
    isInfinity: Boolean = false
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val strokeWidth = 5.dp.toPx()
        val gradientBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF00E676),
                Color(0xFF00BFA5),
                Color(0xFF00C853)
            ),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )

        val path = Path()

        if (isInfinity) {
            // رسم رمز المالانهاية الانسيابي ∞
            val cx = w / 2f
            val cy = h / 2f
            val r = w * 0.22f

            path.moveTo(cx, cy)
            path.cubicTo(cx - r, cy - r * 1.5f, cx - r * 2.2f, cy - r, cx - r * 2.2f, cy)
            path.cubicTo(cx - r * 2.2f, cy + r, cx - r, cy + r * 1.5f, cx, cy)
            path.cubicTo(cx + r, cy - r * 1.5f, cx + r * 2.2f, cy - r, cx + r * 2.2f, cy)
            path.cubicTo(cx + r * 2.2f, cy + r, cx + r, cy + r * 1.5f, cx, cy)
        } else {
            // رسم حرف S المستقبلي الخاص بـ Sigma AutoFlow
            path.moveTo(w * 0.75f, h * 0.22f)
            path.cubicTo(w * 0.35f, h * 0.12f, w * 0.2f, h * 0.35f, w * 0.5f, h * 0.5f)
            path.cubicTo(w * 0.8f, h * 0.65f, w * 0.65f, h * 0.88f, w * 0.25f, h * 0.78f)
        }

        drawPath(
            path = path,
            brush = gradientBrush,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}
