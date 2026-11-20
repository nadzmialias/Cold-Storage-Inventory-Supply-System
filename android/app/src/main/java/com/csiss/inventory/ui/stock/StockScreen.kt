package com.csiss.inventory.ui.stock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csiss.inventory.data.*
import com.csiss.inventory.ui.components.SummaryStat
import com.csiss.inventory.ui.components.SummaryStatsCard
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Navy = Color(0xFF132945)
private val IceBlue = Color(0xFF34B9DB)
private val Background = Color(0xFFF1F6FB)
private val TextDark = Color(0xFF182B48)
private val TextMuted = Color(0xFF718198)
private val AlertRed = Color(0xFFE45465)
private val WarningOrange = Color(0xFFE59A23)
private val NormalGreen = Color(0xFF159C76)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen() {
    val repository = remember { StockRepository() }

    var snapshot by remember { mutableStateOf<StockSnapshot?>(null) }
    var loading by remember { mutableStateOf(true) }
    var manualRefreshPending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val pullToRefreshState = rememberPullToRefreshState()
    val requestRefresh: () -> Unit = {
        if (!loading && !manualRefreshPending) {
            manualRefreshPending = true
            refreshTrigger++
        }
    }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Active Batches, 1 = History
    var showStockInDialog by remember { mutableStateOf(false) }
    var batchForStockOut by remember { mutableStateOf<EnrichedBatch?>(null) }


    LaunchedEffect(refreshTrigger) {
        loading = true
        error = null
        try {
            snapshot = repository.loadLive()
        } catch (e: Exception) {
            error = "Unable to load stock data: ${e.message ?: "Check connection"}"
        } finally {
            loading = false
            manualRefreshPending = false
        }
    }


    PullToRefreshBox(
        isRefreshing = manualRefreshPending,
        onRefresh = requestRefresh,
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        state = pullToRefreshState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = manualRefreshPending,
                containerColor = Navy,
                color = Color.White,
                state = pullToRefreshState
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
        ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "INVENTORY & STOCK CONTROL",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Stock",
                        color = TextDark,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Summary Hero Card
            val currentSnapshot = snapshot
            if (currentSnapshot != null) {
                StockSummaryHero(data = currentSnapshot)
            } else {
                SummaryStatsCard(
                    stats = listOf(
                        SummaryStat("ACTIVE BATCHES", "0"),
                        SummaryStat("LOW STOCK", "0"),
                        SummaryStat("EXPIRING ≤7D", "0")
                    ),
                    showPlaceholders = true
                )
            }


            // Action Buttons Row (Stock In / Stock Out)
            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showStockInDialog = true },
                    enabled = snapshot != null && !loading,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Navy,
                        contentColor = Color.White
                    )
                ) {
                    Text("+ Stock In", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = {
                        snapshot?.batches?.firstOrNull()?.let { batchForStockOut = it }
                    },
                    enabled = !snapshot?.batches.isNullOrEmpty() && !loading,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy)
                ) {
                    Text("- Stock Out", fontWeight = FontWeight.SemiBold)
                }
            }

            // Sub-navigation Tabs (Batches vs Movements)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabPill(
                    label = "Active Batches (${snapshot?.batches?.size ?: 0})",
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                TabPill(
                    label = "Movements (${snapshot?.transactions?.size ?: 0})",
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (loading && snapshot == null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        error?.let {
            Text(
                text = it,
                color = AlertRed,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        // Content List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val data = snapshot ?: return@LazyColumn

            if (selectedTab == 0) {
                if (data.batches.isEmpty()) {
                    item {
                        EmptyStateCard("No active inventory batches in cold storage.")
                    }
                } else {
                    items(data.batches, key = { it.batch.batchId }) { item ->
                        BatchItemCard(
                            item = item,
                            onDispatchClick = { batchForStockOut = item }
                        )
                    }
                }
            } else {
                if (data.transactions.isEmpty()) {
                    item {
                        EmptyStateCard("No stock movements recorded yet.")
                    }
                } else {
                    items(data.transactions, key = { it.movementId }) { tx ->
                        TransactionItemCard(tx = tx)
                    }
                }
            }
        }
        }
    }


    // Stock In Dialog
    if (showStockInDialog && snapshot != null) {
        StockInDialog(
            products = snapshot!!.products,
            storageAreas = snapshot!!.storageAreas,
            suppliers = snapshot!!.suppliers,
            onDismiss = { showStockInDialog = false },
            onSubmit = { productId, supplierId, storageId, batchCode, qty, expiry, refCode, notes ->
                repository.receiveStockBatch(
                    productId = productId,
                    supplierId = supplierId,
                    storageId = storageId,
                    batchCode = batchCode,
                    quantity = qty,
                    expiryDate = expiry,
                    referenceCode = refCode,
                    notes = notes
                )
                showStockInDialog = false
                refreshTrigger++
            }
        )
    }

    // Stock Out / Movement Dialog
    if (batchForStockOut != null && snapshot != null) {
        StockOutDialog(
            batches = snapshot!!.batches,
            initialBatch = batchForStockOut!!,
            onDismiss = { batchForStockOut = null },
            onSubmit = { selectedBatch, txType, reason, qty, refCode, notes ->
                repository.recordStockMovement(
                    batchId = selectedBatch.batch.batchId,
                    currentRemaining = selectedBatch.batch.quantityRemaining,
                    transactionType = txType,
                    reason = reason,
                    quantity = qty,
                    referenceCode = refCode,
                    notes = notes
                )
                batchForStockOut = null
                refreshTrigger++
            }
        )
    }
}

