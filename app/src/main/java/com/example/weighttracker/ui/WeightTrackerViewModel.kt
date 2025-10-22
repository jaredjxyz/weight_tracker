package com.example.weighttracker.ui

import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.weighttracker.data.healthconnect.HealthConnectPermissions
import com.example.weighttracker.data.healthconnect.HealthConnectAvailabilityChecker
import com.example.weighttracker.data.healthconnect.HealthConnectStatus
import com.example.weighttracker.domain.model.WeightEntry
import com.example.weighttracker.domain.model.WeightTrend
import com.example.weighttracker.domain.model.WeightUnit
import com.example.weighttracker.domain.repository.WeightRepository
import com.example.weighttracker.domain.usecase.WeightTrendCalculator
import com.example.weighttracker.health.HealthConnectPermissionManager
import java.time.Instant
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeightTrackerUiState(
    val availability: HealthConnectStatus? = null,
    val permissionsGranted: Boolean = false,
    val entries: List<WeightEntry> = emptyList(),
    val trend: WeightTrend = WeightTrend(emptyList(), emptyList()),
    val preferredUnit: WeightUnit = WeightUnit.Kilograms,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lastSync: Instant? = null
)

class WeightTrackerViewModel(
    private val repository: WeightRepository,
    private val trendCalculator: WeightTrendCalculator,
    private val availabilityChecker: HealthConnectAvailabilityChecker,
    private val permissionManager: HealthConnectPermissionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeightTrackerUiState(isLoading = true))
    val uiState: StateFlow<WeightTrackerUiState> = _uiState.asStateFlow()

    private val repositoryHandler = CoroutineExceptionHandler { _, throwable ->
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = throwable.message ?: "Unable to load data."
            )
        }
    }

    init {
        observeWeights()
        checkAvailabilityAndPermissions()
    }

    private fun observeWeights() {
        viewModelScope.launch(repositoryHandler) {
            repository.weightStream.collect { entries ->
                _uiState.update { state ->
                    state.copy(
                        entries = entries.sortedByDescending { it.recordedAt },
                        trend = trendCalculator.calculate(entries),
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
        }
    }

    private fun checkAvailabilityAndPermissions() {
        viewModelScope.launch {
            val availability = availabilityChecker.availability()
            _uiState.update { it.copy(availability = availability) }

            val hasPermissions = permissionManager.hasAllPermissions()
            _uiState.update { it.copy(permissionsGranted = hasPermissions) }

            if (hasPermissions && availability == HealthConnectStatus.INSTALLED) {
                refreshWeights()
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun refreshHealthConnectState() {
        checkAvailabilityAndPermissions()
    }

    fun refreshWeights() {
        viewModelScope.launch(repositoryHandler) {
            if (_uiState.value.isLoading.not()) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            try {
                Log.d("WeightTracker", "Refreshing weights from repository")
                repository.refresh()
                _uiState.update { it.copy(lastSync = Instant.now(), isLoading = false) }
            } catch (securityException: SecurityException) {
                Log.w("WeightTracker", "SecurityException during refresh", securityException)
                _uiState.update {
                    it.copy(
                        permissionsGranted = false,
                        isLoading = false,
                        errorMessage = securityException.message
                    )
                }
            }
        }
    }

    fun addWeight(value: Double, unit: WeightUnit, timestamp: Instant = Instant.now()) {
        viewModelScope.launch(repositoryHandler) {
            repository.addWeight(value, unit, timestamp)
            _uiState.update { it.copy(lastSync = Instant.now()) }
        }
    }

    fun deleteWeight(recordId: String) {
        viewModelScope.launch(repositoryHandler) {
            repository.deleteWeight(recordId)
            _uiState.update { it.copy(lastSync = Instant.now()) }
        }
    }

    fun onPermissionsResult(granted: Set<String>) {
        viewModelScope.launch {
            Log.d("WeightTracker", "Permissions result callback: $granted")
            val hasAll = if (granted.containsAll(HealthConnectPermissions.weightPermissions)) {
                true
            } else {
                permissionManager.hasAllPermissions()
            }

            _uiState.update {
                it.copy(
                    permissionsGranted = hasAll,
                    errorMessage = if (hasAll) null else "Health Connect access was not granted."
                )
            }

            if (hasAll) {
                Log.d("WeightTracker", "All permissions granted, refreshing weights")
                refreshWeights()
            } else {
                Log.w("WeightTracker", "Health Connect permissions still missing after request")
            }
        }
    }

    fun updatePreferredUnit(unit: WeightUnit) {
        _uiState.update { it.copy(preferredUnit = unit) }
    }

    fun permissionRequestContract(): ActivityResultContract<Set<String>, Set<String>> =
        permissionManager.permissionRequestContract()

    companion object {
        fun factory(
            repository: WeightRepository,
            trendCalculator: WeightTrendCalculator,
            availabilityChecker: HealthConnectAvailabilityChecker,
            permissionManager: HealthConnectPermissionManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WeightTrackerViewModel(
                    repository = repository,
                    trendCalculator = trendCalculator,
                    availabilityChecker = availabilityChecker,
                    permissionManager = permissionManager
                ) as T
            }
        }
    }
}
