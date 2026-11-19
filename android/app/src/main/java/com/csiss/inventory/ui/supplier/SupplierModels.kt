package com.csiss.inventory.ui.supplier

import androidx.compose.ui.graphics.Color

internal data class Supplier(
    val id: Long,
    val name: String,
    val contactPerson: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val isActive: Boolean = true,
    val createdAt: String = ""
) {
    val initials: String
        get() {
            if (name.isBlank()) return "?"
            val parts = name.trim().split("\\s+".toRegex())
            return if (parts.size >= 2) {
                (parts[0].take(1) + parts[1].take(1)).uppercase()
            } else {
                name.take(2).uppercase()
            }
        }
}
