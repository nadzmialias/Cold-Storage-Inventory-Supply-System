package com.csiss.inventory.data

import com.csiss.inventory.ui.supplier.Supplier
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class SupplierRow(
    @SerialName("supplier_id") val id: Long,
    @SerialName("supplier_name") val name: String?,
    @SerialName("contact_person") val contactPerson: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    @SerialName("is_active") val isActive: Boolean? = true,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
private data class NewSupplierRow(
    @SerialName("supplier_name") val name: String,
    @SerialName("contact_person") val contactPerson: String,
    val phone: String,
    val email: String,
    val address: String,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
private data class UpdateSupplierRow(
    @SerialName("supplier_name") val name: String,
    @SerialName("contact_person") val contactPerson: String,
    val phone: String,
    val email: String,
    val address: String
)

@Serializable
private data class UpdateSupplierActiveRow(
    @SerialName("is_active") val isActive: Boolean
)

internal suspend fun loadSuppliersData(): List<Supplier> {
    val rows = SupabaseClientProvider.client
        .from("suppliers")
        .select(
            columns = Columns.raw(
                """
                supplier_id,
                supplier_name,
                contact_person,
                phone,
                email,
                address,
                is_active,
                created_at
                """.trimIndent()
            )
        ) {
            order("is_active", Order.DESCENDING)
            order("supplier_name", Order.ASCENDING)
        }
        .decodeList<SupplierRow>()

    return rows.map { row ->
        Supplier(
            id = row.id,
            name = row.name.orEmpty().ifBlank { "Unnamed Supplier" },
            contactPerson = row.contactPerson.orEmpty(),
            phone = row.phone.orEmpty(),
            email = row.email.orEmpty(),
            address = row.address.orEmpty(),
            isActive = row.isActive ?: true,
            createdAt = row.createdAt.orEmpty()
        )
    }
}

internal suspend fun insertSupplierData(
    name: String,
    contactPerson: String,
    phone: String,
    email: String,
    address: String
) {
    SupabaseClientProvider.client
        .from("suppliers")
        .insert(
            NewSupplierRow(
                name = name,
                contactPerson = contactPerson,
                phone = phone,
                email = email,
                address = address,
                isActive = true
            )
        )
}

internal suspend fun updateSupplierData(
    supplierId: Long,
    name: String,
    contactPerson: String,
    phone: String,
    email: String,
    address: String
) {
    SupabaseClientProvider.client
        .from("suppliers")
        .update(
            UpdateSupplierRow(
                name = name,
                contactPerson = contactPerson,
                phone = phone,
                email = email,
                address = address
            )
        ) {
            filter {
                eq("supplier_id", supplierId)
            }
        }
}

internal suspend fun setSupplierActiveData(
    supplierId: Long,
    isActive: Boolean
) {
    SupabaseClientProvider.client
        .from("suppliers")
        .update(
            UpdateSupplierActiveRow(isActive = isActive)
        ) {
            filter {
                eq("supplier_id", supplierId)
            }
        }
}

internal suspend fun deleteSupplierData(supplierId: Long) {
    SupabaseClientProvider.client
        .from("suppliers")
        .delete {
            filter {
                eq("supplier_id", supplierId)
            }
        }
}
