package com.csiss.inventory.data

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

@Serializable
data class SystemAlertRow(
    @SerialName("alert_id") val alertId: Long = 0,
    @SerialName("product_id") val productId: Long? = null,
    @SerialName("batch_id") val batchId: Long? = null,
    @SerialName("storage_id") val storageId: Long? = null,
    @SerialName("temperature_log_id") val temperatureLogId: Long? = null,
    @SerialName("alert_type") val alertType: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("status") val status: String = "OPEN",
    @SerialName("acknowledged_at") val acknowledgedAt: String? = null,
    @SerialName("resolved_at") val resolvedAt: String? = null,
    @SerialName("created_at") val createdAt: String = ""
) {
    val isResolved: Boolean
        get() = status.uppercase(Locale.ROOT) in setOf("RESOLVED", "DONE", "CLOSED")

    val isAcknowledged: Boolean
        get() = status.uppercase(Locale.ROOT) == "ACKNOWLEDGED"

    val isStockRelated: Boolean
        get() {
            val t = alertType.uppercase(Locale.ROOT)
            return batchId != null || t.contains("STOCK") || t.contains("EXPIR")
        }

    val isTempRelated: Boolean
        get() {
            val t = alertType.uppercase(Locale.ROOT)
            return temperatureLogId != null || t.contains("TEMP") || t == "SENSOR_OFFLINE"
        }
}

@Serializable
private data class NewSystemAlertInsert(
    @SerialName("product_id") val productId: Long? = null,
    @SerialName("batch_id") val batchId: Long? = null,
    @SerialName("storage_id") val storageId: Long? = null,
    @SerialName("temperature_log_id") val temperatureLogId: Long? = null,
    @SerialName("alert_type") val alertType: String,
    @SerialName("title") val title: String,
    @SerialName("message") val message: String,
    @SerialName("status") val status: String
)

@Serializable
private data class AlertStatusUpdate(
    @SerialName("status") val status: String,
    @SerialName("acknowledged_at") val acknowledgedAt: String? = null,
    @SerialName("resolved_at") val resolvedAt: String? = null
)

class AlertRepository {
    private val db get() = SupabaseClientProvider.client.postgrest

    companion object {
        // Tracks locally resolved/acknowledged synthetic alerts if DB insert wasn't permitted
        private val localStatusOverrides = mutableMapOf<Long, String>()
    }

