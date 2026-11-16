package com.csiss.inventory.ui.inventory

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csiss.inventory.ui.components.BottomBar
import com.csiss.inventory.ui.components.BottomTab
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class Page { List, Details, Add, Edit }

@Composable
fun InventoryScreen(
    modifier: Modifier = Modifier,
    onHome: (() -> Unit)? = null,
    onAlerts: (() -> Unit)? = null,
    onProfile: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val isPreview = LocalInspectionMode.current
    var databaseStatus by remember { mutableStateOf("Loading products…") }
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf("") }
    var reloadProducts by remember { mutableIntStateOf(0) }
    val products = remember {
        mutableStateListOf<Product>()
    }

    LaunchedEffect(isPreview, reloadProducts) {
        if (isPreview) return@LaunchedEffect

        try {
            val loadedProducts = loadInventoryProducts()
            products.clear()
            products.addAll(loadedProducts)
            databaseStatus = ""
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            android.util.Log.e("InventoryReadTest", "Failed to load inventory", error)
            databaseStatus = "Couldn't load inventory. Check Logcat."
        }
    }
    var page by remember { mutableStateOf(Page.List) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var deleteDialog by remember { mutableStateOf(false) }
    val selected = products.firstOrNull { it.id == selectedId }

    val goBack: () -> Unit = {
        page = if (page == Page.Edit) Page.Details else Page.List
    }
    BackHandler(enabled = page != Page.List, onBack = goBack)

    MaterialTheme(
        colorScheme = lightColorScheme(primary = Blue, onPrimary = White,
            background = Background, surface = White, onSurface = Ink, error = Red)
    ) {
        Column(modifier.fillMaxSize().background(Background)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("❄", color = Blue, fontSize = 24.sp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Cold Storage", fontWeight = FontWeight.Bold, color = Ink)
                    Text("Inventory Supply System", color = Muted, fontSize = 11.sp)
                }
                if (onAlerts != null) TextButton(onClick = onAlerts) { Text("Alerts") }
            }
            if (!isPreview && databaseStatus.isNotEmpty()) {
                Text(
                    text = databaseStatus,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            if (page == Page.Add || page == Page.Edit && (isSaving || saveError.isNotEmpty())) {
                Text(
                    text = if (isSaving) "Saving product…" else saveError,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (page) {
                    Page.List -> ProductList(products, today, search, { search = it },
                        onAdd = { page = Page.Add },
                        onSelect = { selectedId = it; page = Page.Details })
                    Page.Details -> selected?.let { product ->
                        ProductDetails(product, today, goBack, { page = Page.Edit },
                            { deleteDialog = true })
                    }
                    Page.Add, Page.Edit -> {
                        val editing = selected.takeIf { page == Page.Edit }
                        ProductForm(editing, goBack) { name, category, unit, photo, minimum, description ->
                            if (editing == null) {
                                if (isSaving) return@ProductForm

                                isSaving = true
                                scope.launch {
                                    try {
                                        insertInventoryProduct(context.contentResolver, name, category, unit, photo, minimum,description)

                                        search = ""
                                        page = Page.List
                                        reloadProducts++
                                    } catch (error: CancellationException) {
                                        throw error
                                    } catch (error: Exception) {
                                        android.util.Log.e("InventorySave", "Failed to save product", error)
                                        saveError = "Couldn't save product. Check Logcat."
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            } else {
                            if (isSaving) return@ProductForm

                            saveError = ""
                            isSaving = true
                            scope.launch {
                                try {
                                    updateInventoryMinimum(editing.id, minimum, description)

                                    val index = products.indexOfFirst { it.id == editing.id }
                                    if (index >= 0) {
                                        products[index] = editing.copy(
                                            name = name,
                                            category = category.name,
                                            unit = unit.name,
                                            photo = photo,
                                            minimum = minimum,
                                            description = description
                                        )
                                    }
                                    page = Page.Details
                                } catch (error: CancellationException) {
                                    throw error
                                } catch (error: Exception) {
                                    val reason = error.message.orEmpty()
                                        .substringBefore("URL:")
                                        .substringBefore("Headers:")
                                        .trim()
                                    android.util.Log.w("InventoryUpdate", "${error.javaClass.simpleName}: $reason")
                                    saveError = "Couldn't update minimum stock. Check Logcat."
                                } finally {
                                    isSaving = false
                                }
                            }
                        }
                    }
                }
            }
            }
            BottomBar(
                selectedTab = BottomTab.Inventory,
                enabledTabs = buildSet {
                    add(BottomTab.Inventory)
                    if (onHome != null) add(BottomTab.Home)
                    if (onAlerts != null) add(BottomTab.Alerts)
                    if (onProfile != null) add(BottomTab.Profile)
                },
                onTabSelected = { tab ->
                    when (tab) {
                        BottomTab.Home -> onHome?.invoke()
                        BottomTab.Inventory -> page = Page.List
                        BottomTab.Alerts -> onAlerts?.invoke()
                        BottomTab.Profile -> onProfile?.invoke()
                    }
                }
            )
        }

        if (deleteDialog && selected != null) {
            AlertDialog(
                onDismissRequest = { deleteDialog = false },
                title = { Text("Delete this product?") },
                text = {
                    Text("Remove ${selected.name}? Products with stock or movement history must be archived instead.")
                },
                confirmButton = {
                    Button(
                        enabled = selected.canDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = Red),
                        onClick = {
                            products.removeAll { it.id == selected.id }
                            selectedId = null
                            deleteDialog = false
                            page = Page.List
                        }
                    ) { Text("Delete product") }
                },
                dismissButton = {
                    OutlinedButton(onClick = { deleteDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 820)
@Composable
private fun InventoryScreenPreview() {
    InventoryScreen()
}
