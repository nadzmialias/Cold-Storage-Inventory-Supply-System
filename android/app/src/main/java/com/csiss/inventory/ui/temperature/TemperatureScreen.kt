package com.csiss.inventory.ui.temperature

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.csiss.inventory.data.StorageArea
import com.csiss.inventory.data.TemperatureLog
import com.csiss.inventory.data.TemperatureSnapshot
import com.csiss.inventory.ui.components.CsissLightRefreshIndicator
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// CSISS Temperature color palette (aligned with SummaryStatsCard & app palette)
private val Navy = Color(0xFF132945)
private val IceBlue = Color(0xFF34B9DB)
private val Background = Color(0xFFF1F6FB)
private val TextDark = Color(0xFF182B48)
private val TextMuted = Color(0xFF718198)
private val CardBorder = Color(0xFFE0E6EF)
private val DividerColor = Color(0xFFEDF2F7)
private val AlertRed = Color(0xFFE45465)
private val AlertRedDark = Color(0xFFB42332)
private val AlertRedBg = Color(0xFFFDECEF)
private val AlertRedBorder = Color(0xFFF7B8C0)
private val NormalGreen = Color(0xFF159C76)

private val HeroGradientColors = listOf(
    Color(0xFF312E81),
    Color(0xFF4C1D95),
    Color(0xFF6D28D9)
)
private val HeroMutedText = Color(0xFFEDE9FE)
private val HeroDividerColor = Color(0xFF7C3AED)
private val HeroStatusNormal = Color(0xFF86EFAC)
private val HeroStatusAlert = Color(0xFFFECACA)
private val HeroStatusNeutral = Color(0xFFEDE9FE)
private val ChartAxisText = Color(0xFF526075)
private val ChartLineBlue = Color(0xFF0284C7)
private val ChartAlertRed = Color(0xFFDC2626)
private val TabularDigitsStyle = TextStyle(fontFeatureSettings = "tnum")
private val CardShape = RoundedCornerShape(18.dp)
private val HeroShape = RoundedCornerShape(20.dp)
private val FieldShape = RoundedCornerShape(12.dp)

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

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.setPollingActive(true)
                Lifecycle.Event.ON_STOP -> viewModel.setPollingActive(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.setPollingActive(false)
        }
    }

    val showRefreshIndicator =
        uiState.manualRefreshPending || (uiState.isLoading && !uiState.isInitialLoadComplete)

    PullToRefreshBox(
        isRefreshing = showRefreshIndicator,
        onRefresh = { viewModel.requestManualRefresh() },
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
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
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

            val activeAlertCount = uiState.snapshot?.alerts?.count { alert ->
                alert.status == "OPEN" || alert.status == "ACKNOWLEDGED"
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                SensorConnectionIndicator(
                    lastReadingAt = uiState.snapshot?.logs?.firstOrNull()?.recordedAt,
                    currentTime = uiState.currentTime,
                    serverReachable = uiState.error == null,
                    isLoading = showRefreshIndicator
                )
                AlertsStatusButton(
                    alertCount = activeAlertCount,
                    isLoading = showRefreshIndicator,
                    onClick = onOpenAlerts
                )
            }

            uiState.error?.let { message ->
                TemperatureErrorBanner(
                    message = message,
                    onRetry = { viewModel.requestManualRefresh() }
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

                if (canEditLimits) {
                    Button(
                        onClick = {
                            viewModel.clearLimitSaveError()
                            showLimitEditor = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = FieldShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Navy,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Edit Limits", fontWeight = FontWeight.SemiBold)
                    }
                }

                SectionHeading("Temperature Trend")

                TemperatureChart(
                    logs = data.logs,
                    storage = data.storage
                )

                SectionHeading("Recent Readings")

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = CardShape,
                    color = Color.White,
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (data.logs.isEmpty()) {
                            Text(
                                text = "No readings available.",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }

                        data.logs.take(6).forEachIndexed { index, log ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(
                                                if (log.isAbnormal) AlertRed else NormalGreen,
                                                CircleShape
                                            )
                                    )

                                    Column {
                                        Text(
                                            text = formatTime(log.recordedAt),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextDark
                                        )
                                        Text(
                                            text = if (log.isAbnormal) "Abnormal" else "Normal",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                }

                                Text(
                                    text = formatTemp(log.temperature),
                                    color = if (log.isAbnormal) AlertRed else TextDark,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (index < minOf(6, data.logs.size) - 1) {
                                HorizontalDivider(color = DividerColor)
                            }
                        }
                    }
                }
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

@Composable
private fun TemperatureErrorBanner(
    message: String,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FieldShape,
        color = AlertRedBg,
        border = BorderStroke(1.dp, AlertRedBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = message,
                color = AlertRedDark,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onRetry,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Retry",
                    color = AlertRedDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TemperatureHero(data: TemperatureSnapshot) {
    val latest = data.logs.firstOrNull()
    val minLimit = data.storage.minTemperature
    val maxLimit = data.storage.maxTemperature
    val abnormal = latest != null && (latest.temperature < minLimit || latest.temperature > maxLimit)

    val statusLabel = when {
        latest == null -> "No reading"
        abnormal -> "Out of range"
        else -> "Within range"
    }
    val statusColor = when {
        latest == null -> HeroStatusNeutral
        abnormal -> HeroStatusAlert
        else -> HeroStatusNormal
    }

    TemperatureHeroCard(
        title = data.storage.storageName,
        statusLabel = statusLabel,
        statusColor = statusColor,
        temperatureText = latest?.let { formatTemp(it.temperature) } ?: "--",
        footerText = latest?.let { "Recorded ${formatTime(it.recordedAt)}" }
            ?: "Waiting for sensor readings"
    )
}

@Composable
private fun TemperaturePlaceholderHero(isLoading: Boolean) {
    TemperatureHeroCard(
        title = "Cold Storage",
        statusLabel = if (isLoading) "Loading" else "Unavailable",
        statusColor = HeroStatusNeutral,
        temperatureText = "--",
        footerText = if (isLoading) {
            "Loading sensor reading"
        } else {
            "Sensor data unavailable. Pull down to refresh"
        }
    )
}

@Composable
private fun TemperatureHeroCard(
    title: String,
    statusLabel: String,
    statusColor: Color,
    temperatureText: String,
    footerText: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(colors = HeroGradientColors),
                shape = HeroShape
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(statusColor, CircleShape)
                )
                Text(
                    text = statusLabel,
                    color = statusColor,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Latest reading",
                color = HeroMutedText,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = temperatureText,
                style = TabularDigitsStyle,
                color = Color.White,
                fontSize = 48.sp,
                lineHeight = 52.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }

        HorizontalDivider(color = HeroDividerColor.copy(alpha = 0.6f))

        Text(
            text = footerText,
            style = TabularDigitsStyle,
            color = HeroMutedText,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = CardShape,
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )
            Text(
                text = value,
                color = TextDark,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TemperatureChart(
    logs: List<TemperatureLog>,
    storage: StorageArea
) {
    val ordered = remember(logs) {
        logs.sortedBy {
            runCatching { OffsetDateTime.parse(it.recordedAt).toInstant() }
                .getOrDefault(Instant.EPOCH)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Temperature over time",
                color = TextDark,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )

            if (ordered.size < 2) {
                Text(
                    text = "At least 2 readings are needed to plot a trend. Pull down to refresh",
                    color = ChartAxisText,
                    fontSize = 13.sp
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

            Text(
                text = formatTemp(maxValue.toDouble()),
                style = TabularDigitsStyle,
                color = ChartAxisText,
                fontSize = 11.sp
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val pad = 8.dp.toPx()
                val plotWidth = size.width - pad * 2f
                val plotHeight = size.height - pad * 2f

                fun yPosition(temp: Float): Float =
                    size.height - pad -
                        ((temp - minValue) / (maxValue - minValue)) * plotHeight

                val step = plotWidth / (ordered.size - 1)

                // Shaded allowed-range band between min and max limits
                val yMaxLimit = yPosition(storage.maxTemperature.toFloat())
                val yMinLimit = yPosition(storage.minTemperature.toFloat())
                val bandTop = minOf(yMaxLimit, yMinLimit)
                val bandHeight = kotlin.math.abs(yMinLimit - yMaxLimit)
                drawRect(
                    color = ChartLineBlue.copy(alpha = 0.09f),
                    topLeft = Offset(pad, bandTop),
                    size = Size(plotWidth, bandHeight)
                )

                for (i in 0..4) {
                    val y = pad + i * plotHeight / 4f
                    drawLine(
                        color = DividerColor,
                        start = Offset(pad, y),
                        end = Offset(size.width - pad, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val dashEffect = PathEffect.dashPathEffect(
                    floatArrayOf(6.dp.toPx(), 4.dp.toPx()),
                    0f
                )
                listOf(yMaxLimit, yMinLimit).forEach { y ->
                    drawLine(
                        color = ChartLineBlue.copy(alpha = 0.5f),
                        start = Offset(pad, y),
                        end = Offset(size.width - pad, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                }

                val path = Path()
                ordered.forEachIndexed { index, reading ->
                    val x = pad + index * step
                    val y = yPosition(reading.temperature.toFloat())
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = ChartLineBlue,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                ordered.forEachIndexed { index, reading ->
                    val center = Offset(
                        pad + index * step,
                        yPosition(reading.temperature.toFloat())
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4.5.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = if (reading.isAbnormal) ChartAlertRed else ChartLineBlue,
                        radius = if (reading.isAbnormal) 3.5.dp.toPx() else 3.dp.toPx(),
                        center = center
                    )
                }
            }

            Text(
                text = formatTemp(minValue.toDouble()),
                style = TabularDigitsStyle,
                color = ChartAxisText,
                fontSize = 11.sp
            )

            HorizontalDivider(color = DividerColor)

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(ChartLineBlue, CircleShape)
                    )
                    Text(
                        text = "Within limits",
                        color = ChartAxisText,
                        fontSize = 11.sp
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(ChartAlertRed, CircleShape)
                    )
                    Text(
                        text = "Outside limits",
                        color = ChartAxisText,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String) {
    Text(
        text = title,
        color = TextDark,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold
    )
}

private fun formatTemp(value: Double): String =
    String.format(Locale.US, "%.1f°C", value)

private fun formatTime(value: String): String {
    return runCatching {
        OffsetDateTime.parse(value)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale.ENGLISH))
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
    var minInput by remember(currentMin) { mutableStateOf(currentMin.toString()) }
    var maxInput by remember(currentMax) { mutableStateOf(currentMax.toString()) }

    val minValue = minInput.trim().toDoubleOrNull()
    val maxValue = maxInput.trim().toDoubleOrNull()

    val isValid =
        minValue != null &&
            maxValue != null &&
            minValue.isFinite() &&
            maxValue.isFinite() &&
            minValue < maxValue

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Navy,
        unfocusedBorderColor = CardBorder,
        focusedLabelColor = Navy,
        unfocusedLabelColor = TextMuted,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        cursorColor = Navy
    )

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Adjust Temperature Limits",
                color = TextDark,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Set the allowed temperature range for $storageName. Negative values (e.g. -18) are supported.",
                    color = TextMuted,
                    fontSize = 13.sp
                )

                // KeyboardType.Text ensures minus (-) key is accessible on all Android keyboards
                OutlinedTextField(
                    value = minInput,
                    onValueChange = { minInput = it },
                    label = { Text("Minimum Temperature (°C)") },
                    singleLine = true,
                    enabled = !isSaving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    shape = FieldShape,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = maxInput,
                    onValueChange = { maxInput = it },
                    label = { Text("Maximum Temperature (°C)") },
                    singleLine = true,
                    enabled = !isSaving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    shape = FieldShape,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isValid) {
                    Text(
                        text = "Enter valid numbers. Minimum must be lower than maximum.",
                        color = AlertRedDark,
                        fontSize = 12.sp
                    )
                }

                if (saveError != null) {
                    Text(
                        text = saveError,
                        color = AlertRedDark,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = isValid && !isSaving,
                shape = FieldShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Navy,
                    contentColor = Color.White
                ),
                onClick = {
                    if (isValid) {
                        onSave(minValue!!, maxValue!!)
                    }
                }
            ) {
                Text(
                    text = if (isSaving) "Saving..." else "Save Limits",
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSaving,
                onClick = onDismiss
            ) {
                Text("Cancel", color = TextMuted, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun SensorConnectionIndicator(
    lastReadingAt: String?,
    currentTime: Instant,
    serverReachable: Boolean,
    isLoading: Boolean = false
) {
    val lastReading = lastReadingAt?.let {
        runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull()
    }
    val secondsSinceReading = lastReading?.let {
        Duration.between(it, currentTime).seconds
    }
    val connected = serverReachable &&
        secondsSinceReading != null &&
        secondsSinceReading in -120L..180L
    val statusColor = when {
        isLoading || !serverReachable -> ChartAxisText
        connected -> NormalGreen
        else -> AlertRed
    }
    val statusLabel = when {
        isLoading -> "Checking sensor"
        !serverReachable -> "Can't reach server"
        connected -> "ESP32 Connected"
        else -> "ESP32 Offline"
    }

    Surface(
        modifier = Modifier.semantics {
            contentDescription = "Sensor status: $statusLabel"
        },
        color = statusColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(50.dp)
    ) {
        Row(
            modifier = Modifier
                .height(34.dp)
                .padding(horizontal = 12.dp),
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AlertsStatusButton(
    alertCount: Int?,
    isLoading: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showGrey = isLoading || alertCount == null
    val hasAlerts = (alertCount ?: 0) > 0
    val accent = when {
        showGrey -> ChartAxisText
        hasAlerts -> AlertRed
        else -> NormalGreen
    }
    val label = when {
        isLoading -> "Loading alerts"
        alertCount == null -> "Alerts"
        alertCount == 0 -> "No Alerts"
        alertCount == 1 -> "1 Alert"
        else -> "$alertCount Alerts"
    }

    Surface(
        onClick = onClick,
        modifier = modifier.semantics {
            contentDescription = "Open temperature alerts: $label"
        },
        color = accent.copy(alpha = 0.12f),
        shape = RoundedCornerShape(50.dp)
    ) {
        Row(
            modifier = Modifier
                .height(34.dp)
                .padding(horizontal = 12.dp),
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
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(text = "→", color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
