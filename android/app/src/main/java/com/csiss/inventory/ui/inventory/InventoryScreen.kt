package com.csiss.inventory.ui.inventory

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csiss.inventory.ui.components.BottomBar
import com.csiss.inventory.ui.components.BottomTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

private val Ink = Color(0xFF153154)
private val Muted = Color(0xFF526A88)
private val Blue = Color(0xFF1478ED)
private val Background = Color(0xFFF4F8FD)
private val Line = Color(0xFFDCE7F4)
private val White = Color.White
private val Red = Color(0xFFC32D56)
private val Shape = RoundedCornerShape(12.dp)
private val DateFormat = DateTimeFormatter.ofPattern("dd/MM/uuuu")
    .withResolverStyle(ResolverStyle.STRICT)

private data class Product(
    val id: Int,
    val name: String,
    val category: String,
    val quantity: Double,
    val unit: String,
    val expiry: LocalDate? = null,
    val minimum: Double = 0.0,
    val storage: String = "",
    val supplier: String = "",
    val description: String = "",
    val photo: Uri? = null,
    val hasHistory: Boolean = false
) {
    val canDelete: Boolean get() = quantity == 0.0 && !hasHistory
}

private enum class StockStatus(val title: String, val foreground: Color, val background: Color) {
    InStock("In stock", Color(0xFF176B52), Color(0xFFD7F4E7)),
    Low("Low stock", Color(0xFF9A4D0A), Color(0xFFFFECCD)),
    Near("Near expiry", Color(0xFF6736A6), Color(0xFFF0E6FF)),
    Expired("Expired", Color(0xFFAA2350), Color(0xFFFCE5EC)),
    Out("Out of stock", Color(0xFF495A70), Color(0xFFE8EEF6))
}

private fun Product.status(today: LocalDate): StockStatus {
    val date = expiry
    return when {
        date != null && date.isBefore(today) -> StockStatus.Expired
        quantity == 0.0 -> StockStatus.Out
        date != null && !date.isAfter(today.plusDays(7)) -> StockStatus.Near
        quantity <= minimum -> StockStatus.Low
        else -> StockStatus.InStock
    }
}

private fun Double.display(): String = if (this == toLong().toDouble()) {
    toLong().toString()
} else {
    toString()
}

private fun demoProducts(today: LocalDate) = listOf(
    Product(1, "Frozen Chicken Breast", "Frozen food", 42.5, "kg",
        today.plusDays(45), 10.0, "Freezer A", "FreshCo Supplies",
        "Frozen chicken breast for restaurants and retail."),
    Product(2, "Salmon Fillet", "Seafood", 6.0, "kg", minimum = 10.0),
    Product(3, "Fresh Milk 1L", "Dairy", 36.0, "pack", today.plusDays(4)),
    Product(4, "Frozen Vegetables", "Vegetables", 15.0, "kg"),
    Product(5, "Frozen Peas", "Vegetables", 0.0, "pack")
)

private enum class Page { List, Details, Add, Edit }

@Composable
fun InventoryScreen(
    modifier: Modifier = Modifier,
    onHome: (() -> Unit)? = null,
    onAlerts: (() -> Unit)? = null,
    onProfile: (() -> Unit)? = null
) {
    val today = LocalDate.now()
    val products = remember { mutableStateListOf<Product>().apply { addAll(demoProducts(today)) } }
    var nextId by remember { mutableIntStateOf(6) }
    var page by remember { mutableStateOf(Page.List) }
    var selectedId by remember { mutableStateOf<Int?>(null) }
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
                        ProductForm(editing, goBack) { name, category, quantity, unit, date, photo ->
                            if (editing == null) {
                                products.add(0, Product(nextId++, name, category, quantity, unit, date, photo = photo))
                                search = ""
                                page = Page.List
                            } else {
                                val index = products.indexOfFirst { it.id == editing.id }
                                if (index >= 0) {
                                    products[index] = editing.copy(name = name, category = category,
                                        quantity = quantity, unit = unit, expiry = date, photo = photo)
                                }
                                page = Page.Details
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

@Composable
private fun ProductList(
    products: List<Product>,
    today: LocalDate,
    search: String,
    onSearch: (String) -> Unit,
    onAdd: () -> Unit,
    onSelect: (Int) -> Unit
) {
    val filtered = products.filter { it.name.contains(search.trim(), ignoreCase = true) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Inventory Management", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Manage your products, stock and expiry dates.", color = Muted, fontSize = 12.sp)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Summary("▣", "Total items", products.size, Blue, Color(0xFFEAF3FE), Modifier.weight(1f))
                Summary("⚠", "Low stock", products.count { it.quantity <= it.minimum },
                    Color(0xFFA54E19), Color(0xFFFFF2E7), Modifier.weight(1f))
                Summary("▦", "Expiring soon", products.count { it.status(today) == StockStatus.Near },
                    Red, Color(0xFFFFEDF4), Modifier.weight(1f))
            }
        }
        item {
            Field(search, onSearch, "Search product…")
        }
        item { PrimaryButton("+ Add New Item", onAdd) }
        if (filtered.isEmpty()) item { Text("No products found.", color = Muted) }
        items(filtered, key = { it.id }) { product ->
            ProductCard(product, today, onClick = { onSelect(product.id) })
        }
    }
}

