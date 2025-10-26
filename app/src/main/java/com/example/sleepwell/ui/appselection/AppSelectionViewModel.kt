package com.example.sleepwell.ui.appselection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sleepwell.data.models.AppInfo
import com.example.sleepwell.data.repository.SleepWellRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppSelectionUiState(
    val apps: List<AppInfo> = emptyList(),
    val selectedApps: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val hasChanges: Boolean = false
)

class AppSelectionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SleepWellRepository(application)

    private val _uiState = MutableStateFlow(AppSelectionUiState())
    val uiState: StateFlow<AppSelectionUiState> = _uiState.asStateFlow()

    private var originalSelectedApps: Set<String> = emptySet()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            try {
                repository.alarmSettings.collect { settings ->
                    originalSelectedApps = settings.selectedApps
                    val apps = repository.getInstalledApps()
                    _uiState.value = _uiState.value.copy(
                        apps = apps,
                        selectedApps = settings.selectedApps,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun toggleAppSelection(packageName: String) {
        val currentSelected = _uiState.value.selectedApps.toMutableSet()
        if (currentSelected.contains(packageName)) {
            currentSelected.remove(packageName)
        } else {
            currentSelected.add(packageName)
        }

        _uiState.value = _uiState.value.copy(
            selectedApps = currentSelected,
            hasChanges = currentSelected != originalSelectedApps
        )
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun saveChanges() {
        viewModelScope.launch {
            repository.updateSelectedApps(_uiState.value.selectedApps)
            originalSelectedApps = _uiState.value.selectedApps
            _uiState.value = _uiState.value.copy(hasChanges = false)
        }
    }

    fun discardChanges() {
        _uiState.value = _uiState.value.copy(
            selectedApps = originalSelectedApps,
            hasChanges = false
        )
    }

    fun getFilteredApps(): List<AppInfo> {
        val apps = _uiState.value.apps
        val query = _uiState.value.searchQuery
        val selectedApps = _uiState.value.selectedApps

        return if (query.isEmpty()) {
            apps.map { app ->
                app.copy(isSelected = selectedApps.contains(app.packageName))
            }
        } else {
            apps.filter { app ->
                app.appName.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true)
            }.map { app ->
                app.copy(isSelected = selectedApps.contains(app.packageName))
            }
        }
    }
}