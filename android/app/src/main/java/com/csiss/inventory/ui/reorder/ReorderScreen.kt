package com.csiss.inventory.ui.reorder

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
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
import com.csiss.inventory.data.Reorder
import com.csiss.inventory.data.ReorderHistory
import com.csiss.inventory.data.ReorderOption
import com.csiss.inventory.data.ReorderOptions
import com.csiss.inventory.data.ReorderRepository
import com.csiss.inventory.data.loadInventoryProducts
import com.csiss.inventory.ui.inventory.Product
import com.csiss.inventory.ui.inventory.ProductCard
import com.csiss.inventory.ui.inventory.StockStatus
import com.csiss.inventory.ui.inventory.status
import com.csiss.inventory.ui.components.SummaryStat
import com.csiss.inventory.ui.components.SummaryStatsCard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
private val Finished = setOf("RECEIVED", "REJECTED", "CANCELLED", "ADMIN_REJECTED")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderScreen(modifier: Modifier = Modifier) {
    val repository = remember { ReorderRepository() }
    val scope = rememberCoroutineScope()
    var options by remember { mutableStateOf<ReorderOptions?>(null) }
    var rows by remember { mutableStateOf(emptyList<Reorder>()) }
    var history by remember { mutableStateOf(emptyList<ReorderHistory>()) }
    var requesterName by remember { mutableStateOf<String?>(null) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var inventoryProducts by remember { mutableStateOf<List<Product>?>(null) }
    var showProducts by remember { mutableStateOf(true) }
    var productSearch by remember { mutableStateOf("") }
    var initialProduct by remember { mutableStateOf<ReorderOption?>(null) }
    val productListState = rememberLazyListState()
    val requestListState = rememberLazyListState()
    val formState = rememberLazyListState()
    val detailsState = rememberLazyListState()
    var editing by remember { mutableStateOf(false) }
    var historyOnly by remember { mutableStateOf(false) }
    var statusFilter by remember(historyOnly) { mutableStateOf<String?>(null) }
    var priorityFilter by remember { mutableStateOf<String?>(null) }
    var dateFrom by remember { mutableStateOf("") }
    var dateTo by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var action by remember { mutableStateOf<String?>(null) }

    suspend fun loadDetails(id: Long) {
        val events = repository.history(id)
        val name = if (options?.role in setOf("ADMIN", "STAFF")) {
            repository.requesterName(id)
        } else null
        history = events
        requesterName = name
    }

    suspend fun reload() {
        val updatedOptions = repository.options()
        options = updatedOptions
        rows = repository.requests()
        selectedId?.let { loadDetails(it) }
        inventoryProducts = if (updatedOptions.role in setOf("ADMIN", "STAFF")) {
            loadInventoryProducts(active = true)
        } else null
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
                    ?.substringBefore("Code:")
                    ?.trim()
                    ?.take(240)
                    ?: "Couldn't complete the request."
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(Unit) {
        runTask { reload() }
    }

    val selected = rows.firstOrNull { it.id == selectedId }
    val data = options
    val supplierView = data?.role == "SUPPLIER"
    val allowedToEdit = selected != null && data != null && selected.createdBy == data.userId &&
        when (data.role) {
            "ADMIN" -> selected.status in setOf("PENDING_APPROVAL", "PENDING")
            "STAFF" -> selected.status == "PENDING_APPROVAL"
            else -> false
        }

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
            onRefresh = {
                if (action == null) {
                    runTask { reload() }
                }
            },
            modifier = Modifier.fillMaxSize(),
            indicator = {}
        ) {
            LazyColumn(
                state = when {
                    editing -> formState
                    selected != null -> detailsState
                    showProducts && !supplierView -> productListState
                    else -> requestListState
                },
                modifier = modifier.fillMaxSize().background(Background),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (supplierView) "SUPPLIER PORTAL" else "PURCHASING",
                                color = Muted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = when {
                                    editing && selected == null -> "Create reorder"
                                    editing -> "Edit reorder"
                                    selected != null -> "Reorder details"
                                    supplierView -> "Reorder Requests"
                                    else -> "Reorder Management"
                                },
                                color = Ink,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (busy) {
                    item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                }

                error?.let { message ->
                    item { Text(message, color = MaterialTheme.colorScheme.error) }
                }

                if (data != null && editing) {
                    item {
                        ReorderForm(
                            request = selected,
                            initialProduct = initialProduct,
                            data = data,
                            rows = rows,
                            busy = busy,
                            onBack = {
                                editing = false
                                initialProduct = null
                            }
                        ) { product, supplier, quantity, priority, reason ->
                            runTask {
                                repository.save(selected, product, supplier, quantity, priority, reason)
                                editing = false
                                initialProduct = null
                                showProducts = false
                                historyOnly = false
                                reload()
                            }
                        }
                    }
                } else if (data != null && selected != null) {
                    item {
                        TextButton(
                            enabled = !busy,
                            onClick = {
                                selectedId = null
                                requesterName = null
                                history = emptyList()
                            }
                        ) {
                            Text("‹ Back to requests")
                        }
                        ReorderCard {
                            val productName = data.products
                                .firstOrNull { it.id == selected.productId }?.name
                                ?: "Unavailable"
                            val supplierName = data.suppliers
                                .firstOrNull { it.id == selected.supplierId }?.name
                                ?: "Unavailable"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("RO-${selected.id}", fontWeight = FontWeight.Bold)
                                if (allowedToEdit) {
                                    var menuOpen by remember(selected.id) { mutableStateOf(false) }
                                    Box {
                                        TextButton(enabled = !busy, onClick = { menuOpen = true }) {
                                            Text("More actions")
                                        }
                                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                            DropdownMenuItem(
                                                text = { Text("Edit request") },
                                                enabled = !busy,
                                                onClick = {
                                                    menuOpen = false
                                                    editing = true
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Cancel request") },
                                                enabled = !busy,
                                                onClick = {
                                                    menuOpen = false
                                                    action = "CANCELLED"
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            Text(statusLabel(selected.status), color = Navy)
                            PriorityBadge(selected.priority)
                            Text("Requested  ${requestTime(selected.createdAt)}")
                            if (!supplierView) {
                                val role = selected.createdByRole?.let { statusLabel(it) }
                                val requestedBy = when {
                                    requesterName != null && role != null -> "$requesterName ($role)"
                                    requesterName != null -> requesterName
                                    role != null -> role
                                    else -> "Name unavailable"
                                }
                                Text("Requested by  $requestedBy")
                            } else {
                                selected.createdByRole?.let { Text("Requested by  ${statusLabel(it)}") }
                            }
                            Text("Product  $productName")
                            Text("Supplier  $supplierName")
                            Text("Quantity  ${quantityLabel(selected, data)}")
                            Text("Expected delivery  ${selected.date ?: "To be confirmed"}")
                            if (selected.reference.isNotBlank()) {
                                Text("Reference  ${selected.reference}")
                            }
                            if (selected.notes.isNotBlank()) {
                                Text(selected.notes)
                            }
                            if (selected.additionalReason.isNotBlank()) {
                                Text("Additional order reason  ${selected.additionalReason}")
                            }
                            selected.receiptId?.let { Text("Stock In movement  #$it") }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val actions = when {
                                supplierView -> supplierReorderActions(selected.status)
                                data.role == "ADMIN" && selected.status == "PENDING_APPROVAL" ->
                                    listOf("APPROVED" to "Approve and send", "ADMIN_REJECTED" to "Reject request")

                                selected.status == "DELIVERED" ->
                                    listOf("RECEIVED" to "Link Stock In receipt")

                                else -> emptyList()
                            }

                            actions.forEach { (status, label) ->
                                if (status == "ADMIN_REJECTED") {
                                    TextButton(enabled = !busy, onClick = { action = status }) {
                                        Text(label, color = MaterialTheme.colorScheme.error)
                                    }
                                } else {
                                    ReorderButton(label, !busy) { action = status }
                                }
                            }

                            Text("Status history", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    items(history, key = { it.id }) { event ->
                        ReorderCard {
                            Text(statusLabel(event.status), fontWeight = FontWeight.SemiBold)
                            Text(requestTime(event.changedAt), color = Muted)
                            if (event.note.isNotBlank()) Text(event.note)
                        }
                    }
                } else if (data != null && showProducts && !supplierView) {
                    val today = LocalDate.now(MalaysiaZone)
                    val catalogue = inventoryProducts.orEmpty()
                    val optionsById = data.products.filter { it.active }.associateBy { it.id }

                    item {
                        SummaryStatsCard(
                            stats = listOf(
                                SummaryStat(
                                    "NEED RESTOCK",
                                    inventoryProducts?.count {
                                        val status = it.status(today)
                                        status == StockStatus.Low || status == StockStatus.Out
                                    }?.toString() ?: "—",
                                    Color(0xFFFFB7C5)
                                ),
                                SummaryStat("PENDING", rows.count { it.status in PendingStatuses }.toString()),
                                SummaryStat(
                                    "IN PROGRESS",
                                    rows.count { it.status in ProgressStatuses }.toString(),
                                    Color(0xFF9BE8CA)
                                )
                            )
                        )
                    }
                    item {
                        ReorderButton("View reorder requests", !busy) { showProducts = false }
                        Spacer(Modifier.height(12.dp))
                        ReorderField(productSearch, { productSearch = it }, "Search products", !busy)
                    }
                    val query = productSearch.trim()
                    val products = catalogue.filter {
                        it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true)
                    }
                    if (inventoryProducts != null && products.isEmpty()) {
                        item { ReorderCard { Text("No active products found.", color = Muted) } }
                    }
                    items(products, key = { it.id }) { product ->
                        val option = optionsById[product.id]
                        ProductCard(
                            product = product,
                            today = today,
                            onClick = if (busy || option == null) null else {
                                {
                                    initialProduct = option
                                    editing = true
                                }
                            }
                        )
                    }
                } else if (data != null) {
                    item { ReorderSummary(rows) }

                    item {
                        ReorderTabs(
                            historyOnly = historyOnly,
                            activeCount = rows.count { it.status !in Finished },
                            historyCount = rows.count { it.status in Finished },
                            onSelect = { historyOnly = it }
                        )
                    }

                    if (!supplierView) {
                        item {
                            ReorderButton("Browse products", !busy) { showProducts = true }
                        }
                    }

                    val tabRows = rows.filter { (it.status in Finished) == historyOnly }
                    val from = parseDate(dateFrom)
                    val to = parseDate(dateTo)
                    val validDates = (dateFrom.isBlank() || from != null) &&
                            (dateTo.isBlank() || to != null) &&
                            (from == null || to == null || !from.isAfter(to))

                    item {
                        ReorderFilters(
                            statuses = if (historyOnly) Finished.toList() else ActiveStatuses,
                            status = statusFilter,
                            priority = priorityFilter,
                            from = dateFrom,
                            to = dateTo,
                            validDates = validDates,
                            onStatus = { statusFilter = it },
                            onPriority = { priorityFilter = it },
                            onFrom = { dateFrom = it },
                            onTo = { dateTo = it },
                            onClear = {
                                statusFilter = null
                                priorityFilter = null
                                dateFrom = ""
                                dateTo = ""
                            }
                        )
                    }

                    val visible = tabRows.filter { row ->
                        val day = requestInstant(row.createdAt)?.atZone(MalaysiaZone)?.toLocalDate()
                        validDates &&
                            (statusFilter == null || row.status == statusFilter) &&
                            (priorityFilter == null || row.priority == priorityFilter) &&
                            (from == null || (day != null && !day.isBefore(from))) &&
                            (to == null || (day != null && !day.isAfter(to)))
                    }.sortedWith(
                        compareByDescending<Reorder> { supplierView && !historyOnly && it.priority == "URGENT" }
                            .thenByDescending { requestInstant(it.createdAt) }
                            .thenByDescending { it.id }
                    )

                    item {
                        Text(
                            text = "Requests (${visible.size})",
                            color = Ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (visible.isEmpty() && !busy) {
                        item {
                            ReorderCard {
                                Text(
                                    text = if (!validDates) {
                                        "Check the date range."
                                    } else if (tabRows.isNotEmpty()) {
                                        "No requests match these filters."
                                    } else if (historyOnly) {
                                        "No completed requests yet."
                                    } else {
                                        "No active requests yet."
                                    },
                                    color = Muted
                                )
                            }
                        }
                    }

                    items(visible, key = { it.id }) { request ->
                        Card(
                            onClick = {
                                runTask {
                                    loadDetails(request.id)
                                    selectedId = request.id
                                }
                            },
                            enabled = !busy,
                            shape = CardShape,
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val product = data.products
                                        .firstOrNull { it.id == request.productId }?.name
                                    Text(
                                        text = product ?: "Unavailable product",
                                        color = Ink,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text("RO-${request.id}", color = Muted, fontSize = 12.sp)
                                    Text(requestTime(request.createdAt), color = Muted, fontSize = 12.sp)
                                    PriorityBadge(request.priority)
                                    Text(
                                        quantityLabel(request, data),
                                        color = Muted,
                                        fontSize = 12.sp
                                    )
                                }

                                Text(
                                    text = statusLabel(request.status),
                                    color = Navy,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .background(Color(0xFFE8EEF6), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        action?.let { next ->
            ReorderActionDialog(
                action = next,
                busy = busy,
                error = error,
                onDismiss = {
                    action = null
                    error = null
                }
            ) { input ->
                runTask {
                    if (selected != null) {
                        repository.transition(
                            selected.id,
                            next,
                            if (next == "RECEIVED") "" else input,
                            if (next == "RECEIVED") input.toLongOrNull() else null
                        )
                    }
                    action = null
                    reload()
                }
            }
        }
    }
}

private val PendingStatuses = setOf("PENDING", "PENDING_APPROVAL")
private val ProgressStatuses = setOf("ACCEPTED", "PREPARING", "DISPATCHED")

@Composable
private fun ReorderSummary(rows: List<Reorder>) {
    SummaryStatsCard(
        stats = listOf(
            SummaryStat("PENDING", rows.count { it.status in PendingStatuses }.toString()),
            SummaryStat(
                "IN PROGRESS",
                rows.count { it.status in ProgressStatuses }.toString(),
                Color(0xFFFFB7C5)
            ),
            SummaryStat(
                "DELIVERED",
                rows.count { it.status == "DELIVERED" }.toString(),
                Color(0xFF9BE8CA)
            )
        )
    )
}

@Composable
private fun ReorderTabs(
    historyOnly: Boolean,
    activeCount: Int,
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
            false to "Active ($activeCount)",
            true to "History ($historyCount)"
        ).forEach { (history, label) ->
            val selected = historyOnly == history
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) Navy else Color.Transparent)
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = { onSelect(history) }
                    )
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = if (selected) Color.White else Muted,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun ReorderForm(
    request: Reorder?,
    initialProduct: ReorderOption?,
    data: ReorderOptions,
    rows: List<Reorder>,
    busy: Boolean,
    onBack: () -> Unit,
    onSave: (Long, Long, String, String, String) -> Unit
) {
    var product by remember(request?.id, initialProduct?.id) {
        mutableStateOf(request?.productId ?: initialProduct?.id)
    }
    var supplier by remember(request?.id, initialProduct?.id) {
        val knownSuppliers = initialProduct?.previousSupplierIds.orEmpty()
        val suggested = data.suppliers.singleOrNull { it.id in knownSuppliers }
        mutableStateOf(if (request != null) request.supplierId else suggested?.id)
    }
    var quantity by remember(request?.id) {
        mutableStateOf(request?.quantity?.toString().orEmpty())
    }

    var priority by remember(request?.id) { mutableStateOf(request?.priority ?: "NORMAL") }
    var reason by remember(request?.id, product, supplier) { mutableStateOf(request?.additionalReason.orEmpty()) }
    val openOrders = rows.filter {
        it.productId == product && it.id != request?.id && it.status !in Finished
    }
    val requiresReason = openOrders.isNotEmpty() &&
            (request == null || product != request.productId || supplier != request.supplierId)

    val activeProducts = data.products.filter { it.active }
    val currentProduct = activeProducts.firstOrNull { it.id == product }
    val previousSupplierIds = currentProduct?.previousSupplierIds.orEmpty().toSet()
    val supplierOptions = data.suppliers.sortedByDescending { it.id in previousSupplierIds }
    val parsedQuantity = quantity.trim().replace(',', '.').toBigDecimalOrNull()
    val validQuantity = parsedQuantity != null && parsedQuantity.signum() > 0 &&
            parsedQuantity.toDouble().isFinite() && parsedQuantity.toDouble() > 0
    val unit = currentProduct?.unit.orEmpty()
    val quantityLabel = if (unit.isBlank()) "Reorder quantity" else "Reorder quantity ($unit)"

    ReorderCard {
        TextButton(enabled = !busy, onClick = onBack) { Text("‹ Back") }

        if (request == null && initialProduct != null) {
            Text(initialProduct.name, color = Ink, fontWeight = FontWeight.SemiBold)
        } else {
            ReorderPicker("Product", activeProducts, product, !busy) { productId ->
                if (productId != product) {
                    product = productId
                    val previousIds = activeProducts
                        .firstOrNull { it.id == productId }?.previousSupplierIds.orEmpty()
                    supplier = data.suppliers.filter { it.id in previousIds }.singleOrNull()?.id
                }
            }
        }

        ReorderPicker("Supplier", supplierOptions, supplier, !busy && currentProduct != null) {
            supplier = it
        }

        if (activeProducts.isEmpty()) {
            Text("Add an active product in Inventory first.", color = Muted)
        }
        if (data.suppliers.isEmpty()) {
            Text("Add a supplier in Supplier Management first.", color = Muted)
        }

        ReorderField(
            value = quantity,
            onChange = { quantity = it },
            label = quantityLabel,
            enabled = !busy,
            keyboardType = KeyboardType.Decimal
        )

        ChoiceField("Priority", Priorities, priority, !busy, { statusLabel(it) }) {
            priority = it ?: "NORMAL"
        }

        if (openOrders.isNotEmpty()) {
            Text("Active requests for this product", color = Navy, fontWeight = FontWeight.SemiBold)
            openOrders.forEach { order ->
                val supplierName = data.suppliers.firstOrNull { it.id == order.supplierId }?.name
                Column(
                    Modifier.fillMaxWidth().background(Background, FieldShape).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("RO-${order.id} · ${statusLabel(order.status)}", fontWeight = FontWeight.SemiBold)
                    Text("${quantityLabel(order, data)} · ${supplierName ?: "Supplier #${order.supplierId}"}")
                    Text(
                        listOfNotNull(order.createdByRole?.let { statusLabel(it) }, requestTime(order.createdAt))
                            .joinToString(" · "),
                        color = Muted,
                        fontSize = 12.sp
                    )
                }
            }
        }
        if (requiresReason) {
            ReorderField(reason, { reason = it }, "Reason for additional reorder", !busy)
        }

        ReorderButton(
            label = if (request == null) "Submit reorder request" else "Save changes",
            enabled = !busy && currentProduct != null &&
                    supplierOptions.any { it.id == supplier } && validQuantity &&
                    (!requiresReason || reason.isNotBlank())
        ) {
            val productId = currentProduct?.id ?: return@ReorderButton
            val supplierId = supplier ?: return@ReorderButton
            val amount = parsedQuantity ?: return@ReorderButton
            onSave(productId, supplierId, amount.toString(), priority, reason.trim())
        }
    }
}

@Composable
private fun ReorderPicker(
    label: String,
    options: List<ReorderOption>,
    selected: Long?,
    enabled: Boolean,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fieldWidth = maxWidth

        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled && options.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = FieldShape,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, color = Muted, fontSize = 10.sp)
                Text(
                    text = options.firstOrNull { it.id == selected }?.name ?: "Select $label",
                    color = if (enabled) Ink else Muted,
                    fontSize = 13.sp
                )
            }
            Text(if (expanded) "▴" else "▾", color = Muted)
        }

        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(fieldWidth).heightIn(max = 320.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = {
                        expanded = false
                        onSelect(option.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun ReorderActionDialog(
    action: String,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var input by remember(action) { mutableStateOf("") }

    val label = when (action) {
        "CANCELLED" -> "Reason (required)"
        "RECEIVED" -> "Stock In movement ID"
        else -> "Notes (optional)"
    }
    val title = when (action) {
        "CANCELLED" -> "Cancel request?"
        "RECEIVED" -> "Confirm received products"
        else -> "Mark as ${statusLabel(action).lowercase()}?"
    }
    val valid = when (action) {
        "CANCELLED" -> input.isNotBlank()
        "RECEIVED" -> (input.trim().toLongOrNull() ?: 0) > 0
        else -> true
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (action == "RECEIVED") {
                    Text("Record the delivery in Stock In first. Enter its movement ID to close this request.")
                }

                ReorderField(
                    value = input,
                    onChange = { input = it },
                    label = label,
                    enabled = !busy,
                    keyboardType = if (action == "RECEIVED") KeyboardType.Number else KeyboardType.Text
                )

                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                enabled = !busy && valid,
                onClick = { onConfirm(input.trim()) }
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
private fun ReorderField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = FieldShape,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedBorderColor = Navy,
            unfocusedBorderColor = InputBorder,
            unfocusedLabelColor = Muted
        )
    )
}

@Composable
private fun ReorderButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = FieldShape,
        modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ReorderCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = CardShape
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

private fun statusLabel(status: String): String =
    when (status) {
        "PENDING_APPROVAL" -> "Awaiting Admin approval"
        "PENDING" -> "Awaiting Supplier"
        "ADMIN_REJECTED" -> "Rejected by Admin"
        "APPROVED" -> "Approved"
        else -> status.lowercase().replaceFirstChar { it.titlecase() }
    }

private fun quantityLabel(request: Reorder, options: ReorderOptions): String {
    val quantity = request.quantity.toBigDecimal().stripTrailingZeros().toPlainString()
    val unit = options.products.firstOrNull { it.id == request.productId }?.unit.orEmpty()
    return "$quantity $unit".trim()
}

private val Priorities = listOf("NORMAL", "URGENT")
private val ActiveStatuses = listOf(
    "PENDING_APPROVAL", "PENDING", "ACCEPTED", "PREPARING", "DISPATCHED", "DELIVERED"
)
private val MalaysiaZone = ZoneId.of("Asia/Kuala_Lumpur")
private val RequestTimeFormat = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH)

private fun requestInstant(value: String): Instant? =
    runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()

private fun requestTime(value: String): String =
    requestInstant(value)?.atZone(MalaysiaZone)?.format(RequestTimeFormat)?.let { "$it MYT" } ?: value

private fun parseDate(value: String): LocalDate? =
    runCatching { LocalDate.parse(value.trim()) }.getOrNull()

@Composable
private fun PriorityBadge(priority: String) {
    val urgent = priority == "URGENT"
    Text(
        text = statusLabel(priority),
        color = if (urgent) Color(0xFF972D41) else Muted,
        fontSize = 12.sp,
        modifier = Modifier.background(
            if (urgent) Color(0xFFFBE7EC) else Color(0xFFE8EEF6),
            RoundedCornerShape(8.dp)
        ).padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun ChoiceField(
    title: String,
    values: List<String>,
    selected: String?,
    enabled: Boolean,
    label: (String) -> String,
    onSelect: (String?) -> Unit
) {
    val choices = values.mapIndexed { index, value ->
        ReorderOption(index.toLong(), label(value))
    }
    val selectedId = selected?.let { values.indexOf(it).takeIf { index -> index >= 0 }?.toLong() }
    ReorderPicker(title, choices, selectedId, enabled) { id ->
        onSelect(values[id.toInt()].takeUnless { it == "ALL" })
    }
}

@Composable
private fun ReorderFilters(
    statuses: List<String>,
    status: String?,
    priority: String?,
    from: String,
    to: String,
    validDates: Boolean,
    onStatus: (String?) -> Unit,
    onPriority: (String?) -> Unit,
    onFrom: (String) -> Unit,
    onTo: (String) -> Unit,
    onClear: () -> Unit
) {
    var showDates by remember { mutableStateOf(false) }
    ReorderCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                ChoiceField("Status", listOf("ALL") + statuses, status ?: "ALL", true, ::statusLabel, onStatus)
            }
            Box(Modifier.weight(1f)) {
                ChoiceField("Priority", listOf("ALL") + Priorities, priority ?: "ALL", true, ::statusLabel, onPriority)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { showDates = !showDates }) {
                Text(if (showDates) "Hide dates" else "Filter by date")
            }
            if (status != null || priority != null || from.isNotBlank() || to.isNotBlank()) {
                TextButton(onClick = onClear) { Text("Clear filters") }
            }
        }
        if (showDates) {
            Text("Request date (MYT)", color = Muted, fontSize = 12.sp)
            ReorderDatePicker("From", from, onFrom)
            ReorderDatePicker("To", to, onTo)
        } else if (from.isNotBlank() || to.isNotBlank()) {
            Text("Request date  ${from.ifBlank { "Any date" }} to ${to.ifBlank { "Any date" }}", color = Muted)
        }
        if (!validDates) {
            Text("The end date must be on or after the start date.", color = MaterialTheme.colorScheme.error)
        }
    }
}

private val ReorderDateLabel = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

@Composable
private fun ReorderDatePicker(label: String, value: String, onSelect: (String) -> Unit) {
    val context = LocalContext.current
    val selectedDate = parseDate(value)
    OutlinedButton(
        onClick = {
            val initialDate = selectedDate ?: LocalDate.now(MalaysiaZone)
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    onSelect(LocalDate.of(year, month + 1, day).toString())
                },
                initialDate.year,
                initialDate.monthValue - 1,
                initialDate.dayOfMonth
            ).apply {
                if (selectedDate != null) {
                    setButton(android.content.DialogInterface.BUTTON_NEUTRAL, "Clear") { _, _ ->
                        onSelect("")
                    }
                }
            }.show()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("$label: ${selectedDate?.format(ReorderDateLabel) ?: "Select date"}")
    }
}
