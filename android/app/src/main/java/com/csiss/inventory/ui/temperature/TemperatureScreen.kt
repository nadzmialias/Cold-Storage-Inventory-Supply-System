
package com.csiss.inventory.ui.temperature

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csiss.inventory.data.*
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.Instant
import java.time.Duration
import kotlinx.coroutines.delay
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import com.csiss.inventory.data.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// CSISS color palette
private val Navy = Color(0xFF132945)
private val DeepBlue = Color(0xFF1D466B)
private val IceBlue = Color(0xFF34B9DB)
private val Background = Color(0xFFF1F6FB)
private val TextDark = Color(0xFF182B48)
private val TextMuted = Color(0xFF718198)
private val AlertRed = Color(0xFFE45465)
private val NormalGreen = Color(0xFF159C76)

@Composable
fun TemperatureScreen(
    useDemoData: Boolean = true,
    canEditLimits: Boolean = useDemoData
) {
    val repository = remember { TemperatureRepository() }

    var snapshot by remember {
        mutableStateOf<TemperatureSnapshot?>(null)
    }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var showAlertsPage by remember {
        mutableStateOf(false)
    }
    val demoAlertOverrides = remember {
        mutableStateMapOf<Long, String>()
    }
    var currentTime by remember {
        mutableStateOf(Instant.now())
    }

    val scope = rememberCoroutineScope()

    var showLimitEditor by remember {
        mutableStateOf(false)
    }

    var alertUpdating by remember {
        mutableStateOf(false)
    }

    var alertActionError by remember {
        mutableStateOf<String?>(null)
    }

    var savingLimits by remember {
        mutableStateOf(false)
    }

    var limitSaveError by remember {
        mutableStateOf<String?>(null)
    }

    var demoMinOverride by remember {
        mutableStateOf<Double?>(null)
    }

    var demoMaxOverride by remember {
        mutableStateOf<Double?>(null)
    }



    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            currentTime = Instant.now()
        }
    }

    LaunchedEffect(useDemoData, refresh) {
        loading = true
        error = null

        try {
            snapshot = if (useDemoData) {

                val demo = repository.loadDemo()

                // Keep previously edited temperature limits
                val savedMin = demoMinOverride
                val savedMax = demoMaxOverride

                val adjustedDemo =
                    if (savedMin != null && savedMax != null) {
                        demo.copy(
                            storage = demo.storage.copy(
                                minTemperature = savedMin,
                                maxTemperature = savedMax
                            )
                        )
                    } else {
                        demo
                    }

                // Keep previously changed alert statuses
                adjustedDemo.copy(
                    alerts = adjustedDemo.alerts.map { alert ->
                        alert.copy(
                            status = demoAlertOverrides[alert.alertId]
                                ?: alert.status
                        )
                    }
                )

            } else {
                repository.loadLive()
            }
        } catch (e: Exception) {
            snapshot = null
            error = "Unable to retrieve temperature data. " +
                    "Check your connection and login."
        } finally {
            loading = false
        }
    }

    LaunchedEffect(useDemoData) {

        if (!useDemoData) {

            while (true) {

                delay(20_000L)

                refresh++
            }
        }
    }

    BackHandler(enabled = showAlertsPage) {
        showAlertsPage = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .safeDrawingPadding()
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "CSISS / MONITORING",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "Cold Storage",
                        color = TextDark,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold
                    )


                }


                Button(
                    onClick = { refresh++ },
                    enabled = !loading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Navy,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(
                        horizontal = 14.dp,
                        vertical = 8.dp
                    )
                ) {
                    Text("Refresh")
                }
            }


