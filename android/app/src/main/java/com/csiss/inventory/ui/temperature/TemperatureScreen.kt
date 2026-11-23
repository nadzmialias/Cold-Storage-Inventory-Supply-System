package com.csiss.inventory.ui.temperature

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.csiss.inventory.ui.components.CsissLightRefreshIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csiss.inventory.data.*
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

import androidx.lifecycle.viewmodel.compose.viewModel

// CSISS color palette
private val Navy = Color(0xFF132945)
private val IceBlue = Color(0xFF34B9DB)
private val Background = Color(0xFFF1F6FB)
private val TextDark = Color(0xFF182B48)
private val TextMuted = Color(0xFF718198)
private val AlertRed = Color(0xFFE45465)
private val NormalGreen = Color(0xFF159C76)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemperatureScreen(
    viewModel: TemperatureViewModel = viewModel(),
    canEditLimits: Boolean = false,
    onOpenAlerts: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var showLimitEditor by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    val showRefreshIndicator =
        uiState.manualRefreshPending || (uiState.isLoading && !uiState.isInitialLoadComplete)

    PullToRefreshBox(
        isRefreshing = showRefreshIndicator,
        onRefresh = { viewModel.requestManualRefresh() },
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal +
                            WindowInsetsSides.Bottom
                )
            ),
        state = pullToRefreshState,
        indicator = {
            CsissLightRefreshIndicator(
                state = pullToRefreshState,
                isRefreshing = showRefreshIndicator,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
    Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Header: the primary information is the location, not the refresh action.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "TEMPERATURE MONITORING",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Cold Storage",
                        color = TextDark,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            uiState.snapshot?.let { data ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SensorConnectionIndicator(
                        lastReadingAt = data.logs.firstOrNull()?.recordedAt,
                        currentTime = uiState.currentTime,
                        serverReachable = uiState.error == null
                    )
                    AlertsStatusButton(
                        alertCount = data.alerts.count { alert ->
                            alert.status == "OPEN" || alert.status == "ACKNOWLEDGED"
                        },
                        onClick = onOpenAlerts
                    )
                }
            }

            uiState.error?.let {
                Text(
                    it,
                    color = AlertRed,
                    fontSize = 14.sp
                )
            }

            if (uiState.snapshot == null) {
                TemperaturePlaceholderHero(isLoading = uiState.isLoading)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "MINIMUM",
                        value = "—",
                        modifier = Modifier.weight(1f)
                    )

                    MetricCard(
                        title = "MAXIMUM",
                        value = "—",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            uiState.snapshot?.let { data ->
                TemperatureHero(data = data)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "MINIMUM",
                        value = formatTemp(data.storage.minTemperature),
                        modifier = Modifier.weight(1f)
                    )

                    MetricCard(
                        title = "MAXIMUM",
                        value = formatTemp(data.storage.maxTemperature),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Edit Limits button below the cards
                if (canEditLimits) {
                    Button(
                        onClick = {
                            viewModel.clearLimitSaveError()
                            showLimitEditor = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Navy,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Edit Limits", fontWeight = FontWeight.SemiBold)
                    }
                }

                // Temperature history
                SectionHeading("Temperature Trend")

                TemperatureChart(
                    logs = data.logs,
                    storage = data.storage
                )

                // Recent reading history
                SectionHeading("Recent Readings")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White,
                            RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (data.logs.isEmpty()) {
                        Text(
                            "No readings available.",
                            color = TextMuted
                        )
                    }

                    data.logs.take(6).forEachIndexed { index, log ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement =
                                    Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            if (log.isAbnormal)
                                                AlertRed
                                            else
                                                NormalGreen,
                                            CircleShape
                                        )
                                )

                                Column {
                                    Text(
                                        formatTime(log.recordedAt),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextDark
                                    )
                                    Text(
                                        if (log.isAbnormal)
                                            "Abnormal"
                                        else
                                            "Normal",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            Text(
                                formatTemp(log.temperature),
                                color = if (log.isAbnormal)
                                    AlertRed else TextDark,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (index < minOf(6, data.logs.size) - 1) {
                            HorizontalDivider(
                                color = Color(0xFFF0F2F7)
                            )
                        }
                    }
                }

                Text(
                    "CSISS · Cold Storage Inventory Supply System",
                    modifier = Modifier.fillMaxWidth(),
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        if (showLimitEditor) {
            uiState.snapshot?.let { data ->

                EditTemperatureLimitsDialog(
                    storageName = data.storage.storageName,
                    currentMin = data.storage.minTemperature,
                    currentMax = data.storage.maxTemperature,
                    isSaving = uiState.savingLimits,
                    saveError = uiState.limitSaveError,

                    onDismiss = {
                        showLimitEditor = false
                        viewModel.clearLimitSaveError()
                    },

                    onSave = { newMin, newMax ->
                        viewModel.updateLimits(
                            storageId = data.storage.storageId,
                            minimum = newMin,
                            maximum = newMax
                        ) {
                            showLimitEditor = false
                        }
                    }
                )
            }
        }
    }
}

// ========================================
// MAIN TEMPERATURE HERO CARD
// Keep the existing purple gradient, with a more disciplined hierarchy.
// ========================================

@Composable
private fun TemperatureHero(data: TemperatureSnapshot) {
    val latest = data.logs.firstOrNull()
    val abnormal = latest != null && (
        latest.temperature < data.storage.minTemperature ||
            latest.temperature > data.storage.maxTemperature
    )
    val statusLabel = when {
        latest == null -> "NO READING"
        abnormal -> "OUT OF RANGE"
        else -> "WITHIN RANGE"
    }
    val statusColor = when {
        latest == null -> Color(0xFFDAE3EF)
        abnormal -> Color(0xFFFFBEC6)
        else -> Color(0xFF98F0CD)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF141C43),
                        Color(0xFF373C85),
                        Color(0xFF6651A7)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = data.storage.storageName,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    maxLines = 2
                )
                Text(
                    text = "TEMPERATURE SENSOR",
                    color = Color(0xFFBECBE6),
                    fontSize = 10.sp,
                    letterSpacing = 0.9.sp
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "LATEST RECORDED TEMPERATURE",
                color = Color(0xFFD2DBED),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp
            )
            Text(
                text = latest?.let { formatTemp(it.temperature) } ?: "--",
                color = Color.White,
                fontSize = 52.sp,
                fontWeight = FontWeight.SemiBold
            )
            Surface(
                color = statusColor.copy(alpha = 0.16f),
                shape = RoundedCornerShape(50.dp)
            ) {
                Text(
                    text = statusLabel,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.16f))

        Text(
            text = latest?.let { "Recorded ${formatTime(it.recordedAt)}" }
                ?: "Waiting for sensor readings",
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFD2DBED),
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}


@Composable
private fun TemperaturePlaceholderHero(
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF141C43),
                        Color(0xFF373C85),
                        Color(0xFF6651A7)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Cold Storage",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "TEMPERATURE SENSOR",
                color = Color(0xFFBECBE6),
                fontSize = 10.sp,
                letterSpacing = 0.9.sp
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "LATEST RECORDED TEMPERATURE",
                color = Color(0xFFD2DBED),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                "—",
                color = Color.White,
                fontSize = 52.sp,
                fontWeight = FontWeight.SemiBold
            )

            Surface(
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(50.dp)
            ) {
                Text(
                    text = if (isLoading) "LOADING" else "UNAVAILABLE",
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 8.dp
                    ),
                    color = Color(0xFFDAE3EF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.16f)
        )

        Text(
            text = if (isLoading) {
                "Retrieving temperature data..."
            } else {
                "Temperature data unavailable"
            },
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFD2DBED),
            fontSize = 12.sp,
            textAlign =
                androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}