    suspend fun loadAndSyncAlerts(): List<SystemAlertRow> {
        val existing = runCatching {
            db.from("system_alerts")
                .select {
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<SystemAlertRow>()
        }.getOrDefault(emptyList())

        val stockSnapshot = runCatching { StockRepository().loadLive() }.getOrNull()
        val today = LocalDate.now()
        val nowIso = Instant.now().toString()
        val fallbackLiveAlerts = mutableListOf<SystemAlertRow>()

        if (stockSnapshot != null) {
            val storageById = stockSnapshot.storageAreas.associateBy { it.storageId }

            for (item in stockSnapshot.batches) {
                val prodName = item.product?.productName ?: "Product #${item.batch.productId}"
                val unit = item.product?.unit ?: "kg"
                val zoneName = storageById[item.batch.storageId]?.storageName ?: "Cold Storage"
                val batchTimestamp = item.batch.receivedAt?.takeIf { it.isNotBlank() } ?: nowIso

                // 1. Check Low Stock
                if (item.isLowStock) {
                    val alreadyInDb = existing.any {
                        it.batchId == item.batch.batchId &&
                                it.alertType.uppercase(Locale.ROOT).contains("STOCK") &&
                                !it.isResolved
                    }
                    val title = "Low Stock: $prodName"
                    val msg = "Batch ${item.batch.batchCode} in $zoneName has only ${String.format(Locale.US, "%.2f %s", item.batch.quantityRemaining, unit)} left."

                    if (!alreadyInDb) {
                        val inserted = tryInsertAlert(
                            productId = item.batch.productId,
                            batchId = item.batch.batchId,
                            storageId = item.batch.storageId,
                            alertType = "LOW_STOCK",
                            title = title,
                            message = msg
                        )
                        if (!inserted) {
                            val syntheticId = -(item.batch.batchId * 10 + 1)
                            val overrideStatus = localStatusOverrides[syntheticId] ?: "OPEN"
                            fallbackLiveAlerts.add(
                                SystemAlertRow(
                                    alertId = syntheticId,
                                    productId = item.batch.productId,
                                    batchId = item.batch.batchId,
                                    storageId = item.batch.storageId,
                                    alertType = "LOW_STOCK",
                                    title = title,
                                    message = msg,
                                    status = overrideStatus,
                                    createdAt = batchTimestamp
                                )
                            )
                        }
                    }
                }

                // 2. Check Expiring Soon (<= 7 days)
                if (item.isExpiringSoon) {
                    val daysLeft = runCatching {
                        ChronoUnit.DAYS.between(today, LocalDate.parse(item.batch.expiryDate))
                    }.getOrDefault(0L)

                    val alreadyInDb = existing.any {
                        it.batchId == item.batch.batchId &&
                                it.alertType.uppercase(Locale.ROOT).contains("EXPIR") &&
                                !it.isResolved
                    }
                    val prefix = if (daysLeft < 0) "Expired Batch" else "Expiring Soon"
                    val title = "$prefix: $prodName"
                    val msg = "Batch ${item.batch.batchCode} in $zoneName expires on ${item.batch.expiryDate} ($daysLeft days)."

                    if (!alreadyInDb) {
                        val inserted = tryInsertAlert(
                            productId = item.batch.productId,
                            batchId = item.batch.batchId,
                            storageId = item.batch.storageId,
                            alertType = "EXPIRY_WARNING",
                            title = title,
                            message = msg
                        )
                        if (!inserted) {
                            val syntheticId = -(item.batch.batchId * 10 + 2)
                            val overrideStatus = localStatusOverrides[syntheticId] ?: "OPEN"
                            fallbackLiveAlerts.add(
                                SystemAlertRow(
                                    alertId = syntheticId,
                                    productId = item.batch.productId,
                                    batchId = item.batch.batchId,
                                    storageId = item.batch.storageId,
                                    alertType = "EXPIRY_WARNING",
                                    title = title,
                                    message = msg,
                                    status = overrideStatus,
                                    createdAt = batchTimestamp
                                )
                            )
                        }
                    }
                }
            }
        }

        val refreshedDbAlerts = runCatching {
            db.from("system_alerts")
                .select {
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<SystemAlertRow>()
        }.getOrDefault(existing)

        return fallbackLiveAlerts + refreshedDbAlerts
    }

    private suspend fun tryInsertAlert(
        productId: Long?,
        batchId: Long?,
        storageId: Long?,
        alertType: String,
        title: String,
        message: String
    ): Boolean {
        for (candidateStatus in listOf("OPEN", "ACTIVE", "UNREAD")) {
            val success = runCatching {
                db.from("system_alerts").insert(
                    NewSystemAlertInsert(
                        productId = productId,
                        batchId = batchId,
                        storageId = storageId,
                        alertType = alertType,
                        title = title,
                        message = message,
                        status = candidateStatus
                    )
                )
            }.isSuccess
            if (success) return true
        }
        return false
    }

    suspend fun acknowledgeAlert(alertId: Long) {
        if (alertId < 0) {
            localStatusOverrides[alertId] = "ACKNOWLEDGED"
            return
        }
        val nowIso = Instant.now().toString()
        db.from("system_alerts").update(
            AlertStatusUpdate(
                status = "ACKNOWLEDGED",
                acknowledgedAt = nowIso
            )
        ) {
            filter { eq("alert_id", alertId) }
        }
    }

    suspend fun resolveAlert(alertId: Long, currentAckAt: String?) {
        if (alertId < 0) {
            localStatusOverrides[alertId] = "RESOLVED"
            return
        }
        val nowIso = Instant.now().toString()
        db.from("system_alerts").update(
            AlertStatusUpdate(
                status = "RESOLVED",
                acknowledgedAt = currentAckAt ?: nowIso,
                resolvedAt = nowIso
            )
        ) {
            filter { eq("alert_id", alertId) }
        }
    }
}