package com.whatsup.automation.presentation.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.presentation.components.GlassmorphicCard
import com.whatsup.automation.presentation.components.GradientButton
import com.whatsup.automation.presentation.theme.*

/**
 * شاشة إنشاء حملة بث جديدة مع ميزة التقسيم الذكي لمنع الحظر.
 */
@Composable
fun CreateCampaignScreen(
    uiState: GroupsUiState,
    onBackClick: () -> Unit,
    onGroupNameChange: (String) -> Unit,
    onGroupDescChange: (String) -> Unit,
    onPartitionModeChange: (Boolean) -> Unit,
    onPartitionSizeChange: (Int) -> Unit,
    onContactSearchChange: (String) -> Unit,
    onToggleContact: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onCreateConfirm: () -> Unit
) {
    val filteredContacts = remember(uiState.deviceContacts, uiState.contactSearchQuery) {
        val list = if (uiState.contactSearchQuery.isBlank()) {
            uiState.deviceContacts
        } else {
            uiState.deviceContacts.filter {
                it.name.contains(uiState.contactSearchQuery, ignoreCase = true) ||
                it.phone.contains(uiState.contactSearchQuery)
            }
        }
        list.sortedBy { it.name.lowercase() }
    }

    val selectedContacts = remember(uiState.deviceContacts, uiState.selectedContactPhones) {
        uiState.deviceContacts.filter { uiState.selectedContactPhones.contains(it.phone) }
    }

    val selectedCount = uiState.selectedContactPhones.size
    val partitionSize = uiState.partitionSize
    val calculatedGroupCount = if (uiState.isPartitionMode && selectedCount > 0 && partitionSize > 0) {
        (selectedCount + partitionSize - 1) / partitionSize
    } else {
        1
    }

    Scaffold(
        containerColor = DarkBgPrimary,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(DarkBgPrimary)
                                .size(40.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = TextPrimary)
                        }

                        Column {
                            Text(
                                text = "حملة بث جديدة",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (selectedCount > 0) "$selectedCount جهة اتصال محددة" else "اختر جهات الاتصال للحملة",
                                fontSize = 12.sp,
                                color = if (selectedCount > 0) WhatsAppGreen else TextSecondary
                            )
                        }
                    }

                    Row {
                        TextButton(onClick = onSelectAll) {
                            Text("تحديد الكل", fontSize = 12.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                        }
                        if (selectedCount > 0) {
                            TextButton(onClick = onClearAll) {
                                Text("إلغاء", fontSize = 12.sp, color = CoralError)
                            }
                        }
                    }
                }

                // فقاعات جهات الاتصال المحددة (Horizontal Scroll مثل واتساب)
                if (selectedContacts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(selectedContacts, key = { it.phone }) { contact ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0x3325D366))
                                    .border(1.dp, WhatsAppGreen.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                    .clickable { onToggleContact(contact.phone) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = contact.name.take(12),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "إزالة",
                                        tint = WhatsAppGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = DarkSurface,
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (calculatedGroupCount > 1) "سيتم إنشاء $calculatedGroupCount بطاقة متساوية" else "سيتم إنشاء بطاقة واحدة",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedCount > 0) CyberCyan else TextMuted
                        )
                        Text(
                            text = "$selectedCount جهة اتصال جاهزة للإدراج",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    GradientButton(
                        text = if (calculatedGroupCount > 1) "إنشاء $calculatedGroupCount بطاقة" else "إنشاء الحملة",
                        onClick = onCreateConfirm,
                        enabled = uiState.newGroupName.isNotBlank() && selectedCount > 0,
                        icon = Icons.Filled.Check
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            // 1. إعدادات اسم الحملة والتقسيم الذكي
            item {
                GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "1. بيانات الحملة والتقسيم",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        OutlinedTextField(
                            value = uiState.newGroupName,
                            onValueChange = onGroupNameChange,
                            label = { Text("اسم الحملة *") },
                            placeholder = { Text("مثال: حملة العملاء أو عروض خاصة") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WhatsAppGreen,
                                unfocusedBorderColor = Color(0x26FFFFFF),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = uiState.newGroupDescription,
                            onValueChange = onGroupDescChange,
                            label = { Text("وصف الحملة (اختياري)") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = Color(0x26FFFFFF),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        HorizontalDivider(color = Color(0x1AFFFFFF))

                        // مفتاح التقسيم الذكي
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x1A06B6D4))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "التقسيم الذكي لدفعات وبطاقات",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan
                                )
                                Text(
                                    text = "تجزئة جهات الاتصال إلى بطاقات متساوية لمنع الحظر",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            Switch(
                                checked = uiState.isPartitionMode,
                                onCheckedChange = onPartitionModeChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = CyberCyan
                                )
                            )
                        }

                        if (uiState.isPartitionMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = if (uiState.partitionSize > 0) uiState.partitionSize.toString() else "",
                                    onValueChange = { str ->
                                        val num = str.toIntOrNull() ?: 100
                                        onPartitionSizeChange(num)
                                    },
                                    label = { Text("حجم كل بطاقة (شخص)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = Color(0x26FFFFFF),
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                if (selectedCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0x2625D366))
                                            .border(1.dp, WhatsAppGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = "$calculatedGroupCount بطاقة",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WhatsAppGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. قسم اختيار جهات الاتصال والبحث
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2. اختيار جهات الاتصال (${filteredContacts.size} متاح):",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.contactSearchQuery,
                    onValueChange = onContactSearchChange,
                    placeholder = { Text("ابحث بالاسم أو رقم الهاتف...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted) },
                    trailingIcon = {
                        if (uiState.contactSearchQuery.isNotBlank()) {
                            IconButton(onClick = { onContactSearchChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "مسح", tint = TextMuted)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = Color(0x26FFFFFF),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            if (filteredContacts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد جهات اتصال مطابقة لبحثك",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                }
            } else {
                items(filteredContacts, key = { it.phone }) { contact ->
                    val isSelected = uiState.selectedContactPhones.contains(contact.phone)
                    GlassmorphicCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleContact(contact.phone) },
                        cornerRadius = 12.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0x3325D366) else Color(0x1F06B6D4)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.name.take(1).ifBlank { "؟" },
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) WhatsAppGreen else CyberCyan
                                    )
                                }

                                Column {
                                    Text(
                                        text = contact.name,
                                        fontSize = 14.sp,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = contact.phone,
                                        fontSize = 12.sp,
                                        color = TextMuted,
                                        style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Ltr)
                                    )
                                }
                            }

                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onToggleContact(contact.phone) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = WhatsAppGreen,
                                    uncheckedColor = TextMuted
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
