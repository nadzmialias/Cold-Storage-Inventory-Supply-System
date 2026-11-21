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
import java.time.LocalDate

@Composable
internal fun ProductDetails(product: Product, today: LocalDate, back: () -> Unit,
                            edit: () -> Unit, delete: () -> Unit,
                            archive: () -> Unit, restore: () -> Unit,
                            busy: Boolean = false) {
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { ScreenTitle("Product details", "Product catalogue / PRD-${product.id.toString().padStart(4, '0')}", back) }
        item { ProductCard(product, today) }
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = White) {
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
        if (product.isActive) {
            item {
                Button(onClick = edit, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
                    Text("Edit product")
                }
            }
            item {
                OutlinedButton(onClick = archive, enabled = product.canArchive && !busy,
                    modifier = Modifier.fillMaxWidth().height(48.dp), shape = Shape) {
                    Text("Archive product", fontWeight = FontWeight.SemiBold)
                }
                if (!product.canArchive) {
                    Text("Clear remaining stock before archiving.", color = Muted,
                        fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        } else {
            item {
                Text("Archived product", color = Muted, fontWeight = FontWeight.SemiBold)
                Button(onClick = restore, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(48.dp), shape = Shape,
                    colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
                    Text("Restore product")
                }
            }
        }
        if (product.canDelete) item {
            OutlinedButton(
                onClick = delete, enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Red.copy(alpha = 0.35f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)
            ) { Text("Delete product", fontWeight = FontWeight.SemiBold) }
        }

    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(1f), color = Muted, fontSize = 12.sp)
        Text(value, Modifier.weight(1f), color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
    HorizontalDivider(color = Line)
}
