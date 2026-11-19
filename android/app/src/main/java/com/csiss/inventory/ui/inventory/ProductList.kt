package com.csiss.inventory.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
    onSelect: (Long) -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean
) {
    val filtered = products.filter { it.name.contains(search.trim(), ignoreCase = true) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = 20.dp,
            vertical = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "PRODUCT CATALOGUE",
                        color = Color(0xFF718198),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Text("Inventory Management", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onRefresh,
                    enabled = !isRefreshing,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = White),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text("Refresh", fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF141C43), Color(0xFF373C85), Color(0xFF6651A7))),
                        RoundedCornerShape(20.dp)
                    ).padding(horizontal = 8.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Summary("TOTAL ITEMS", products.size, White, Modifier.weight(1f))
                Summary("LOW STOCK", products.count { it.quantity <= it.minimum }, Color(0xFFFFB7C5), Modifier.weight(1f))
                Summary("EXPIRING SOON", products.count { it.status(today) == StockStatus.Near }, Color(0xFF9BE8CA), Modifier.weight(1f))
            }
        }
        item { Field(search, onSearch, "Search product…") }
        item { PrimaryButton("+ Add New Item", onAdd) }
        if (isRefreshing) item {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF6651A7),
                trackColor = Color(0xFFE8DDF8)
            )
        }
        item {
            Text("Products (${filtered.size})", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        if (filtered.isEmpty() && !isRefreshing) item {
            Surface(shape = RoundedCornerShape(20.dp), color = White, modifier = Modifier.fillMaxWidth()) {
                Text("No products found.", color = Muted, modifier = Modifier.padding(24.dp), fontSize = 13.sp)
            }
        }
        items(filtered, key = { it.id }) { product ->
            ProductCard(product, today, onClick = { onSelect(product.id) })
        }
    }
}

@Composable
private fun Summary(label: String, count: Int, color: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(count.toString(), color = color, fontWeight = FontWeight.Bold, fontSize = 25.sp)
        Text(label, color = White, fontSize = 8.sp, maxLines = 1)
    }
}
