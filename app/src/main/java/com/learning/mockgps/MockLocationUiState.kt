package com.learning.mockgps

data class MockLocationUiState(
    val isMocking: Boolean = false,
    val isLoading: Boolean = false,
    val latitude: String = MockLocationConstants.DEFAULT_LATITUDE,
    val longitude: String = MockLocationConstants.DEFAULT_LONGITUDE,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showMapPicker: Boolean = false
)
