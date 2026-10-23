package com.whatsup.automation.presentation.screens.rules.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.whatsup.automation.domain.model.Rule
import com.whatsup.automation.domain.model.RuleAction
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.components.LiquidGradientButton
import com.whatsup.automation.presentation.theme.*

@Composable
fun RuleFormDialog(
    initialRule: Rule?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        patternValue: String,
        replyMessage: String,
        priority: Int
    ) -> Unit
) {
    var name by remember { mutableStateOf(initialRule?.name ?: "") }
    var patternValue by remember { mutableStateOf(initialRule?.patternValue ?: "") }
    var priorityText by remember { mutableStateOf((initialRule?.priority ?: 1).toString()) }

    val initialReply = remember(initialRule) {
        initialRule?.actions?.firstNotNullOfOrNull {
            when (it) {
                is RuleAction.SendReply -> it.message
                is RuleAction.SaveContactAndReply -> it.replyMessage
            }
        } ?: ""
    }
    var replyMessage by remember { mutableStateOf(initialReply) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            BentoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                borderBrush = ObsidianGlowBorder.Emerald
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (initialRule == null) "إضافة قاعدة رد تلقائي" else "تعديل قاعدة الرد",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم القاعدة *") },
                        placeholder = { Text("مثال: الرد على التحية") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WhatsAppGreen,
                            unfocusedBorderColor = Color(0x26FFFFFF),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = patternValue,
                        onValueChange = { patternValue = it },
                        label = { Text("الكلمات المفتاحية المطلوبة *") },
                        placeholder = { Text("مثال: سلام، مرحبا، هلا") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = Color(0x26FFFFFF),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = replyMessage,
                        onValueChange = { replyMessage = it },
                        label = { Text("نص الرد التلقائي *") },
                        placeholder = { Text("وعليكم السلام ورحمة الله وبركاته...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WhatsAppGreen,
                            unfocusedBorderColor = Color(0x26FFFFFF),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = priorityText,
                        onValueChange = { priorityText = it },
                        label = { Text("ترتيب الأولوية") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = Color(0x26FFFFFF),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Text("إلغاء", color = TextMuted, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        LiquidGradientButton(
                            text = if (initialRule == null) "حفظ القاعدة" else "تحديث القاعدة",
                            onClick = {
                                val prio = priorityText.toIntOrNull() ?: 1
                                onSave(name, patternValue, replyMessage, prio)
                            },
                            enabled = name.isNotBlank() && patternValue.isNotBlank(),
                            icon = Icons.Filled.Check
                        )
                    }
                }
            }
        }
    }
}
