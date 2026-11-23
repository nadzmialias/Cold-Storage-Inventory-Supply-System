package com.csiss.inventory.data

import io.github.jan.supabase.auth.auth
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
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ReportType(
    val dbValue: String,
    val label: String,
    val shortLabel: String,
    val description: String,
    val supportsDateRange: Boolean
) {
    INVENTORY(
        dbValue = "INVENTORY",
        label = "Inventory Summary",
        shortLabel = "Inventory",
        description = "Current product catalogue, available quantities, and stock status.",
        supportsDateRange = false
    ),
    STOCK_MOVEMENT(
        dbValue = "STOCK_MOVEMENT",
        label = "Stock Movement",
        shortLabel = "Stock",
        description = "Stock In and Stock Out transactions with reference codes and reasons.",
        supportsDateRange = true
    ),
    EXPIRY(
        dbValue = "EXPIRY",
        label = "Expiry Tracking",
        shortLabel = "Expiry",
        description = "Active batches ordered by expiry date, highlighting near-expiry & expired stock.",
        supportsDateRange = false
    ),
    SUPPLIER(
        dbValue = "SUPPLIER",
        label = "Supplier Directory",
        shortLabel = "Supplier",
        description = "Registered suppliers, contacts, and active/inactive status.",
        supportsDateRange = false
    ),
    TEMPERATURE(
        dbValue = "TEMPERATURE",
        label = "Temperature Logs",
        shortLabel = "Temp",
        description = "Cold storage readings, min/max compliance, and abnormal temperature events.",
        supportsDateRange = true
    );

    companion object {
        fun fromDb(value: String): ReportType =
            entries.firstOrNull { it.dbValue.equals(value, ignoreCase = true) } ?: INVENTORY
    }
}

enum class ReportFormat(
    val dbValue: String,
    val label: String,
    val mimeType: String,
    val extension: String
) {
    PDF("PDF", "PDF", "application/pdf", "pdf"),
    CSV("CSV", "CSV", "text/csv", "csv");

    companion object {
        fun fromDb(value: String): ReportFormat =
            entries.firstOrNull { it.dbValue.equals(value, ignoreCase = true) } ?: PDF
    }
}

@Serializable
data class ReportRecord(
    @SerialName("report_id") val reportId: Long,
    @SerialName("report_name") val reportName: String,
    @SerialName("report_type") val reportType: String,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("file_format") val fileFormat: String,
    @SerialName("generated_by") val generatedBy: String,
    @SerialName("generated_at") val generatedAt: String
)

@Serializable
private data class NewReportInsert(
    @SerialName("report_name") val reportName: String,
    @SerialName("report_type") val reportType: String,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("file_format") val fileFormat: String,
    @SerialName("generated_by") val generatedBy: String
)

