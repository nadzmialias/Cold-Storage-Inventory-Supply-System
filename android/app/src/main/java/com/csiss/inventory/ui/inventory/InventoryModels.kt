package com.csiss.inventory.ui.inventory

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import io.github.jan.supabase.storage.storage
import java.time.LocalDate

internal data class Product(
    val id: Long,
    val name: String,
    val category: String,
    val quantity: Double,
    val unit: String,
    val expiry: LocalDate? = null,
    val minimum: Double = 0.0,
    val storage: String = "",
    val supplier: String = "",
    val description: String = "",
    val photo: Uri? = null,
    val hasHistory: Boolean = false,
    val imagePath: String? = null,
    val isActive: Boolean = true,
    val hasBatches: Boolean = false
) {
    val canArchive: Boolean get() = isActive && quantity == 0.0
    val canDelete: Boolean get() = quantity == 0.0 && !hasHistory && !hasBatches
}

internal enum class StockStatus(val title: String, val foreground: Color, val background: Color) {
    InStock("In stock", Color(0xFF176B52), Color(0xFFD7F4E7)),
    Low("Low stock", Color(0xFF9A4D0A), Color(0xFFFFECCD)),
    Near("Near expiry", Color(0xFF6736A6), Color(0xFFF0E6FF)),
    Expired("Expired", Color(0xFFAA2350), Color(0xFFFCE5EC)),
    Out("Out of stock", Color(0xFF495A70), Color(0xFFE8EEF6))
}

internal fun Product.status(today: LocalDate): StockStatus {
    val date = expiry
    return when {
        date != null && date.isBefore(today) -> StockStatus.Expired
        quantity == 0.0 -> StockStatus.Out
        date != null && !date.isAfter(today.plusDays(7)) -> StockStatus.Near
        quantity <= minimum -> StockStatus.Low
        else -> StockStatus.InStock
    }
}

internal fun Double.display(): String = if (this == toLong().toDouble()) {
    toLong().toString()
} else {
    toString()
}
