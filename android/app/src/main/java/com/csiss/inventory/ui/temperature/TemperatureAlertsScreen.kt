
package com.csiss.inventory.ui.temperature

import com.csiss.inventory.ui.components.OutlineRefreshButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csiss.inventory.data.TemperatureAlert
import com.csiss.inventory.ui.components.OutlineRefreshButton
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Navy = Color(0xFF132945)
private val Background = Color(0xFFF1F6FB)
private val Muted = Color(0xFF718198)
private val AlertRed = Color(0xFFE45465)
private val WarningOrange = Color(0xFFE59A23)
private val SuccessGreen = Color(0xFF159C76)

@Composable
fun TemperatureAlertsScreen(
    alerts: List<TemperatureAlert>,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    canChangeStatus: Boolean,
    onStatusChange: (Long, String) -> Unit,
    isRefreshing: Boolean = false
) {

    val sortedAlerts = alerts.sortedByDescending {
        it.createdAt
    }

    val unresolvedCount = alerts.count {
        it.status == "OPEN" ||
                it.status == "ACKNOWLEDGED"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp,
                    vertical = 12.dp
                ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // Back and Refresh buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                TextButton(
                    onClick = onBack,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "← Back",
                        color = Navy,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                OutlineRefreshButton(
                    onClick = onRefresh,
                    isRefreshing = isRefreshing,
                    enabled = !isLoading
                )
            }

            // Title and alert count
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                Text(
                    text = "Temperature Alerts",
                    color = Navy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )

                Text(
                    text = "$unresolvedCount unresolved • ${alerts.size} total",
                    color = Muted,
                    fontSize = 13.sp
                )
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                modifier = Modifier.padding(20.dp),
                color = AlertRed
            )
        }

        if (!canChangeStatus) {
            Text(
                text = "Alert management is read-only " +
                        "until live permissions are enabled.",
                modifier = Modifier.padding(
                    horizontal = 20.dp,
                    vertical = 8.dp
                ),
                color = Muted,
                fontSize = 12.sp
            )
        }

        // ALL ALERTS
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if (
                sortedAlerts.isEmpty() &&
                !isLoading &&
                errorMessage == null
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Text(
                            "No temperature alerts recorded.",
                            modifier = Modifier.padding(20.dp),
                            color = Muted
                        )
                    }
                }
            }

            items(
                items = sortedAlerts,
                key = { it.alertId }
            ) { alert ->

                TemperatureAlertItem(
                    alert = alert,
                    enabled = canChangeStatus && !isLoading,
                    onStatusChange = { newStatus ->
                        onStatusChange(
                            alert.alertId,
                            newStatus
                        )
                    }
                )
            }
        }
    }
}



// ========================================
// SINGLE ALERT CARD
// ========================================

@Composable
private fun TemperatureAlertItem(
    alert: TemperatureAlert,
    enabled: Boolean,
    onStatusChange: (String) -> Unit
) {
    val statusColor = when (alert.status) {
        "OPEN" -> AlertRed
        "ACKNOWLEDGED" -> WarningOrange
        "RESOLVED" -> SuccessGreen
        else -> Muted
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = if (
                        alert.title.equals(
                            "Abnormal Cold Storage Temperature",
                            ignoreCase = true
                        )
                    ) {
                        "Abnormal Temperature"
                    } else {
                        alert.title
                    },
                    modifier = Modifier.weight(1f),
                    color = Navy,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = alert.status,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        )
                    )
                }
            }

            Text(
                text = alert.message,
                fontSize = 13.sp,
                color = Color(0xFF475569)
            )

            Text(
                text = formatAlertTime(alert.createdAt),
                color = Muted,
                fontSize = 12.sp
            )

            HorizontalDivider(
                color = Color(0xFFE8EEF5)
            )

            // STATUS ACTIONS
            when (alert.status) {

                "OPEN" -> {
                    Button(
                        onClick = {
                            onStatusChange("ACKNOWLEDGED")
                        },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Navy,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Acknowledge Alert")
                    }
                }

                "ACKNOWLEDGED" -> {
                    Button(
                        onClick = {
                            onStatusChange("RESOLVED")
                        },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuccessGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Mark as Resolved")
                    }
                }

                "RESOLVED" -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "✓",
                            color = SuccessGreen,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Alert Resolved",
                            color = SuccessGreen,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}


// ========================================
// FORMAT TIMESTAMP
// ========================================

private fun formatAlertTime(value: String): String {
    return runCatching {
        OffsetDateTime.parse(value)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(
                DateTimeFormatter.ofPattern(
                    "dd MMM yyyy, HH:mm",
                    Locale.ENGLISH
                )
            )
    }.getOrElse { value }
}