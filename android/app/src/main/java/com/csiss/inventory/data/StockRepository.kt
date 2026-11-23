package com.csiss.inventory.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.ZoneId

// =====================================
// 1. MASTER & BATCH MODELS
// =====================================

@Serializable
data class ProductItem(
    @SerialName("product_id")
    val productId: Long,

    @SerialName("product_code")
    val productCode: String,

    @SerialName("product_name")
    val productName: String,

    val category: String,
    val unit: String = "units",

    @SerialName("min_stock_level")
    val minStockLevel: Double = 0.0,

    @SerialName("is_active")
    val isActive: Boolean = true
)

@Serializable
data class SupplierItem(
    @SerialName("supplier_id")
    val supplierId: Long,

    @SerialName("supplier_name")
    val supplierName: String,

    @SerialName("is_active")
    val isActive: Boolean = true
)

@Serializable
data class InventoryBatch(
    @SerialName("batch_id")
    val batchId: Long,

    @SerialName("product_id")
    val productId: Long,

    @SerialName("supplier_id")
    val supplierId: Long? = null,

    @SerialName("storage_id")
    val storageId: Long? = null,

    @SerialName("batch_code")
    val batchCode: String,

    @SerialName("quantity_remaining")
    val quantityRemaining: Double,

    @SerialName("expiry_date")
    val expiryDate: String,

    @SerialName("received_at")
    val receivedAt: String? = null
)

@Serializable
data class StockTransaction(
    @SerialName("movement_id")
    val movementId: Long,

    @SerialName("batch_id")
    val batchId: Long,

    @SerialName("user_id")
    val userId: String? = null,

    @SerialName("transaction_type")
    val transactionType: String,

    val reason: String? = null,
    val quantity: Double,

    @SerialName("reference_code")
    val referenceCode: String? = null,

    val notes: String? = null,

    @SerialName("recorded_at")
    val recordedAt: String
)

// =====================================
// 2. INSERT PAYLOADS
// =====================================

@Serializable
private data class NewBatchPayload(
    @SerialName("product_id")
    val productId: Long,

    @SerialName("supplier_id")
    val supplierId: Long?,

    @SerialName("storage_id")
    val storageId: Long,

    @SerialName("batch_code")
    val batchCode: String,

    @SerialName("quantity_remaining")
    val quantityRemaining: Double,

    @SerialName("expiry_date")
    val expiryDate: String
)

@Serializable
private data class NewTransactionPayload(
    @SerialName("batch_id")
    val batchId: Long,

    @SerialName("user_id")
    val userId: String?,

    @SerialName("transaction_type")
    val transactionType: String,

    val reason: String,
    val quantity: Double,

    @SerialName("reference_code")
    val referenceCode: String?,

    val notes: String?
)

// =====================================
// 3. UI SNAPSHOT MODEL
// =====================================

data class EnrichedBatch(
    val batch: InventoryBatch,
    val product: ProductItem?,
    val storage: StorageArea?,
    val supplier: SupplierItem?
) {
    val isLowStock: Boolean
        get() = product != null && batch.quantityRemaining <= product.minStockLevel

    val isExpiringSoon: Boolean
        get() = runCatching {
            val today = LocalDate.now(ZoneId.of("Asia/Kuala_Lumpur"))
            val exp = LocalDate.parse(batch.expiryDate)
            !exp.isAfter(today.plusDays(7))
        }.getOrDefault(false)

    val isExpired: Boolean
        get() = runCatching {
            val today = LocalDate.now(ZoneId.of("Asia/Kuala_Lumpur"))
            LocalDate.parse(batch.expiryDate).isBefore(today)
        }.getOrDefault(false)
}

data class StockSnapshot(
    val batches: List<EnrichedBatch>,
    val transactions: List<StockTransaction>,
    val products: List<ProductItem>,
    val storageAreas: List<StorageArea>,
    val suppliers: List<SupplierItem>
)

// =====================================
// 4. STOCK REPOSITORY
// =====================================

class StockRepository {

    private val client
        get() = SupabaseClientProvider.client