// ========================================
// SENSOR INDICATOR + ALERTS BUTTON
// ========================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SensorConnectionIndicator(
                    lastReadingAt = snapshot
                        ?.logs
                        ?.firstOrNull()
                        ?.recordedAt,
                    currentTime = currentTime,
                    demoMode = useDemoData,
                    isLoading =  loading && snapshot == null
                )

                //Spacer(modifier = Modifier.height(12.dp))

                AlertsStatusButton(
                    alertCount = snapshot?.alerts?.count { alert ->
                        alert.status == "OPEN" ||
                                alert.status == "ACKNOWLEDGED"
                    } ?: 0,

                    onClick = {
                        showAlertsPage = true
                    },
                )
            }


            if (loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            error?.let {
                Text(
                    it,
                    color = AlertRed,
                    fontSize = 14.sp
                )
            }

            snapshot?.let { data ->

                // Main temperature display
                TemperatureHero(data)

                // Minimum and Maximum temperature cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "MINIMUM",
                        value = formatTemp(data.storage.minTemperature),
                        symbol = "↓",
                        modifier = Modifier.weight(1f)
                    )

                    MetricCard(
                        title = "MAXIMUM",
                        value = formatTemp(data.storage.maxTemperature),
                        symbol = "↑",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Edit Limits button below the cards
                if (canEditLimits) {
                    OutlinedButton(
                        onClick = {
                            limitSaveError = null
                            showLimitEditor = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Navy,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Edit Limits")
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
            snapshot?.let { data ->

                EditTemperatureLimitsDialog(
                    currentMin = data.storage.minTemperature,
                    currentMax = data.storage.maxTemperature,
                    isSaving = savingLimits,
                    saveError = limitSaveError,

                    onDismiss = {
                        showLimitEditor = false
                        limitSaveError = null
                    },

                    onSave = { newMin, newMax ->

                        if (useDemoData) {

                            // Save inside demo UI state
                            demoMinOverride = newMin
                            demoMaxOverride = newMax

                            snapshot = snapshot?.copy(
                                storage = data.storage.copy(
                                    minTemperature = newMin,
                                    maxTemperature = newMax
                                )
                            )

                            showLimitEditor = false

                        } else {

                            scope.launch {
                                savingLimits = true
                                limitSaveError = null

                                try {
                                    val updatedStorage =
                                        repository.updateLimits(
                                            storageId = data.storage.storageId,
                                            minimum = newMin,
                                            maximum = newMax
                                        )

                                    snapshot = snapshot?.copy(
                                        storage = updatedStorage
                                    )

                                    showLimitEditor = false

                                } catch (e: Exception) {
                                    limitSaveError =
                                        "Unable to save limits. " +
                                                "Check your Admin login and permissions."
                                } finally {
                                    savingLimits = false
                                }
                            }
                        }
                    }
                )
            }
        }


// ========================================
// ANIMATED TEMPERATURE ALERTS PAGE
// ========================================

        AnimatedVisibility(
            visible = showAlertsPage,

            enter = slideInHorizontally(
                initialOffsetX = { fullWidth ->
                    fullWidth
                },
                animationSpec = tween(
                    durationMillis = 350
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = 300
                )
            ),

            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth ->
                    fullWidth
                },
                animationSpec = tween(
                    durationMillis = 350
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = 250
                )
            ),

            modifier = Modifier.fillMaxSize()
        ) {


            TemperatureAlertsScreen(
                alerts = snapshot?.alerts.orEmpty(),

                isLoading = loading || alertUpdating,

                errorMessage = alertActionError ?: error,

                onBack = {
                    showAlertsPage = false
                    alertActionError = null
                },

                onRefresh = {
                    alertActionError = null
                    refresh++
                },

                canChangeStatus = true,

                onStatusChange = { alertId, newStatus ->

                    if (useDemoData) {

                        // Keep existing demo functionality.
                        snapshot?.let { current ->

                            val oldStatus = current.alerts
                                .firstOrNull {
                                    it.alertId == alertId
                                }?.status

                            val validTransition =
                                (oldStatus == "OPEN" &&
                                        newStatus == "ACKNOWLEDGED") ||
                                        (oldStatus == "ACKNOWLEDGED" &&
                                                newStatus == "RESOLVED")

                            if (validTransition) {
                                demoAlertOverrides[alertId] = newStatus

                                snapshot = current.copy(
                                    alerts = current.alerts.map { alert ->
                                        if (alert.alertId == alertId) {
                                            alert.copy(status = newStatus)
                                        } else {
                                            alert
                                        }
                                    }
                                )
                            }
                        }

                    } else {

                        // Live mode: save to Supabase.
                        scope.launch {
                            alertUpdating = true
                            alertActionError = null

                            try {
                                val updatedAlert =
                                    repository.updateAlertStatus(
                                        alertId = alertId,
                                        newStatus = newStatus
                                    )

                                // Update the screen after the database
                                // confirms a successful change.
                                snapshot = snapshot?.let { current ->
                                    current.copy(
                                        alerts = current.alerts.map { alert ->
                                            if (alert.alertId == alertId) {
                                                updatedAlert
                                            } else {
                                                alert
                                            }
                                        }
                                    )
                                }

                            } catch (e: Exception) {
                                alertActionError =
                                    "Unable to update alert. " +
                                            "Check your connection, login " +
                                            "and permissions."

                            } finally {
                                alertUpdating = false
                            }
                        }
                    }
                }
            )


        }
    }

}

