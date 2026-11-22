package com.csiss.inventory.ui.supplier

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.csiss.inventory.ui.components.SummaryStat
import com.csiss.inventory.ui.components.SummaryStatsCard

@Composable
internal fun SupplierList(
    suppliers: List<Supplier>,
    search: String,
    onSearch: (String) -> Unit,
    onAdd: () -> Unit,
    onSelect: (Long) -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    hasLoadedData: Boolean
) {
    // Client-side filtering keeps full stats card constant
    val filtered = if (search.isBlank()) {
        suppliers
    } else {
        val query = search.trim().lowercase()
        suppliers.filter { s ->
            s.name.lowercase().contains(query) ||
                    s.contactPerson.lowercase().contains(query) ||
                    s.phone.lowercase().contains(query) ||
                    s.email.lowercase().contains(query)
        }
    }

    val totalCount = suppliers.size
    val activeCount = suppliers.count { it.isActive }
    val inactiveCount = totalCount - activeCount

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 12.dp,
            bottom = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title & Refresh
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "SUPPLIER CATALOGUE",
                        color = Color(0xFF718198),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Supplier",
                        color = Ink,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Stats Card (always full numbers)
        item {
            SummaryStatsCard(
                stats = listOf(
                    SummaryStat("TOTAL SUPPLIERS", totalCount.toString()),
                    SummaryStat("ACTIVE", activeCount.toString()),
                    SummaryStat("INACTIVE", inactiveCount.toString())
                ),
                showPlaceholders = !hasLoadedData
            )
        }


        // Search Bar
        item {
            SupplierField(
                value = search,
                onChange = onSearch,
                label = "Search supplier..."
            )
        }

        // + Add New Supplier Button
        item {
            PrimaryButton(
                label = "+ Add New Supplier",
                onClick = onAdd
            )
        }

        // Section header
        item {
            Text(
                text = "Suppliers (${filtered.size})",
                color = Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        if (filtered.isEmpty() && !isRefreshing) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No suppliers found",
                        color = Muted,
                        modifier = Modifier.padding(24.dp),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // List of Suppliers (includes both active and inactive)
        itemsIndexed(filtered, key = { _, s -> s.id }) { index, supplier ->
            SupplierCard(
                supplier = supplier,
                index = index,
                onClick = { onSelect(supplier.id) }
            )
        }
    }
}

@Composable
private fun SupplierSummary(label: String, count: Int, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = count.toString(),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp
        )
        Text(
            text = label,
            color = Color(0xFFC4B5FD),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(Color(0xFF7C3AED))
    )
}