// ========================================
// UI SUB-COMPONENTS
// ========================================


@Composable
private fun StockSummaryHero(data: StockSnapshot) {
    val lowStockCount = data.batches.count { it.isLowStock }
    val expiringCount = data.batches.count { it.isExpiringSoon }

    SummaryStatsCard(
        stats = listOf(
            SummaryStat(
                "ACTIVE BATCHES",
                data.batches.size.toString()
            ),
            SummaryStat(
                "LOW STOCK",
                lowStockCount.toString(),
                if (lowStockCount > 0)
                    Color(0xFFFFBEC6)
                else
                    Color(0xFF98F0CD)
            ),
            SummaryStat(
                "EXPIRING ≤7D",
                expiringCount.toString(),
                if (expiringCount > 0)
                    Color(0xFFFDE68A)
                else
                    Color(0xFF98F0CD)
            )
        )
    )
}

@Composable
private fun TabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = if (selected) Navy else Color.Transparent,
        shape = RoundedCornerShape(9.dp)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (selected) Color.White else TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun BatchItemCard(
    item: EnrichedBatch,
    onDispatchClick: () -> Unit
) {
    val unit = item.product?.unit ?: "units"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.product?.productName ?: "Product #${item.batch.productId}",
                        color = TextDark,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Batch: ${item.batch.batchCode} • ${item.storage?.storageName ?: "Unassigned"}",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = String.format(Locale.US, "%.2f %s", item.batch.quantityRemaining, unit),
                    color = if (item.isLowStock) AlertRed else Navy,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Status Badges & Dispatch Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(
                        text = "Exp: ${item.batch.expiryDate}",
                        color = if (item.isExpiringSoon) WarningOrange else TextMuted
                    )
                    if (item.isLowStock) {
                        StatusBadge(text = "LOW STOCK", color = AlertRed)
                    }
                    if (!item.isLowStock && !item.isExpiringSoon) {
                        StatusBadge(text = "OPTIMAL", color = NormalGreen)
                    }
                }

                TextButton(
                    onClick = onDispatchClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Move / Out →",
                        color = IceBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionItemCard(tx: StockTransaction) {
    val isInbound = tx.transactionType == "IN"
    val badgeColor = when (tx.transactionType) {
        "IN" -> NormalGreen
        "OUT" -> Navy
        "DISPOSAL" -> AlertRed
        else -> WarningOrange
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusBadge(text = tx.transactionType, color = badgeColor)
                    Text(
                        text = tx.reason ?: "MOVEMENT",
                        color = TextDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "Batch #${tx.batchId}" +
                            (tx.referenceCode?.let { " • Ref: $it" } ?: ""),
                    color = TextMuted,
                    fontSize = 12.sp
                )
                if (!tx.notes.isNullOrBlank()) {
                    Text(text = tx.notes, color = TextMuted, fontSize = 11.sp)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isInbound) "+" else "-") +
                            String.format(Locale.US, "%.2f", tx.quantity),
                    color = if (isInbound) NormalGreen else AlertRed,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatMovementTime(tx.recordedAt),
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(50.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Text(
            text = message,
            color = TextMuted,
            modifier = Modifier.padding(20.dp)
        )
    }
}

// ========================================
// STOCK-IN DIALOG
// ========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockInDialog(
    products: List<ProductItem>,
    storageAreas: List<StorageArea>,
    suppliers: List<SupplierItem>,
    onDismiss: () -> Unit,
    onSubmit: suspend (
        productId: Long,
        supplierId: Long?,
        storageId: Long,
        batchCode: String,
        qty: Double,
        expiry: String,
        refCode: String?,
        notes: String?
    ) -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedProduct by remember { mutableStateOf(products.firstOrNull()) }
    var selectedStorage by remember { mutableStateOf(storageAreas.firstOrNull()) }
    var selectedSupplier by remember { mutableStateOf(suppliers.firstOrNull()) }

    var batchCode by remember {
        mutableStateOf("BATCH-${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}-${(10..99).random()}")
    }
    var quantityInput by remember { mutableStateOf("") }
    var expiryDateInput by remember { mutableStateOf(LocalDate.now().plusMonths(3).toString()) }
    var referenceCode by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var prodExpanded by remember { mutableStateOf(false) }
    var storExpanded by remember { mutableStateOf(false) }
    var suppExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Stock In (Receive Batch)", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Product Dropdown
                ExposedDropdownMenuBox(
                    expanded = prodExpanded,
                    onExpandedChange = { prodExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedProduct?.productName ?: "Select Product",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Product") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = prodExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = prodExpanded,
                        onDismissRequest = { prodExpanded = false }
                    ) {
                        products.forEach { product ->
                            DropdownMenuItem(
                                text = { Text("${product.productName} (${product.productCode})") },
                                onClick = {
                                    selectedProduct = product
                                    prodExpanded = false
                                }
                            )
                        }
                    }
                }

                // Storage Area Dropdown
                ExposedDropdownMenuBox(
                    expanded = storExpanded,
                    onExpandedChange = { storExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedStorage?.storageName ?: "Select Storage",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cold Storage Zone") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = storExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = storExpanded,
                        onDismissRequest = { storExpanded = false }
                    ) {
                        storageAreas.forEach { area ->
                            DropdownMenuItem(
                                text = { Text(area.storageName) },
                                onClick = {
                                    selectedStorage = area
                                    storExpanded = false
                                }
                            )
                        }
                    }
                }

                // Supplier Dropdown
                ExposedDropdownMenuBox(
                    expanded = suppExpanded,
                    onExpandedChange = { suppExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedSupplier?.supplierName ?: "None",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Supplier") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = suppExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = suppExpanded,
                        onDismissRequest = { suppExpanded = false }
                    ) {
                        suppliers.forEach { supplier ->
                            DropdownMenuItem(
                                text = { Text(supplier.supplierName) },
                                onClick = {
                                    selectedSupplier = supplier
                                    suppExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = batchCode,
                    onValueChange = { batchCode = it },
                    label = { Text("Batch Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = { quantityInput = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = expiryDateInput,
                        onValueChange = { expiryDateInput = it },
                        label = { Text("Expiry (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                }

                OutlinedTextField(
                    value = referenceCode,
                    onValueChange = { referenceCode = it },
                    label = { Text("Reference / DO No. (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMsg?.let {
                    Text(text = it, color = AlertRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = {
                    val qty = quantityInput.trim().toDoubleOrNull()
                    val prod = selectedProduct
                    val stor = selectedStorage
                    if (prod == null || stor == null || qty == null || qty <= 0.0 || batchCode.isBlank()) {
                        errorMsg = "Please complete Product, Storage, Batch Code, and valid Quantity."
                        return@Button
                    }
                    scope.launch {
                        isSaving = true
                        errorMsg = null
                        try {
                            onSubmit(
                                prod.productId,
                                selectedSupplier?.supplierId,
                                stor.storageId,
                                batchCode,
                                qty,
                                expiryDateInput,
                                referenceCode,
                                notes
                            )
                        } catch (e: Exception) {
                            errorMsg = e.message ?: "Failed to save batch."
                        } finally {
                            isSaving = false
                        }
                    }
                }
            ) {
                Text(if (isSaving) "Saving..." else "Confirm Stock In")
            }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ========================================
// STOCK-OUT / MOVEMENT DIALOG
// ========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockOutDialog(
    batches: List<EnrichedBatch>,
    initialBatch: EnrichedBatch,
    onDismiss: () -> Unit,
    onSubmit: suspend (
        selectedBatch: EnrichedBatch,
        txType: String,
        reason: String,
        qty: Double,
        refCode: String?,
        notes: String?
    ) -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedBatch by remember { mutableStateOf(initialBatch) }
    var txType by remember { mutableStateOf("OUT") }
    var reason by remember { mutableStateOf("SALE_DISPATCH") }
    var quantityInput by remember { mutableStateOf("") }
    var referenceCode by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var batchExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }

    val movementOptions = listOf(
        Triple("OUT", "SALE_DISPATCH", "Stock Out (Dispatch)"),
        Triple("DISPOSAL", "SPOILED", "Disposal (Spoiled)"),
        Triple("DISPOSAL", "EXPIRED", "Disposal (Expired)"),
        Triple("ADJUSTMENT", "INVENTORY_AUDIT", "Audit Adjustment (Set Exact Qty)")
    )

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Record Stock Movement", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Batch Selector
                ExposedDropdownMenuBox(
                    expanded = batchExpanded,
                    onExpandedChange = { batchExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${selectedBatch.product?.productName ?: "Batch"} (${selectedBatch.batch.batchCode})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Batch (Rem: ${selectedBatch.batch.quantityRemaining})") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = batchExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = batchExpanded,
                        onDismissRequest = { batchExpanded = false }
                    ) {
                        batches.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Text("${item.product?.productName} • ${item.batch.batchCode} (Rem: ${item.batch.quantityRemaining})")
                                },
                                onClick = {
                                    selectedBatch = item
                                    batchExpanded = false
                                }
                            )
                        }
                    }
                }

                // Movement Type & Reason Selector
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "$txType — $reason",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Movement Type & Reason") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        movementOptions.forEach { (type, rsn, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    txType = type
                                    reason = rsn
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = { quantityInput = it },
                    label = {
                        Text(
                            if (txType == "ADJUSTMENT") "New Exact Quantity"
                            else "Quantity to Deduct (Max ${selectedBatch.batch.quantityRemaining})"
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = referenceCode,
                    onValueChange = { referenceCode = it },
                    label = { Text("Reference / Invoice No. (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMsg?.let {
                    Text(text = it, color = AlertRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = {
                    val qty = quantityInput.trim().toDoubleOrNull()
                    if (qty == null || qty <= 0.0) {
                        errorMsg = "Enter a valid positive quantity."
                        return@Button
                    }
                    if (txType != "ADJUSTMENT" && qty > selectedBatch.batch.quantityRemaining) {
                        errorMsg = "Exceeds remaining batch stock (${selectedBatch.batch.quantityRemaining})."
                        return@Button
                    }
                    scope.launch {
                        isSaving = true
                        errorMsg = null
                        try {
                            onSubmit(selectedBatch, txType, reason, qty, referenceCode, notes)
                        } catch (e: Exception) {
                            errorMsg = e.message ?: "Failed to record movement."
                        } finally {
                            isSaving = false
                        }
                    }
                }
            ) {
                Text(if (isSaving) "Saving..." else "Submit Movement")
            }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatMovementTime(value: String): String {
    return runCatching {
        OffsetDateTime.parse(value)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale.ENGLISH))
    }.getOrElse { value }
}