    suspend fun loadLive(): StockSnapshot = withContext(Dispatchers.IO) {
        coroutineScope {
            val productsDeferred = async {
                client.from("products")
                    .select(
                        columns = Columns.raw(
                            "product_id,product_code,product_name,category,unit,min_stock_level,is_active"
                        )
                    ) {
                        filter { eq("is_active", true) }
                        order("product_name", Order.ASCENDING)
                    }
                    .decodeList<ProductItem>()
            }

            val storagesDeferred = async {
                client.from("storage_areas")
                    .select(
                        columns = Columns.raw(
                            "storage_id,storage_name,min_temperature,max_temperature"
                        )
                    ) {
                        filter { eq("is_active", true) }
                        order("storage_id", Order.ASCENDING)
                    }
                    .decodeList<StorageArea>()
            }

            val suppliersDeferred = async {
                client.from("suppliers")
                    .select(
                        columns = Columns.raw(
                            "supplier_id,supplier_name,is_active"
                        )
                    ) {
                        filter { eq("is_active", true) }
                        order("supplier_name", Order.ASCENDING)
                    }
                    .decodeList<SupplierItem>()
            }

            val batchesDeferred = async {
                client.from("inventory_batches")
                    .select(
                        columns = Columns.raw(
                            "batch_id,product_id,supplier_id,storage_id,batch_code," +
                                    "quantity_remaining,expiry_date,received_at"
                        )
                    ) {
                        filter { gt("quantity_remaining", 0.0) }
                        order("expiry_date", Order.ASCENDING)
                    }
                    .decodeList<InventoryBatch>()
            }

            val transactionsDeferred = async {
                client.from("stock_transactions")
                    .select(
                        columns = Columns.raw(
                            "movement_id,batch_id,user_id,transaction_type,reason," +
                                    "quantity,reference_code,notes,recorded_at"
                        )
                    ) {
                        order("recorded_at", Order.DESCENDING)
                        limit(50)
                    }
                    .decodeList<StockTransaction>()
            }

            val products = productsDeferred.await()
            val storages = storagesDeferred.await()
            val suppliers = suppliersDeferred.await()
            val rawBatches = batchesDeferred.await()
            val transactions = transactionsDeferred.await()

            val productMap = products.associateBy { it.productId }
            val storageMap = storages.associateBy { it.storageId }
            val supplierMap = suppliers.associateBy { it.supplierId }

            val enriched = rawBatches.map { batch ->
                EnrichedBatch(
                    batch = batch,
                    product = productMap[batch.productId],
                    storage = batch.storageId?.let { storageMap[it] },
                    supplier = batch.supplierId?.let { supplierMap[it] }
                )
            }

            StockSnapshot(
                batches = enriched,
                transactions = transactions,
                products = products,
                storageAreas = storages,
                suppliers = suppliers
            )
        }
    }

    /**
     * STOCK IN: Creates a new inventory_batches record and logs an 'IN' transaction.
     * Returns the created StockTransaction so its movement_id can be referenced in Reorder.
     */
    suspend fun receiveStockBatch(
        userId: String?,
        productId: Long,
        supplierId: Long?,
        storageId: Long,
        batchCode: String,
        quantity: Double,
        expiryDate: String,
        referenceCode: String?,
        notes: String?
    ): StockTransaction = withContext(Dispatchers.IO) {
        require(quantity > 0.0) { "Quantity must be greater than zero." }
        require(batchCode.isNotBlank()) { "Batch code is required." }
        runCatching { LocalDate.parse(expiryDate.trim()) }
            .getOrElse { throw IllegalArgumentException("Expiry date must be YYYY-MM-DD.") }

        val createdBatch = client.from("inventory_batches")
            .insert(
                NewBatchPayload(
                    productId = productId,
                    supplierId = supplierId,
                    storageId = storageId,
                    batchCode = batchCode.trim(),
                    quantityRemaining = quantity,
                    expiryDate = expiryDate.trim()
                )
            ) {
                select(
                    columns = Columns.raw(
                        "batch_id,product_id,supplier_id,storage_id,batch_code," +
                                "quantity_remaining,expiry_date,received_at"
                    )
                )
            }
            .decodeSingle<InventoryBatch>()

        client.from("stock_transactions")
            .insert(
                NewTransactionPayload(
                    batchId = createdBatch.batchId,
                    userId = userId,
                    transactionType = "IN",
                    reason = "PURCHASE_RECEIPT",
                    quantity = quantity,
                    referenceCode = referenceCode?.trim()?.ifBlank { null },
                    notes = notes?.trim()?.ifBlank { null }
                )
            ) {
                select(
                    columns = Columns.raw(
                        "movement_id,batch_id,user_id,transaction_type,reason," +
                                "quantity,reference_code,notes,recorded_at"
                    )
                )
            }
            .decodeSingle<StockTransaction>()
    }

    /**
     * STOCK OUT / DISPOSAL / ADJUSTMENT: Logs movement & updates quantity_remaining
     */
    suspend fun recordStockMovement(
        userId: String?,
        batchId: Long,
        currentRemaining: Double,
        transactionType: String, // "OUT", "DISPOSAL", "ADJUSTMENT"
        reason: String,
        quantity: Double,
        referenceCode: String?,
        notes: String?
    ) = withContext(Dispatchers.IO) {
        require(quantity > 0.0) { "Quantity must be greater than zero." }

        if (transactionType in listOf("OUT", "DISPOSAL")) {
            require(quantity <= currentRemaining) {
                "Insufficient stock. Remaining: $currentRemaining"
            }
        }

        client.from("stock_transactions")
            .insert(
                NewTransactionPayload(
                    batchId = batchId,
                    userId = userId,
                    transactionType = transactionType,
                    reason = reason,
                    quantity = quantity,
                    referenceCode = referenceCode?.trim()?.ifBlank { null },
                    notes = notes?.trim()?.ifBlank { null }
                )
            )

        val updatedRemaining = when (transactionType) {
            "OUT", "DISPOSAL" -> (currentRemaining - quantity).coerceAtLeast(0.0)
            "ADJUSTMENT" -> quantity
            else -> currentRemaining
        }

        client.from("inventory_batches")
            .update({
                set("quantity_remaining", updatedRemaining)
            }) {
                filter { eq("batch_id", batchId) }
            }
    }
}