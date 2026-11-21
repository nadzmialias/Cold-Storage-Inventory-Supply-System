package com.csiss.inventory.ui.inventory

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.csiss.inventory.data.changeInventoryProductState
import com.csiss.inventory.data.insertInventoryProduct
import com.csiss.inventory.data.loadInventoryProducts
import com.csiss.inventory.data.updateInventoryMinimum
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class Page {
    List, Details, Add, Edit
}

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
    val scope = rememberCoroutineScope()

    var databaseStatus by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(!isPreview) }
    var reloadProducts by remember { mutableIntStateOf(0) }

    val products = remember {
        mutableStateListOf<Product>()
    }

    LaunchedEffect(isPreview, reloadProducts) {
        if (isPreview) return@LaunchedEffect

        isRefreshing = true

        try {
            val loadedProducts = loadInventoryProducts(active = null)
            products.clear()
            products.addAll(loadedProducts)
            databaseStatus = ""
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            android.util.Log.e(
                "InventoryReadTest",
                "Failed to load inventory",
                error
            )
            databaseStatus = "Couldn't load inventory. Check Logcat."
        } finally {
            isRefreshing = false
        }
    }

    var page by remember { mutableStateOf(Page.List) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var pendingAction by remember { mutableStateOf<String?>(null) }
    var actionSaving by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf("") }
    var showArchived by rememberSaveable { mutableStateOf(false) }

    val selected = products.firstOrNull {
        it.id == selectedId
    }

    val goBack: () -> Unit = {
        page = if (page == Page.Edit) {
            Page.Details
        } else {
            Page.List
        }
    }

    BackHandler(
        enabled = page != Page.List,
        onBack = { if (!actionSaving && !isSaving) goBack() }
    )

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Blue,
            onPrimary = White,
            background = Background,
            surface = White,
            onSurface = Ink,
            error = Red
        )
    ) {
        Column(
            modifier
                .fillMaxSize()
                .background(Background)
        ) {
            if (!isPreview && databaseStatus.isNotEmpty()) {
                Text(
                    text = databaseStatus,
                    color = Red,
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    )
                )
            }

            if (
                (page == Page.Add || page == Page.Edit) &&
                (isSaving || saveError.isNotEmpty())
            ) {
                Text(
                    text = if (isSaving) {
                        "Saving product…"
                    } else {
                        saveError
                    },
                    color = if (isSaving) Ink else Red,
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    )
                )
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (page) {
                    Page.List -> {
                        ProductList(
                            products = products,
                            today = today,
                            search = search,
                            onSearch = { search = it },
                            onAdd = {
                                saveError = ""
                                page = Page.Add
                            },
                            onSelect = {
                                selectedId = it
                                page = Page.Details
                            },
                            onRefresh = {
                                isRefreshing = true
                                reloadProducts++
                            },
                            isRefreshing = isRefreshing,
                            showArchived = showArchived,
                            onShowArchived = { showArchived = it }
                        )
                    }

                    Page.Details -> {
                        selected?.let { product ->
                            ProductDetails(
                                product = product,
                                today = today,
                                back = goBack,
                                edit = {
                                    saveError = ""
                                    page = Page.Edit
                                },
                                delete = {
                                    actionError = ""
                                    pendingAction = "delete"
                                },
                                archive = {
                                    actionError = ""
                                    pendingAction = "archive"
                                },
                                restore = {
                                    actionError = ""
                                    pendingAction = "restore"
                                },
                                busy = actionSaving
                            )
                        }
                    }

                    Page.Add, Page.Edit -> {
                        val editing = selected.takeIf {
                            page == Page.Edit
                        }

                        ProductForm(
                            product = editing,
                            back = goBack
                        ) { name, category, unit, photo, minimum, description ->
                            if (isSaving) return@ProductForm

                            saveError = ""
                            isSaving = true

                            scope.launch {
                                try {
                                    if (editing == null) {
                                        insertInventoryProduct(
                                            context.contentResolver,
                                            name,
                                            category,
                                            unit,
                                            photo,
                                            minimum,
                                            description
                                        )

                                        search = ""
                                        showArchived = false
                                        page = Page.List
                                        reloadProducts++
                                    } else {
                                        val savedImagePath =
                                            updateInventoryMinimum(
                                                productId = editing.id,
                                                minimum = minimum,
                                                description = description,
                                                resolver = context.contentResolver,
                                                photo = photo
                                            )

                                        val index = products.indexOfFirst {
                                            it.id == editing.id
                                        }

                                        if (index >= 0) {
                                            products[index] = editing.copy(
                                                name = name,
                                                category = category.name,
                                                unit = unit.name,
                                                photo = null,
                                                imagePath = savedImagePath,
                                                minimum = minimum,
                                                description = description
                                            )
                                        }

                                        page = Page.Details
                                    }
                                } catch (error: CancellationException) {
                                    throw error
                                } catch (error: Exception) {
                                    val reason = error.message.orEmpty()
                                        .substringBefore("URL:")
                                        .substringBefore("Headers:")
                                        .trim()

                                    val tag = if (editing == null) {
                                        "InventorySave"
                                    } else {
                                        "InventoryUpdate"
                                    }

                                    android.util.Log.w(
                                        tag,
                                        "${error.javaClass.simpleName}: $reason"
                                    )

                                    saveError = if (editing == null) {
                                        "Couldn't save product. Check Logcat."
                                    } else {
                                        "Couldn't update product. Check Logcat."
                                    }
                                } finally {
                                    isSaving = false
                                }
                            }
                        }
                    }
                }
            }
        }

        val action = pendingAction
        val actionProduct = selected
        if (action != null && actionProduct != null) {
            val label = when (action) {
                "archive" -> "Archive"
                "restore" -> "Restore"
                else -> "Delete"
            }
            InventoryDialog(
                onDismissRequest = { if (!actionSaving) pendingAction = null },
                title = { Text("$label this product?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(when (action) {
                            "archive" -> "Hide ${actionProduct.name} from the active catalogue? Its batch and movement history will be preserved."
                            "restore" -> "Return ${actionProduct.name} to the active catalogue?"
                            else -> "Permanently delete ${actionProduct.name}? This action cannot be undone."
                        })
                        if (actionError.isNotEmpty()) {
                            Text(actionError, color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    Button(enabled = !actionSaving, onClick = {
                        if (!actionSaving) {
                            actionSaving = true
                            actionError = ""
                            scope.launch {
                                try {
                                    changeInventoryProductState(actionProduct.id, action)
                                    // Only change the screen after Supabase confirms success.
                                    if (action == "delete") {
                                        products.removeAll { it.id == actionProduct.id }
                                    } else {
                                        val index = products.indexOfFirst { it.id == actionProduct.id }
                                        if (index >= 0) products[index] = actionProduct.copy(
                                            isActive = action == "restore"
                                        )
                                    }
                                    pendingAction = null
                                    selectedId = null
                                    page = Page.List
                                    reloadProducts++
                                } catch (error: CancellationException) {
                                    throw error
                                } catch (error: Exception) {
                                    android.util.Log.e("InventoryAction", "Failed to $action product", error)
                                    actionError = "Couldn't ${action.lowercase()} product. Refresh and try again; check Logcat InventoryAction if it continues."
                                } finally {
                                    actionSaving = false
                                }
                            }
                        }
                    }) { Text(if (actionSaving) "Saving…" else label) }
                },
                dismissButton = {
                    TextButton(enabled = !actionSaving, onClick = { pendingAction = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 820
)
@Composable
private fun InventoryScreenPreview() {
    InventoryScreen()
}