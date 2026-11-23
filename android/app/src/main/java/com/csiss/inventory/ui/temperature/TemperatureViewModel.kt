package com.csiss.inventory.ui.temperature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.csiss.inventory.data.TemperatureRepository
import com.csiss.inventory.data.TemperatureSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

// A data class to hold the entire UI state
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

    init {
        // Start the periodic current time updater
        startTimeUpdater()
        
        // Load initial data
        refreshData()
        
        // Start periodic auto-refresh (every 20 seconds)
        startPeriodicRefresh()
    }

    private fun startTimeUpdater() {
        viewModelScope.launch {
            while (true) {
                delay(30_000L)
                _uiState.update { it.copy(currentTime = Instant.now()) }
            }
        }
    }

    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(20_000L)
                // Only auto-refresh if we aren't already loading and no manual refresh is pending
                if (!_uiState.value.isLoading && !_uiState.value.manualRefreshPending) {
                   fetchTemperatureData()
                }
            }
        }
    }

    fun requestManualRefresh() {
        if (!_uiState.value.isLoading && !_uiState.value.manualRefreshPending) {
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
                        isInitialLoadComplete = true
                    ) 
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = "Unable to retrieve temperature data. Check your connection and login.",
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
                
                // Update the snapshot with the new storage data
                _uiState.update { currentState ->
                    val updatedSnapshot = currentState.snapshot?.copy(storage = updatedStorage)
                    currentState.copy(
                        snapshot = updatedSnapshot,
                        savingLimits = false
                    )
                }
                onSuccess()
            } catch (e: Exception) {
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