// ========================================
// MINIMUM / MAXIMUM TEMPERATURE CARDS
// ========================================

@Composable
private fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFFE7EEF4), RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp
        )
        Text(
            text = value,
            color = TextDark,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ========================================
// TEMPERATURE HISTORY CHART
// ========================================

@Composable
private fun TemperatureChart(
    logs: List<TemperatureLog>,
    storage: StorageArea
) {
    val ordered = logs.sortedBy { it.recordedAt }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White,
                RoundedCornerShape(22.dp)
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Temperature over time",
            color = TextDark,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )

        if (ordered.size < 2) {
            Text(
                "More readings are needed to show a trend.",
                color = TextMuted
            )
            return@Column
        }

        val minValue = minOf(
            ordered.minOf { it.temperature }.toFloat(),
            storage.minTemperature.toFloat()
        ) - 2f

        val maxValue = maxOf(
            ordered.maxOf { it.temperature }.toFloat(),
            storage.maxTemperature.toFloat()
        ) + 2f

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                formatTemp(maxValue.toDouble()),
                color = TextMuted,
                fontSize = 11.sp
            )
            Text(
                "Limit markers",
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) {
            val pad = 9.dp.toPx()
            val plotWidth = size.width - pad * 2f
            val plotHeight = size.height - pad * 2f

            fun yPosition(temp: Float): Float =
                size.height - pad -
                        ((temp - minValue) /
                                (maxValue - minValue)) * plotHeight

            val step = plotWidth / (ordered.size - 1)

            // Horizontal grid lines
            for (i in 0..4) {
                val y = pad + i * plotHeight / 4f

                drawLine(
                    color = Color(0xFFE8EFF6),
                    start = Offset(pad, y),
                    end = Offset(size.width - pad, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Allowed temperature limits
            listOf(
                storage.minTemperature.toFloat(),
                storage.maxTemperature.toFloat()
            ).forEach { limit ->
                val y = yPosition(limit)

                drawLine(
                    color = Color(0xFFB7CDD9),
                    start = Offset(pad, y),
                    end = Offset(size.width - pad, y),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Temperature trend line
            val path = Path()

            ordered.forEachIndexed { index, reading ->
                val x = pad + index * step
                val y = yPosition(reading.temperature.toFloat())

                if (index == 0) path.moveTo(x, y)
                else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = IceBlue,
                style = Stroke(width = 3.dp.toPx())
            )

            // Reading points
            ordered.forEachIndexed { index, reading ->
                drawCircle(
                    color = if (reading.isAbnormal)
                        AlertRed else IceBlue,
                    radius = 5.dp.toPx(),
                    center = Offset(
                        pad + index * step,
                        yPosition(reading.temperature.toFloat())
                    )
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                formatTime(ordered.first().recordedAt),
                fontSize = 10.sp,
                color = TextMuted
            )

            Text(
                formatTime(ordered.last().recordedAt),
                fontSize = 10.sp,
                color = TextMuted
            )
        }

        Text(
            "Blue: within limits  ·  Red: outside limits",
            color = TextMuted,
            fontSize = 11.sp
        )
    }
}

// ========================================
// SHARED DISPLAY HELPERS
// ========================================

@Composable
private fun SectionHeading(title: String) {
    Text(
        text = title,
        color = TextDark,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold
    )
}

private fun formatTemp(value: Double): String =
    String.format(Locale.US, "%.1f°C", value)

private fun formatTime(value: String): String {
    return runCatching {
        OffsetDateTime.parse(value)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(
                DateTimeFormatter.ofPattern(
                    "dd MMM, HH:mm",
                    Locale.ENGLISH
                )
            )
    }.getOrElse { value }
}


@Composable
private fun EditTemperatureLimitsDialog(
    storageName: String,
    currentMin: Double,
    currentMax: Double,
    isSaving: Boolean,
    saveError: String?,
    onDismiss: () -> Unit,
    onSave: (Double, Double) -> Unit
) {
    var minInput by remember(currentMin) {
        mutableStateOf(currentMin.toString())
    }

    var maxInput by remember(currentMax) {
        mutableStateOf(currentMax.toString())
    }

    val minValue = minInput.trim().toDoubleOrNull()
    val maxValue = maxInput.trim().toDoubleOrNull()

    val isValid =
        minValue != null &&
                maxValue != null &&
                minValue.isFinite() &&
                maxValue.isFinite() &&
                minValue < maxValue

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },

        title = {
            Text(
                text = "Adjust Temperature Limits",
                fontWeight = FontWeight.Bold
            )
        },

        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                Text(
                    "Set the allowed temperature range for $storageName."
                )

                OutlinedTextField(
                    value = minInput,
                    onValueChange = { minInput = it },
                    label = { Text("Minimum Temperature (°C)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = maxInput,
                    onValueChange = { maxInput = it },
                    label = { Text("Maximum Temperature (°C)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isValid) {
                    Text(
                        text = "Enter valid numbers. " +
                                "Minimum must be lower than maximum.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                if (saveError != null) {
                    Text(
                        text = saveError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },

        confirmButton = {
            Button(
                enabled = isValid && !isSaving,
                onClick = {
                    if (isValid) {
                        onSave(minValue!!, maxValue!!)
                    }
                }
            ) {
                Text(
                    if (isSaving) "Saving..."
                    else "Save Limits"
                )
            }
        },

        dismissButton = {
            TextButton(
                enabled = !isSaving,
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}


@Composable
private fun SensorConnectionIndicator(
    lastReadingAt: String?,
    currentTime: Instant,
    serverReachable: Boolean
) {
    val lastReading = lastReadingAt?.let {
        runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull()
    }
    val secondsSinceReading = lastReading?.let {
        Duration.between(it, currentTime).seconds
    }
    // A recent reading is evidence of connection; allow modest clock skew.
    val connected = serverReachable &&
            secondsSinceReading != null &&
            secondsSinceReading in -120L..180L
    val statusColor = when {
        !serverReachable -> TextMuted
        connected -> NormalGreen
        else -> AlertRed
    }
    val statusLabel = when {
        !serverReachable -> "Can't reach server"
        connected -> "ESP32 Connected"
        else -> "ESP32 Offline"
    }
    Surface(
        modifier = Modifier.semantics {
            contentDescription = "Sensor status: $statusLabel"
        },
        color = statusColor.copy(alpha = 0.10f),
        shape = RoundedCornerShape(50.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(statusColor, CircleShape)
            )
            Text(
                text = statusLabel,
                color = statusColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AlertsStatusButton(
    alertCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasAlerts = alertCount > 0
    val accent = if (hasAlerts) AlertRed else NormalGreen
    val label = when (alertCount) {
        0 -> "No Alerts"
        1 -> "1 Alert"
        else -> "$alertCount Alerts"
    }

    Surface(
        modifier = modifier
            .semantics { contentDescription = "Open temperature alerts: $label" }
            .clickable(onClick = onClick),
        color = accent.copy(alpha = 0.10f),
        shape = RoundedCornerShape(50.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(accent, CircleShape)
            )
            Text(
                text = label,
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(text = "→", color = accent, fontSize = 14.sp)
        }
    }
}