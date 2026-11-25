package com.csiss.inventory.ui.reorder

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SupplierReorderScreen(modifier: Modifier = Modifier) {
    ReorderScreen(modifier)
}

internal fun supplierReorderActions(status: String): List<Pair<String, String>> =
    when (status) {
        "PENDING" -> listOf("ACCEPTED" to "Accept request", "REJECTED" to "Reject request")
        "ACCEPTED" -> listOf("PREPARING" to "Start preparation")
        "PREPARING" -> listOf("DISPATCHED" to "Mark dispatched")
        "DISPATCHED" -> listOf("DELIVERED" to "Mark delivered")
        else -> emptyList()
    }
