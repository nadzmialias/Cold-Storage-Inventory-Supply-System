package com.csiss.inventory.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.csiss.inventory.ui.components.CsissLightRefreshIndicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csiss.inventory.ui.components.SummaryStat
import com.csiss.inventory.ui.components.SummaryStatsCard
import java.time.LocalDate
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProductList(
    products: List<Product>,
    today: LocalDate,
    search: String,
    onSearch: (String) -> Unit,
    onAdd: (() -> Unit)?,
    onSelect: (Long) -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    showArchived: Boolean,
    onShowArchived: (Boolean) -> Unit
) {
    var selectedStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }

    val visibleProducts = products.filter { it.isActive != showArchived }
    val categories = visibleProducts
        .map { it.category.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .sortedWith(String.CASE_INSENSITIVE_ORDER)

    val searchTerm = search.trim()
    val hasFilters = selectedStatus != null || selectedCategory != null
    val filtered = visibleProducts.filter { product ->
        product.name.contains(searchTerm, ignoreCase = true) &&
                (selectedCategory == null || product.category.trim() == selectedCategory) &&
                (selectedStatus == null || product.status(today).name == selectedStatus)
    }

    val statusOptions = listOf(
        StockStatus.InStock.name to "In Stock",
        StockStatus.Low.name to "Low Stock",
        StockStatus.Out.name to "Out of Stock",
        StockStatus.Near.name to "Expiring ≤7 Days",
        StockStatus.Expired.name to "Expired"
    )

    val pullToRefreshState = rememberPullToRefreshState()


    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            if (!isRefreshing) onRefresh()
        },
        modifier = Modifier.fillMaxSize(),
        state = pullToRefreshState,
        indicator = {
            CsissLightRefreshIndicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {

    LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "PRODUCT CATALOGUE",
                        color = Muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Inventory",
                        color = Ink,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            item {
                SummaryStatsCard(
                    stats = listOf(
                        SummaryStat(
                            label = "TOTAL ITEMS",
                            value = visibleProducts.size.toString()
                        ),
                        SummaryStat(
                            label = "NEEDS RESTOCK",
                            value = visibleProducts.count { product ->
                                val status = product.status(today)
                                status == StockStatus.Low || status == StockStatus.Out
                            }.toString(),
                            valueColor = Color(0xFFFFB7C5)
                        ),
                        SummaryStat(
                            label = "EXPIRING ≤7D",
                            value = visibleProducts.count {
                                it.status(today) == StockStatus.Near
                            }.toString(),
                            valueColor = Color(0xFF9BE8CA)
                        )
                    ),
                    showPlaceholders = isRefreshing && products.isEmpty()
                )
            }

            item {
                InventoryTabs(
                    activeLabel = "Active (${products.count { it.isActive }})",
                    archivedLabel = "Archived (${products.count { !it.isActive }})",
                    showArchived = showArchived,
                    onActiveClick = {
                        if (showArchived) {
                            selectedStatus = null
                            selectedCategory = null
                            onShowArchived(false)
                        }
                    },
                    onArchivedClick = {
                        if (!showArchived) {
                            selectedStatus = null
                            selectedCategory = null
                            onShowArchived(true)
                        }
                    }
                )
            }

            item {
                Field(search, onSearch, "Search product…")
            }

            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProductFilterMenu(
                            label = "Status",
                            selected = selectedStatus,
                            options = statusOptions,
                            onSelect = { selectedStatus = it },
                            modifier = Modifier.weight(1f)
                        )
                        ProductFilterMenu(
                            label = "Category",
                            selected = selectedCategory,
                            options = categories.map { it to it },
                            onSelect = { selectedCategory = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (hasFilters) {
                        TextButton(
                            onClick = {
                                selectedStatus = null
                                selectedCategory = null
                            }
                        ) {
                            Text("Clear filters")
                        }
                    }
                }
            }

            if (!showArchived && onAdd != null) {
                item { PrimaryButton("+ Add New Item", onAdd) }
            }

            item {
                Text(
                    text = "Products (${filtered.size})",
                    color = Ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            if (filtered.isEmpty() && !isRefreshing) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = White
                    ) {
                        Text(
                            text = if (hasFilters || searchTerm.isNotEmpty()) {
                                "No products match your search or filters."
                            } else {
                                "No products found."
                            },
                            color = Muted,
                            modifier = Modifier.padding(24.dp),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            items(filtered, key = { it.id }) { product ->
                ProductCard(product, today, onClick = { onSelect(product.id) })
            }
        }
    }
}

@Composable
private fun ProductFilterMenu(
    label: String,
    selected: String?,
    options: List<Pair<String, String>>,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second
        ?: selected
        ?: "All"

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, color = Muted, fontSize = 10.sp)
                Text(
                    text = selectedLabel,
                    color = Ink,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text("▾", modifier = Modifier.padding(start = 4.dp), color = Ink)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All") },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )

            options.forEach { (value, title) ->
                DropdownMenuItem(
                    text = { Text(title) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun InventoryTabs(
    activeLabel: String,
    archivedLabel: String,
    showArchived: Boolean,
    onActiveClick: () -> Unit,
    onArchivedClick: () -> Unit
) {
    val tabShape = RoundedCornerShape(8.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(12.dp))
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(activeLabel, archivedLabel).forEachIndexed { index, label ->
            val isArchivedTab = index == 1
            val selected = showArchived == isArchivedTab

            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(tabShape)
                    .background(if (selected) Blue else Color.Transparent)
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = if (isArchivedTab) onArchivedClick else onActiveClick
                    )
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) White else Muted,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}
