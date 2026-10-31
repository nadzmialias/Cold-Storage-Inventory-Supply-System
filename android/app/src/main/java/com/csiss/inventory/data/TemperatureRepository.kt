
package com.csiss.inventory.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

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

    suspend fun loadLive(): TemperatureSnapshot {

        // ==========================================
        // 1. FETCH STORAGE AREA
        // ==========================================

        val storage = client.from("storage_areas")
            .select(
                columns = Columns.raw(
                    "storage_id,storage_name,min_temperature,max_temperature"
                )
            ) {
                filter {
                    eq("storage_id", 1L)
                }
            }
            .decodeSingle<StorageArea>()

        // ==========================================
        // 2. FETCH LATEST TEMPERATURE READINGS
        // ==========================================

        val logs = client.from("temperature_logs")
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

        // ==========================================
        // 3. FETCH TEMPERATURE ALERTS
        // ==========================================

        val alerts = client.from("system_alerts")
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

        // ==========================================
        // 4. RETURN LIVE DATA
        // ==========================================

        return TemperatureSnapshot(
            storage = storage,
            logs = logs,
            alerts = alerts
        )
    }


    //UPDATE TEMP LIMIT
    suspend fun updateLimits(
        storageId: Long,
        minimum: Double,
        maximum: Double
    ): StorageArea {

        require(
            minimum.isFinite() &&
                    maximum.isFinite() &&
                    minimum < maximum
        ) {
            "Invalid temperature limits"
        }

        return client.from("storage_areas")
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


    private suspend fun loadAllAlerts(): List<TemperatureAlert> {
        val allAlerts = mutableListOf<TemperatureAlert>()
        val pageSize = 200L
        var start = 0L

        while (true) {
            val page = client.from("system_alerts")
                .select(
                    columns = Columns.raw(
                        "alert_id,title,message,status,created_at"
                    )
                ) {
                    filter {
                        eq("alert_type", "TEMP_OUT_OF_RANGE")
                    }

                    order(
                        "created_at",
                        Order.DESCENDING
                    )

                    range(start..(start + pageSize - 1))
                }
                .decodeList<TemperatureAlert>()

            allAlerts.addAll(page)

            if (page.size < pageSize.toInt()) {
                break
            }

            start += pageSize
        }

        return allAlerts
    }


    suspend fun updateAlertStatus(
        alertId: Long,
        newStatus: String
    ): TemperatureAlert {

        // Determine which transition is allowed.
        val expectedStatus = when (newStatus) {
            "ACKNOWLEDGED" -> "OPEN"
            "RESOLVED" -> "ACKNOWLEDGED"
            else -> throw IllegalArgumentException(
                "Invalid alert status"
            )
        }

        val updatedAlerts = client.from("system_alerts")
            .update(
                {
                    set("status", newStatus)
                }
            ) {
                select(
                    columns = Columns.raw(
                        "alert_id,title,message,status,created_at"
                    )
                )

                filter {
                    eq("alert_id", alertId)
                    eq("alert_type", "TEMP_OUT_OF_RANGE")
                    eq("status", expectedStatus)
                }
            }
            .decodeList<TemperatureAlert>()

        return updatedAlerts.singleOrNull()
            ?: error(
                "Alert could not be updated. " +
                        "It may have changed or you may lack permission."
            )
    }



    // DEMO DATA FOR UI DEVELOPMENT
    fun loadDemo(): TemperatureSnapshot {

        val storage = StorageArea(
            storageId = 1,
            storageName = "Freezer Room A",
            minTemperature = -25.0,
            maxTemperature = -18.0
        )

        val demoNow = Instant.now().minusSeconds(60)

        // Sample readings, newest first.
        val logs = listOf(
            TemperatureLog(
                logId = 4,
                temperature = -16.5,
                recordedAt = demoNow.toString(),
                minLimit = -25.0,
                maxLimit = -18.0
            ),
            TemperatureLog(
                logId = 3,
                temperature = -20.5,
                recordedAt = demoNow.minusSeconds(60).toString(),
                minLimit = -25.0,
                maxLimit = -18.0
            ),
            TemperatureLog(
                logId = 2,
                temperature = -19.8,
                recordedAt = demoNow.minusSeconds(120).toString(),
                minLimit = -25.0,
                maxLimit = -18.0
            ),
            TemperatureLog(
                logId = 1,
                temperature = -21.0,
                recordedAt = demoNow.minusSeconds(180).toString(),
                minLimit = -25.0,
                maxLimit = -18.0
            )
        )
        val alerts = listOf(
            TemperatureAlert(
                alertId = 1,
                title = "Abnormal Temperature",
                message = "Temperature -16.5°C exceeds " +
                        "the maximum limit of -18°C.",
                status = "OPEN",
                createdAt = demoNow.toString()
            )
        )

        return TemperatureSnapshot(
            storage = storage,
            logs = logs,
            alerts = alerts
        )
    }

}
