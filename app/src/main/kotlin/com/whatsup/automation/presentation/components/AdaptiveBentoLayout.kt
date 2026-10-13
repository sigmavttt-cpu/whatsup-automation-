package com.whatsup.automation.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * غلاف تخطيط بنتو التكيفي (Adaptive Bento Layout).
 * يتحقق تلقائياً من عرض الشاشة (Breakpoint: 600dp)؛
 * إذا كان الهاتف عمودياً، يُعرض المحتوى في عمود واحد،
 * وإذا كان جهازاً لوحياً (Tablet) أو شاشة عريضة، يوفر إمكانية التوزيع في عمودين متجاورين.
 */
@Composable
fun AdaptiveBentoContainer(
    modifier: Modifier = Modifier,
    tabletBreakpoint: Dp = 600.dp,
    content: @Composable (isExpanded: Boolean) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isExpanded = maxWidth >= tabletBreakpoint
        content(isExpanded)
    }
}

/**
 * حاوية صف ثنائية للشاشات اللوحية أو عمودية للهواتف
 */
@Composable
fun ResponsiveBentoRow(
    modifier: Modifier = Modifier,
    spacing: Dp = 16.dp,
    leftContent: @Composable BoxScope.() -> Unit,
    rightContent: @Composable BoxScope.() -> Unit
) {
    AdaptiveBentoContainer(modifier = modifier) { isExpanded ->
        if (isExpanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                Box(modifier = Modifier.weight(1f)) { leftContent() }
                Box(modifier = Modifier.weight(1f)) { rightContent() }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) { leftContent() }
                Box(modifier = Modifier.fillMaxWidth()) { rightContent() }
            }
        }
    }
}
