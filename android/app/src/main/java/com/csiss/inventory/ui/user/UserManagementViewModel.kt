package com.csiss.inventory.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.csiss.inventory.data.ManagedUser
import com.csiss.inventory.data.UserDraft
import com.csiss.inventory.data.UserManagementRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserManagementUiState(
    val users: List<ManagedUser> = emptyList(),
    val isLoading: Boolean = true,
    val hasLoadedOnce: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

class UserManagementViewModel(
    private val repository: UserManagementRepository = UserManagementRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserManagementUiState())
    val uiState: StateFlow<UserManagementUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    fun refresh() {
        val state = _uiState.value
        if (!state.isLoading && !state.isSaving) {
            loadUsers()
        }
    }

    private fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val loaded = repository.loadUsers()
                _uiState.update {
                    it.copy(
                        users = loaded,
                        isLoading = false,
                        hasLoadedOnce = true,
                        error = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = friendlyError(e, "Unable to load users. Check your connection and try again.")
                    )
                }
            }
        }
    }

    fun saveUser(
        editingId: String?,
        draft: UserDraft,
        onSuccess: () -> Unit
    ) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                if (editingId == null) {
                    repository.inviteUser(draft)
                } else {
                    repository.updateUser(editingId, draft)
                }
                val refreshed = repository.loadUsers()
                _uiState.update {
                    it.copy(
                        users = refreshed,
                        isSaving = false,
                        hasLoadedOnce = true
                    )
                }
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = friendlyError(e, "Unable to save user. Please check your details and try again.")
                    )
                }
            }
        }
    }

    fun changeStatus(
        userId: String,
        newStatus: String,
        onSuccess: () -> Unit
    ) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                repository.changeStatus(userId, newStatus)
                val refreshed = repository.loadUsers()
                _uiState.update {
                    it.copy(
                        users = refreshed,
                        isSaving = false,
                        hasLoadedOnce = true
                    )
                }
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = friendlyError(e, "Unable to update account status. Please try again.")
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun friendlyError(e: Exception, fallback: String): String {
        val raw = e.message?.trim().orEmpty()
        return if (raw.isNotEmpty() && !raw.contains("http", ignoreCase = true) && !raw.contains("Exception")) {
            raw
        } else {
            fallback
        }
    }
}
