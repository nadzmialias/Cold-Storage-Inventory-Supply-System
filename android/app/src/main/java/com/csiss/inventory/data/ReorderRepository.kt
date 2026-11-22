package com.csiss.inventory.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

@Serializable
internal data class ReorderOption(
    val id: Long,
    val name: String,
    val unit: String? = null,
    val active: Boolean = true,
    val previousSupplierIds: List<Long> = emptyList()
)

@Serializable
internal data class ReorderOptions(
    val role: String,
    val userId: String,
    val products: List<ReorderOption>,
    val suppliers: List<ReorderOption>
)

@Serializable
internal data class ReorderHistory(
    val id: Long,
    val status: String,
    val note: String,
    @SerialName("changed_at") val changedAt: String
)

@Serializable
internal data class Reorder(
    val id: Long,
    @SerialName("product_id") val productId: Long,
    @SerialName("supplier_id") val supplierId: Long,
    val quantity: Double,
    @SerialName("expected_delivery") val date: String? = null,
    val reference: String,
    val notes: String,
    val status: String,
    @SerialName("created_by") val createdBy: String,
    @SerialName("receipt_id") val receiptId: Long? = null,
    @SerialName("released_to_supplier") val releasedToSupplier: Boolean = true,
    @SerialName("created_at") val createdAt: String,
    @SerialName("created_by_role") val createdByRole: String? = null,
    @SerialName("additional_reason") val additionalReason: String = "",
    val priority: String = "NORMAL"
)

@Serializable
private data class ReorderRequester(val name: String? = null)

internal class ReorderRepository {
    private val client = SupabaseClientProvider.client

    suspend fun options() = client.postgrest.rpc("csiss_reorder_options").decodeAs<ReorderOptions>()

    suspend fun requests() = client.from("csiss_reorders").select()
        .decodeList<Reorder>().sortedByDescending { it.id }

    suspend fun requesterName(id: Long): String? =
        client.postgrest.rpc("csiss_reorder_requester", buildJsonObject {
            put("p_reorder_id", JsonPrimitive(id))
        }).decodeAs<ReorderRequester>().name

    suspend fun history(id: Long) = client.from("csiss_reorder_history").select {
        filter { eq("reorder_id", id) }
    }.decodeList<ReorderHistory>().sortedByDescending { it.id }

    suspend fun save(
        request: Reorder?, product: Long, supplier: Long, quantity: String,
        priority: String, additionalReason: String
    ) {
        client.postgrest.rpc("csiss_reorder_save_v2", buildJsonObject {
            put("p_id", request?.id?.let { JsonPrimitive(it) } ?: JsonNull)
            put("p_product", JsonPrimitive(product))
            put("p_supplier", JsonPrimitive(supplier))
            put("p_quantity", JsonPrimitive(quantity))
            put("p_priority", JsonPrimitive(priority))
            put("p_reason", JsonPrimitive(additionalReason))
            put("p_date", request?.date?.let { JsonPrimitive(it) } ?: JsonNull)
            put("p_reference", JsonPrimitive(request?.reference.orEmpty()))
            put("p_notes", JsonPrimitive(request?.notes.orEmpty()))
        })
    }

    suspend fun transition(id: Long, status: String, note: String, receipt: Long?) {
        client.postgrest.rpc("csiss_reorder_transition", buildJsonObject {
            put("p_id", JsonPrimitive(id))
            put("p_status", JsonPrimitive(status))
            put("p_note", JsonPrimitive(note))
            put("p_receipt", receipt?.let { JsonPrimitive(it) } ?: JsonNull)
        })
    }
}
