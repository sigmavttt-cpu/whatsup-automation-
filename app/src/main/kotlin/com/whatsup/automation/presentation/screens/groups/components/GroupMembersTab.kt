package com.whatsup.automation.presentation.screens.groups.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.PersonOff
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
import com.whatsup.automation.domain.model.GroupMember
import com.whatsup.automation.presentation.components.AppEmptyState
import com.whatsup.automation.presentation.components.GlassmorphicCard
import com.whatsup.automation.presentation.theme.*

@Composable
fun MembersListTab(
    members: List<GroupMember>,
    onRemoveMember: (Long) -> Unit,
    onOpenAddMember: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (members.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            contentAlignment = Alignment.Center
        ) {
            AppEmptyState(
                icon = Icons.Outlined.PersonOff,
                title = "لا يوجد أعضاء في هذه المجموعة حالياً",
                subtitle = "قم بإضافة جهات اتصال لبدء توجيه الرسائل والحملات لهم",
                actionLabel = "إضافة أعضاء الآن",
                onActionClick = onOpenAddMember
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "قائمة جهات الاتصال المسجلة بالبطاقة:",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )

                    TextButton(
                        onClick = onOpenAddMember,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة جهة اتصال", fontSize = 12.sp, color = WhatsAppGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(members, key = { it.id }) { member ->
                MemberRowItem(
                    member = member,
                    onRemove = { onRemoveMember(member.id) }
                )
            }
        }
    }
}

@Composable
fun MemberRowItem(
    member: GroupMember,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassmorphicCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x2606B6D4)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.contactName.take(1).ifBlank { "؟" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                }

                Column {
                    Text(
                        text = member.contactName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
                    ) {
                        Text(
                            text = member.phone,
                            fontSize = 12.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // زر الإزالة الأحمر مع touch target مريح
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x33F43F5E))
                        .border(1.dp, CoralError, CircleShape)
                        .size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = "إزالة العضو",
                        tint = CoralError,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
