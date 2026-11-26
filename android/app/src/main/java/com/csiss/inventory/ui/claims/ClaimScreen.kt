package com.csiss.inventory.ui.claims

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.csiss.inventory.data.*
import com.csiss.inventory.ui.components.CsissLightRefreshIndicator
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// COLOURS (match project theme)
// ─────────────────────────────────────────────────────────────────────────────

private val Navy       = Color(0xFF132945)
private val Background = Color(0xFFF1F6FB)

// ─────────────────────────────────────────────────────────────────────────────
// VIEW-MODEL
// ─────────────────────────────────────────────────────────────────────────────

class ClaimViewModel : ViewModel() {
    private val repo = ClaimRepository(com.csiss.inventory.data.SupabaseClientProvider.client)

    var options   by mutableStateOf<ClaimOptions?>(null)
    var claims    by mutableStateOf<List<SupplierClaim>>(emptyList())
    var isLoading by mutableStateOf(false)
    var error     by mutableStateOf<String?>(null)
    var message   by mutableStateOf<String?>(null)

    // ── derived counts ──────────────────────────────────────────────────────
    val pendingCount:    Int get() = claims.count { it.status == "PENDING" }
    val inProgressCount: Int get() = claims.count {
        it.status in listOf("APPROVED", "PROCESSING", "DISPATCHED", "DELIVERED")
    }
    val closedCount: Int get() = claims.count {
        it.status in listOf("CLOSED", "REJECTED", "CANCELLED")
    }

    val isSupplier: Boolean get() = options?.role == "SUPPLIER"
    val isStaff:    Boolean get() = options?.role in listOf("STAFF", "ADMIN")

    fun load() = viewModelScope.launch {
        isLoading = true; error = null
        runCatching {
            options = repo.options()
            claims  = repo.claims()
        }.onFailure { error = it.message }
        isLoading = false
    }

    suspend fun save(
        claimId: Long?, productId: Long, supplierId: Long, batchId: Long?,
        qty: Double, claimType: String, reasonCat: String, reasonDesc: String,
        evidenceUrl: String?, priority: String
    ) = runCatching {
        repo.save(claimId, productId, supplierId, batchId, qty,
            claimType, reasonCat, reasonDesc, evidenceUrl, priority)
        message = if (claimId == null) "Claim submitted." else "Claim updated."
        load()
    }.onFailure { error = it.message }

    suspend fun transition(
        claimId: Long, newStatus: String,
        note: String? = null, reference: String? = null, receiptId: Long? = null
    ) = runCatching {
        repo.transition(claimId, newStatus, note, reference, receiptId)
        message = "Status updated to $newStatus."
        load()
    }.onFailure { error = it.message }

    suspend fun history(claimId: Long): List<ClaimHistory> =
        runCatching { repo.history(claimId) }.getOrDefault(emptyList())
}

