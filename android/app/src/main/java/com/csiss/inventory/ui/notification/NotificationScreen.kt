package com.csiss.inventory.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csiss.inventory.data.AlertRepository
import com.csiss.inventory.data.SystemAlertRow
import com.csiss.inventory.ui.components.OutlineRefreshButton
import com.csiss.inventory.ui.components.SummaryStat
import com.csiss.inventory.ui.components.SummaryStatsCard
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Navy = Color(0xFF132945)
private val IceBlue = Color(0xFF34B9DB)
private val Background = Color(0xFFF1F6FB)
private val TextDark = Color(0xFF182B48)
private val TextMuted = Color(0xFF718198)
private val AlertRed = Color(0xFFE45465)
private val WarningOrange = Color(0xFFE59A23)
private val NormalGreen = Color(0xFF159C76)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    initialFilter: Int = 0,
    onOpenCountChanged: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { AlertRepository() }
    val scope = rememberCoroutineScope()

    var alerts by remember { mutableStateOf<List<SystemAlertRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var manualRefreshPending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val pullToRefreshState = rememberPullToRefreshState()
    val requestRefresh: () -> Unit = {
        if (!loading && !manualRefreshPending) {
            manualRefreshPending = true
            refreshTrigger++
        }
    }
    // 0 = Open, 1 = Stock & Expiry, 2 = Temperature, 3 = Done
    var selectedFilter by remember(initialFilter) { mutableIntStateOf(initialFilter) }

    LaunchedEffect(refreshTrigger) {
        loading = true
        error = null
        try {
            val loaded = repository.loadAndSyncAlerts()
            alerts = loaded
            val openAlerts = loaded.filter { !it.isResolved }
            onOpenCountChanged(openAlerts.size)
            AlertNotificationHelper.showSystemPopupIfNeeded(context, openAlerts)
        } catch (e: Exception) {
            error = "Unable to load alerts: ${e.message ?: "Check connection"}"
        } finally {
            loading = false
            manualRefreshPending = false
        }
    }

    val filteredAlerts = remember(alerts, selectedFilter) {
        when (selectedFilter) {
            0 -> alerts.filter { !it.isResolved }
            1 -> alerts.filter { it.isStockRelated }
            2 -> alerts.filter { it.isTempRelated }
            else -> alerts.filter { it.isResolved }
        }
    }

    PullToRefreshBox(
        isRefreshing = manualRefreshPending,
        onRefresh = requestRefresh,
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        state = pullToRefreshState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = manualRefreshPending,
                containerColor = Navy,
                color = Color.White,
                state = pullToRefreshState
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
        ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "REAL-TIME MONITORING",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Alerts",
                        color = TextDark,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                OutlineRefreshButton(
                    onClick = requestRefresh,
                    isRefreshing = manualRefreshPending,
                    enabled = !loading
                )

            }

            AlertSummaryHero(alerts = alerts)

            // Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val activeCount = alerts.count { !it.isResolved }
                val stockCount = alerts.count { it.isStockRelated }
                val tempCount = alerts.count { it.isTempRelated }
                val resolvedCount = alerts.count { it.isResolved }

                FilterPill(
                    label = "Open ($activeCount)",
                    selected = selectedFilter == 0,
                    onClick = { selectedFilter = 0 },
                    modifier = Modifier.weight(1f)
                )
                FilterPill(
                    label = "Stock ($stockCount)",
                    selected = selectedFilter == 1,
                    onClick = { selectedFilter = 1 },
                    modifier = Modifier.weight(1f)
                )
                FilterPill(
                    label = "Temp ($tempCount)",
                    selected = selectedFilter == 2,
                    onClick = { selectedFilter = 2 },
                    modifier = Modifier.weight(1f)
                )
                FilterPill(
                    label = "Done ($resolvedCount)",
                    selected = selectedFilter == 3,
                    onClick = { selectedFilter = 3 },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (loading && alerts.isEmpty()) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        error?.let {
            Text(
                text = it,
                color = AlertRed,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }



            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    horizontal = 20.dp,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredAlerts.isEmpty() && !loading) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Text(
                                text = "No notifications in this category.",
                                color = TextMuted,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                } else {
                    items(
                        filteredAlerts,
                        key = { it.alertId }
                    ) { alert ->
                        AlertCard(
                            alert = alert,

                            onAcknowledge = {
                                scope.launch {
                                    runCatching {
                                        repository.acknowledgeAlert(
                                            alert.alertId
                                        )
                                        refreshTrigger++
                                    }.onFailure { e ->
                                        error = e.message
                                            ?: "Failed to acknowledge alert."
                                    }
                                }
                            },

                            onResolve = {
                                scope.launch {
                                    runCatching {
                                        repository.resolveAlert(
                                            alert.alertId,
                                            alert.acknowledgedAt
                                        )
                                        refreshTrigger++
                                    }.onFailure {
                                        error = "Unable to resolve alert. Please try again."
                                    }
                                }
                            }
                        )
                    }
                }
            } // End LazyColumn

        } // End main Column

    } // End outer PullToRefreshBox

} // End NotificationScreen

@Composable
private fun AlertSummaryHero(alerts: List<SystemAlertRow>) {
    val openCount = alerts.count {
        !it.isResolved && !it.isAcknowledged
    }
    val ackCount = alerts.count { it.isAcknowledged }
    val resolvedCount = alerts.count { it.isResolved }

    SummaryStatsCard(
        stats = listOf(
            SummaryStat(
                "UNREAD / ACTIVE",
                openCount.toString(),
                if (openCount > 0)
                    Color(0xFFFFBEC6)
                else
                    Color.White
            ),
            SummaryStat(
                "ACKNOWLEDGED",
                ackCount.toString(),
                Color(0xFFFDE68A)
            ),
            SummaryStat(
                "RESOLVED",
                resolvedCount.toString(),
                Color(0xFF98F0CD)
            )
        )
    )
}


@Composable
private fun HeroMetric(label: String, value: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,  verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = value,
            color = accent,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color(0xFFD2DBED),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp
        )
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = if (selected) Navy else Color.Transparent,
        shape = RoundedCornerShape(9.dp)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (selected) Color.White else TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AlertCard(
    alert: SystemAlertRow,
    onAcknowledge: () -> Unit,
    onResolve: () -> Unit
) {
    val typeColor = when {
        alert.alertType.uppercase().contains("STOCK") ||
                alert.alertType.uppercase().contains("TEMP") -> AlertRed
        alert.alertType.uppercase().contains("EXPIR") -> WarningOrange
        else -> IceBlue
    }
    val statusColor = when {
        alert.isResolved -> NormalGreen
        alert.isAcknowledged -> WarningOrange
        else -> AlertRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BadgeChip(text = alert.alertType, color = typeColor)
                    BadgeChip(text = alert.status, color = statusColor)
                }
                Text(
                    text = formatAlertTime(alert.createdAt),
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Text(
                text = alert.title,
                color = TextDark,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = alert.message,
                color = TextMuted,
                fontSize = 13.sp
            )

            if (!alert.isResolved) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!alert.isAcknowledged) {
                        TextButton(onClick = onAcknowledge) {
                            Text("Acknowledge", color = WarningOrange, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (alert.isAcknowledged || !alert.isTempRelated) {
                        TextButton(onClick = onResolve) {
                            Text(
                                "Mark Resolved ✓",
                                color = NormalGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(50.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun formatAlertTime(value: String): String {
    return runCatching {
        OffsetDateTime.parse(value)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale.ENGLISH))
    }.getOrElse { value }
}