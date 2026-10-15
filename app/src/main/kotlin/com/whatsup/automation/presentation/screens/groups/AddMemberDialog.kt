package com.whatsup.automation.presentation.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
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
import com.whatsup.automation.presentation.components.GradientButton
import com.whatsup.automation.presentation.theme.*

/**
 * نافذة حوار إضافة جهة اتصال إلى المجموعة أو حملة البث.
 */
@Composable
fun AddMemberDialog(
    uiState: GroupsUiState,
    onDismiss: () -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleContact: (String) -> Unit,
    onCustomNameChange: (String) -> Unit,
    onCustomPhoneChange: (String) -> Unit,
    onConfirmFromContacts: () -> Unit,
    onConfirmCustom: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = من الهاتف, 1 = إدخال يدوي

    val currentMemberPhones = remember(uiState.selectedGroupMembers) {
        uiState.selectedGroupMembers.map { it.phone }.toSet()
    }

    val availableContacts = remember(uiState.deviceContacts, uiState.addMemberSearchQuery, currentMemberPhones) {
        uiState.deviceContacts.filter { !currentMemberPhones.contains(it.phone) }.let { list ->
            if (uiState.addMemberSearchQuery.isBlank()) list
            else list.filter {
                it.name.contains(uiState.addMemberSearchQuery, ignoreCase = true) ||
                it.phone.contains(uiState.addMemberSearchQuery)
            }
        }.sortedBy { it.name.lowercase() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = "إضافة جهة اتصال للبطاقة",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // تبويبات طريقة الإضافة
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBgPrimary)
                        .padding(2.dp)
                ) {
                    TabButton(
                        title = "من دفتر الهاتف",
                        count = 0,
                        isSelected = selectedTab == 0,
                        tooltipText = "استيراد واختيار جهات الاتصال من دفتر الهاتف",
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    TabButton(
                        title = "إدخال يدوي",
                        count = 0,
                        isSelected = selectedTab == 1,
                        tooltipText = "كتابة الاسم ورقم الهاتف يدوياً وإضافته للحملة",
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = uiState.addMemberSearchQuery,
                        onValueChange = onSearchChange,
                        placeholder = { Text("ابحث في جهات الاتصال...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = Color(0x26FFFFFF),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    if (availableContacts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا توجد جهات اتصال متاحة للإضافة", fontSize = 13.sp, color = TextMuted)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(availableContacts, key = { it.phone }) { contact ->
                                val isSelected = uiState.selectedMembersToAdd.contains(contact.phone)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onToggleContact(contact.phone) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { onToggleContact(contact.phone) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = WhatsAppGreen,
                                                uncheckedColor = TextMuted
                                            )
                                        )
                                        Column {
                                            Text(text = contact.name, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                                            Text(
                                                text = contact.phone,
                                                fontSize = 11.sp,
                                                color = TextMuted,
                                                style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Ltr)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = uiState.customAddName,
                        onValueChange = onCustomNameChange,
                        label = { Text("الاسم *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WhatsAppGreen,
                            unfocusedBorderColor = Color(0x26FFFFFF),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = uiState.customAddPhone,
                        onValueChange = onCustomPhoneChange,
                        label = { Text("رقم الهاتف (مع رمز الدولة) *") },
                        placeholder = { Text("+967...") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WhatsAppGreen,
                            unfocusedBorderColor = Color(0x26FFFFFF),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            }
        },
        confirmButton = {
            if (selectedTab == 0) {
                GradientButton(
                    text = "إضافة المحدد (${uiState.selectedMembersToAdd.size})",
                    onClick = onConfirmFromContacts,
                    enabled = uiState.selectedMembersToAdd.isNotEmpty(),
                    icon = Icons.Filled.Check
                )
            } else {
                GradientButton(
                    text = "إضافة الرقم",
                    onClick = onConfirmCustom,
                    enabled = uiState.customAddName.isNotBlank() && uiState.customAddPhone.isNotBlank(),
                    icon = Icons.Filled.Check
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = TextMuted)
            }
        }
    )
}