// ─────────────────────────────────────────────────────────────────────────────
// ENTRY POINT
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimScreen(modifier: Modifier = Modifier) {
    val vm: ClaimViewModel = viewModel()
    LaunchedEffect(Unit) { vm.load() }

    val scope = rememberCoroutineScope()

    // ── sheet / dialog state ───────────────────────────────────────────────
    var selectedClaim    by remember { mutableStateOf<SupplierClaim?>(null) }
    var showDetail       by remember { mutableStateOf(false) }
    var showForm         by remember { mutableStateOf(false) }
    var editClaim        by remember { mutableStateOf<SupplierClaim?>(null) }
    var showActionDialog by remember { mutableStateOf<String?>(null) }
    var historyList      by remember { mutableStateOf<List<ClaimHistory>>(emptyList()) }
    var showHistory      by remember { mutableStateOf(false) }

    // ── snackbar ───────────────────────────────────────────────────────────
    val snackState = remember { SnackbarHostState() }
    LaunchedEffect(vm.error, vm.message) {
        vm.error?.let   { snackState.showSnackbar(it); vm.error   = null }
        vm.message?.let { snackState.showSnackbar(it); vm.message = null }
    }

    // ── filter ─────────────────────────────────────────────────────────────
    var filterStatus by remember { mutableStateOf("ALL") }
    val statuses = listOf(
        "ALL", "PENDING", "APPROVED", "PROCESSING", "DISPATCHED",
        "DELIVERED", "CLOSED", "REJECTED", "CANCELLED"
    )
    val displayClaims = if (filterStatus == "ALL") vm.claims
                        else vm.claims.filter { it.status == filterStatus }

    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        containerColor = Background,
        snackbarHost   = { SnackbarHost(snackState) },
        floatingActionButton = {
            if (vm.isStaff) {
                FloatingActionButton(
                    onClick        = { editClaim = null; showForm = true },
                    containerColor = Navy,
                    contentColor   = Color.White
                ) { Text("+", fontSize = 24.sp, fontWeight = FontWeight.Medium) }
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = vm.isLoading,
            onRefresh    = { vm.load() },
            modifier     = modifier.padding(padding),
            state        = pullToRefreshState,
            indicator    = {
                CsissLightRefreshIndicator(
                    state        = pullToRefreshState,
                    isRefreshing = vm.isLoading,
                    modifier     = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {
            LazyColumn(
                contentPadding      = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier            = Modifier.fillMaxSize()
            ) {
                // ── header ────────────────────────────────────────────────
                item {
                    Text(
                        "Supplier Claims",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 22.sp,
                        color      = Navy
                    )
                    Spacer(Modifier.height(4.dp))
                }

                // ── summary stats ─────────────────────────────────────────
                item { ClaimSummaryCard(vm) }

                // ── filter tabs ───────────────────────────────────────────
                item {
                    PrimaryScrollableTabRow(
                        selectedTabIndex = statuses.indexOf(filterStatus).coerceAtLeast(0),
                        containerColor   = Color.Transparent,
                        edgePadding      = 0.dp
                    ) {
                        statuses.forEach { s ->
                            Tab(
                                selected = filterStatus == s,
                                onClick  = { filterStatus = s },
                                text     = {
                                    Text(
                                        s.lowercase().replaceFirstChar { it.uppercase() },
                                        fontSize = 12.sp
                                    )
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                // ── empty state ───────────────────────────────────────────
                if (displayClaims.isEmpty() && !vm.isLoading) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("No claims found.", color = Color.Gray) }
                    }
                }

                // ── claim cards ───────────────────────────────────────────
                items(displayClaims, key = { it.id }) { claim ->
                    ClaimCard(
                        claim   = claim,
                        options = vm.options,
                        onClick = {
                            selectedClaim = claim
                            showDetail    = true
                            scope.launch {
                                historyList = vm.history(claim.id)
                                showHistory = true
                            }
                        }
                    )
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    // ── detail bottom sheet ───────────────────────────────────────────────
    if (showDetail && selectedClaim != null) {
        ClaimDetailSheet(
            claim       = selectedClaim!!,
            options     = vm.options,
            historyList = historyList,
            showHistory = showHistory,
            isStaff     = vm.isStaff,
            isSupplier  = vm.isSupplier,
            onEdit      = { editClaim = it; showForm = true; showDetail = false },
            onDismiss   = { showDetail = false },
            onAction    = { action -> showActionDialog = action }
        )
    }

    // ── create / edit form ────────────────────────────────────────────────
    if (showForm) {
        ClaimFormDialog(
            existing  = editClaim,
            options   = vm.options,
            onDismiss = { showForm = false },
            onSave    = { pId, sId, bId, qty, type, cat, desc, ev, pri ->
                scope.launch {
                    vm.save(editClaim?.id, pId, sId, bId, qty, type, cat, desc, ev, pri)
                    showForm = false
                }
            }
        )
    }

    // ── action dialogs ────────────────────────────────────────────────────
    showActionDialog?.let { action ->
        ClaimActionDialog(
            action    = action,
            claim     = selectedClaim,
            onDismiss = { showActionDialog = null },
            onConfirm = { status, note, ref, receiptId ->
                scope.launch {
                    vm.transition(selectedClaim!!.id, status, note, ref, receiptId)
                    showActionDialog = null
                    showDetail       = false
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SUMMARY CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ClaimSummaryCard(vm: ClaimViewModel) {
    Card(
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = Navy),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("Pending",     vm.pendingCount.toString(),    Color(0xFFFFC107))
            StatItem("In Progress", vm.inProgressCount.toString(), Color(0xFF4FC3F7))
            StatItem("Closed",      vm.closedCount.toString(),     Color(0xFF81C784))
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CLAIM CARD (list item)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ClaimCard(
    claim: SupplierClaim,
    options: ClaimOptions?,
    onClick: () -> Unit
) {
    val productName  = options?.products?.find  { it.id == claim.productId  }?.name ?: "Product #${claim.productId}"
    val supplierName = options?.suppliers?.find { it.id == claim.supplierId }?.name ?: "Supplier #${claim.supplierId}"

    Card(
        shape     = RoundedCornerShape(10.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    claim.claimCode ?: "CLM-PENDING",
                    fontWeight = FontWeight.Bold,
                    color      = Navy,
                    modifier   = Modifier.weight(1f)
                )
                StatusBadge(claim.status)
            }
            Spacer(Modifier.height(4.dp))
            Text(productName,  fontSize = 13.sp, color = Color.DarkGray)
            Text(supplierName, fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TypeChip(claim.claimType)
                PriorityChip(claim.priority)
                Text(
                    "Qty: ${claim.affectedQuantity.toInt()}",
                    fontSize = 11.sp,
                    color    = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DETAIL SHEET
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClaimDetailSheet(
    claim: SupplierClaim,
    options: ClaimOptions?,
    historyList: List<ClaimHistory>,
    showHistory: Boolean,
    isStaff: Boolean,
    isSupplier: Boolean,
    onEdit: (SupplierClaim) -> Unit,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit
) {
    val productName  = options?.products?.find  { it.id == claim.productId  }?.name  ?: "—"
    val supplierName = options?.suppliers?.find { it.id == claim.supplierId }?.name ?: "—"
    val batchCode    = options?.batches?.find   { it.id == claim.batchId   }?.batchCode ?: "—"

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // ── title row ─────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        claim.claimCode ?: "New Claim",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp,
                        color      = Navy
                    )
                    Text(
                        "${claim.claimType} · ${claim.reasonCategory}",
                        fontSize = 13.sp,
                        color    = Color.Gray
                    )
                }
                StatusBadge(claim.status)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // ── info rows ─────────────────────────────────────────────────
            DetailRow("Product",      productName)
            DetailRow("Supplier",     supplierName)
            DetailRow("Batch",        batchCode)
            DetailRow("Affected Qty", claim.affectedQuantity.toString())
            DetailRow("Priority",     claim.priority)
            DetailRow("Reason",       claim.reasonDescription)
            claim.resolutionReference?.let { DetailRow("Reference", it) }
            claim.resolutionNotes?.let     { DetailRow("Notes", it) }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // ── action buttons ────────────────────────────────────────────
            ActionButtons(
                claim      = claim,
                isStaff    = isStaff,
                isSupplier = isSupplier,
                onEdit     = { onEdit(claim) },
                onAction   = onAction
            )

            // ── history timeline ──────────────────────────────────────────
            if (showHistory && historyList.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Status History",
                    fontWeight = FontWeight.SemiBold,
                    color      = Navy,
                    fontSize   = 14.sp
                )
                Spacer(Modifier.height(6.dp))
                historyList.forEach { h -> HistoryItem(h) }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ACTION BUTTONS (inside detail sheet)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ActionButtons(
    claim: SupplierClaim,
    isStaff: Boolean,
    isSupplier: Boolean,
    onEdit: () -> Unit,
    onAction: (String) -> Unit
) {
    val status = claim.status
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // Staff: Edit when PENDING
        if (isStaff && status == "PENDING") {
            OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Text("Edit Claim")
            }
        }

        // Staff: Cancel when PENDING
        if (isStaff && status == "PENDING") {
            OutlinedButton(
                onClick  = { onAction("CANCEL") },
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
            ) {
                Text("Cancel Claim")
            }
        }

        // Supplier: Approve / Reject when PENDING
        if (isSupplier && status == "PENDING") {
            Button(
                onClick  = { onAction("APPROVE") },
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
            ) { Text("Approve Claim") }
            OutlinedButton(
                onClick  = { onAction("REJECT") },
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
            ) { Text("Reject Claim") }
        }

        // Supplier: Mark Processing when APPROVED
        if (isSupplier && status == "APPROVED") {
            Button(
                onClick  = { onAction("PROCESSING") },
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = Navy)
            ) { Text("Mark as Processing") }
        }

        // Supplier: Dispatched (REPLACEMENT) or Refund Issued (REFUND) when PROCESSING
        if (isSupplier && status == "PROCESSING") {
            if (claim.claimType == "REPLACEMENT") {
                Button(
                    onClick  = { onAction("DISPATCHED") },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = Navy)
                ) { Text("Mark as Dispatched") }
            } else {
                Button(
                    onClick  = { onAction("REFUND_ISSUED") },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                ) { Text("Issue Refund") }
            }
        }

        // Supplier: Delivered when DISPATCHED
        if (isSupplier && status == "DISPATCHED") {
            Button(
                onClick  = { onAction("DELIVERED") },
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
            ) { Text("Mark as Delivered") }
        }

        // Staff: Close REPLACEMENT after stock-in
        if (isStaff && status == "DELIVERED") {
            Button(
                onClick  = { onAction("CLOSE_REPLACEMENT") },
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = Navy)
            ) { Text("Close — Link Stock-In") }
        }

        // Staff: Close REFUND after credit reference
        if (isStaff && status == "REFUND_ISSUED") {
            Button(
                onClick  = { onAction("CLOSE_REFUND") },
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = Navy)
            ) { Text("Close — Confirm Refund") }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FORM DIALOG (create / edit)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClaimFormDialog(
    existing: SupplierClaim?,
    options:  ClaimOptions?,
    onDismiss: () -> Unit,
    onSave:   (Long, Long, Long?, Double, String, String, String, String?, String) -> Unit
) {
    var selectedProductId  by remember { mutableStateOf(existing?.productId  ?: 0L) }
    var selectedSupplierId by remember { mutableStateOf(existing?.supplierId ?: 0L) }
    var selectedBatchId    by remember { mutableStateOf(existing?.batchId) }
    var quantity           by remember { mutableStateOf(existing?.affectedQuantity?.toString() ?: "") }
    var claimType          by remember { mutableStateOf(existing?.claimType ?: "REPLACEMENT") }
    var reasonCategory     by remember { mutableStateOf(existing?.reasonCategory ?: "") }
    var reasonDescription  by remember { mutableStateOf(existing?.reasonDescription ?: "") }
    var evidenceUrl        by remember { mutableStateOf(existing?.evidenceUrl ?: "") }
    var priority           by remember { mutableStateOf(existing?.priority ?: "NORMAL") }

    var productExpanded  by remember { mutableStateOf(false) }
    var supplierExpanded by remember { mutableStateOf(false) }
    var batchExpanded    by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }

    val reasonCategories = listOf(
        "DAMAGED_GOODS", "WRONG_ITEM", "SHORT_SHIPMENT",
        "EXPIRED_GOODS", "QUALITY_ISSUE", "OTHER"
    )
    val priorities = listOf("LOW", "NORMAL", "HIGH", "URGENT")

    val filteredBatches = options?.batches?.filter {
        (selectedProductId  == 0L || it.productId  == selectedProductId) &&
        (selectedSupplierId == 0L || it.supplierId == selectedSupplierId)
    } ?: emptyList()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Color.White,
        title = {
            Text(
                if (existing == null) "New Supplier Claim" else "Edit Claim",
                fontWeight = FontWeight.Bold,
                color      = Navy
            )
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Product
                ExposedDropdownMenuBox(
                    expanded        = productExpanded,
                    onExpandedChange = { productExpanded = it }
                ) {
                    OutlinedTextField(
                        value         = options?.products?.find { it.id == selectedProductId }?.name ?: "Select Product",
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Product *") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(productExpanded) },
                        modifier      = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded        = productExpanded,
                        onDismissRequest = { productExpanded = false }
                    ) {
                        options?.products?.forEach { p ->
                            DropdownMenuItem(
                                text    = { Text("${p.name} (${p.code})") },
                                onClick = {
                                    selectedProductId = p.id
                                    selectedBatchId   = null
                                    productExpanded   = false
                                }
                            )
                        }
                    }
                }

                // Supplier
                ExposedDropdownMenuBox(
                    expanded        = supplierExpanded,
                    onExpandedChange = { supplierExpanded = it }
                ) {
                    OutlinedTextField(
                        value         = options?.suppliers?.find { it.id == selectedSupplierId }?.name ?: "Select Supplier",
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Supplier *") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(supplierExpanded) },
                        modifier      = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded        = supplierExpanded,
                        onDismissRequest = { supplierExpanded = false }
                    ) {
                        options?.suppliers?.forEach { s ->
                            DropdownMenuItem(
                                text    = { Text(s.name) },
                                onClick = {
                                    selectedSupplierId = s.id
                                    selectedBatchId    = null
                                    supplierExpanded   = false
                                }
                            )
                        }
                    }
                }

                // Batch (optional)
                ExposedDropdownMenuBox(
                    expanded        = batchExpanded,
                    onExpandedChange = { batchExpanded = it }
                ) {
                    OutlinedTextField(
                        value         = filteredBatches.find { it.id == selectedBatchId }?.batchCode ?: "None (optional)",
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Batch / Lot") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(batchExpanded) },
                        modifier      = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded        = batchExpanded,
                        onDismissRequest = { batchExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text    = { Text("None") },
                            onClick = { selectedBatchId = null; batchExpanded = false }
                        )
                        filteredBatches.forEach { b ->
                            DropdownMenuItem(
                                text    = { Text("${b.batchCode} · Qty: ${b.quantityRemaining.toInt()}") },
                                onClick = { selectedBatchId = b.id; batchExpanded = false }
                            )
                        }
                    }
                }

                // Quantity
                OutlinedTextField(
                    value           = quantity,
                    onValueChange   = { quantity = it },
                    label           = { Text("Affected Quantity *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier        = Modifier.fillMaxWidth()
                )

                // Claim Type toggle
                Text("Claim Type *", fontSize = 12.sp, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("REPLACEMENT", "REFUND").forEach { t ->
                        FilterChip(
                            selected = claimType == t,
                            onClick  = { claimType = t },
                            label    = { Text(t) }
                        )
                    }
                }

                // Reason Category
                ExposedDropdownMenuBox(
                    expanded        = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value         = if (reasonCategory.isEmpty()) "Select Category" else reasonCategory,
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Reason Category *") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                        modifier      = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded        = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        reasonCategories.forEach { cat ->
                            DropdownMenuItem(
                                text    = { Text(cat.replace('_', ' ')) },
                                onClick = { reasonCategory = cat; categoryExpanded = false }
                            )
                        }
                    }
                }

                // Description
                OutlinedTextField(
                    value         = reasonDescription,
                    onValueChange = { reasonDescription = it },
                    label         = { Text("Reason Description *") },
                    minLines      = 3,
                    modifier      = Modifier.fillMaxWidth()
                )

                // Priority
                ExposedDropdownMenuBox(
                    expanded        = priorityExpanded,
                    onExpandedChange = { priorityExpanded = it }
                ) {
                    OutlinedTextField(
                        value         = priority,
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Priority") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(priorityExpanded) },
                        modifier      = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded        = priorityExpanded,
                        onDismissRequest = { priorityExpanded = false }
                    ) {
                        priorities.forEach { p ->
                            DropdownMenuItem(
                                text    = { Text(p) },
                                onClick = { priority = p; priorityExpanded = false }
                            )
                        }
                    }
                }

                // Evidence URL (optional)
                OutlinedTextField(
                    value         = evidenceUrl,
                    onValueChange = { evidenceUrl = it },
                    label         = { Text("Evidence URL (optional)") },
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                colors  = ButtonDefaults.buttonColors(containerColor = Navy),
                onClick = {
                    val qty = quantity.toDoubleOrNull() ?: return@Button
                    if (selectedProductId == 0L || selectedSupplierId == 0L ||
                        reasonCategory.isBlank() || reasonDescription.isBlank()
                    ) return@Button
                    onSave(
                        selectedProductId, selectedSupplierId, selectedBatchId,
                        qty, claimType, reasonCategory, reasonDescription,
                        evidenceUrl.takeIf { it.isNotBlank() }, priority
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// ACTION DIALOGS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ClaimActionDialog(
    action: String,
    claim:  SupplierClaim?,
    onDismiss: () -> Unit,
    onConfirm: (status: String, note: String?, reference: String?, receiptId: Long?) -> Unit
) {
    var note      by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }

    val (title, targetStatus, needsNote, needsRef) = when (action) {
        "APPROVE"           -> Quad("Approve Claim",          "APPROVED",      false, false)
        "REJECT"            -> Quad("Reject Claim",           "REJECTED",      true,  false)
        "CANCEL"            -> Quad("Cancel Claim",           "CANCELLED",     false, false)
        "PROCESSING"        -> Quad("Mark as Processing",     "PROCESSING",    false, false)
        "DISPATCHED"        -> Quad("Mark as Dispatched",     "DISPATCHED",    false, true)
        "DELIVERED"         -> Quad("Mark as Delivered",      "DELIVERED",     false, false)
        "REFUND_ISSUED"     -> Quad("Issue Refund",           "REFUND_ISSUED", false, true)
        "CLOSE_REPLACEMENT" -> Quad("Close — Link Stock-In",  "CLOSED",        false, true)
        "CLOSE_REFUND"      -> Quad("Close — Confirm Refund", "CLOSED",        false, true)
        else                -> Quad(action, action,            false, false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Color.White,
        title = { Text(title, fontWeight = FontWeight.Bold, color = Navy) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (action) {
                    "REJECT"            -> Text("Please provide a rejection reason.")
                    "CANCEL"            -> Text("Are you sure you want to cancel this claim?")
                    "CLOSE_REPLACEMENT" -> Text("Enter the Stock-In Movement ID from the received replacement stock.")
                    "CLOSE_REFUND"      -> Text("Enter the credit note or bank reference for this refund.")
                    "REFUND_ISSUED"     -> Text("Enter the refund reference / credit note number.")
                    "DISPATCHED"        -> Text("Enter the tracking reference for the dispatched replacement.")
                }
                if (needsNote) {
                    OutlinedTextField(
                        value         = note,
                        onValueChange = { note = it },
                        label         = { Text(if (action == "REJECT") "Rejection Reason *" else "Note") },
                        minLines      = 2,
                        modifier      = Modifier.fillMaxWidth()
                    )
                }
                if (needsRef) {
                    OutlinedTextField(
                        value         = reference,
                        onValueChange = { reference = it },
                        label         = {
                            Text(
                                when (action) {
                                    "CLOSE_REPLACEMENT" -> "Movement ID *"
                                    "DISPATCHED"        -> "Tracking Reference *"
                                    else                -> "Reference *"
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                colors  = ButtonDefaults.buttonColors(containerColor = Navy),
                onClick = {
                    if (needsNote && note.isBlank()) return@Button
                    if (needsRef  && reference.isBlank()) return@Button
                    val receiptId = if (action == "CLOSE_REPLACEMENT") reference.toLongOrNull() else null
                    val ref       = if (action != "CLOSE_REPLACEMENT") reference.takeIf { it.isNotBlank() } else null
                    onConfirm(targetStatus, note.takeIf { it.isNotBlank() }, ref, receiptId)
                }
            ) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPER: Quad for destructuring 4-tuple
// ─────────────────────────────────────────────────────────────────────────────

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
private operator fun <A, B, C, D> Quad<A, B, C, D>.component1() = a
private operator fun <A, B, C, D> Quad<A, B, C, D>.component2() = b
private operator fun <A, B, C, D> Quad<A, B, C, D>.component3() = c
private operator fun <A, B, C, D> Quad<A, B, C, D>.component4() = d

// ─────────────────────────────────────────────────────────────────────────────
// SMALL COMPOSABLES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            "$label:",
            modifier  = Modifier.width(130.dp),
            fontSize  = 13.sp,
            color     = Color.Gray
        )
        Text(
            value,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier   = Modifier.weight(1f),
            maxLines   = 4,
            overflow   = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (bg, fg) = when (status) {
        "PENDING"       -> Color(0xFFFFF8E1) to Color(0xFFE65100)
        "APPROVED"      -> Color(0xFFE8F5E9) to Color(0xFF388E3C)
        "PROCESSING"    -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        "DISPATCHED"    -> Color(0xFFEDE7F6) to Color(0xFF4527A0)
        "DELIVERED"     -> Color(0xFFE0F7FA) to Color(0xFF00695C)
        "REFUND_ISSUED" -> Color(0xFFF3E5F5) to Color(0xFF6A1B9A)
        "CLOSED"        -> Color(0xFFF5F5F5) to Color(0xFF616161)
        "REJECTED"      -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        "CANCELLED"     -> Color(0xFFFFEBEE) to Color(0xFFB71C1C)
        else            -> Color(0xFFF5F5F5) to Color(0xFF616161)
    }
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(
            status.replace('_', ' '),
            color      = fg,
            fontSize   = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun TypeChip(type: String) {
    val color = if (type == "REPLACEMENT") Color(0xFF1565C0) else Color(0xFF388E3C)
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.1f)) {
        Text(
            type,
            color    = color,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun PriorityChip(priority: String) {
    val color = when (priority) {
        "URGENT" -> Color(0xFFC62828)
        "HIGH"   -> Color(0xFFE65100)
        "LOW"    -> Color(0xFF616161)
        else     -> Color(0xFF1565C0)
    }
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.1f)) {
        Text(
            priority,
            color    = color,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun HistoryItem(h: ClaimHistory) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Navy)
                .align(Alignment.CenterVertically)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(h.status, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Navy)
            h.note?.let { Text(it, fontSize = 12.sp, color = Color.DarkGray) }
            Text(
                "${h.changedByRole ?: ""} · ${h.changedAt.take(16).replace('T', ' ')}",
                fontSize = 11.sp,
                color    = Color.Gray
            )
        }
    }
}
