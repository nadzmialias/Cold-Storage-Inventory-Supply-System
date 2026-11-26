package com.csiss.inventory.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

// ─────────────────────────────────────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class ClaimProduct(
    val id: Long,
    val name: String,
    val code: String,
    val category: String,
    val unit: String
)

@Serializable
data class ClaimSupplier(
    val id: Long,
    val name: String
)

@Serializable
data class ClaimBatch(
    val id: Long,
    val batchCode: String,
    val productId: Long,
    val supplierId: Long,
    val quantityRemaining: Double,
    val expiryDate: String? = null
)

@Serializable
data class ClaimOptions(
    val userId: String,
    val role: String,
    val products: List<ClaimProduct>,
    val suppliers: List<ClaimSupplier>,
    val batches: List<ClaimBatch>
)

@Serializable
data class SupplierClaim(
    val id: Long = 0,
    @SerialName("claim_code") val claimCode: String? = null,
    @SerialName("product_id") val productId: Long,
    @SerialName("supplier_id") val supplierId: Long,
    @SerialName("batch_id") val batchId: Long? = null,
    @SerialName("affected_quantity") val affectedQuantity: Double,
    @SerialName("claim_type") val claimType: String,           // REPLACEMENT | REFUND
    @SerialName("reason_category") val reasonCategory: String,
    @SerialName("reason_description") val reasonDescription: String,
    @SerialName("evidence_url") val evidenceUrl: String? = null,
    val status: String = "PENDING",
    val priority: String = "NORMAL",                           // LOW | NORMAL | HIGH | URGENT
    @SerialName("resolution_reference") val resolutionReference: String? = null,
    @SerialName("resolution_notes") val resolutionNotes: String? = null,
    @SerialName("receipt_id") val receiptId: Long? = null,
    @SerialName("created_by") val createdBy: String = "",
    @SerialName("created_by_role") val createdByRole: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class ClaimHistory(
    val id: Long = 0,
    @SerialName("claim_id") val claimId: Long,
    val status: String,
    val note: String? = null,
    @SerialName("changed_by") val changedBy: String,
    @SerialName("changed_by_role") val changedByRole: String? = null,
    @SerialName("changed_at") val changedAt: String = ""
)

@Serializable
private data class ClaimRequester(val name: String? = null)

// ─────────────────────────────────────────────────────────────────────────────
// REPOSITORY
// ─────────────────────────────────────────────────────────────────────────────

class ClaimRepository(private val client: SupabaseClient) {

    /** Load dropdown options (products, suppliers, batches) + current user info */
    suspend fun options(): ClaimOptions =
        client.postgrest.rpc("supplier_claim_options")
            .decodeAs()

    /** Load all claims (staff/admin: all; supplier: filtered by RLS) */
    suspend fun claims(): List<SupplierClaim> =
        client.from("supplier_claims")
            .select()
            .decodeList<SupplierClaim>()
            .sortedByDescending { it.id }

    /** Get display name of the person who created a claim */
    suspend fun requesterName(claimId: Long): String? =
        client.postgrest.rpc(
            "supplier_claim_requester",
            buildJsonObject {
                put("p_claim_id", JsonPrimitive(claimId))
            }
        ).decodeAs<ClaimRequester>().name

    /** Load audit history for one claim */
    suspend fun history(claimId: Long): List<ClaimHistory> =
        client.from("supplier_claim_history")
            .select {
                filter { eq("claim_id", claimId) }
                order("changed_at", Order.DESCENDING)
            }
            .decodeList()

    /**
     * Create a new claim (claimId = null) or update an existing one (claimId != null).
     * Returns the saved claim's ID.
     */
    suspend fun save(
        claimId: Long?,
        productId: Long,
        supplierId: Long,
        batchId: Long?,
        affectedQuantity: Double,
        claimType: String,
        reasonCategory: String,
        reasonDescription: String,
        evidenceUrl: String?,
        priority: String
    ): Long =
        client.postgrest.rpc(
            "supplier_claim_save",
            buildJsonObject {
                put("p_id", claimId?.let { JsonPrimitive(it) } ?: JsonNull)
                put("p_product", JsonPrimitive(productId))
                put("p_supplier", JsonPrimitive(supplierId))
                put("p_batch", batchId?.let { JsonPrimitive(it) } ?: JsonNull)
                put("p_quantity", JsonPrimitive(affectedQuantity))
                put("p_type", JsonPrimitive(claimType))
                put("p_category", JsonPrimitive(reasonCategory))
                put("p_priority", JsonPrimitive(priority))
                put("p_description", JsonPrimitive(reasonDescription))
                put("p_evidence", evidenceUrl?.let { JsonPrimitive(it) } ?: JsonNull)
            }
        ).decodeAs()

    /**
     * Move a claim to a new status.
     * [reference] is used for REFUND_ISSUED (credit ref) or CLOSED REPLACEMENT (stock-in movement_id as string).
     * [receiptId] links the stock-in transaction when closing a REPLACEMENT claim.
     */
    suspend fun transition(
        claimId: Long,
        newStatus: String,
        note: String? = null,
        reference: String? = null,
        receiptId: Long? = null
    ) {
        client.postgrest.rpc(
            "supplier_claim_transition",
            buildJsonObject {
                put("p_id", JsonPrimitive(claimId))
                put("p_status", JsonPrimitive(newStatus))
                put("p_note", note?.let { JsonPrimitive(it) } ?: JsonNull)
                put("p_reference", reference?.let { JsonPrimitive(it) } ?: JsonNull)
                put("p_receipt_id", receiptId?.let { JsonPrimitive(it) } ?: JsonNull)
            }
        )
    }
}
