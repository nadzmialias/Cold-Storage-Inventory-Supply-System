package com.csiss.inventory.ui.inventory

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.csiss.inventory.data.insertInventoryProduct
import com.csiss.inventory.data.loadInventoryProducts
import com.csiss.inventory.data.updateInventoryMinimum
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class Page {
    List, Details, Add, Edit
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var hasLoadedData by remember {
        mutableStateOf(isPreview)
    }
    var manualRefreshPending by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    val requestRefresh: () -> Unit = {
        if (!isPreview && !isRefreshing && !manualRefreshPending) {
            manualRefreshPending = true
            reloadProducts++
        }
    }
    val products = remember { mutableStateListOf<Product>() }

    LaunchedEffect(isPreview, reloadProducts) {
        if (isPreview) return@LaunchedEffect

        isRefreshing = true

        try {
            val loadedProducts = loadInventoryProducts()
            products.clear()
            products.addAll(loadedProducts)
            hasLoadedData = true
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
            manualRefreshPending = false
        }
    }

    var page by remember { mutableStateOf(Page.List) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var deleteDialog by remember { mutableStateOf(false) }

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
        onBack = goBack
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
                                    containerColor = Blue,
                                    color = White,
                                    state = pullToRefreshState
                                )
                            }
                        ) {
                            ProductList(
                                hasLoadedData = hasLoadedData,
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
                                onRefresh = requestRefresh,
                                isRefreshing = isRefreshing
                            )
                        }
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
                                    deleteDialog = true
                                }
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

        if (deleteDialog && selected != null) {
            AlertDialog(
                onDismissRequest = {
                    deleteDialog = false
                },
                title = {
                    Text("Delete this product?")
                },
                text = {
                    Text(
                        "Remove ${selected.name}? Products with stock " +
                                "or movement history must be archived instead."
                    )
                },
                confirmButton = {
                    Button(
                        enabled = selected.canDelete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Red
                        ),
                        onClick = {
                            products.removeAll {
                                it.id == selected.id
                            }

                            selectedId = null
                            deleteDialog = false
                            page = Page.List
                        }
                    ) {
                        Text("Delete product")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            deleteDialog = false
                        }
                    ) {
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