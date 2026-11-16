package com.csiss.inventory.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@Composable
internal fun ProductList(
    products: List<Product>,
    today: LocalDate,
    search: String,
    onSearch: (String) -> Unit,
    onAdd: () -> Unit,
    onSelect: (Long) -> Unit
) {
    val filtered = products.filter { it.name.contains(search.trim(), ignoreCase = true) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Inventory Management", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Manage your products, stock and expiry dates.", color = Muted, fontSize = 12.sp)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Summary("▣", "Total items", products.size, Blue, Color(0xFFEAF3FE), Modifier.weight(1f))
                Summary("⚠", "Low stock", products.count { it.quantity <= it.minimum },
                    Color(0xFFA54E19), Color(0xFFFFF2E7), Modifier.weight(1f))
                Summary("▦", "Expiring soon", products.count { it.status(today) == StockStatus.Near },
                    Red, Color(0xFFFFEDF4), Modifier.weight(1f))
            }
        }
        item {
            Field(search, onSearch, "Search product…")
        }
        item { PrimaryButton("+ Add New Item", onAdd) }
        if (filtered.isEmpty()) item { Text("No products found.", color = Muted) }
        items(filtered, key = { it.id }) { product ->
            ProductCard(product, today, onClick = { onSelect(product.id) })
        }
    }
}

@Composable
private fun Summary(icon: String, label: String, count: Int, color: Color, bg: Color, modifier: Modifier) {
    Column(
        modifier.background(bg, Shape).padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(icon, color = color, fontSize = 19.sp)
        Text(label, color = color, fontSize = 10.sp)
        Text(count.toString(), color = color, fontWeight = FontWeight.Bold, fontSize = 21.sp)
    }
}
