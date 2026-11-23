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

// =====================================
// 1. STORAGE AREA MODEL
// =====================================

@Serializable
data class StorageArea(
    @SerialName("storage_id")
    val storageId: Long,

    @SerialName("storage_name")
    val storageName: String,

    @SerialName("min_temperature")
    val minTemperature: Double,

    @SerialName("max_temperature")
    val maxTemperature: Double
)

// =====================================
// 2. TEMPERATURE LOG MODEL
// =====================================

@Serializable
data class TemperatureLog(
    @SerialName("log_id")
    val logId: Long,

    val temperature: Double,

    @SerialName("recorded_at")
    val recordedAt: String,

    @SerialName("min_limit_at_reading")
    val minLimit: Double,

    @SerialName("max_limit_at_reading")
    val maxLimit: Double
) {
    val isAbnormal: Boolean
        get() = temperature < minLimit || temperature > maxLimit
}

// =====================================
// 3. SYSTEM ALERT MODEL
// =====================================

@Serializable
data class TemperatureAlert(
    @SerialName("alert_id")
    val alertId: Long,

    val title: String,
    val message: String,
    val status: String,

    @SerialName("created_at")
    val createdAt: String
)

// =====================================
// 4. COMBINED DATA
// =====================================

data class TemperatureSnapshot(
    val storage: StorageArea,
    val logs: List<TemperatureLog>,
    val alerts: List<TemperatureAlert>
)

// =====================================
// 5. TEMPERATURE REPOSITORY
// =====================================

class TemperatureRepository {

    private val client
        get() = SupabaseClientProvider.client

    suspend fun loadLive(): TemperatureSnapshot = withContext(Dispatchers.IO) {
        val storage = client.from("storage_areas")
            .select(
                columns = Columns.raw(
                    "storage_id,storage_name,min_temperature,max_temperature"
                )
            ) {
                filter {
                    eq("is_active", true)
                }
                order(
                    column = "storage_id",
                    order = Order.ASCENDING
                )
                limit(1)
            }
            .decodeList<StorageArea>()
            .firstOrNull()
            ?: error("No active storage area is set up.")

        coroutineScope {
            val logsDeferred = async {
                client.from("temperature_logs")
                    .select(
                        columns = Columns.raw(
                            "log_id,temperature,recorded_at," +
                                "min_limit_at_reading,max_limit_at_reading"
                        )
                    ) {
                        filter {
                            eq("storage_id", storage.storageId)
                        }
                        order(
                            column = "recorded_at",
                            order = Order.DESCENDING
                        )
                        limit(20)
                    }
                    .decodeList<TemperatureLog>()
            }

            val alertsDeferred = async {
                client.from("system_alerts")
                    .select(
                        columns = Columns.raw(
                            "alert_id,title,message,status,created_at"
                        )
                    ) {
                        filter {
                            eq("alert_type", "TEMP_OUT_OF_RANGE")
                        }
                        order(
                            column = "created_at",
                            order = Order.DESCENDING
                        )
                        limit(100)
                    }
                    .decodeList<TemperatureAlert>()
            }

            TemperatureSnapshot(
                storage = storage,
                logs = logsDeferred.await(),
                alerts = alertsDeferred.await()
            )
        }
    }

    suspend fun updateLimits(
        storageId: Long,
        minimum: Double,
        maximum: Double
    ): StorageArea = withContext(Dispatchers.IO) {
        require(
            minimum.isFinite() &&
                maximum.isFinite() &&
                minimum < maximum
        ) {
            "Invalid temperature limits"
        }

        client.from("storage_areas")
            .update(
                {
                    set("min_temperature", minimum)
                    set("max_temperature", maximum)
                }
            ) {
                select(
                    columns = Columns.raw(
                        "storage_id,storage_name,min_temperature,max_temperature"
                    )
                )
                filter {
                    eq("storage_id", storageId)
                }
            }
            .decodeSingle<StorageArea>()
    }
}
