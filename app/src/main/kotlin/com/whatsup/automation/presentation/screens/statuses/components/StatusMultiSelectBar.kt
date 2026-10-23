package com.whatsup.automation.presentation.screens.statuses.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.theme.*

@Composable
fun StatusMultiSelectBar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onMuteSelected: () -> Unit,
    onUnmuteSelected: () -> Unit,
    onFavoriteSelected: () -> Unit,
    onRequestDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    BentoCard(
        modifier = modifier.fillMaxWidth(),
        borderBrush = ObsidianGlowBorder.Cyan
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "محدد ($selectedCount)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = CyberCyan
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onSelectAll) {
                    Text("تحديد الكل", fontSize = 11.sp, color = TextPrimary)
                }

                if (selectedCount > 0) {
                    Button(
                        onClick = onMuteSelected,
                        colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("استثناء", fontSize = 10.sp, color = Color.White)
                    }

                    Button(
                        onClick = onUnmuteSelected,
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("تفعيل", fontSize = 10.sp, color = DarkBgPrimary)
                    }

                    Button(
                        onClick = onFavoriteSelected,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("تفضيل", fontSize = 10.sp, color = DarkBgPrimary)
                    }

                    Button(
                        onClick = onRequestDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("حذف", fontSize = 10.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
