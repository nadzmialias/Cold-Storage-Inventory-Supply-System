package com.csiss.inventory.ui.report

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
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
private val IceBlueSoft = Color(0xFFE6F6FB)
private val InfoText = Color(0xFF1E7E9C)
private val Background = Color(0xFFF1F6FB)
private val Ink = Color(0xFF182B48)
private val Muted = Color(0xFF718198)
private val CardBorder = Color(0xFFE0E6EF)
private val DividerColor = Color(0xFFEDF2F7)
private val RedDark = Color(0xFFB42332)
private val RedBg = Color(0xFFFDECEF)
private val RedBorder = Color(0xFFF7B8C0)
private val GreenDark = Color(0xFF117A5C)
private val GreenBg = Color(0xFFE3F7F0)
private val GreenBorder = Color(0xFF9EE2C8)
private val AmberDark = Color(0xFF9A5504)
private val AmberBg = Color(0xFFFFF3DC)
private val NeutralBadge = Color(0xFF495A70)
private val NeutralBadgeBg = Color(0xFFE8EEF6)

private val CardShape = RoundedCornerShape(18.dp)
private val FieldShape = RoundedCornerShape(12.dp)
private val BadgeShape = RoundedCornerShape(50.dp)
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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "ANALYTICS & EXPORTS",
                        color = Muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Reports",
                        color = Ink,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Generate Report Card (Option B: clean straight into report builder)
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
                                openError = "No app found to open ${exported.format.label} files. You can view it in Downloads/CSISS."
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Report History (${filteredHistory.size})",
                        color = Ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HistoryFilterPill(
                            label = "All",
                            selected = historyFilter == "ALL",
                            onClick = { historyFilter = "ALL" }
                        )
                        ReportType.entries.forEach { type ->
                            HistoryFilterPill(
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
                        title = "Loading report history...",
                        subtitle = null
                    )
                }
            } else if (filteredHistory.isEmpty()) {
                item {
                    ReportEmptyCard(
                        title = if (historyFilter == "ALL") {
                            "No reports generated yet"
                        } else {
                            "No reports in this category"
                        },
                        subtitle = "Choose a report type above and tap Generate & Export Report."
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Generate New Report",
                color = Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            // Report Type Dropdown
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { if (!isGenerating) typeExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedType.label,
                    onValueChange = {},
                    readOnly = true,
                    enabled = !isGenerating,
                    label = { Text("Report Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    shape = FieldShape,
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
                                Column {
                                    Text(type.label, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(type.description, color = Muted, fontSize = 11.sp, maxLines = 2)
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
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            // Date Range Selector (for Stock Movement & Temperature)
            if (selectedType.supportsDateRange) {
                HorizontalDivider(color = DividerColor)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "DATE RANGE",
                        color = Muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val today = LocalDate.now()
                        val is7Days = !useAllTime && startDate == today.minusDays(7) && endDate == today
                        val is30Days = !useAllTime && startDate == today.minusDays(30) && endDate == today

                        PresetChip(
                            label = "Last 7 Days",
                            selected = is7Days,
                            enabled = !isGenerating,
                            onClick = { onSelectPreset(7) },
                            modifier = Modifier.weight(1f)
                        )
                        PresetChip(
                            label = "Last 30 Days",
                            selected = is30Days,
                            enabled = !isGenerating,
                            onClick = { onSelectPreset(30) },
                            modifier = Modifier.weight(1f)
                        )
                        PresetChip(
                            label = "All Time",
                            selected = useAllTime,
                            enabled = !isGenerating,
                            onClick = { onSelectPreset(null) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!useAllTime) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DatePickerButton(
                                label = "Start Date",
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
                                label = "End Date",
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
                                text = "Start Date must be on or before End Date.",
                                color = RedDark,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = DividerColor)

            // File Format Segmented Toggle
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "EXPORT FORMAT",
                    color = Muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = FieldShape,
                    color = Background,
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
                                shape = RoundedCornerShape(9.dp),
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
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (format == ReportFormat.PDF) "PDF Document" else "CSV Spreadsheet",
                                        color = if (selected) Color.White else Muted,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
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
                    .height(48.dp),
                shape = FieldShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Navy,
                    contentColor = Color.White
                )
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Generating ${selectedFormat.label}...", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Text("Generate & Export ${selectedFormat.label}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
        color = GreenBg,
        border = BorderStroke(1.dp, GreenBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Report Exported",
                    color = GreenDark,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Text("Dismiss", color = GreenDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Text(
                text = "${exported.fileName} (${exported.rowCount} rows)\nSaved to ${exported.locationLabel}",
                color = Ink,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpen,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = FieldShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Navy,
                        contentColor = Color.White
                    )
                ) {
                    Text("Open File", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = FieldShape,
                    border = BorderStroke(1.dp, GreenDark.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = GreenDark
                    )
                ) {
                    Text("Share", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ReportHistoryCard(record: ReportRecord) {
    val type = ReportType.fromDb(record.reportType)
    val format = ReportFormat.fromDb(record.fileFormat)

    val (typeFg, typeBg) = when (type) {
        ReportType.INVENTORY -> Navy to IceBlueSoft
        ReportType.STOCK_MOVEMENT -> InfoText to IceBlueSoft
        ReportType.EXPIRY -> AmberDark to AmberBg
        ReportType.SUPPLIER -> NeutralBadge to NeutralBadgeBg
        ReportType.TEMPERATURE -> RedDark to RedBg
    }
    val (fmtFg, fmtBg) = when (format) {
        ReportFormat.PDF -> RedDark to RedBg
        ReportFormat.CSV -> GreenDark to GreenBg
    }

    val rangeText = if (!record.startDate.isNullOrBlank() && !record.endDate.isNullOrBlank()) {
        val start = runCatching { LocalDate.parse(record.startDate).format(DisplayDateFormat) }
            .getOrElse { record.startDate }
        val end = runCatching { LocalDate.parse(record.endDate).format(DisplayDateFormat) }
            .getOrElse { record.endDate }
        "$start – $end"
    } else {
        "All dates (Snapshot)"
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
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReportBadge(text = type.shortLabel, textColor = typeFg, backgroundColor = typeBg)
                    ReportBadge(text = format.label, textColor = fmtFg, backgroundColor = fmtBg)
                }

                Text(
                    text = formatGeneratedAt(record.generatedAt),
                    color = Muted,
                    fontSize = 11.sp
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
                text = "Range: $rangeText",
                color = Muted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 36.dp),
        shape = RoundedCornerShape(9.dp),
        color = if (selected) Navy else Background,
        border = if (selected) null else BorderStroke(1.dp, CardBorder)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                color = if (selected) Color.White else Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
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
        shape = FieldShape,
        border = BorderStroke(1.dp, CardBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Ink
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(label, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = date?.format(DisplayDateFormat) ?: "Select",
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
private fun HistoryFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = BadgeShape,
        color = if (selected) Navy else Color.White,
        border = if (selected) null else BorderStroke(1.dp, CardBorder)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ReportBadge(
    text: String,
    textColor: Color,
    backgroundColor: Color
) {
    Surface(shape = BadgeShape, color = backgroundColor) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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
        shape = FieldShape,
        color = RedBg,
        border = BorderStroke(1.dp, RedBorder)
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
                color = RedDark,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f)
            )
            if (onRetry != null) {
                TextButton(onClick = onRetry) {
                    Text("Retry", color = RedDark, fontWeight = FontWeight.Bold)
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
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    color = Muted,
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
    unfocusedLabelColor = Muted,
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
