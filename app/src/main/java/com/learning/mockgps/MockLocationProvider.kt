package com.learning.mockgps

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.Log

sealed class MockLocationError {
    data object MockLocationAppNotSelected : MockLocationError()
    data object DeveloperOptionsDisabled : MockLocationError()
    data object SecurityException : MockLocationError()
    data class InvalidCoordinates(val message: String) : MockLocationError()
    data class Unknown(val message: String) : MockLocationError()
}

class MockLocationProvider(
    private val context: Context,
    private val providerName: String = LocationManager.GPS_PROVIDER
) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /**
     * Checks if Developer Options are enabled on the device.
     */
    fun isDeveloperOptionsEnabled(): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
            ) == 1
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check Developer Options status", e)
            false
        }
    }

    /**
     * Checks if this app is selected as the mock location app in Developer Options.
     * Note: This is a best-effort check. The actual test will be when we try to add the provider.
     */
    fun isMockLocationAppSelected(): Boolean {
        return try {
            // On Android 6.0+, we can't directly check which app is selected as mock location provider
            // The most reliable check is to try adding a test provider
            // But we can attempt to check via Settings if available
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Try to check if we have the permission granted (requires being set as mock location app)
                val mockLocationApp = Settings.Secure.getString(
                    context.contentResolver,
                    MockLocationConstants.MOCK_LOCATION_SETTING
                )
                // If mockLocationApp is null or "0", mock locations might not be enabled
                // However, this isn't always reliable, so we do a test provider check
                mockLocationApp == "1" || canAddTestProvider()
            } else {
                @Suppress("DEPRECATION")
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.ALLOW_MOCK_LOCATION,
                    0
                ) == 1
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check mock location app status", e)
            false
        }
    }

    /**
     * Attempts to add and immediately remove a test provider to check if we have permission.
     */
    private fun canAddTestProvider(): Boolean {
        return try {
            val testProviderName = "test_check_provider"
            locationManager.addTestProvider(
                testProviderName,
                false, false, false, false, true, true, true,
                ProviderProperties.POWER_USAGE_LOW,
                ProviderProperties.ACCURACY_FINE
            )
            locationManager.removeTestProvider(testProviderName)
            true
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Starts the mock location provider.
     * @return Result.success if started successfully, Result.failure with MockLocationError otherwise.
     */
    fun start(): Result<Unit> {
        if (!isDeveloperOptionsEnabled()) {
            return Result.failure(MockLocationException(MockLocationError.DeveloperOptionsDisabled))
        }

        return try {
            locationManager.addTestProvider(
                providerName,
                false, // requiresNetwork
                false, // requiresSatellite
                false, // requiresCell
                false, // hasMonetaryCost
                true,  // supportsAltitude
                true,  // supportsSpeed
                true,  // supportsBearing
                ProviderProperties.POWER_USAGE_LOW,
                ProviderProperties.ACCURACY_FINE
            )
            locationManager.setTestProviderEnabled(providerName, true)
            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: App not selected as mock location app", e)
            Result.failure(MockLocationException(MockLocationError.MockLocationAppNotSelected))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start mock location provider", e)
            Result.failure(MockLocationException(MockLocationError.Unknown(e.message ?: "Unknown error")))
        }
    }

    /**
     * Sets a mock location with the given coordinates.
     * @param latitude Latitude in degrees (-90.0 to 90.0)
     * @param longitude Longitude in degrees (-180.0 to 180.0)
     * @return Result.success if location was set, Result.failure with error details otherwise.
     */
    fun setLocation(latitude: Double, longitude: Double): Result<Unit> {
        // Validate coordinates
        if (latitude !in MockLocationConstants.MIN_LATITUDE..MockLocationConstants.MAX_LATITUDE) {
            val error = MockLocationError.InvalidCoordinates(
                "Latitude must be between -90.0 and 90.0, got: $latitude"
            )
            Log.e(TAG, "Invalid latitude: $latitude")
            return Result.failure(MockLocationException(error))
        }

        if (longitude !in MockLocationConstants.MIN_LONGITUDE..MockLocationConstants.MAX_LONGITUDE) {
            val error = MockLocationError.InvalidCoordinates(
                "Longitude must be between -180.0 and 180.0, got: $longitude"
            )
            Log.e(TAG, "Invalid longitude: $longitude")
            return Result.failure(MockLocationException(error))
        }

        val location = Location(providerName).apply {
            this.latitude = latitude
            this.longitude = longitude
            this.accuracy = MockLocationConstants.DEFAULT_ACCURACY
            this.altitude = MockLocationConstants.DEFAULT_ALTITUDE
            this.speed = MockLocationConstants.DEFAULT_SPEED
            this.bearing = MockLocationConstants.DEFAULT_BEARING
            this.time = System.currentTimeMillis()
            this.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }

        return try {
            locationManager.setTestProviderLocation(providerName, location)
            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while setting location", e)
            Result.failure(MockLocationException(MockLocationError.MockLocationAppNotSelected))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set mock location", e)
            Result.failure(MockLocationException(MockLocationError.Unknown(e.message ?: "Unknown error")))
        }
    }

    /**
     * Stops the mock location provider and removes it.
     * @return Result.success if stopped successfully, Result.failure otherwise.
     */
    fun stop(): Result<Unit> {
        return try {
            locationManager.setTestProviderEnabled(providerName, false)
            locationManager.removeTestProvider(providerName)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop mock location provider", e)
            Result.failure(MockLocationException(MockLocationError.Unknown(e.message ?: "Unknown error")))
        }
    }

    companion object {
        private const val TAG = "MockLocationProvider"
    }
}

/**
 * Exception wrapper for MockLocationError to use with Result.
 */
class MockLocationException(val error: MockLocationError) : Exception() {
    override val message: String
        get() = when (error) {
            is MockLocationError.MockLocationAppNotSelected ->
                "This app is not selected as the mock location app. Go to Developer Options → Select mock location app → Choose this app."
            is MockLocationError.DeveloperOptionsDisabled ->
                "Developer Options are not enabled. Go to Settings → About Phone → Tap Build Number 7 times."
            is MockLocationError.SecurityException ->
                "Security exception occurred. Please check app permissions."
            is MockLocationError.InvalidCoordinates ->
                error.message
            is MockLocationError.Unknown ->
                error.message
        }
}
