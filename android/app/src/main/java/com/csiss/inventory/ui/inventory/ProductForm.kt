package com.csiss.inventory.ui.inventory

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

@Composable
internal fun ProductForm(
    product: Product?,
    back: () -> Unit,
    save: (String, CategoryOption, UnitOption, Uri?, Double, String) -> Unit
) {
    var name by rememberSaveable(product?.id) { mutableStateOf(product?.name.orEmpty()) }
    var category by rememberSaveable(product?.id) { mutableStateOf(product?.category.orEmpty()) }
    var minimumStock by rememberSaveable(product?.id) { mutableStateOf(product?.minimum?.display() ?: "0") }
    var unit by rememberSaveable(product?.id) { mutableStateOf(product?.unit.orEmpty()) }
    var photo by rememberSaveable(product?.id) { mutableStateOf(product?.photo) }
    var description by rememberSaveable(product?.id) {
        mutableStateOf(product?.description.orEmpty())
    }
    var error by rememberSaveable { mutableStateOf("") }
    val isPreview = LocalInspectionMode.current
    var categories by remember { mutableStateOf<List<CategoryOption>>(emptyList()) }
    var units by remember { mutableStateOf<List<UnitOption>>(emptyList()) }
    var optionsLoading by remember { mutableStateOf(!isPreview) }
    var optionsError by remember { mutableStateOf(false) }
    var optionsRetry by remember { mutableIntStateOf(0) }

    LaunchedEffect(isPreview, optionsRetry) {
        if (isPreview) return@LaunchedEffect

        optionsLoading = true
        optionsError = false
        try {
            categories = loadCategories()
            units = loadUnits()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            android.util.Log.e("InventoryOptions", "Failed to load product options", error)
            optionsError = true
        } finally {
            optionsLoading = false
        }
    }
    val scope = rememberCoroutineScope()
    var adding by rememberSaveable { mutableStateOf<String?>(null) }
    var optionName by rememberSaveable { mutableStateOf("") }
    var optionError by remember { mutableStateOf("") }
    var optionSaving by remember { mutableStateOf(false) }
    val optionsReady = !optionsLoading && !optionsError
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        if (it != null) photo = it
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenTitle(if (product == null) "Add product" else "Edit product", "Complete the required information.", back) }
        item { Field(name, { name = it }, "Product name *") }
        if (optionsLoading) item { Text("Loading categories and units…") }
        if (optionsError) item {
            Text("Couldn't load categories and units.", color = Red)
            TextButton(onClick = { optionsRetry++ }) { Text("Retry") }
        }
        item {
            Choice("Category", category, categories.map { it.name }, optionsReady,
                onAdd = {
                    optionName = ""
                    optionError = ""
                    adding = "category"
                },
                choose = { category = it }
            )
        }
        item {
            Field(
                minimumStock,
                { minimumStock = it },
                "Minimum stock",
                KeyboardType.Decimal
            )
        }
        item {
            Choice("Unit", unit, units.map { it.name }, optionsReady,
                onAdd = {
                    optionName = ""
                    optionError = ""
                    adding = "unit"
                },
                choose = { unit = it }
            )
        }
        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = Shape,
                minLines = 3
            )
        }
        item {
            val savedImagePath = product?.imagePath
            val hasPhoto = photo != null || !savedImagePath.isNullOrBlank()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Photo (optional)", fontWeight = FontWeight.Medium)

                if (hasPhoto) {
                    Box(Modifier.fillMaxWidth().height(160.dp)) {
                        ProductThumbnail(
                            uri = photo,
                            imagePath = if (photo == null) savedImagePath else null
                        )
                    }
                }

                OutlinedButton(
                    onClick = { photoPicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shape
                ) {
                    Text(if (hasPhoto) "Change image" else "Choose image")
                }
            }
        }
        if (error.isNotEmpty()) item { Text(error, color = Red) }
        item {
            PrimaryButton(if (product == null) "Save product" else "Update product", onClick = {
                val minimum = minimumStock.trim().replace(',', '.').toDoubleOrNull()
                val selectedCategory = categories.firstOrNull { it.name.equals(category, ignoreCase = true) }
                val selectedUnit = units.firstOrNull { it.name.equals(unit, ignoreCase = true) }
                error = when {
                    !optionsReady -> "Wait for categories and units to load."
                    name.isBlank() || selectedCategory == null || selectedUnit == null -> "Complete all required fields."
                    minimum == null || !minimum.isFinite() || minimum < 0 ->
                        "Enter a valid minimum stock of zero or more."
                    else -> ""
                }
                if (error.isEmpty()  && selectedCategory != null && selectedUnit != null && minimum != null) {
                    save(name.trim(), selectedCategory,  selectedUnit, photo, minimum, description.trim())
                }
            })
        }
    }
    adding?.let { label ->
        AlertDialog(
            onDismissRequest = { if (!optionSaving) adding = null },
            title = { Text("Add $label") },
            text = {
                Column {
                    Field(optionName, { optionName = it }, "Name")
                    if (optionError.isNotEmpty()) Text(optionError, color = Red)
                }
            },
            confirmButton = {
                TextButton(enabled = !optionSaving && optionName.isNotBlank(), onClick = {
                    val newName = optionName.trim()
                    val isCategory = label == "category"
                    optionSaving = true
                    optionError = ""
                    scope.launch {
                        try {
                            addInventoryOption(isCategory, newName)
                            if (isCategory) category = newName else unit = newName
                            optionsRetry++
                            adding = null
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            android.util.Log.e("InventoryOptions", "Failed to add $label", error)
                            optionError = "Couldn't add $label. Check the name and your permissions."
                        } finally {
                            optionSaving = false
                        }
                    }
                }) { Text(if (optionSaving) "Saving…" else "Save") }
            },
            dismissButton = {
                TextButton(enabled = !optionSaving, onClick = { adding = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun Choice(
    label: String,
    value: String,
    options: List<String>,
    enabled: Boolean,
    onAdd: () -> Unit,
    choose: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val menuLabel = label.lowercase(Locale.ROOT)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fieldWidth = maxWidth
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = Shape,
            border = BorderStroke(1.dp, Line),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = White)
        ) {
            Column(Modifier.weight(1f)) {
                Text("$label *", style = MaterialTheme.typography.labelSmall, color = Muted)
                Text(value.ifEmpty { "Select $menuLabel" }, color = if (value.isEmpty()) Muted else Ink)
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
                    text = { Text(option) },
                    trailingIcon = if (option == value) ({ Text("✓", color = Blue) }) else null,
                    onClick = {
                        expanded = false
                        choose(option)
                    }
                )
            }
            if (options.isNotEmpty()) HorizontalDivider()
            DropdownMenuItem(
                text = { Text("+ Add $menuLabel", color = Blue, fontWeight = FontWeight.Medium) },
                onClick = {
                    expanded = false
                    onAdd()
                }
            )
        }
    }
}
