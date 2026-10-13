package com.whatsup.automation.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.whatsup.automation.presentation.theme.DarkBgSecondary
import com.whatsup.automation.presentation.theme.DarkBgTertiary

/**
 * تأثير الوميض الهيكلي الناعم (Shimmer Effect Modifier)
 * يمنح المستخدم إحساساً انسيابياً بأن البيانات قادمة بدلاً من شاشات الانتظار البيضاء.
 */
fun Modifier.shimmerLoading(
    durationMillis: Int = 1300,
    baseColor: Color = DarkBgSecondary.copy(alpha = 0.7f),
    highlightColor: Color = Color.White.copy(alpha = 0.6f)
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslation"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            baseColor,
            highlightColor,
            baseColor
        ),
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    background(brush)
}

/**
 * بطاقة بنتو عظمية وميضية (Bento Skeleton Card) تحاكي بطاقات البيانات أثناء التحميل
 */
@Composable
fun BentoSkeletonCard(
    modifier: Modifier = Modifier,
    height: Dp = 100.dp,
    cornerRadius: Dp = 24.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .shimmerLoading()
    )
}

/**
 * قائمة بطاقات عظمية متعددة للتحميل السريع (Skeleton Loading List)
 */
@Composable
fun BentoSkeletonList(
    modifier: Modifier = Modifier,
    itemCount: Int = 4,
    itemHeight: Dp = 86.dp,
    spacing: Dp = 12.dp
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        repeat(itemCount) {
            BentoSkeletonCard(height = itemHeight)
        }
    }
}
