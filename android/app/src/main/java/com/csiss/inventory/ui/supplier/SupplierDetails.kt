package com.csiss.inventory.ui.supplier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SupplierDetails(
    supplier: Supplier,
    back: () -> Unit,
    edit: () -> Unit,
    toggleActive: () -> Unit,
    delete: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back Button & Screen Title
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = back,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("‹ Back", color = Ink, fontSize = 14.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Supplier Details",
                    color = Ink,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Hero Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = White,
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Initials Avatar
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .background(
                                if (supplier.isActive) Color(0xFF3B82F6) else Color(0xFF9CA3AF),
                                RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = supplier.initials,
                            color = White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }

                    // Company Name
                    Text(
                        text = supplier.name,
                        color = Ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    // Status Badge
                    Text(
                        text = if (supplier.isActive) "Active" else "Inactive",
                        color = if (supplier.isActive) Green else GrayBadge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .background(
                                if (supplier.isActive) GreenBg else GrayBadgeBg,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Info Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = White,
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DetailSection("CONTACT PERSON", supplier.contactPerson.ifBlank { "N/A" })
                    HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 8.dp))

                    DetailSection("PHONE NUMBER", supplier.phone.ifBlank { "N/A" })
                    HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 8.dp))

                    DetailSection("EMAIL ADDRESS", supplier.email.ifBlank { "N/A" })
                    HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 8.dp))

                    DetailSection("ADDRESS", supplier.address.ifBlank { "N/A" })
                }
            }
        }

        // Action Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Edit Button
                PrimaryButton(
                    label = "Edit",
                    onClick = edit
                )

                // Set as Inactive / Active Button
                PrimaryButton(
                    label = if (supplier.isActive) "Set as Inactive" else "Set as Active",
                    onClick = toggleActive,
                    textColor = if (supplier.isActive) Yellow else Color(0xFF34D399)
                )

                // Delete Button
                OutlinedDeleteButton(
                    label = "Delete",
                    onClick = delete
                )
            }
        }
    }
}

@Composable
private fun DetailSection(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            color = Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            color = Ink,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}