@Composable
private fun Summary(icon: String, label: String, count: Int, color: Color, bg: Color, modifier: Modifier) {
    Column(
        modifier.background(bg, Shape).padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(icon, color = color, fontSize = 19.sp)
        Text(label, color = color, fontSize = 10.sp)
        Text(count.toString(), color = color, fontWeight = FontWeight.Bold, fontSize = 21.sp)
    }
}

@Composable
private fun ProductCard(product: Product, today: LocalDate, onClick: (() -> Unit)? = null) {
    val status = product.status(today)
    val modifier = Modifier.fillMaxWidth()
    Surface(
        modifier = if (onClick == null) modifier else modifier.clickable(onClick = onClick),
        shape = Shape, color = White, border = BorderStroke(1.dp, Line)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(Color(0xFFEAF3FE), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center) { ProductThumbnail(product.photo) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(product.category, color = Muted, fontSize = 11.sp)
                Text("${product.quantity.display()} ${product.unit}", color = Muted, fontSize = 11.sp)
            }
            Text(status.title, color = status.foreground, fontSize = 10.sp,
                modifier = Modifier.background(status.background, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp))
            if (onClick != null) Text(" ›", color = Blue)
        }
    }
}

@Composable
private fun ProductThumbnail(uri: Uri?) {
    val resolver = LocalContext.current.contentResolver
    val bitmap by produceState<Bitmap?>(null, uri, resolver) {
        value = uri?.let { imageUri ->
            withContext(Dispatchers.IO) {
                try {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    resolver.openInputStream(imageUri)?.use {
                        BitmapFactory.decodeStream(it, null, bounds)
                    }
                    val options = BitmapFactory.Options()
                    while (maxOf(bounds.outWidth, bounds.outHeight) / options.inSampleSize.coerceAtLeast(1) > 256) {
                        options.inSampleSize = options.inSampleSize.coerceAtLeast(1) * 2
                    }
                    resolver.openInputStream(imageUri)?.use {
                        BitmapFactory.decodeStream(it, null, options)
                    }
                } catch (_: java.io.IOException) {
                    null
                } catch (_: SecurityException) {
                    null
                }
            }
        }
    }
    val image = bitmap
    if (image == null) {
        Text("❄", color = Blue)
    } else {
        Image(image.asImageBitmap(), contentDescription = "Product photo",
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop)
    }
}

