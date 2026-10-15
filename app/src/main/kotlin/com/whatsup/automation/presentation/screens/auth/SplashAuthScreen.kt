package com.whatsup.automation.presentation.screens.auth

import android.os.SystemClock
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import kotlinx.coroutines.delay
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.whatsup.automation.data.security.LicenseState
import com.whatsup.automation.presentation.screens.activation.ActivationViewModel
import com.whatsup.automation.presentation.screens.auth.components.LoginAiCoreCanvas
import com.whatsup.automation.presentation.screens.auth.components.SplashRadarView
import com.whatsup.automation.presentation.screens.auth.components.SubscriptionAuthDialog
import com.whatsup.automation.presentation.theme.*

/**
 * مراحل واجهة البداية والدخول السينمائية (Splash & Login Harmonic Phases)
 */
enum class SplashPhase {
    SPLASH_ORBITAL,   // مرحلة السبلاش: الرادار المداري وشعار اللانهاية
    LOGIN_INTERACTIVE // مرحلة الدخول: نواة الذكاء الاصطناعي 3D ونظام الاشتراكات
}

/**
 * شاشة البداية وتسجيل الدخول المتكاملة (Sigma 2.0 AutoFlow Splash & Auth Screen).
 * مطابقة بنسبة 100% للنموذج الأولي في sigma_autoflow_v2_0 (1).html وللصور المرفقة:
 * - شاشة البداية: شعار اللانهاية ∞ المتوهج + رادار متعدد الحلقات + موجات سونار + 3 أيقونات مدارية + شريط ليزر للتقدم.
 * - شاشة تسجيل الدخول: شعار حرف S السيبراني المحاط بإطار سداسي وماسي دوّار + نواة 3D AI Core مع 78 عقدة تفاعلية قابلة للسحب + 3 كبسولات HUD.
 * - تسجيل الدخول عبر نظام الاشتراكات الفعلي: فحص حالة الترخيص وإتاحة تفعيل الاشتراك بكود رسمي أو الدخول الفوري إذا كان الاشتراك نشطاً.
 */