@Serializable
private data class ReportProductRow(
    @SerialName("product_id") val productId: Long,
    @SerialName("product_code") val productCode: String = "",
    @SerialName("product_name") val productName: String,
    val category: String = "",
    val unit: String = "",
    @SerialName("min_stock_level") val minStockLevel: Double = 0.0,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
private data class ReportBatchRow(
    @SerialName("batch_id") val batchId: Long,
    @SerialName("product_id") val productId: Long,
    @SerialName("storage_id") val storageId: Long,
    @SerialName("batch_code") val batchCode: String,
    @SerialName("quantity_remaining") val quantityRemaining: Double = 0.0,
    @SerialName("expiry_date") val expiryDate: String
)

@Serializable
private data class ReportMovementRow(
    @SerialName("movement_id") val movementId: Long,
    @SerialName("batch_id") val batchId: Long,
    @SerialName("transaction_type") val transactionType: String,
    val reason: String = "",
    val quantity: Double = 0.0,
    @SerialName("reference_code") val referenceCode: String? = null,
    @SerialName("recorded_at") val recordedAt: String
)

@Serializable
private data class ReportSupplierRow(
    @SerialName("supplier_id") val supplierId: Long,
    @SerialName("supplier_name") val supplierName: String,
    @SerialName("contact_person") val contactPerson: String? = null,
    val phone: String? = null,
    val email: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
private data class ReportStorageRow(
    @SerialName("storage_id") val storageId: Long,
    @SerialName("storage_name") val storageName: String,
    @SerialName("min_temperature") val minTemperature: Double = 0.0,
    @SerialName("max_temperature") val maxTemperature: Double = 0.0
)

@Serializable
private data class ReportTempLogRow(
    @SerialName("log_id") val logId: Long,
    @SerialName("storage_id") val storageId: Long,
    val temperature: Double,
    @SerialName("min_limit_at_reading") val minLimit: Double,
    @SerialName("max_limit_at_reading") val maxLimit: Double,
    @SerialName("recorded_at") val recordedAt: String
)

data class GeneratedReportData(
    val reportName: String,
    val type: ReportType,
    val dateRangeLabel: String,
    val generatedAtLabel: String,
    val summaryMetrics: List<Pair<String, String>>,
    val headers: List<String>,
    val columnWeights: List<Float>,
    val rows: List<List<String>>
)

class ReportRepository {

    private val client
        get() = SupabaseClientProvider.client

    private val displayTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH)
    private val displayDateFormatter =
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

    suspend fun loadReportHistory(): List<ReportRecord> = withContext(Dispatchers.IO) {
        client.from("reports")
            .select(
                columns = Columns.raw(
                    "report_id,report_name,report_type,start_date,end_date,file_format,generated_by,generated_at"
                )
            ) {
                order(column = "generated_at", order = Order.DESCENDING)
                limit(100)
            }
            .decodeList<ReportRecord>()
    }

    suspend fun buildReportData(
        type: ReportType,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): GeneratedReportData = withContext(Dispatchers.IO) {
        val now = OffsetDateTime.now(ZoneId.systemDefault())
        val generatedAtLabel = now.format(displayTimeFormatter)
        val dateRangeLabel = if (startDate != null && endDate != null) {
            "${startDate.format(displayDateFormatter)} – ${endDate.format(displayDateFormatter)}"
        } else {
            "All Dates (Snapshot)"
        }
        val reportName = buildReportName(type, startDate, endDate, now.toLocalDate())

        when (type) {
            ReportType.INVENTORY -> buildInventoryReport(reportName, dateRangeLabel, generatedAtLabel)
            ReportType.STOCK_MOVEMENT -> buildStockMovementReport(
                reportName, dateRangeLabel, generatedAtLabel, startDate, endDate
            )
            ReportType.EXPIRY -> buildExpiryReport(reportName, dateRangeLabel, generatedAtLabel)
            ReportType.SUPPLIER -> buildSupplierReport(reportName, dateRangeLabel, generatedAtLabel)
            ReportType.TEMPERATURE -> buildTemperatureReport(
                reportName, dateRangeLabel, generatedAtLabel, startDate, endDate
            )
        }
    }

    suspend fun saveReportRecord(
        reportName: String,
        type: ReportType,
        startDate: LocalDate?,
        endDate: LocalDate?,
        format: ReportFormat
    ): ReportRecord = withContext(Dispatchers.IO) {
        val userId = client.auth.currentUserOrNull()?.id
            ?: error("Please sign in again to save report history.")

        val validRange = startDate != null && endDate != null
        val payload = NewReportInsert(
            reportName = reportName.take(150),
            reportType = type.dbValue,
            startDate = if (validRange) startDate.toString() else null,
            endDate = if (validRange) endDate.toString() else null,
            fileFormat = format.dbValue,
            generatedBy = userId
        )

        client.from("reports")
            .insert(payload) {
                select(
                    columns = Columns.raw(
                        "report_id,report_name,report_type,start_date,end_date,file_format,generated_by,generated_at"
                    )
                )
            }
            .decodeSingle<ReportRecord>()
    }

    private suspend fun buildInventoryReport(
        reportName: String,
        dateRangeLabel: String,
        generatedAtLabel: String
    ): GeneratedReportData = coroutineScope {
        val productsDeferred = async {
            client.from("products")
                .select(
                    Columns.raw("product_id,product_code,product_name,category,unit,min_stock_level,is_active")
                ) {
                    order("product_name", Order.ASCENDING)
                }
                .decodeList<ReportProductRow>()
        }
        val batchesDeferred = async {
            client.from("inventory_batches")
                .select(Columns.raw("batch_id,product_id,storage_id,batch_code,quantity_remaining,expiry_date"))
                .decodeList<ReportBatchRow>()
        }

        val products = productsDeferred.await().filter { it.isActive }
        val batches = batchesDeferred.await()
        val qtyByProduct = batches.groupBy { it.productId }
            .mapValues { (_, list) -> list.sumOf { it.quantityRemaining } }

        var lowCount = 0
        var outCount = 0
        val rows = products.map { p ->
            val available = qtyByProduct[p.productId] ?: 0.0
            val status = when {
                available <= 0.0 -> {
                    outCount++
                    "Out of Stock"
                }
                available <= p.minStockLevel -> {
                    lowCount++
                    "Low Stock"
                }
                else -> "In Stock"
            }
            listOf(
                p.productCode.ifBlank { "PRD-${p.productId}" },
                p.productName,
                p.category.ifBlank { "—" },
                "${formatQty(available)} ${p.unit}".trim(),
                "${formatQty(p.minStockLevel)} ${p.unit}".trim(),
                status
            )
        }

        GeneratedReportData(
            reportName = reportName,
            type = ReportType.INVENTORY,
            dateRangeLabel = dateRangeLabel,
            generatedAtLabel = generatedAtLabel,
            summaryMetrics = listOf(
                "Active Products" to products.size.toString(),
                "Low Stock" to lowCount.toString(),
                "Out of Stock" to outCount.toString()
            ),
            headers = listOf("Code", "Product", "Category", "Available", "Min Level", "Status"),
            columnWeights = listOf(1.1f, 2.1f, 1.3f, 1.2f, 1.1f, 1.2f),
            rows = rows
        )
    }

    private suspend fun buildStockMovementReport(
        reportName: String,
        dateRangeLabel: String,
        generatedAtLabel: String,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): GeneratedReportData = coroutineScope {
        val zone = ZoneId.systemDefault()
        val startIso = startDate?.atStartOfDay(zone)?.toOffsetDateTime()?.toString()
        val endIso = endDate?.plusDays(1)?.atStartOfDay(zone)?.minusSeconds(1)?.toOffsetDateTime()?.toString()

        val movementsDeferred = async {
            client.from("stock_transactions")
                .select(
                    Columns.raw("movement_id,batch_id,transaction_type,reason,quantity,reference_code,recorded_at")
                ) {
                    filter {
                        if (startIso != null) gte("recorded_at", startIso)
                        if (endIso != null) lte("recorded_at", endIso)
                    }
                    order("recorded_at", Order.DESCENDING)
                    limit(500)
                }
                .decodeList<ReportMovementRow>()
        }
        val batchesDeferred = async {
            client.from("inventory_batches")
                .select(Columns.raw("batch_id,product_id,storage_id,batch_code,quantity_remaining,expiry_date"))
                .decodeList<ReportBatchRow>()
        }
        val productsDeferred = async {
            client.from("products")
                .select(Columns.raw("product_id,product_code,product_name,category,unit,min_stock_level,is_active"))
                .decodeList<ReportProductRow>()
        }

        val movements = movementsDeferred.await()
        val batchMap = batchesDeferred.await().associateBy { it.batchId }
        val productMap = productsDeferred.await().associateBy { it.productId }

        val inCount = movements.count { it.transactionType.equals("IN", ignoreCase = true) }
        val outCount = movements.size - inCount

        val rows = movements.map { m ->
            val batch = batchMap[m.batchId]
            val product = batch?.let { productMap[it.productId] }
            listOf(
                formatTimestamp(m.recordedAt),
                m.transactionType,
                product?.productName ?: "Batch #${m.batchId}",
                batch?.batchCode ?: "#${m.batchId}",
                formatQty(m.quantity),
                m.reason.replace('_', ' ').ifBlank { "—" }
            )
        }

        GeneratedReportData(
            reportName = reportName,
            type = ReportType.STOCK_MOVEMENT,
            dateRangeLabel = dateRangeLabel,
            generatedAtLabel = generatedAtLabel,
            summaryMetrics = listOf(
                "Total Movements" to movements.size.toString(),
                "Stock In" to inCount.toString(),
                "Stock Out" to outCount.toString()
            ),
            headers = listOf("Date", "Type", "Product", "Batch", "Qty", "Reason"),
            columnWeights = listOf(1.6f, 0.7f, 1.9f, 1.3f, 0.9f, 1.6f),
            rows = rows
        )
    }

    private suspend fun buildExpiryReport(
        reportName: String,
        dateRangeLabel: String,
        generatedAtLabel: String
    ): GeneratedReportData = coroutineScope {
        val batchesDeferred = async {
            client.from("inventory_batches")
                .select(Columns.raw("batch_id,product_id,storage_id,batch_code,quantity_remaining,expiry_date")) {
                    filter { gt("quantity_remaining", 0) }
                    order("expiry_date", Order.ASCENDING)
                }
                .decodeList<ReportBatchRow>()
        }
        val productsDeferred = async {
            client.from("products")
                .select(Columns.raw("product_id,product_code,product_name,category,unit,min_stock_level,is_active"))
                .decodeList<ReportProductRow>()
        }
        val storagesDeferred = async {
            client.from("storage_areas")
                .select(Columns.raw("storage_id,storage_name,min_temperature,max_temperature"))
                .decodeList<ReportStorageRow>()
        }

        val batches = batchesDeferred.await()
        val productMap = productsDeferred.await().associateBy { it.productId }
        val storageMap = storagesDeferred.await().associateBy { it.storageId }
        val today = LocalDate.now()

        var expiredCount = 0
        var nearCount = 0
        val rows = batches.map { b ->
            val product = productMap[b.productId]
            val storage = storageMap[b.storageId]
            val expiry = runCatching { LocalDate.parse(b.expiryDate) }.getOrNull()
            val status = when {
                expiry != null && expiry.isBefore(today) -> {
                    expiredCount++
                    "Expired"
                }
                expiry != null && !expiry.isAfter(today.plusDays(7)) -> {
                    nearCount++
                    "Expiring <=7d"
                }
                else -> "Valid"
            }
            listOf(
                product?.productName ?: "Product #${b.productId}",
                b.batchCode,
                storage?.storageName ?: "Zone #${b.storageId}",
                "${formatQty(b.quantityRemaining)} ${product?.unit.orEmpty()}".trim(),
                expiry?.format(displayDateFormatter) ?: b.expiryDate,
                status
            )
        }

        GeneratedReportData(
            reportName = reportName,
            type = ReportType.EXPIRY,
            dateRangeLabel = dateRangeLabel,
            generatedAtLabel = generatedAtLabel,
            summaryMetrics = listOf(
                "Active Batches" to batches.size.toString(),
                "Expiring <=7d" to nearCount.toString(),
                "Expired" to expiredCount.toString()
            ),
            headers = listOf("Product", "Batch", "Storage", "Remaining", "Expiry Date", "Status"),
            columnWeights = listOf(1.9f, 1.4f, 1.3f, 1.1f, 1.3f, 1.1f),
            rows = rows
        )
    }

    private suspend fun buildSupplierReport(
        reportName: String,
        dateRangeLabel: String,
        generatedAtLabel: String
    ): GeneratedReportData {
        val suppliers = client.from("suppliers")
            .select(Columns.raw("supplier_id,supplier_name,contact_person,phone,email,is_active")) {
                order("supplier_name", Order.ASCENDING)
            }
            .decodeList<ReportSupplierRow>()

        val activeCount = suppliers.count { it.isActive }
        val inactiveCount = suppliers.size - activeCount

        val rows = suppliers.map { s ->
            listOf(
                s.supplierName,
                s.contactPerson.orEmpty().ifBlank { "—" },
                s.phone.orEmpty().ifBlank { "—" },
                s.email.orEmpty().ifBlank { "—" },
                if (s.isActive) "Active" else "Inactive"
            )
        }

        return GeneratedReportData(
            reportName = reportName,
            type = ReportType.SUPPLIER,
            dateRangeLabel = dateRangeLabel,
            generatedAtLabel = generatedAtLabel,
            summaryMetrics = listOf(
                "Total Suppliers" to suppliers.size.toString(),
                "Active" to activeCount.toString(),
                "Inactive" to inactiveCount.toString()
            ),
            headers = listOf("Supplier", "Contact", "Phone", "Email", "Status"),
            columnWeights = listOf(2.0f, 1.5f, 1.3f, 2.2f, 1.0f),
            rows = rows
        )
    }

    private suspend fun buildTemperatureReport(
        reportName: String,
        dateRangeLabel: String,
        generatedAtLabel: String,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): GeneratedReportData = coroutineScope {
        val zone = ZoneId.systemDefault()
        val startIso = startDate?.atStartOfDay(zone)?.toOffsetDateTime()?.toString()
        val endIso = endDate?.plusDays(1)?.atStartOfDay(zone)?.minusSeconds(1)?.toOffsetDateTime()?.toString()

        val storagesDeferred = async {
            client.from("storage_areas")
                .select(Columns.raw("storage_id,storage_name,min_temperature,max_temperature"))
                .decodeList<ReportStorageRow>()
        }
        val logsDeferred = async {
            client.from("temperature_logs")
                .select(
                    Columns.raw("log_id,storage_id,temperature,min_limit_at_reading,max_limit_at_reading,recorded_at")
                ) {
                    filter {
                        if (startIso != null) gte("recorded_at", startIso)
                        if (endIso != null) lte("recorded_at", endIso)
                    }
                    order("recorded_at", Order.DESCENDING)
                    limit(500)
                }
                .decodeList<ReportTempLogRow>()
        }

        val storageMap = storagesDeferred.await().associateBy { it.storageId }
        val logs = logsDeferred.await()

        val avgTemp = if (logs.isNotEmpty()) {
            String.format(Locale.US, "%.1f°C", logs.map { it.temperature }.average())
        } else "—"
        val abnormalCount = logs.count { it.temperature < it.minLimit || it.temperature > it.maxLimit }

        val rows = logs.map { log ->
            val abnormal = log.temperature < log.minLimit || log.temperature > log.maxLimit
            val zoneName = storageMap[log.storageId]?.storageName ?: "Zone #${log.storageId}"
            listOf(
                formatTimestamp(log.recordedAt),
                zoneName,
                String.format(Locale.US, "%.1f°C", log.temperature),
                String.format(Locale.US, "%.1f to %.1f°C", log.minLimit, log.maxLimit),
                if (abnormal) "Abnormal" else "Normal"
            )
        }

        GeneratedReportData(
            reportName = reportName,
            type = ReportType.TEMPERATURE,
            dateRangeLabel = dateRangeLabel,
            generatedAtLabel = generatedAtLabel,
            summaryMetrics = listOf(
                "Readings Included" to logs.size.toString(),
                "Average Temp" to avgTemp,
                "Out of Range" to abnormalCount.toString()
            ),
            headers = listOf("Recorded At", "Storage Zone", "Temp", "Allowed Range", "Status"),
            columnWeights = listOf(1.9f, 1.8f, 1.0f, 1.7f, 1.1f),
            rows = rows
        )
    }

    private fun buildReportName(
        type: ReportType,
        startDate: LocalDate?,
        endDate: LocalDate?,
        today: LocalDate
    ): String {
        val suffix = if (startDate != null && endDate != null) {
            "${startDate}_${endDate}"
        } else {
            today.toString()
        }
        return "${type.label} ($suffix)"
    }

    private fun formatQty(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString()
        else String.format(Locale.US, "%.2f", value)

    private fun formatTimestamp(raw: String): String =
        runCatching {
            OffsetDateTime.parse(raw)
                .atZoneSameInstant(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale.ENGLISH))
        }.getOrElse { raw }
}
