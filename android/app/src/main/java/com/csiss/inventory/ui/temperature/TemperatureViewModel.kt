package com.csiss.inventory.ui.temperature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.csiss.inventory.data.TemperatureRepository
import com.csiss.inventory.data.TemperatureSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class TemperatureUiState(
    val snapshot: TemperatureSnapshot? = null,
    val isLoading: Boolean = true,
    val isInitialLoadComplete: Boolean = false,
    val manualRefreshPending: Boolean = false,
    val error: String? = null,
    val savingLimits: Boolean = false,
    val limitSaveError: String? = null,
    val currentTime: Instant = Instant.now()
)

class TemperatureViewModel(
    private val repository: TemperatureRepository = TemperatureRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemperatureUiState())
    val uiState: StateFlow<TemperatureUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var timeUpdaterJob: Job? = null

    init {
        refreshData()
    }

    fun setPollingActive(active: Boolean) {
        if (active) {
            if (timeUpdaterJob?.isActive != true) {
                timeUpdaterJob = viewModelScope.launch {
                    while (true) {
                        delay(30_000L)
                        _uiState.update { it.copy(currentTime = Instant.now()) }
                    }
                }
            }
            if (pollingJob?.isActive != true) {
                pollingJob = viewModelScope.launch {
                    while (true) {
                        delay(20_000L)
                        val state = _uiState.value
                        if (!state.isLoading && !state.manualRefreshPending) {
                            fetchTemperatureData()
                        }
                    }
                }
            }
        } else {
            pollingJob?.cancel()
            pollingJob = null
            timeUpdaterJob?.cancel()
            timeUpdaterJob = null
        }
    }

    fun requestManualRefresh() {
        val state = _uiState.value
        if (!state.isLoading && !state.manualRefreshPending) {
            _uiState.update { it.copy(manualRefreshPending = true, isLoading = true) }
            fetchTemperatureData()
        }
    }

    private fun refreshData() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        fetchTemperatureData()
    }

    private fun fetchTemperatureData() {
        viewModelScope.launch {
            try {
                val data = repository.loadLive()
                _uiState.update {
                    it.copy(
                        snapshot = data,
                        error = null,
                        isLoading = false,
                        manualRefreshPending = false,
                        isInitialLoadComplete = true,
                        currentTime = Instant.now()
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Unable to retrieve temperature data. Check your connection and try again.",
                        isLoading = false,
                        manualRefreshPending = false
                    )
                }
            }
        }
    }

    fun updateLimits(storageId: Long, minimum: Double, maximum: Double, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(savingLimits = true, limitSaveError = null) }
            try {
                val updatedStorage = repository.updateLimits(
                    storageId = storageId,
                    minimum = minimum,
                    maximum = maximum
                )
                val refreshed = try {
                    repository.loadLive()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
                _uiState.update { currentState ->
                    val updatedSnapshot = refreshed ?: currentState.snapshot?.copy(storage = updatedStorage)
                    currentState.copy(
                        snapshot = updatedSnapshot,
                        savingLimits = false,
                        currentTime = Instant.now()
                    )
                }
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        limitSaveError = "Unable to save limits. Check your Admin login and permissions.",
                        savingLimits = false
                    )
                }
            }
        }
    }

    fun clearLimitSaveError() {
        _uiState.update { it.copy(limitSaveError = null) }
    }
}
