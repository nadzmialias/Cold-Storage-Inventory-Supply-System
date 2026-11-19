package com.csiss.inventory.ui.supplier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val Ink = Color(0xFF182B48)
internal val Muted = Color(0xFF718198)
internal val Blue = Color(0xFF132945)
internal val Background = Color(0xFFF1F6FB)
internal val Line = Color(0xFFE0E6EF)
internal val White = Color.White
internal val Red = Color(0xFFE45465)
internal val RedBorder = Color(0xFFFDA4AF)
internal val Green = Color(0xFF059669)
internal val GreenBg = Color(0xFFD1FAE5)
internal val GrayBadge = Color(0xFF4B5563)
internal val GrayBadgeBg = Color(0xFFE5E7EB)
internal val Yellow = Color(0xFFFBBF24)
internal val Shape = RoundedCornerShape(14.dp)

private val CardColors = listOf(
    Color(0xFF3B82F6),
    Color(0xFF10B981),
    Color(0xFF8B5CF6),
    Color(0xFFF59E0B),
    Color(0xFFEC4899),
    Color(0xFF06B6D4),
    Color(0xFF6366F1)
)

@Composable
internal fun SupplierCard(
    supplier: Supplier,
    index: Int,
    onClick: (() -> Unit)? = null
) {
    val modifier = Modifier.fillMaxWidth()
    val avatarColor = if (!supplier.isActive) {
        Color(0xFF9CA3AF)
    } else {
        CardColors[index % CardColors.size]
    }

    Surface(
        modifier = if (onClick == null) modifier else modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = White,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Squircle Initials Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(avatarColor, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = supplier.initials,
                    color = White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            // Details
            Column(Modifier.weight(1f)) {
                Text(
                    text = supplier.name,
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = supplier.contactPerson.ifBlank { "General Contact" },
                    color = Muted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
                Text(
                    text = supplier.phone.ifBlank { "No phone number" },
                    color = Muted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

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
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )

            if (onClick != null) {
                Text(
                    text = " ›",
                    color = Color(0xFF34B9DB),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
internal fun SupplierField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    keyboard: KeyboardType = KeyboardType.Text,
    placeholder: String = "",
    isError: Boolean = false,
    errorMessage: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            placeholder = if (placeholder.isEmpty()) null else { { Text(placeholder) } },
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            singleLine = singleLine,
            minLines = minLines,
            isError = isError,
            shape = Shape,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = White,
                unfocusedContainerColor = White,
                unfocusedBorderColor = Line,
                focusedBorderColor = Blue,
                focusedTextColor = Ink,
                unfocusedTextColor = Ink,
                focusedLabelColor = Blue,
                unfocusedLabelColor = Muted,
                errorBorderColor = Red
            )
        )
        if (isError && errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Red,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
internal fun SupplierScreenTitle(
    title: String,
    caption: String,
    back: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(
            onClick = back,
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("‹ Back", color = Blue, fontSize = 14.sp)
        }
        Text(
            text = title,
            color = Ink,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = caption,
            color = Muted,
            fontSize = 12.sp
        )
    }
}

@Composable
internal fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    textColor: Color = White
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Blue,
            contentColor = textColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = textColor
        )
    }
}

@Composable
internal fun OutlinedDeleteButton(
    label: String = "Delete",
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, RedBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = Red
        )
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Red
        )
    }
}
