package com.csiss.inventory.ui.report

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.csiss.inventory.data.ReportFormat
import com.csiss.inventory.data.ReportRecord
import com.csiss.inventory.data.ReportRepository
import com.csiss.inventory.data.ReportType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ReportUiState(
    val history: List<ReportRecord> = emptyList(),
    val isLoading: Boolean = true,
    val hasLoadedOnce: Boolean = false,
    val isGenerating: Boolean = false,
    val error: String? = null,
    val lastExport: ExportedReportFile? = null
)

class ReportViewModel(
    private val repository: ReportRepository = ReportRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun refresh() {
        val state = _uiState.value
        if (!state.isLoading && !state.isGenerating) {
            loadHistory()
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val records = repository.loadReportHistory()
                _uiState.update {
                    it.copy(
                        history = records,
                        isLoading = false,
                        hasLoadedOnce = true,
                        error = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Unable to load report history. Check your connection and try again."
                    )
                }
            }
        }
    }

    fun generateReport(
        context: Context,
        type: ReportType,
        format: ReportFormat,
        startDate: LocalDate?,
        endDate: LocalDate?
    ) {
        if (_uiState.value.isGenerating) return

        val effectiveStart = if (type.supportsDateRange) startDate else null
        val effectiveEnd = if (type.supportsDateRange) endDate else null

        if ((effectiveStart == null) != (effectiveEnd == null)) {
            _uiState.update { it.copy(error = "Select both Start Date and End Date, or choose All Time.") }
            return
        }
        if (effectiveStart != null && effectiveEnd != null && effectiveStart.isAfter(effectiveEnd)) {
            _uiState.update { it.copy(error = "Start Date must be on or before End Date.") }
            return
        }

        val appContext = context.applicationContext
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null, lastExport = null) }
            try {
                val reportData = repository.buildReportData(
                    type = type,
                    startDate = effectiveStart,
                    endDate = effectiveEnd
                )

                val exportedFile = ReportExporter.export(
                    context = appContext,
                    data = reportData,
                    format = format
                )

                val savedRecord = try {
                    repository.saveReportRecord(
                        reportName = reportData.reportName,
                        type = type,
                        startDate = effectiveStart,
                        endDate = effectiveEnd,
                        format = format
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }

                val updatedHistory = if (savedRecord != null) {
                    listOf(savedRecord) + _uiState.value.history.filterNot { it.reportId == savedRecord.reportId }
                } else {
                    _uiState.value.history
                }

                _uiState.update {
                    it.copy(
                        history = updatedHistory,
                        hasLoadedOnce = true,
                        isGenerating = false,
                        lastExport = exportedFile,
                        error = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        error = "Unable to generate report. Please check your connection and try again."
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearLastExport() {
        _uiState.update { it.copy(lastExport = null) }
    }
}
