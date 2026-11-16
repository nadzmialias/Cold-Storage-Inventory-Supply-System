package com.csiss.inventory.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.storage.storage
import java.time.LocalDate

@Composable
internal fun ProductDetails(product: Product, today: LocalDate, back: () -> Unit,
                           edit: () -> Unit, delete: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenTitle("Product details", "Product catalogue / PRD-${product.id.toString().padStart(4, '0')}", back) }
        item { ProductCard(product, today) }
        item {
            DetailLine("Total available", "${product.quantity.display()} ${product.unit}")
            DetailLine("Minimum stock", "${product.minimum.display()} ${product.unit}")
            DetailLine("Nearest expiry", product.expiry?.format(DateFormat) ?: "—")
            DetailLine("Storage", product.storage.ifBlank { "—" })
            DetailLine("Supplier", product.supplier.ifBlank { "—" })
        }
        item {
            Text("Description", fontWeight = FontWeight.Bold)
            Text(product.description.ifBlank { "—" }, color = Muted)
        }
        item { PrimaryButton("Edit product", edit) }
        item {
            OutlinedButton(onClick = delete, modifier = Modifier.fillMaxWidth(), shape = Shape) {
                Text("Delete product")
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(label, Modifier.weight(1f), color = Muted, fontSize = 13.sp)
        Text(value, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
    HorizontalDivider(color = Line)
}