// ========================================
// MAIN TEMPERATURE HERO CARD
// ========================================

@Composable
private fun TemperatureHero(data: TemperatureSnapshot) {
    val latest = data.logs.firstOrNull()
    val abnormal = latest != null &&
            (
                    latest.temperature < data.storage.minTemperature ||
                            latest.temperature > data.storage.maxTemperature
                    )

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
                shape = RoundedCornerShape(26.dp)
            )

            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                data.storage.storageName,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp
            )

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        Color.White.copy(alpha = 0.12f),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("❄", fontSize = 24.sp)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "LATEST RECORDED TEMPERATURE",
                color = Color(0xFFB8CFE2),
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )

            Text(
                latest?.let { formatTemp(it.temperature) } ?: "--",
                color = Color.White,
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold
            )

            val status = when {
                latest == null -> "NO DATA"
                abnormal -> "ABNORMAL"
                else -> "NORMAL"
            }

            val statusColor = when {
                latest == null -> Color(0xFFCAD8E8)
                abnormal -> Color(0xFFFFB2BB)
                else -> Color(0xFF8FF1C6)
            }

            Box(
                modifier = Modifier
                    .background(
                        statusColor.copy(alpha = 0.15f),
                        RoundedCornerShape(50.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                Text(
                    "● $status",
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.15f)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                latest?.let {
                    "Recorded ${formatTime(it.recordedAt)}"
                } ?: "Waiting for sensor readings",
                color = Color(0xFFBCD2E5),
                fontSize = 12.sp
            )
        }
    }
}

// ========================================
// METRIC CARDS
// ========================================

@Composable
private fun MetricCard(
    title: String,
    value: String,
    symbol: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                Color.White,
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                symbol,
                color = IceBlue,
                fontSize = 18.sp
            )
            Text(
                title,
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            value,
            color = TextDark,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
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
                "● Normal    ● Abnormal",
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
            "Blue points: within range. Red points: outside range.",
            color = TextMuted,
            fontSize = 11.sp
        )
    }
}

// ========================================
// ALERT CARD
// ========================================

