package com.csiss.inventory.ui.inventory

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.csiss.inventory.data.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.Locale

@Serializable
internal data class CategoryOption(
    @SerialName("category_id") val id: Long,
    @SerialName("category_name") val name: String
)

@Serializable
internal data class UnitOption(
    @SerialName("unit_id") val id: Long,
    @SerialName("unit_name") val name: String
)

@Serializable
private data class NewProductRow(
    @SerialName("product_code") val code: String,
    @SerialName("product_name") val name: String,
    val description: String,
    val category: String,
    val unit: String,
    @SerialName("category_id") val categoryId: Long,
    @SerialName("unit_id") val unitId: Long,
    @SerialName("min_stock_level") val minimum: Double,
    @SerialName("image_path")
    val imagePath: String? = null
)
@Serializable
private data class ProductRow(
    @SerialName("product_id") val id: Long,
    @SerialName("product_name") val name: String?,
    val category: String?,
    val unit: String?,
    @SerialName("image_path") val imagePath: String? = null,
    val description: String? = null,
    @SerialName("min_stock_level") val minimum: Double?,
    @SerialName("inventory_batches") val batches: List<InventoryBatchRow>
)

@Serializable
private data class InventoryBatchRow(
    @SerialName("quantity_remaining") val quantity: Double?,
    @SerialName("expiry_date") val expiry: String?
)

internal suspend fun loadInventoryProducts(): List<Product> {
    val rows = SupabaseClientProvider.client
        .from("products")
        .select(
            columns = Columns.raw(
                """
            product_id, product_name, category, unit, min_stock_level,image_path,description,
            inventory_batches(quantity_remaining, expiry_date)
            """.trimIndent()
            )
        ) {
            filter { eq("is_active", true) }
        }
        .decodeList<ProductRow>()

    return rows.map { row ->
        val batchesWithStock = row.batches.filter { (it.quantity ?: 0.0) > 0.0 }

        Product(
            id = row.id,
            name = row.name.orEmpty().ifBlank { "Unnamed product" },
            category = row.category.orEmpty(),
            quantity = batchesWithStock.sumOf { it.quantity ?: 0.0 },
            unit = row.unit.orEmpty(),
            expiry = batchesWithStock.mapNotNull { batch ->
                batch.expiry?.let { LocalDate.parse(it) }
            }.minOrNull(),
            minimum = row.minimum ?: 0.0,
            imagePath = row.imagePath,
            description = row.description.orEmpty(),
        )
    }

}

internal suspend fun insertInventoryProduct(
    resolver: android.content.ContentResolver,
    name: String,
    category: CategoryOption,
    unit: UnitOption,
    photo: Uri?,
    minimum: Double,
    description: String
) {
    val imagePath = photo?.let { selectedPhoto ->
        val uri = selectedPhoto
        val extension = when (resolver.getType(uri)) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> error("Choose a JPG, PNG, or WebP image.")
        }

        val bytes = withContext(Dispatchers.IO) {
            resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Couldn't read the selected image.")
        }

        val path = "products/${java.util.UUID.randomUUID()}.$extension"
        SupabaseClientProvider.client.storage
            .from("product-images")
            .upload(path, bytes)

        path
    }
    SupabaseClientProvider.client.from("products").insert(
        NewProductRow(
            code = "PRD-${java.util.UUID.randomUUID().toString().take(8)}",
            name = name,
            description = description,
            category = category.name,
            unit = unit.name,
            categoryId = category.id,
            unitId = unit.id,
            minimum = minimum,
            imagePath = imagePath
        )
    )

}

internal suspend fun loadCategories(): List<CategoryOption> =
    SupabaseClientProvider.client.from("categories")
        .select(columns = Columns.raw("category_id, category_name"))
        .decodeList<CategoryOption>()
        .sortedBy { it.name.lowercase(Locale.ROOT) }

internal suspend fun loadUnits(): List<UnitOption> =
    SupabaseClientProvider.client.from("units")
        .select(columns = Columns.raw("unit_id, unit_name"))
        .decodeList<UnitOption>()
        .sortedBy { it.name.lowercase(Locale.ROOT) }

internal suspend fun addInventoryOption(isCategory: Boolean, name: String) {
    val table = if (isCategory) "categories" else "units"
    val column = if (isCategory) "category_name" else "unit_name"
    SupabaseClientProvider.client.from(table).insert(mapOf(column to name))
}

internal suspend fun updateInventoryMinimum(
    productId: Long,
    minimum: Double,
    description: String
) {
    val updatedRows = SupabaseClientProvider.client.from("products")
        .update({
            set("min_stock_level", minimum)
            set("description", description)
        }) {
            filter { eq("product_id", productId) }
            select(columns = Columns.raw("product_id, description"))
        }
        .decodeList<UpdatedProductRow>()

    val updated = updatedRows.singleOrNull()
        ?: error("No product row updated for product_id=$productId")

    check(updated.description == description) {
        "Supabase returned a different description for product_id=$productId"
    }
}

@Serializable
private data class UpdatedProductRow(
    val description: String? = null
)
