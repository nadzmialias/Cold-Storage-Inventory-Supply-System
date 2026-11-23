package com.csiss.inventory.ui.report

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.csiss.inventory.data.ReportFormat
import com.csiss.inventory.data.ReportRecord
import com.csiss.inventory.data.ReportType
import com.csiss.inventory.ui.components.CsissLightRefreshIndicator
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Navy = Color(0xFF132945)
private val IceBlue = Color(0xFF0284C7)
private val Background = Color(0xFFF1F6FB)
private val SubtleSurface = Color(0xFFF5F7FA)
private val TrackBg = Color(0xFFECEFF4)
private val TagBg = Color(0xFFEEF2F6)
private val Ink = Color(0xFF182B48)
private val Slate = Color(0xFF526075)
private val CardBorder = Color(0xFFE0E6EF)
private val DividerColor = Color(0xFFEDF2F7)
private val RedDark = Color(0xFFB42332)
private val RedAccent = Color(0xFFDC2626)
private val GreenAccent = Color(0xFF16A34A)
private val AmberAccent = Color(0xFFD97706)

private val CardShape = RoundedCornerShape(16.dp)
private val TrackShape = RoundedCornerShape(12.dp)
private val ControlShape = RoundedCornerShape(10.dp)
private val SegmentShape = RoundedCornerShape(8.dp)
private val TagShape = RoundedCornerShape(6.dp)
private val TabularDigitsStyle = TextStyle(fontFeatureSettings = "tnum")
private val DisplayDateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    var selectedTypeName by rememberSaveable { mutableStateOf(ReportType.INVENTORY.dbValue) }
    var selectedFormatName by rememberSaveable { mutableStateOf(ReportFormat.PDF.dbValue) }
    var startDateIso by rememberSaveable { mutableStateOf(LocalDate.now().minusDays(7).toString()) }
    var endDateIso by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var useAllTime by rememberSaveable { mutableStateOf(false) }
    var historyFilter by rememberSaveable { mutableStateOf("ALL") }
    var openError by remember { mutableStateOf<String?>(null) }

    val selectedType = ReportType.fromDb(selectedTypeName)
    val selectedFormat = ReportFormat.fromDb(selectedFormatName)
    val parsedStart = runCatching { LocalDate.parse(startDateIso) }.getOrNull()
    val parsedEnd = runCatching { LocalDate.parse(endDateIso) }.getOrNull()

    val filteredHistory = remember(uiState.history, historyFilter) {
        if (historyFilter == "ALL") {
            uiState.history
        } else {
            uiState.history.filter { it.reportType.equals(historyFilter, ignoreCase = true) }
        }
    }

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        state = pullToRefreshState,
        indicator = {
            CsissLightRefreshIndicator(
                state = pullToRefreshState,
                isRefreshing = uiState.isLoading,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Analytics & exports",
                        color = Slate,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Reports",
                        color = Ink,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            item {
                GenerateReportCard(
                    selectedType = selectedType,
                    selectedFormat = selectedFormat,
                    useAllTime = useAllTime,
                    startDate = parsedStart,
                    endDate = parsedEnd,
                    isGenerating = uiState.isGenerating,
                    onSelectType = {
                        selectedTypeName = it.dbValue
                        viewModel.clearError()
                    },
                    onSelectFormat = { selectedFormatName = it.dbValue },
                    onSelectPreset = { days ->
                        if (days == null) {
                            useAllTime = true
                        } else {
                            useAllTime = false
                            val today = LocalDate.now()
                            startDateIso = today.minusDays(days.toLong()).toString()
                            endDateIso = today.toString()
                        }
                        viewModel.clearError()
                    },
                    onPickStartDate = { date ->
                        useAllTime = false
                        startDateIso = date.toString()
                        viewModel.clearError()
                    },
                    onPickEndDate = { date ->
                        useAllTime = false
                        endDateIso = date.toString()
                        viewModel.clearError()
                    },
                    onGenerate = {
                        openError = null
                        val start = if (selectedType.supportsDateRange && !useAllTime) parsedStart else null
                        val end = if (selectedType.supportsDateRange && !useAllTime) parsedEnd else null
                        viewModel.generateReport(
                            context = context,
                            type = selectedType,
                            format = selectedFormat,
                            startDate = start,
                            endDate = end
                        )
                    }
                )
            }

            uiState.lastExport?.let { exported ->
                item {
                    ExportSuccessCard(
                        exported = exported,
                        onOpen = {
                            val opened = openExportedFile(context, exported)
                            if (!opened) {
                                openError = "No app found to open ${exported.format.label} files. Check Downloads/CSISS"
                            }
                        },
                        onShare = {
                            shareExportedFile(context, exported)
                        },
                        onDismiss = {
                            viewModel.clearLastExport()
                            openError = null
                        }
                    )
                }
            }

            val displayError = openError ?: uiState.error
            if (!displayError.isNullOrBlank()) {
                item {
                    ReportErrorBanner(
                        message = displayError,
                        onRetry = if (uiState.error != null) ({ viewModel.refresh() }) else null
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Report history",
                            color = Ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${filteredHistory.size}",
                            style = TabularDigitsStyle,
                            color = Slate,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HistoryFilterChip(
                            label = "All",
                            selected = historyFilter == "ALL",
                            onClick = { historyFilter = "ALL" }
                        )
                        ReportType.entries.forEach { type ->
                            HistoryFilterChip(
                                label = type.shortLabel,
                                selected = historyFilter == type.dbValue,
                                onClick = { historyFilter = type.dbValue }
                            )
                        }
                    }
                }
            }

            if (!uiState.hasLoadedOnce && uiState.isLoading) {
                item {
                    ReportEmptyCard(
                        title = "Loading report history",
                        subtitle = null
                    )
                }
            } else if (filteredHistory.isEmpty()) {
                item {
                    ReportEmptyCard(
                        title = if (historyFilter == "ALL") {
                            "No reports yet"
                        } else {
                            "No reports in this category"
                        },
                        subtitle = "Choose a report type above and tap Export"
                    )
                }
            } else {
                items(filteredHistory, key = { it.reportId }) { record ->
                    ReportHistoryCard(record = record)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenerateReportCard(
    selectedType: ReportType,
    selectedFormat: ReportFormat,
    useAllTime: Boolean,
    startDate: LocalDate?,
    endDate: LocalDate?,
    isGenerating: Boolean,
    onSelectType: (ReportType) -> Unit,
    onSelectFormat: (ReportFormat) -> Unit,
    onSelectPreset: (Int?) -> Unit,
    onPickStartDate: (LocalDate) -> Unit,
    onPickEndDate: (LocalDate) -> Unit,
    onGenerate: () -> Unit
) {
    val context = LocalContext.current
    var typeExpanded by remember { mutableStateOf(false) }

    val dateRangeInvalid = selectedType.supportsDateRange &&
        !useAllTime &&
        startDate != null &&
        endDate != null &&
        startDate.isAfter(endDate)

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Generate report",
                color = Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { if (!isGenerating) typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedType.label,
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isGenerating,
                        label = { Text("Report type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        shape = ControlShape,
                        colors = reportFieldColors(),
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = !isGenerating)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false },
                        containerColor = Color.White
                    ) {
                        ReportType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = type.label,
                                            color = Ink,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = type.description,
                                            color = Slate,
                                            fontSize = 12.sp,
                                            maxLines = 2
                                        )
                                    }
                                },
                                onClick = {
                                    onSelectType(type)
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = selectedType.description,
                    color = Slate,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            if (selectedType.supportsDateRange) {
                HorizontalDivider(color = DividerColor)

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Date range",
                        color = Slate,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = TrackShape,
                        color = TrackBg,
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val today = LocalDate.now()
                            val is7Days = !useAllTime && startDate == today.minusDays(7) && endDate == today
                            val is30Days = !useAllTime && startDate == today.minusDays(30) && endDate == today

                            PresetSegment(
                                label = "7 days",
                                selected = is7Days,
                                enabled = !isGenerating,
                                onClick = { onSelectPreset(7) },
                                modifier = Modifier.weight(1f)
                            )
                            PresetSegment(
                                label = "30 days",
                                selected = is30Days,
                                enabled = !isGenerating,
                                onClick = { onSelectPreset(30) },
                                modifier = Modifier.weight(1f)
                            )
                            PresetSegment(
                                label = "All time",
                                selected = useAllTime,
                                enabled = !isGenerating,
                                onClick = { onSelectPreset(null) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (!useAllTime) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DatePickerButton(
                                label = "Start date",
                                date = startDate,
                                enabled = !isGenerating,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val init = startDate ?: LocalDate.now().minusDays(7)
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d -> onPickStartDate(LocalDate.of(y, m + 1, d)) },
                                        init.year,
                                        init.monthValue - 1,
                                        init.dayOfMonth
                                    ).show()
                                }
                            )
                            DatePickerButton(
                                label = "End date",
                                date = endDate,
                                enabled = !isGenerating,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val init = endDate ?: LocalDate.now()
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d -> onPickEndDate(LocalDate.of(y, m + 1, d)) },
                                        init.year,
                                        init.monthValue - 1,
                                        init.dayOfMonth
                                    ).show()
                                }
                            )
                        }

                        if (dateRangeInvalid) {
                            Text(
                                text = "Start date must be on or before end date",
                                color = RedDark,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = DividerColor)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Export format",
                    color = Slate,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = TrackShape,
                    color = TrackBg,
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectableGroup()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ReportFormat.entries.forEach { format ->
                            val selected = format == selectedFormat
                            Surface(
                                shape = SegmentShape,
                                color = if (selected) Navy else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 40.dp)
                                    .selectable(
                                        selected = selected,
                                        enabled = !isGenerating,
                                        role = Role.Tab,
                                        onClick = { onSelectFormat(format) }
                                    )
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = if (format == ReportFormat.PDF) "PDF document" else "CSV sheet",
                                        color = if (selected) Color.White else Slate,
                                        fontSize = 13.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onGenerate,
                enabled = !isGenerating && !dateRangeInvalid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = ControlShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Navy,
                    contentColor = Color.White
                )
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Exporting ${selectedFormat.label}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "Export ${selectedFormat.label}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportSuccessCard(
    exported: ExportedReportFile,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = Color.White,
        border = BorderStroke(1.dp, GreenAccent.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(GreenAccent, CircleShape)
                    )
                    Text(
                        text = "Report exported",
                        color = Ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Dismiss",
                        color = Slate,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = SegmentShape,
                color = SubtleSurface,
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = exported.fileName,
                        style = TabularDigitsStyle,
                        color = Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    HorizontalDivider(color = DividerColor)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${exported.rowCount} rows",
                            style = TabularDigitsStyle,
                            color = Slate,
                            fontSize = 12.sp
                        )
                        Text(
                            text = exported.locationLabel,
                            color = Slate,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onOpen,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = ControlShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Navy,
                        contentColor = Color.White
                    )
                ) {
                    Text("Open file", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }

                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = ControlShape,
                    border = BorderStroke(1.dp, CardBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = Ink
                    )
                ) {
                    Text("Share", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun ReportHistoryCard(record: ReportRecord) {
    val type = ReportType.fromDb(record.reportType)
    val format = ReportFormat.fromDb(record.fileFormat)

    val dotColor = when (type) {
        ReportType.INVENTORY -> Navy
        ReportType.STOCK_MOVEMENT -> IceBlue
        ReportType.EXPIRY -> AmberAccent
        ReportType.SUPPLIER -> Slate
        ReportType.TEMPERATURE -> RedAccent
    }

    val rangeText = if (!record.startDate.isNullOrBlank() && !record.endDate.isNullOrBlank()) {
        val start = runCatching { LocalDate.parse(record.startDate).format(DisplayDateFormat) }
            .getOrElse { record.startDate }
        val end = runCatching { LocalDate.parse(record.endDate).format(DisplayDateFormat) }
            .getOrElse { record.endDate }
        "$start to $end"
    } else {
        "All dates (snapshot)"
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(dotColor, CircleShape)
                    )
                    Text(
                        text = type.shortLabel,
                        color = Slate,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Surface(
                        shape = TagShape,
                        color = TagBg
                    ) {
                        Text(
                            text = format.label,
                            color = Ink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = formatGeneratedAt(record.generatedAt),
                    style = TabularDigitsStyle,
                    color = Slate,
                    fontSize = 12.sp
                )
            }

            Text(
                text = record.reportName,
                color = Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = rangeText,
                style = TabularDigitsStyle,
                color = Slate,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PresetSegment(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 40.dp),
        shape = SegmentShape,
        color = if (selected) Navy else Color.Transparent
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                color = if (selected) Color.White else Slate,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DatePickerButton(
    label: String,
    date: LocalDate?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 52.dp),
        shape = ControlShape,
        border = BorderStroke(1.dp, CardBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Ink
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                color = Slate,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = date?.format(DisplayDateFormat) ?: "Select",
                style = TabularDigitsStyle,
                color = Ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HistoryFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = SegmentShape,
        color = if (selected) Navy else Color.White,
        border = if (selected) null else BorderStroke(1.dp, CardBorder)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Slate,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ReportErrorBanner(
    message: String,
    onRetry: (() -> Unit)?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ControlShape,
        color = Color.White,
        border = BorderStroke(1.dp, RedAccent.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(RedAccent, CircleShape)
            )
            Text(
                text = message,
                color = RedDark,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f)
            )
            if (onRetry != null) {
                TextButton(
                    onClick = onRetry,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Retry",
                        color = RedDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportEmptyCard(
    title: String,
    subtitle: String?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                color = Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = Slate,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun reportFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Navy,
    unfocusedBorderColor = CardBorder,
    focusedLabelColor = Navy,
    unfocusedLabelColor = Slate,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    cursorColor = Navy
)

private fun formatGeneratedAt(raw: String): String =
    runCatching {
        OffsetDateTime.parse(raw)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale.ENGLISH))
    }.getOrElse { raw }

private fun openExportedFile(context: Context, exported: ExportedReportFile): Boolean {
    val uri = exported.uri ?: return false
    return runCatching {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, exported.format.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}

private fun shareExportedFile(context: Context, exported: ExportedReportFile) {
    val uri = exported.uri ?: return
    runCatching {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = exported.format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, exported.fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(shareIntent, "Share ${exported.fileName}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