@Composable
private fun AlertCard(alert: TemperatureAlert) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFFFFF1F2),
                RoundedCornerShape(18.dp)
            )
            .border(
                1.dp,
                Color(0xFFFFDDE2),
                RoundedCornerShape(18.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("!", color = AlertRed, fontWeight = FontWeight.Bold)

            Text(
                alert.title,
                color = Color(0xFF9A263B),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Text(
            alert.message,
            color = Color(0xFF8D4A57),
            fontSize = 13.sp
        )

        Text(
            "${alert.status} · ${formatTime(alert.createdAt)}",
            color = Color(0xFFAD6371),
            fontSize = 11.sp
        )
    }
}

// ========================================
// SMALL REUSABLE COMPONENTS
// ========================================

@Composable
private fun SectionHeading(title: String) {
    Text(
        title,
        color = TextDark,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun InfoCard(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White,
                RoundedCornerShape(18.dp)
            )
            .padding(18.dp)
    ) {
        Text(message, color = TextMuted)
    }
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
                    "Set the allowed temperature range " +
                            "for Freezer Room A."
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
    demoMode: Boolean,
    isLoading: Boolean
) {
    val lastReading = lastReadingAt?.let {
        runCatching {
            OffsetDateTime.parse(it).toInstant()
        }.getOrNull()
    }

    val secondsSinceReading = lastReading?.let {
        Duration.between(it, currentTime).seconds
    }

    // Recent if within 5 minutes.
    // Allow up to 2 minutes of clock difference.
    val isConnected = secondsSinceReading != null &&
            secondsSinceReading in -120L..180L

    val statusColor = when {
        isLoading -> TextMuted
        isConnected -> NormalGreen
        else -> AlertRed
    }

    val statusLabel = when {
        isLoading -> "Checking sensor"
        isConnected -> "ESP32 Connected"
        else -> "ESP32 Disconnected"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = statusColor.copy(alpha = 0.12f),
            shape = RoundedCornerShape(50.dp)
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 7.dp
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            statusColor,
                            CircleShape
                        )
                )

                Text(
                    text = statusLabel,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


@Composable
private fun AlertIconButton(
    onClick: () -> Unit
) {
    val alertColor = Color(0xFFEF2020)

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .semantics {
                contentDescription = "Open temperature alerts"
            }
    ) {
        Canvas(
            modifier = Modifier.size(38.dp)
        ) {
            val borderWidth = 3.5.dp.toPx()

            // Rounded square outline
            drawRoundRect(
                color = alertColor,
                topLeft = Offset(
                    borderWidth / 2,
                    borderWidth / 2
                ),
                size = Size(
                    size.width - borderWidth,
                    size.height - borderWidth
                ),
                cornerRadius = CornerRadius(
                    10.dp.toPx()
                ),
                style = Stroke(
                    width = borderWidth
                )
            )

            val centerX = size.width / 2

            // Exclamation mark vertical line
            drawLine(
                color = alertColor,
                start = Offset(
                    centerX,
                    size.height * 0.25f
                ),
                end = Offset(
                    centerX,
                    size.height * 0.60f
                ),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Exclamation mark dot
            drawCircle(
                color = alertColor,
                radius = 2.5.dp.toPx(),
                center = Offset(
                    centerX,
                    size.height * 0.77f
                )
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

    val accentColor = if (hasAlerts) {
        Color(0xFFE53935) // Red
    } else {
        Color(0xFF159C76) // Green
    }

    val label = when {
        alertCount <= 0 -> "No Alerts"
        alertCount == 1 -> "1 Alert"
        else -> "$alertCount Alerts"
    }

    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = accentColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(50.dp)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 7.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // Rounded exclamation icon
            Canvas(
                modifier = Modifier.size(22.dp)
            ) {
                val stroke = 2.dp.toPx()

                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(
                        size.width - stroke,
                        size.height - stroke
                    ),
                    cornerRadius = CornerRadius(
                        6.dp.toPx()
                    ),
                    style = Stroke(width = stroke)
                )

                val centerX = size.width / 2

                drawLine(
                    color = accentColor,
                    start = Offset(
                        centerX,
                        size.height * 0.23f
                    ),
                    end = Offset(
                        centerX,
                        size.height * 0.58f
                    ),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                drawCircle(
                    color = accentColor,
                    radius = 1.5.dp.toPx(),
                    center = Offset(
                        centerX,
                        size.height * 0.76f
                    )
                )
            }

            Text(
                text = label,
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
