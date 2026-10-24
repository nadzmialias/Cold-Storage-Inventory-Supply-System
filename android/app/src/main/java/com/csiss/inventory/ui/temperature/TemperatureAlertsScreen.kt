
package com.csiss.inventory.ui.temperature

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import com.csiss.inventory.data.TemperatureAlert
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val AlertPageBackground = Color(0xFFF1F6FB)
private val AlertPageNavy = Color(0xFF132945)
private val AlertPageMuted = Color(0xFF718198)
private val AlertPageRed = Color(0xFFE45465)

@Composable
fun TemperatureAlertsScreen(
    alerts: List<TemperatureAlert>,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    val sortedAlerts = alerts.sortedByDescending {
        it.createdAt
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AlertPageBackground)
    ) {

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text(
                    "← Back",
                    color = AlertPageNavy
                )
            }

            Text(
                "Temperature Alerts",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AlertPageNavy
            )

            Button(
                onClick = onRefresh,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlertPageNavy
                ),
                contentPadding = PaddingValues(
                    horizontal = 12.dp,
                    vertical = 6.dp
                )
            ) {
                Text("Refresh", fontSize = 11.sp)
            }
        }

        Text(
            text = "${alerts.size} temperature alerts",
            modifier = Modifier.padding(
                horizontal = 20.dp
            ),
            color = AlertPageMuted,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(12.dp))

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                modifier = Modifier.padding(20.dp),
                color = AlertPageRed
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            "No temperature alerts recorded.",
                            modifier = Modifier.padding(24.dp),
                            color = AlertPageMuted
                        )
                    }
                }
            }

            items(
                items = sortedAlerts,
                key = { it.alertId }
            ) { alert ->
                TemperatureAlertItem(alert)
            }
        }
    }
}

@Composable
private fun TemperatureAlertItem(
    alert: TemperatureAlert
) {
    val isOpen = alert.status.equals(
        "OPEN",
        ignoreCase = true
    )

    val statusColor = if (isOpen) {
        Color(0xFFE45465)
    } else {
        Color(0xFF159C76)
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = alert.title,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    color = AlertPageNavy,
                    fontSize = 15.sp
                )

                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = alert.status,
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 5.dp
                        ),
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = alert.message,
                color = Color(0xFF475569),
                fontSize = 13.sp
            )

            HorizontalDivider(
                color = Color(0xFFE8EEF5)
            )

            Text(
                text = formatAlertTime(alert.createdAt),
                color = AlertPageMuted,
                fontSize = 12.sp
            )
        }
    }
}

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
