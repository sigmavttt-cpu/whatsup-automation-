package com.whatsup.automation.presentation.screens.chatshub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.whatsup.automation.domain.model.VaultItem
import com.whatsup.automation.domain.model.VaultMediaType
import com.whatsup.automation.presentation.components.AppEmptyState
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DeletedMessagesDialog(
    deletedMessages: List<VaultItem>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            BentoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                borderBrush = ObsidianGlowBorder.Cyan
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x22E11D48)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(20.dp))
                            }
                            Text(
                                text = "الرسائل المحذوفة لدى الجميع",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextMuted, modifier = Modifier.size(22.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (deletedMessages.isEmpty()) {
                        AppEmptyState(
                            icon = Icons.Filled.DeleteSweep,
                            title = "لا توجد رسائل محذوفة حالياً",
                            subtitle = "أي رسالة يتم حذفها من قبل الطرف الآخر ستظهر هنا مع توقيتها الأصلي."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(deletedMessages, key = { it.id }) { item ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DarkBgTertiary)
                                        .border(1.dp, Color(0x22E11D48), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.senderName.ifBlank { item.senderPhone },
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                            Text(
                                                text = timeFormat.format(Date(item.timestamp)),
                                                fontSize = 11.sp,
                                                color = TextMuted,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Text(
                                            text = item.originalText ?: "رسالة محذوفة",
                                            fontSize = 12.sp,
                                            color = Color(0xFFE11D48),
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ViewOnceVaultDialog(
    mediaVaultItems: List<VaultItem>,
    onDismiss: () -> Unit,
    onOpenMedia: (VaultItem) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            BentoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                borderBrush = ObsidianGlowBorder.Emerald
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(WhatsAppGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(20.dp))
                            }
                            Text(
                                text = "وسائط العرض لمرة واحدة المحفوظة",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextMuted, modifier = Modifier.size(22.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (mediaVaultItems.isEmpty()) {
                        AppEmptyState(
                            icon = Icons.Filled.Lock,
                            title = "لا توجد وسائط محفوظة حالياً",
                            subtitle = "أي صورة أو فيديو بخاصية العرض لمرة واحدة يتم تخزينها هنا تلقائياً قبل أن تحترق."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(mediaVaultItems, key = { it.id }) { item ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DarkBgTertiary)
                                        .border(1.dp, Color(0x3300E676), RoundedCornerShape(12.dp))
                                        .clickable { onOpenMedia(item) }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = when (item.mediaType) {
                                                    VaultMediaType.VIDEO -> Icons.Filled.Videocam
                                                    VaultMediaType.AUDIO -> Icons.Filled.Mic
                                                    else -> Icons.Filled.Image
                                                },
                                                contentDescription = null,
                                                tint = WhatsAppGreenLight,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = item.senderName.ifBlank { item.senderPhone },
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = item.getFormattedSize(),
                                                    fontSize = 10.sp,
                                                    color = TextMuted,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = { onOpenMedia(item) },
                                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("فتح", fontSize = 11.sp, color = DarkBgPrimary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
