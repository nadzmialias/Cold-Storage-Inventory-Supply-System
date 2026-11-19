package com.csiss.inventory.ui.supplier

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.csiss.inventory.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private enum class SupplierPage {
    List, Details, Add, Edit
}

@Composable
fun SupplierScreen(
    modifier: Modifier = Modifier,
    onHome: (() -> Unit)? = null,
    onAlerts: (() -> Unit)? = null,
    onProfile: (() -> Unit)? = null
) {
    val isPreview = LocalInspectionMode.current
    val scope = rememberCoroutineScope()

    var databaseStatus by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(!isPreview) }
    var reloadKey by remember { mutableIntStateOf(0) }

    val suppliers = remember { mutableStateListOf<Supplier>() }

    // Fetch suppliers on launch and refresh
    LaunchedEffect(isPreview, reloadKey) {
        if (isPreview) return@LaunchedEffect

        isRefreshing = true
        try {
            val list = loadSuppliersData()
            suppliers.clear()
            suppliers.addAll(list)
            databaseStatus = ""
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            android.util.Log.e("SupplierRead", "Failed to load suppliers", error)
            databaseStatus = "Couldn't load suppliers. Check network/Supabase."
        } finally {
            isRefreshing = false
        }
    }

    var page by remember { mutableStateOf(SupplierPage.List) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var deleteDialog by remember { mutableStateOf(false) }
    var toggleDialog by remember { mutableStateOf(false) }

    val selected = suppliers.firstOrNull { it.id == selectedId }

    val goBack: () -> Unit = {
        page = when (page) {
            SupplierPage.Edit -> SupplierPage.Details
            SupplierPage.Details -> SupplierPage.List
            SupplierPage.Add -> SupplierPage.List
            SupplierPage.List -> SupplierPage.List
        }
    }

    BackHandler(enabled = page != SupplierPage.List, onBack = goBack)

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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if ((page == SupplierPage.Add || page == SupplierPage.Edit) &&
                (isSaving || saveError.isNotEmpty())
            ) {
                Text(
                    text = if (isSaving) "Saving supplier…" else saveError,
                    color = if (isSaving) Ink else Red,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (page) {
                    SupplierPage.List -> {
                        SupplierList(
                            suppliers = suppliers,
                            search = search,
                            onSearch = { search = it },
                            onAdd = {
                                saveError = ""
                                page = SupplierPage.Add
                            },
                            onSelect = { id ->
                                selectedId = id
                                page = SupplierPage.Details
                            },
                            onRefresh = { reloadKey++ },
                            isRefreshing = isRefreshing
                        )
                    }

                    SupplierPage.Details -> {
                        if (selected != null) {
                            SupplierDetails(
                                supplier = selected,
                                back = { page = SupplierPage.List },
                                edit = {
                                    saveError = ""
                                    page = SupplierPage.Edit
                                },
                                toggleActive = { toggleDialog = true },
                                delete = { deleteDialog = true }
                            )
                        } else {
                            page = SupplierPage.List
                        }
                    }

                    SupplierPage.Add, SupplierPage.Edit -> {
                        SupplierForm(
                            supplier = if (page == SupplierPage.Edit) selected else null,
                            back = goBack,
                            save = { name, contactPerson, phone, email, address ->
                                isSaving = true
                                saveError = ""
                                scope.launch {
                                    try {
                                        if (selected == null || page == SupplierPage.Add) {
                                            insertSupplierData(name, contactPerson, phone, email, address)
                                            reloadKey++
                                            page = SupplierPage.List
                                        } else {
                                            updateSupplierData(selected.id, name, contactPerson, phone, email, address)
                                            val index = suppliers.indexOfFirst { it.id == selected.id }
                                            if (index >= 0) {
                                                suppliers[index] = selected.copy(
                                                    name = name,
                                                    contactPerson = contactPerson,
                                                    phone = phone,
                                                    email = email,
                                                    address = address
                                                )
                                            }
                                            page = SupplierPage.Details
                                        }
                                    } catch (error: CancellationException) {
                                        throw error
                                    } catch (error: Exception) {
                                        android.util.Log.e("SupplierSave", "Save error", error)
                                        saveError = "Failed to save supplier. Check network."
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            },
                            delete = if (page == SupplierPage.Edit) {
                                { deleteDialog = true }
                            } else null
                        )
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (deleteDialog && selected != null) {
            AlertDialog(
                onDismissRequest = { deleteDialog = false },
                title = { Text("Delete Supplier") },
                text = {
                    Text("Are you sure you want to permanently delete \"${selected.name}\"?")
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Red),
                        onClick = {
                            scope.launch {
                                try {
                                    deleteSupplierData(selected.id)
                                    suppliers.removeAll { it.id == selected.id }
                                    selectedId = null
                                    deleteDialog = false
                                    page = SupplierPage.List
                                } catch (e: Exception) {
                                    android.util.Log.e("SupplierDelete", "Error", e)
                                }
                            }
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { deleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Toggle Status Confirmation Dialog
        if (toggleDialog && selected != null) {
            val targetStatus = !selected.isActive
            val actionName = if (targetStatus) "activate" else "deactivate"
            AlertDialog(
                onDismissRequest = { toggleDialog = false },
                title = { Text(if (targetStatus) "Activate Supplier" else "Deactivate Supplier") },
                text = {
                    Text("Are you sure you want to $actionName \"${selected.name}\"?")
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (targetStatus) Color(0xFF059669) else Color(0xFFF59E0B)
                        ),
                        onClick = {
                            scope.launch {
                                try {
                                    setSupplierActiveData(selected.id, targetStatus)
                                    val index = suppliers.indexOfFirst { it.id == selected.id }
                                    if (index >= 0) {
                                        suppliers[index] = selected.copy(isActive = targetStatus)
                                    }
                                    toggleDialog = false
                                } catch (e: Exception) {
                                    android.util.Log.e("SupplierToggle", "Error", e)
                                }
                            }
                        }
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { toggleDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
