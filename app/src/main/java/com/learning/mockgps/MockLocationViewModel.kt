package com.learning.mockgps

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MockLocationActions(
    val onLatitudeChange: (String) -> Unit,
    val onLongitudeChange: (String) -> Unit,
    val onStartMocking: () -> Unit,
    val onStopMocking: () -> Unit,
    val onClearMessages: () -> Unit
)

class MockLocationViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application

    private val _uiState = MutableStateFlow(MockLocationUiState())
    val uiState: StateFlow<MockLocationUiState> = _uiState.asStateFlow()

    val actions = MockLocationActions(
        onLatitudeChange = ::updateLatitude,
        onLongitudeChange = ::updateLongitude,
        onStartMocking = ::startMocking,
        onStopMocking = ::stopMocking,
        onClearMessages = ::clearMessages
    )

    init {
        viewModelScope.launch {
            MockLocationService.serviceState.collect { serviceState ->
                val current = _uiState.value
                _uiState.value = current.copy(
                    isMocking = serviceState.isMocking,
                    isLoading = false,
                    errorMessage = serviceState.errorMessage ?: current.errorMessage,
                    successMessage = if (serviceState.isMocking) {
                        context.getString(
                            R.string.success_mock_active,
                            serviceState.latitude.toString(),
                            serviceState.longitude.toString()
                        )
                    } else if (!serviceState.isMocking && current.isMocking) {
                        // Transitioned from mocking to not mocking
                        context.getString(R.string.success_mock_disabled)
                    } else {
                        current.successMessage
                    },
                    // Restore coordinate fields when returning to the app with active mock
                    latitude = if (serviceState.isMocking) {
                        serviceState.latitude.toString()
                    } else {
                        current.latitude
                    },
                    longitude = if (serviceState.isMocking) {
                        serviceState.longitude.toString()
                    } else {
                        current.longitude
                    }
                )
            }
        }
    }

    fun updateLatitude(value: String) {
        _uiState.value = _uiState.value.copy(latitude = value, errorMessage = null)
    }

    fun updateLongitude(value: String) {
        _uiState.value = _uiState.value.copy(longitude = value, errorMessage = null)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    fun startMocking() {
        val lat = _uiState.value.latitude.toDoubleOrNull()
        val lng = _uiState.value.longitude.toDoubleOrNull()

        if (lat == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = context.getString(R.string.error_invalid_latitude)
            )
            return
        }

        if (lng == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = context.getString(R.string.error_invalid_longitude)
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)

        val intent = Intent(context, MockLocationService::class.java).apply {
            action = MockLocationService.ACTION_START_MOCKING
            putExtra(MockLocationService.EXTRA_LATITUDE, lat)
            putExtra(MockLocationService.EXTRA_LONGITUDE, lng)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopMocking() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)

        val intent = Intent(context, MockLocationService::class.java).apply {
            action = MockLocationService.ACTION_STOP_MOCKING
        }
        context.startService(intent)
    }

    override fun onCleared() {
        super.onCleared()
        // Do nothing — service must persist beyond ViewModel lifecycle
    }
}
