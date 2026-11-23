package com.csiss.inventory.ui.stock

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csiss.inventory.data.EnrichedBatch
import com.csiss.inventory.data.ProductItem
import com.csiss.inventory.data.StockRepository
import com.csiss.inventory.data.StockSnapshot
import com.csiss.inventory.data.StockTransaction
import com.csiss.inventory.data.StorageArea
import com.csiss.inventory.data.SupplierItem
import com.csiss.inventory.ui.components.SummaryStat
import com.csiss.inventory.ui.components.SummaryStatsCard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Navy = Color(0xFF132945)
private val Ink = Color(0xFF182B48)
private val Muted = Color(0xFF506583)
private val Background = Color(0xFFF1F6FB)
private val InputBorder = Color(0xFFD8E0EB)
private val CardShape = RoundedCornerShape(20.dp)
private val FieldShape = RoundedCornerShape(12.dp)
private val MalaysiaZone = ZoneId.of("Asia/Kuala_Lumpur")
private val TimeFormat = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH)
private val DateLabelFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(
    currentUserId: String? = null,
    modifier: Modifier = Modifier
) {
    val repository = remember { StockRepository() }
    val scope = rememberCoroutineScope()

    var snapshot by remember { mutableStateOf<StockSnapshot?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var bannerMessage by remember { mutableStateOf<String?>(null) }

    var showHistoryTab by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var batchFilter by remember { mutableStateOf("ALL") } // ALL, LOW, EXPIRING

    var showStockInDialog by remember { mutableStateOf(false) }
    var stockOutTarget by remember { mutableStateOf<EnrichedBatch?>(null) }

    suspend fun reload() {
        snapshot = repository.loadLive()
    }

    fun runTask(work: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                work()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message
                    ?.substringBefore("URL:")
                    ?.substringBefore("Headers:")
                    ?.trim()
                    ?.take(220)
                    ?: "Couldn't complete stock operation."
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(Unit) {
        runTask { reload() }
    }

    val batches = snapshot?.batches.orEmpty()
    val transactions = snapshot?.transactions.orEmpty()
    val lowCount = batches.count { it.isLowStock }
    val expiringCount = batches.count { it.isExpiringSoon }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Navy,
            onPrimary = Color.White,
            surface = Color.White,
            onSurface = Ink
        )
    ) {
        PullToRefreshBox(
            isRefreshing = busy,
            onRefresh = { runTask { reload() } },
            modifier = modifier.fillMaxSize(),
            indicator = {}
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "COLD CHAIN OPERATIONS",
                            color = Muted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Stock Management",
                            color = Ink,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (busy) {
                    item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                }

                bannerMessage?.let { msg ->
                    item {
                        Surface(
                            color = Color(0xFFE6F6EE),
                            shape = FieldShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = msg,
                                    color = Color(0xFF135E3D),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { bannerMessage = null }) {
                                    Text("Dismiss", color = Color(0xFF135E3D))
                                }
                            }
                        }
                    }
                }

                error?.let { msg ->
                    item {
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }
                }

                // Summary Card matching Reorder & Inventory screens
                item {
                    SummaryStatsCard(
                        stats = listOf(
                            SummaryStat("ACTIVE BATCHES", batches.size.toString()),
                            SummaryStat(
                                "LOW STOCK",
                                lowCount.toString(),
                                if (lowCount > 0) Color(0xFFFFB7C5) else Color.White
                            ),
                            SummaryStat(
                                "EXPIRING ≤7D",
                                expiringCount.toString(),
                                if (expiringCount > 0) Color(0xFFFDE68A) else Color(0xFF9BE8CA)
                            )
                        )
                    )
                }

                // Primary Action Buttons: + Stock In & - Stock Out
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showStockInDialog = true },
                            enabled = !busy && snapshot != null,
                            shape = FieldShape,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 46.dp)
                        ) {
                            Text("+ Stock In", fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = {
                                if (batches.isNotEmpty()) {
                                    stockOutTarget = batches.first()
                                }
                            },
                            enabled = !busy && batches.isNotEmpty(),
                            shape = FieldShape,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 46.dp)
                        ) {
                            Text("- Stock Out", color = Navy, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Segmented Tab Bar: Active Batches | Movements
                item {
                    StockTabs(
                        showHistory = showHistoryTab,
                        batchCount = batches.size,
                        historyCount = transactions.size,
                        onSelect = { showHistoryTab = it }
                    )
                }

                if (!showHistoryTab) {
                    // Search & Status Filter Chips
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("Search product, batch code, or storage") },
                                singleLine = true,
                                shape = FieldShape,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = Navy,
                                    unfocusedBorderColor = InputBorder
                                )
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    "ALL" to "All (${batches.size})",
                                    "LOW" to "Low Stock ($lowCount)",
                                    "EXPIRING" to "Expiring ($expiringCount)"
                                ).forEach { (key, label) ->
                                    FilterChip(
                                        selected = batchFilter == key,
                                        onClick = { batchFilter = key },
                                        label = { Text(label, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                    }

                    val q = searchQuery.trim()
                    val filteredBatches = batches.filter { item ->
                        val matchesStatus = when (batchFilter) {
                            "LOW" -> item.isLowStock
                            "EXPIRING" -> item.isExpiringSoon
                            else -> true
                        }
                        val matchesQuery = q.isEmpty() ||
                                item.batch.batchCode.contains(q, ignoreCase = true) ||
                                (item.product?.productName?.contains(q, ignoreCase = true) == true) ||
                                (item.storage?.storageName?.contains(q, ignoreCase = true) == true)
                        matchesStatus && matchesQuery
                    }

                    if (filteredBatches.isEmpty() && !busy) {
                        item {
                            StockCard {
                                Text(
                                    text = "No matching active batches found.",
                                    color = Muted,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    items(filteredBatches, key = { it.batch.batchId }) { item ->
                        BatchCard(
                            item = item,
                            enabled = !busy,
                            onMoveClick = { stockOutTarget = item }
                        )
                    }
                } else {
                    // Movements History Tab
                    val batchMap = batches.associateBy { it.batch.batchId }

                    if (transactions.isEmpty() && !busy) {
                        item {
                            StockCard {
                                Text(
                                    text = "No stock movements recorded yet.",
                                    color = Muted,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    items(transactions, key = { it.movementId }) { tx ->
                        val linkedBatch = batchMap[tx.batchId]
                        TransactionCard(tx = tx, linkedBatch = linkedBatch)
                    }
                }
            }
        }

        // Stock In Dialog
        val currentSnapshot = snapshot
        if (showStockInDialog && currentSnapshot != null) {
            StockInDialog(
                products = currentSnapshot.products,
                storages = currentSnapshot.storageAreas,
                suppliers = currentSnapshot.suppliers,
                busy = busy,
                onDismiss = { showStockInDialog = false },
                onSubmit = { productId, supplierId, storageId, batchCode, qty, expiry, ref, notes ->
                    runTask {
                        val createdTx = repository.receiveStockBatch(
                            userId = currentUserId,
                            productId = productId,
                            supplierId = supplierId,
                            storageId = storageId,
                            batchCode = batchCode,
                            quantity = qty,
                            expiryDate = expiry,
                            referenceCode = ref,
                            notes = notes
                        )
                        showStockInDialog = false
                        bannerMessage = "Stock In saved (Movement ID #${createdTx.movementId})"
                        reload()
                    }
                }
            )
        }

        // Stock Out / Disposal / Adjustment Dialog
        val target = stockOutTarget
        if (target != null) {
            StockOutDialog(
                batches = batches,
                initialBatch = target,
                busy = busy,
                onDismiss = { stockOutTarget = null },
                onSubmit = { selectedBatch, txType, reason, qty, ref, notes ->
                    runTask {
                        repository.recordStockMovement(
                            userId = currentUserId,
                            batchId = selectedBatch.batch.batchId,
                            currentRemaining = selectedBatch.batch.quantityRemaining,
                            transactionType = txType,
                            reason = reason,
                            quantity = qty,
                            referenceCode = ref,
                            notes = notes
                        )
                        stockOutTarget = null
                        bannerMessage = "Stock movement ($txType) recorded."
                        reload()
                    }
                }
            )
        }
    }
}

@Composable
private fun StockTabs(
    showHistory: Boolean,
    batchCount: Int,
    historyCount: Int,
    onSelect: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, FieldShape)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            false to "Active Batches ($batchCount)",
            true to "Movements ($historyCount)"
        ).forEach { (isHistory, label) ->
            val selected = showHistory == isHistory
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) Navy else Color.Transparent)
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = { onSelect(isHistory) }
                    )
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    color = if (selected) Color.White else Muted,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun BatchCard(
    item: EnrichedBatch,
    enabled: Boolean,
    onMoveClick: () -> Unit
) {
    val unit = item.product?.unit ?: "units"
    val qtyText = item.batch.quantityRemaining.toBigDecimal().stripTrailingZeros().toPlainString()

    StockCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.product?.productName ?: "Product #${item.batch.productId}",
                    color = Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Batch ${item.batch.batchCode} · ${item.storage?.storageName ?: "Cold Storage"}",
                    color = Muted,
                    fontSize = 12.sp
                )
            }

            Surface(
                color = Color(0xFFE8EEF6),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "$qtyText $unit",
                    color = Navy,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Expiry: ${item.batch.expiryDate}",
                    color = if (item.isExpiringSoon) Color(0xFF972D41) else Muted,
                    fontSize = 12.sp,
                    fontWeight = if (item.isExpiringSoon) FontWeight.SemiBold else FontWeight.Normal
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (item.isLowStock) {
                        StatusChip("Low Stock", Color(0xFFFBE7EC), Color(0xFF972D41))
                    }
                    if (item.isExpired) {
                        StatusChip("Expired", Color(0xFFFBE7EC), Color(0xFF972D41))
                    } else if (item.isExpiringSoon) {
                        StatusChip("Expiring Soon", Color(0xFFFEF3C7), Color(0xFF92400E))
                    }
                }
            }

            OutlinedButton(
                onClick = onMoveClick,
                enabled = enabled,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Move / Out", color = Navy, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TransactionCard(
    tx: StockTransaction,
    linkedBatch: EnrichedBatch?
) {
    val isIn = tx.transactionType == "IN"
    val badgeBg = if (isIn) Color(0xFFE6F6EE) else Color(0xFFFBE7EC)
    val badgeFg = if (isIn) Color(0xFF135E3D) else Color(0xFF972D41)
    val qtyText = tx.quantity.toBigDecimal().stripTrailingZeros().toPlainString()
    val unit = linkedBatch?.product?.unit ?: "units"

    StockCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusChip(tx.transactionType, badgeBg, badgeFg)
                Text(
                    text = "Movement #${tx.movementId}",
                    color = Navy,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "${if (isIn) "+" else "-"}$qtyText $unit",
                color = badgeFg,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = linkedBatch?.product?.productName ?: "Batch #${tx.batchId}",
            color = Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        val reasonClean = tx.reason?.replace('_', ' ')?.lowercase()
            ?.replaceFirstChar { it.titlecase() } ?: "Stock movement"
        Text(
            text = "$reasonClean · ${formatMytTime(tx.recordedAt)}",
            color = Muted,
            fontSize = 12.sp
        )

        if (!tx.referenceCode.isNullOrBlank()) {
            Text(text = "Ref: ${tx.referenceCode}", color = Muted, fontSize = 12.sp)
        }
        if (!tx.notes.isNullOrBlank()) {
            Text(text = tx.notes, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun StockInDialog(
    products: List<ProductItem>,
    storages: List<StorageArea>,
    suppliers: List<SupplierItem>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Long, Long?, Long, String, Double, String, String?, String?) -> Unit
) {
    val context = LocalContext.current
    var selectedProduct by remember { mutableStateOf(products.firstOrNull()) }
    var selectedStorage by remember { mutableStateOf(storages.firstOrNull()) }
    var selectedSupplier by remember { mutableStateOf<SupplierItem?>(null) }
    var batchCode by remember {
        mutableStateOf("BCH-${LocalDate.now(MalaysiaZone).toString().replace("-", "")}")
    }
    var quantity by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf(LocalDate.now(MalaysiaZone).plusDays(30).toString()) }
    var referenceCode by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val parsedQty = quantity.trim().toDoubleOrNull() ?: 0.0
    val valid = selectedProduct != null && selectedStorage != null && batchCode.isNotBlank() && parsedQty > 0.0

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Record Stock In", fontWeight = FontWeight.Bold, color = Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DropdownSelector(
                    label = "Product",
                    items = products,
                    selected = selectedProduct,
                    itemLabel = { "${it.productName} (${it.unit})" },
                    onSelect = { selectedProduct = it }
                )
                DropdownSelector(
                    label = "Storage Zone",
                    items = storages,
                    selected = selectedStorage,
                    itemLabel = { it.storageName },
                    onSelect = { selectedStorage = it }
                )
                DropdownSelector(
                    label = "Supplier (Optional)",
                    items = suppliers,
                    selected = selectedSupplier,
                    itemLabel = { it.supplierName },
                    onSelect = { selectedSupplier = it }
                )
                OutlinedTextField(
                    value = batchCode,
                    onValueChange = { batchCode = it },
                    label = { Text("Batch Code") },
                    singleLine = true,
                    shape = FieldShape,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity (${selectedProduct?.unit ?: "units"})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = FieldShape,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        val init = runCatching { LocalDate.parse(expiryDate) }.getOrElse { LocalDate.now(MalaysiaZone) }
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> expiryDate = LocalDate.of(y, m + 1, d).toString() },
                            init.year,
                            init.monthValue - 1,
                            init.dayOfMonth
                        ).show()
                    },
                    shape = FieldShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val label = runCatching { LocalDate.parse(expiryDate).format(DateLabelFormat) }.getOrDefault(expiryDate)
                    Text("Expiry Date: $label", color = Ink)
                }
                OutlinedTextField(
                    value = referenceCode,
                    onValueChange = { referenceCode = it },
                    label = { Text("PO / Reorder Reference (Optional)") },
                    singleLine = true,
                    shape = FieldShape,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    singleLine = true,
                    shape = FieldShape,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !busy && valid,
                onClick = {
                    val p = selectedProduct ?: return@Button
                    val s = selectedStorage ?: return@Button
                    onSubmit(
                        p.productId,
                        selectedSupplier?.supplierId,
                        s.storageId,
                        batchCode,
                        parsedQty,
                        expiryDate,
                        referenceCode,
                        notes
                    )
                }
            ) {
                Text(if (busy) "Saving…" else "Save Stock In")
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun StockOutDialog(
    batches: List<EnrichedBatch>,
    initialBatch: EnrichedBatch,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (EnrichedBatch, String, String, Double, String?, String?) -> Unit
) {
    var selectedBatch by remember { mutableStateOf(initialBatch) }
    var txType by remember { mutableStateOf("OUT") }
    var reason by remember { mutableStateOf("SALE_DISPATCH") }
    var quantity by remember { mutableStateOf("") }
    var referenceCode by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val parsedQty = quantity.trim().toDoubleOrNull() ?: 0.0
    val withinLimit = txType == "ADJUSTMENT" || parsedQty <= selectedBatch.batch.quantityRemaining
    val valid = parsedQty > 0.0 && withinLimit

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Record Stock Movement", fontWeight = FontWeight.Bold, color = Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DropdownSelector(
                    label = "Batch",
                    items = batches,
                    selected = selectedBatch,
                    itemLabel = {
                        "${it.product?.productName ?: "Product"} · ${it.batch.batchCode} (${it.batch.quantityRemaining})"
                    },
                    onSelect = { selectedBatch = it }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("OUT" to "Stock Out", "DISPOSAL" to "Disposal", "ADJUSTMENT" to "Adjust").forEach { (type, label) ->
                        FilterChip(
                            selected = txType == type,
                            onClick = {
                                txType = type
                                reason = when (type) {
                                    "OUT" -> "SALE_DISPATCH"
                                    "DISPOSAL" -> "EXPIRED"
                                    else -> "INVENTORY_AUDIT"
                                }
                            },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = {
                        val rem = selectedBatch.batch.quantityRemaining
                        Text(if (txType == "ADJUSTMENT") "New Exact Quantity" else "Quantity (Max: $rem)")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = FieldShape,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!withinLimit && parsedQty > 0.0) {
                    Text(
                        text = "Exceeds available batch stock (${selectedBatch.batch.quantityRemaining}).",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
                OutlinedTextField(
                    value = referenceCode,
                    onValueChange = { referenceCode = it },
                    label = { Text("Reference Code (Optional)") },
                    singleLine = true,
                    shape = FieldShape,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    singleLine = true,
                    shape = FieldShape,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !busy && valid,
                onClick = {
                    onSubmit(selectedBatch, txType, reason, parsedQty, referenceCode, notes)
                }
            ) {
                Text(if (busy) "Saving…" else "Confirm")
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun <T> DropdownSelector(
    label: String,
    items: List<T>,
    selected: T?,
    itemLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = maxWidth
        OutlinedButton(
            onClick = { expanded = true },
            enabled = items.isNotEmpty(),
            shape = FieldShape,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, color = Muted, fontSize = 10.sp)
                Text(
                    text = selected?.let(itemLabel) ?: "Select $label",
                    color = Ink,
                    fontSize = 13.sp
                )
            }
            Text(if (expanded) "▴" else "▾", color = Muted)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(width)
                .heightIn(max = 260.dp)
        ) {
            items.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(itemLabel(entry)) },
                    onClick = {
                        expanded = false
                        onSelect(entry)
                    }
                )
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, bg: Color, fg: Color) {
    Text(
        text = text,
        color = fg,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(bg, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun StockCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = CardShape
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

private fun formatMytTime(raw: String): String =
    runCatching {
        OffsetDateTime.parse(raw).toInstant().atZone(MalaysiaZone).format(TimeFormat) + " MYT"
    }.getOrElse {
        runCatching {
            Instant.parse(raw).atZone(MalaysiaZone).format(TimeFormat) + " MYT"
        }.getOrDefault(raw)
    }