@Composable
private fun ProductDetails(product: Product, today: LocalDate, back: () -> Unit,
                           edit: () -> Unit, delete: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenTitle("Product details", "Product catalogue / PRD-${product.id.toString().padStart(4, '0')}", back) }
        item { ProductCard(product, today) }
        item {
            DetailLine("Total available", "${product.quantity.display()} ${product.unit}")
            DetailLine("Minimum stock", "${product.minimum.display()} ${product.unit}")
            DetailLine("Nearest expiry", product.expiry?.format(DateFormat) ?: "—")
            DetailLine("Storage", product.storage.ifBlank { "—" })
            DetailLine("Supplier", product.supplier.ifBlank { "—" })
        }
        item {
            Text("Description", fontWeight = FontWeight.Bold)
            Text(product.description.ifBlank { "—" }, color = Muted)
        }
        item { PrimaryButton("Edit product", edit) }
        item {
            OutlinedButton(onClick = delete, modifier = Modifier.fillMaxWidth(), shape = Shape) {
                Text("Delete product")
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(label, Modifier.weight(1f), color = Muted, fontSize = 13.sp)
        Text(value, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
    HorizontalDivider(color = Line)
}

@Composable
private fun ProductForm(
    product: Product?,
    back: () -> Unit,
    save: (String, String, Double, String, LocalDate?, Uri?) -> Unit
) {
    var name by rememberSaveable(product?.id) { mutableStateOf(product?.name.orEmpty()) }
    var category by rememberSaveable(product?.id) { mutableStateOf(product?.category.orEmpty()) }
    var quantity by rememberSaveable(product?.id) { mutableStateOf(product?.quantity?.display().orEmpty()) }
    var unit by rememberSaveable(product?.id) { mutableStateOf(product?.unit.orEmpty()) }
    var expiry by rememberSaveable(product?.id) { mutableStateOf(product?.expiry?.format(DateFormat).orEmpty()) }
    var photo by rememberSaveable(product?.id) { mutableStateOf(product?.photo) }
    var error by rememberSaveable { mutableStateOf("") }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        if (it != null) photo = it
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenTitle(if (product == null) "Add product" else "Edit product", "Complete the required information.", back) }
        item { Field(name, { name = it }, "Product name *") }
        item { Choice("Category *", category, listOf("Frozen food", "Seafood", "Dairy", "Vegetables", "Other")) { category = it } }
        item { Field(quantity, { quantity = it }, "Quantity *", KeyboardType.Decimal) }
        item { Choice("Unit *", unit, listOf("kg", "pack", "pcs", "box", "L")) { unit = it } }
        item { Field(expiry, { expiry = it }, "Expiry date (optional)", placeholder = "dd/MM/yyyy") }
        item {
            Text("Photo (optional)", fontWeight = FontWeight.Medium)
            OutlinedButton(onClick = { photoPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth(), shape = Shape) {
                Text(if (photo == null) "Choose image" else "Image selected · Change image")
            }
        }
        if (error.isNotEmpty()) item { Text(error, color = Red) }
        item {
            PrimaryButton(if (product == null) "Save product" else "Update product", onClick = {
                val amount = quantity.trim().replace(',', '.').toDoubleOrNull()
                val date = runCatching { LocalDate.parse(expiry.trim(), DateFormat) }.getOrNull()
                error = when {
                    name.isBlank() || category.isBlank() || unit.isBlank() -> "Complete all required fields."
                    amount == null || !amount.isFinite() || amount < 0 -> "Enter a valid quantity of zero or more."
                    expiry.isNotBlank() && date == null -> "Enter a valid expiry date in dd/MM/yyyy format."
                    else -> ""
                }
                if (error.isEmpty() && amount != null) save(name.trim(), category, amount, unit, date, photo)
            })
        }
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String,
                  keyboard: KeyboardType = KeyboardType.Text, placeholder: String = "") {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        placeholder = if (placeholder.isEmpty()) null else { { Text(placeholder) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        singleLine = true, shape = Shape, modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = White, unfocusedContainerColor = White, unfocusedBorderColor = Line
        )
    )
}

@Composable
private fun Choice(label: String, value: String, options: List<String>, choose: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, fontWeight = FontWeight.Medium)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(),
                shape = Shape, border = BorderStroke(1.dp, Line),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = White)) {
                Text(value.ifEmpty { "Select ${label.substringBefore(" ").lowercase(Locale.ROOT)}" },
                    modifier = Modifier.weight(1f), color = Ink)
                Text("⌄")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = {
                        choose(option)
                        expanded = false
                    })
                }
            }
        }
    }
}

@Composable
private fun ScreenTitle(title: String, caption: String, back: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = back) { Text("‹ Back") }
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Text(caption, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick, shape = Shape, contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = White),
        modifier = Modifier.fillMaxWidth().height(46.dp).background(
            Brush.horizontalGradient(listOf(Blue, Color(0xFF459DED))), Shape
        )
    ) {
        Text(label, color = White, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 820)
@Composable
private fun InventoryScreenPreview() {
    InventoryScreen()
}