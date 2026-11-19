package com.csiss.inventory.ui.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import java.time.LocalDate

@Composable
internal fun ProductDetails(product: Product, today: LocalDate, back: () -> Unit,
                            edit: () -> Unit, delete: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { ScreenTitle("Product details", "Product catalogue / PRD-${product.id.toString().padStart(4, '0')}", back) }
        item { ProductCard(product, today) }
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = White,
                border = BorderStroke(
                    1.dp,
                    Color(0xFFE5E7EB)
                )
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Stock information", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    DetailLine("Total available", "${product.quantity.display()} ${product.unit}")
                    DetailLine("Minimum stock", "${product.minimum.display()} ${product.unit}")
                    DetailLine("Nearest expiry", product.expiry?.format(DateFormat) ?: "—")
                    DetailLine("Storage", product.storage.ifBlank { "—" })
                    DetailLine("Supplier", product.supplier.ifBlank { "—" })
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = White) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Description", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(product.description.ifBlank { "—" }, color = Muted, fontSize = 13.sp, lineHeight = 21.sp)
                }
            }
        }
        item { PrimaryButton("Edit product", edit) }
        item {
            OutlinedButton(
                onClick = delete, modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Red.copy(alpha = 0.35f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)
            ) { Text("Delete product",fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}


@Composable
private fun DetailLine(
    label: String,
    value: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Text(
            text = value,
            color = Ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp
        )
    }
}
