package com.whatsup.automation.presentation.screens.logs.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.presentation.components.GlassmorphicCard
import com.whatsup.automation.presentation.theme.*

@Composable
fun LogsDiagnosticsCard(
    diagnostics: List<String>,
    modifier: Modifier = Modifier
) {
    GlassmorphicCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        borderBrush = Brush.linearGradient(listOf(CyberCyan.copy(alpha = 0.4f), Color(0x10FFFFFF)))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل أحداث المحرك والجلسة (Live Engine)",
                    fontSize = 13.sp,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${diagnostics.size} حدث",
                    fontSize = 10.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }

            HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)

            if (diagnostics.isEmpty()) {
                Text(
                    text = "لا توجد أحداث مسجلة من المحرك حتى الآن.",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            } else {
                diagnostics.takeLast(40).reversed().forEach { line ->
                    val isError = line.contains("ERROR") || line.contains("Error")
                    val isSuccess = line.contains("Connected") || line.contains("SUCCESS")
                    Text(
                        text = line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = when {
                            isError -> RoseError
                            isSuccess -> WhatsAppGreenLight
                            else -> TextSecondary
                        },
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
