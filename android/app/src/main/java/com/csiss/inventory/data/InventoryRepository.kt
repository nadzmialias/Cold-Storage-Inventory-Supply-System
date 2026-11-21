package com.csiss.inventory.data

import android.content.ContentResolver
import android.net.Uri
import com.csiss.inventory.ui.inventory.Product
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

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
    @SerialName("image_path") val imagePath: String? = null
)

@Serializable
private data class ProductRow(
    @SerialName("product_id") val id: Long,
    @SerialName("product_name") val name: String?,
    @SerialName("is_active") val isActive: Boolean,
    val category: String?,
    val unit: String?,
    @SerialName("image_path") val imagePath: String? = null,
    val description: String? = null,
    @SerialName("min_stock_level") val minimum: Double?,
    @SerialName("inventory_batches")
    val batches: List<InventoryBatchRow>
)

@Serializable
private data class InventoryBatchRow(
    @SerialName("quantity_remaining") val quantity: Double?,
    @SerialName("expiry_date") val expiry: String?,
    @SerialName("stock_transactions") val movements: List<InventoryMovementRow> = emptyList(),
    val suppliers: BatchSupplierRow? = null,
    @SerialName("storage_areas")
    val storageArea: BatchStorageRow? = null
)

@Serializable
private data class InventoryMovementRow(
    @SerialName("movement_id") val id: Long
)

@Serializable
private data class BatchSupplierRow(
    @SerialName("supplier_name") val name: String? = null
)

@Serializable
private data class BatchStorageRow(
    @SerialName("storage_name") val name: String? = null
)

internal suspend fun loadInventoryProducts(active: Boolean? = true): List<Product> {
    val rows = SupabaseClientProvider.client
        .from("products")
        .select(
            columns = Columns.raw(
                """
                product_id,
                product_name,
                is_active,
                category,
                unit,
                min_stock_level,
                image_path,
                description,
                inventory_batches(
                    quantity_remaining,
                    expiry_date,
                    stock_transactions(movement_id),
                    suppliers(supplier_name),
                    storage_areas(storage_name)
                )
                """.trimIndent()
            )
        ) {
            filter {
                if (active != null) eq("is_active", active)
            }
        }
        .decodeList<ProductRow>()

    return rows.map { row ->
        val batchesWithStock = row.batches.filter {
            (it.quantity ?: 0.0) > 0.0
        }

        Product(
            id = row.id,
            isActive = row.isActive,
            hasBatches = row.batches.isNotEmpty(),
            hasHistory = row.batches.any { it.movements.isNotEmpty() },
            name = row.name.orEmpty().ifBlank {
                "Unnamed product"
            },
            category = row.category.orEmpty(),
            quantity = batchesWithStock.sumOf {
                it.quantity ?: 0.0
            },
            unit = row.unit.orEmpty(),
            expiry = batchesWithStock.mapNotNull { batch ->
                batch.expiry?.let {
                    LocalDate.parse(it)
                }
            }.minOrNull(),
            minimum = row.minimum ?: 0.0,
            imagePath = row.imagePath,
            description = row.description.orEmpty(),
            storage = batchesWithStock
                .mapNotNull { it.storageArea?.name }
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(", "),
            supplier = batchesWithStock
                .mapNotNull { it.suppliers?.name }
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(", ")
        )
    }
}

private suspend fun uploadProductImage(
    resolver: ContentResolver,
    uri: Uri
): String {
    val extension = when (resolver.getType(uri)) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> error("Choose a JPG, PNG, or WebP image.")
    }

    val bytes = withContext(Dispatchers.IO) {
        resolver.openInputStream(uri)?.use {
            it.readBytes()
        } ?: error("Couldn't read the selected image.")
    }

    val path = "products/${UUID.randomUUID()}.$extension"

    SupabaseClientProvider.client.storage
        .from("product-images")
        .upload(path, bytes)

    return path
}

internal suspend fun insertInventoryProduct(
    resolver: ContentResolver,
    name: String,
    category: CategoryOption,
    unit: UnitOption,
    photo: Uri?,
    minimum: Double,
    description: String
) {
    val imagePath = photo?.let {
        uploadProductImage(resolver, it)
    }

    SupabaseClientProvider.client
        .from("products")
        .insert(
            NewProductRow(
                code = "PRD-${UUID.randomUUID().toString().take(8)}",
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
    SupabaseClientProvider.client
        .from("categories")
        .select(
            columns = Columns.raw("category_id, category_name")
        )
        .decodeList<CategoryOption>()
        .sortedBy {
            it.name.lowercase(Locale.ROOT)
        }

internal suspend fun loadUnits(): List<UnitOption> =
    SupabaseClientProvider.client
        .from("units")
        .select(
            columns = Columns.raw("unit_id, unit_name")
        )
        .decodeList<UnitOption>()
        .sortedBy {
            it.name.lowercase(Locale.ROOT)
        }

internal suspend fun addInventoryOption(
    isCategory: Boolean,
    name: String
) {
    val table = if (isCategory) "categories" else "units"
    val column = if (isCategory) "category_name" else "unit_name"

    SupabaseClientProvider.client
        .from(table)
        .insert(mapOf(column to name))
}

internal suspend fun updateInventoryMinimum(
    productId: Long,
    minimum: Double,
    description: String,
    resolver: ContentResolver,
    photo: Uri?
): String? {
    val newImagePath = photo?.let {
        uploadProductImage(resolver, it)
    }

    val updatedRows = SupabaseClientProvider.client
        .from("products")
        .update({
            set("min_stock_level", minimum)
            set("description", description)

            if (newImagePath != null) {
                set("image_path", newImagePath)
            }
        }) {
            filter {
                eq("product_id", productId)
            }

            select(
                columns = Columns.raw("description, image_path")
            )
        }
        .decodeList<UpdatedProductRow>()

    val updated = updatedRows.singleOrNull()
        ?: error("No product row updated for product_id=$productId")

    check(updated.description == description) {
        "Supabase returned a different description."
    }

    if (newImagePath != null) {
        check(updated.imagePath == newImagePath) {
            "Supabase returned a different image path."
        }
    }

    return updated.imagePath
}

@Serializable
private data class UpdatedProductRow(
    val description: String? = null,
    @SerialName("image_path") val imagePath: String? = null
)

internal suspend fun changeInventoryProductState(productId: Long, action: String) {
    require(action in setOf("archive", "restore", "delete"))

    SupabaseClientProvider.client.postgrest.rpc(
        function = "csiss_inventory_product_action",
        parameters = buildJsonObject {
            put("p_product_id", JsonPrimitive(productId))
            put("p_action", JsonPrimitive(action))
        }
    )
}