@Composable
fun SplashAuthScreen(
    onLoginSuccess: () -> Unit,
    onLinkNewAccount: () -> Unit = {},
    modifier: Modifier = Modifier,
    activationViewModel: ActivationViewModel = hiltViewModel()
) {
    val haptic = LocalHapticFeedback.current
    val licenseState by activationViewModel.licenseState.collectAsState()

    var currentPhase by remember { mutableStateOf(SplashPhase.SPLASH_ORBITAL) }
    var showActivationDialog by remember { mutableStateOf(false) }

    // تقدم مؤشر ليزر السبلاش (3 ثوانٍ كاملة بدقة على ساعة الهاردوير الحقيقية)
    var laserProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentPhase) {
        if (currentPhase == SplashPhase.SPLASH_ORBITAL) {
            val durationMs = 3000L
            val startTime = SystemClock.uptimeMillis()
            while (true) {
                val elapsed = SystemClock.uptimeMillis() - startTime
                val fraction = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                laserProgress = fraction
                if (fraction >= 1f) break
                delay(16)
            }
            delay(120)
            currentPhase = SplashPhase.LOGIN_INTERACTIVE
        }
    }

    val isLogin = currentPhase == SplashPhase.LOGIN_INTERACTIVE

    // مؤقت زمني حي حقيقي لا يعتمد على أنظمة الأندرويد لضمان استمرار دوران ونبض الحركات 60fps
    var animTimeMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = SystemClock.uptimeMillis()
        while (true) {
            animTimeMs = SystemClock.uptimeMillis() - start
            delay(16)
        }
    }

    val floatTextY = (kotlin.math.sin(animTimeMs * 0.0028) * 4.0).toFloat()
    val btnPulseScale = 1.0f + (kotlin.math.sin(animTimeMs * 0.004) * 0.02).toFloat()
    val btnGlowSweep = ((animTimeMs % 2200L).toFloat() / 2200f)

    // تحولات انسيابية
    val contentAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(600),
        label = "ContentAlpha"
    )

    val buttonsOffset by animateFloatAsState(
        targetValue = if (isLogin) 0f else 80f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "ButtonsOffset"
    )

    val buttonsAlpha by animateFloatAsState(
        targetValue = if (isLogin) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 150),
        label = "ButtonsAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040B08)) // الخلفية الليلية العميقة المطابقة للـ HTML
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // النقر في مرحلة السبلاش يتخطاه فوراً لمرحلة الدخول
                if (currentPhase == SplashPhase.SPLASH_ORBITAL) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    currentPhase = SplashPhase.LOGIN_INTERACTIVE
                }
            }
    ) {
        // 1. هالة النيون المحيطية العلوية (Ambient Aurora Glow)
        Box(
            modifier = Modifier
                .size(460.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-80).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x3300E676),
                            Color(0x1000BFA5),
                            Color.Transparent
                        )
                    )
                )
        )

        // 2. المحتوى الرئيسي المقسم
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
                modifier = Modifier.padding(top = 28.dp)
            ) {
                // الشعار التفاعلي: اللانهاية في السبلاش أو حرف S السيبراني في الدخول
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .clickable {
                            // النقر على الشعار في شاشة الدخول يعيد عرض السبلاش
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentPhase = if (isLogin) SplashPhase.SPLASH_ORBITAL else SplashPhase.LOGIN_INTERACTIVE
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(
                        targetState = isLogin,
                        animationSpec = tween(500),
                        label = "LogoCrossfade"
                    ) { showLoginLogo ->
                        if (showLoginLogo) {
                            // شعار حرف S السيبراني المحاط بإطار ماسي/سداسي دوّار (مطابق للصورة 1)
                            CyberSMonogram(
                                modifier = Modifier.size(86.dp),
                                animTimeMs = animTimeMs
                            )
                        } else {
                            // شعار اللانهاية المتوهج المنحني (مطابق للصورة 2)
                            GlowingInfinityLogo(
                                modifier = Modifier.size(80.dp),
                                animTimeMs = animTimeMs
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // اسم التطبيق الرئيسي مع اهتزاز ميكرو انسيابي
                Text(
                    text = "Sigma",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.offset(y = (-floatTextY).dp)
                )

                // السطر الفرعي الفاخر: AutoFlow (بدون 2.0 وبحركة انسيابية)
                Text(
                    text = "AutoFlow",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E676),
                    modifier = Modifier
                        .offset(y = floatTextY.dp)
                        .padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // النص الوصفي المتغير بنعومة
                Crossfade(
                    targetState = isLogin,
                    animationSpec = tween(400),
                    label = "TaglineCrossfade"
                ) { showLoginText ->
                    Text(
                        text = if (showLoginText) "مرحباً بك في تجربة الأتمتة الذكية" else "أتمتة ذكية .. تواصل باحتراف",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // الجزء الأوسط: التبديل بين رادار السبلاش ونواة الذكاء الاصطناعي ثلاثية الأبعاد
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(
                    targetState = isLogin,
                    animationSpec = tween(600),
                    label = "CenterAnimationCrossfade"
                ) { showLoginAnim ->
                    if (showLoginAnim) {
                        // نواة الذكاء الاصطناعي ثلاثية الأبعاد التفاعلية + كبسولات HUD
                        LoginAiCoreCanvas(modifier = Modifier.fillMaxWidth())
                    } else {
                        // رادار السبلاش متعدد الحلقات + شريط الليزر
                        SplashRadarView(
                            laserProgress = laserProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // الجزء السفلي: أزرار الدخول وربط الحساب ونظام الاشتراكات (يظهر في مرحلة الدخول)
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // شارة الاشتراك النشط إن وُجد (مستوحى من نظام التراخيص)
                    if (licenseState is LicenseState.Active) {
                        val active = licenseState as LicenseState.Active
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0x2600E676))
                                .border(1.dp, Color(0x6600E676), RoundedCornerShape(20.dp))
                                .clickable { showActivationDialog = true }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "الاشتراك نشط (متبقي ${active.remainingDays} يوماً) 🛡️",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E676)
                                )
                            }
                        }
                    }

                    // الزر الأساسي: «تسجيل الدخول» زمردي نيون فخم بحركة نبضية وشعاع متوهج
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (licenseState is LicenseState.Active) {
                                // اشتراك نشط: دخول فوري
                                onLoginSuccess()
                            } else {
                                // غير مفعل أو منتهٍ: فتح نافذة تفعيل الاشتراك
                                showActivationDialog = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .scale(btnPulseScale)
                            .shadow(24.dp, RoundedCornerShape(28.dp), spotColor = Color(0x9900E676))
                            .border(
                                width = 1.5.dp,
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF00E676).copy(alpha = 0.3f),
                                        Color.White.copy(alpha = 0.9f),
                                        Color(0xFF00E676).copy(alpha = 0.3f)
                                    ),
                                    startX = (btnGlowSweep * 700f) - 350f,
                                    endX = (btnGlowSweep * 700f) + 100f
                                ),
                                shape = RoundedCornerShape(28.dp)
                            ),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00C853)
                        )
                    ) {
                        Text(
                            text = if (licenseState is LicenseState.Active) "تسجيل الدخول" else "تسجيل الدخول بكود الاشتراك",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // نافذة إدارة وتفعيل الاشتراكات الزجاجية الفاخرة
        if (showActivationDialog) {
            SubscriptionAuthDialog(
                viewModel = activationViewModel,
                onDismiss = { showActivationDialog = false },
                onActivationSuccess = {
                    showActivationDialog = false
                    onLoginSuccess()
                }
            )
        }
    }
}

/**
 * رسم شعار اللانهاية المتوهج (∞) لشاشة البداية
 */
@Composable
private fun GlowingInfinityLogo(
    modifier: Modifier = Modifier,
    animTimeMs: Long = 0L
) {
    val pulseScale = 1.0f + (kotlin.math.sin(animTimeMs * 0.0028) * 0.05f).toFloat()

    Canvas(modifier = modifier.scale(pulseScale)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = w * 0.22f

        val path = Path().apply {
            moveTo(cx, cy)
            cubicTo(cx - r, cy - r * 1.5f, cx - r * 2.2f, cy - r, cx - r * 2.2f, cy)
            cubicTo(cx - r * 2.2f, cy + r, cx - r, cy + r * 1.5f, cx, cy)
            cubicTo(cx + r, cy - r * 1.5f, cx + r * 2.2f, cy - r, cx + r * 2.2f, cy)
            cubicTo(cx + r * 2.2f, cy + r, cx + r, cy + r * 1.5f, cx, cy)
        }

        // مسار نيون متوهج عريض
        drawPath(
            path = path,
            color = Color(0x5500E676),
            style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
        )

        // تيار البلازما الأساسي
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF34D399), Color(0xFF00E676), Color(0xFF06B6D4)),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            ),
            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
        )

        // سلك النور الأبيض في المركز
        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.85f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * رسم شعار حرف S السيبراني المحاط بإطار سداسي وماسي دوّار لشاشة الدخول (مطابق للصورة 1)
 */
@Composable
private fun CyberSMonogram(
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
