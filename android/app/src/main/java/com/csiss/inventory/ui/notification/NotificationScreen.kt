package com.csiss.inventory.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csiss.inventory.data.AlertRepository
import com.csiss.inventory.data.SystemAlertRow
import com.csiss.inventory.ui.components.CsissLightRefreshIndicator
import com.csiss.inventory.ui.components.SummaryStat
import com.csiss.inventory.ui.components.SummaryStatsCard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Navy = Color(0xFF132945)
private val Ink = Color(0xFF182B48)
private val Muted = Color(0xFF506583)
private val Background = Color(0xFFF1F6FB)
private val CardShape = RoundedCornerShape(20.dp)
private val FieldShape = RoundedCornerShape(12.dp)
private val MalaysiaZone = ZoneId.of("Asia/Kuala_Lumpur")
private val TimeFormat = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    initialFilter: Int = 0, // 0 = Open, 1 = Stock, 2 = Temp, 3 = Done
    userRole: String = "STAFF",
    onOpenCountChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val repository = remember { AlertRepository() }
    val scope = rememberCoroutineScope()

    var alerts by remember { mutableStateOf<List<SystemAlertRow>>(emptyList()) }
    var selectedTab by remember(initialFilter) { mutableIntStateOf(initialFilter) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        val loaded = repository.loadAndSyncAlerts()
        alerts = loaded
        onOpenCountChanged(loaded.count { !it.isResolved })
    }

    fun runTask(work: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                work()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message
                    ?.substringBefore("URL:")
                    ?.trim()
                    ?.take(200)
                    ?: "Couldn't update notifications."
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(Unit) {
        runTask { reload() }
    }

    val openAlerts = alerts.filter { !it.isResolved }
    val stockOpenCount = openAlerts.count { it.isStockRelated }
    val tempOpenCount = openAlerts.count { it.isTempRelated }
    val resolvedCount = alerts.count { it.isResolved }

    val visibleAlerts = when (selectedTab) {
        1 -> openAlerts.filter { it.isStockRelated }
        2 -> openAlerts.filter { it.isTempRelated }
        3 -> alerts.filter { it.isResolved }
        else -> openAlerts
    }
    val pullToRefreshState = rememberPullToRefreshState()

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Navy,
            onPrimary = Color.White,
            surface = Color.White,
            onSurface = Ink
        )
    ) {
        PullToRefreshBox(
            isRefreshing = busy,
            onRefresh = { runTask { reload() } },
            modifier = modifier.fillMaxSize(),
            state = pullToRefreshState,
            indicator = {
                CsissLightRefreshIndicator(
                    state = pullToRefreshState,
                    isRefreshing = busy,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "MONITORING & WARNINGS",
                            color = Muted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "System Notifications",
                            color = Ink,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                error?.let { msg ->
                    item {
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }
                }

                // Summary Card matching Reorder & Inventory screens
                item {
                    SummaryStatsCard(
                        stats = listOf(
                            SummaryStat(
                                "OPEN ALERTS",
                                openAlerts.size.toString(),
                                if (openAlerts.isNotEmpty()) Color(0xFFFFB7C5) else Color.White
                            ),
                            SummaryStat("STOCK / EXPIRY", stockOpenCount.toString()),
                            SummaryStat(
                                "TEMP BREACH",
                                tempOpenCount.toString(),
                                if (tempOpenCount > 0) Color(0xFFFDE68A) else Color(0xFF9BE8CA)
                            )
                        )
                    )
                }

                // Category Tabs: Open | Stock | Temp | Done
                item {
                    AlertCategoryTabs(
                        selectedTab = selectedTab,
                        openCount = openAlerts.size,
                        stockCount = stockOpenCount,
                        tempCount = tempOpenCount,
                        doneCount = resolvedCount,
                        onSelect = { selectedTab = it }
                    )
                }

                if (visibleAlerts.isEmpty() && !busy) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            shape = CardShape
                        ) {
                            Text(
                                text = if (selectedTab == 3) {
                                    "No resolved notifications yet."
                                } else {
                                    "All clear — no active alerts in this category."
                                },
                                color = Muted,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                items(visibleAlerts, key = { it.alertId }) { alert ->
                    AlertCard(
                        alert = alert,
                        enabled = !busy,
                        canResolve = userRole == "ADMIN" || userRole == "STAFF",
                        onAcknowledge = {
                            runTask {
                                repository.acknowledgeAlert(alert.alertId)
                                reload()
                            }
                        },
                        onResolve = {
                            runTask {
                                repository.resolveAlert(alert.alertId, alert.acknowledgedAt)
                                reload()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertCategoryTabs(
    selectedTab: Int,
    openCount: Int,
    stockCount: Int,
    tempCount: Int,
    doneCount: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, FieldShape)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            0 to "Open ($openCount)",
            1 to "Stock ($stockCount)",
            2 to "Temp ($tempCount)",
            3 to "Done ($doneCount)"
        ).forEach { (index, label) ->
            val selected = selectedTab == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) Navy else Color.Transparent)
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = { onSelect(index) }
                    )
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = if (selected) Color.White else Muted,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun AlertCard(
    alert: SystemAlertRow,
    enabled: Boolean,
    canResolve: Boolean,
    onAcknowledge: () -> Unit,
    onResolve: () -> Unit
) {
    val categoryLabel = when {
        alert.isTempRelated -> "TEMPERATURE"
        alert.isStockRelated -> "STOCK / EXPIRY"
        else -> "SYSTEM"
    }
    val statusBg = when {
        alert.isResolved -> Color(0xFFE6F6EE)
        alert.isAcknowledged -> Color(0xFFFEF3C7)
        else -> Color(0xFFFBE7EC)
    }
    val statusFg = when {
        alert.isResolved -> Color(0xFF135E3D)
        alert.isAcknowledged -> Color(0xFF92400E)
        else -> Color(0xFF972D41)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = CardShape
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
                Text(
                    text = categoryLabel,
                    color = Muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = alert.status.uppercase(Locale.ROOT),
                    color = statusFg,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .background(statusBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Text(
                text = alert.title,
                color = Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = alert.message,
                color = Muted,
                fontSize = 13.sp
            )

            Text(
                text = "Triggered: ${formatAlertTime(alert.createdAt)}",
                color = Muted,
                fontSize = 11.sp
            )

            if (!alert.isResolved) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!alert.isAcknowledged) {
                        OutlinedButton(
                            onClick = onAcknowledge,
                            enabled = enabled,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Acknowledge", color = Navy, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (canResolve) {
                        Button(
                            onClick = onResolve,
                            enabled = enabled,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mark Resolved", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

private fun formatAlertTime(raw: String): String =
    runCatching {
        OffsetDateTime.parse(raw).toInstant().atZone(MalaysiaZone).format(TimeFormat) + " MYT"
    }.getOrElse {
        runCatching {
            Instant.parse(raw).atZone(MalaysiaZone).format(TimeFormat) + " MYT"
        }.getOrDefault(raw)
    }