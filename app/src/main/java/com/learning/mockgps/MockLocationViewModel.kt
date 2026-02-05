package com.learning.mockgps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.learning.mockgps.util.getErrorMessage
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
    private val mockLocationProvider = MockLocationProvider(application)

    private val _uiState = MutableStateFlow(MockLocationUiState())
    val uiState: StateFlow<MockLocationUiState> = _uiState.asStateFlow()

    val actions = MockLocationActions(
        onLatitudeChange = ::updateLatitude,
        onLongitudeChange = ::updateLongitude,
        onStartMocking = ::startMocking,
        onStopMocking = ::stopMocking,
        onClearMessages = ::clearMessages
    )

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
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)

            // Parse coordinates
            val lat = _uiState.value.latitude.toDoubleOrNull()
            val lng = _uiState.value.longitude.toDoubleOrNull()

            if (lat == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = context.getString(R.string.error_invalid_latitude)
                )
                return@launch
            }

            if (lng == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = context.getString(R.string.error_invalid_longitude)
                )
                return@launch
            }

            // Start the mock provider
            val startResult = mockLocationProvider.start()
            if (startResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = startResult.getErrorMessage(context.getString(R.string.error_start_provider))
                )
                return@launch
            }

            // Set the location
            val locationResult = mockLocationProvider.setLocation(lat, lng)
            if (locationResult.isFailure) {
                mockLocationProvider.stop()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = locationResult.getErrorMessage(context.getString(R.string.error_set_location))
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isMocking = true,
                successMessage = context.getString(R.string.success_mock_active, lat.toString(), lng.toString())
            )
        }
    }

    fun stopMocking() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)

            val result = mockLocationProvider.stop()
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.getErrorMessage(context.getString(R.string.error_stop_provider))
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isMocking = false,
                successMessage = context.getString(R.string.success_mock_disabled)
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up when ViewModel is destroyed
        if (_uiState.value.isMocking) {
            mockLocationProvider.stop()
        }
    }
}
