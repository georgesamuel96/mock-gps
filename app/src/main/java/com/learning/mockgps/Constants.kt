package com.learning.mockgps

object MockLocationConstants {
    // Default coordinates (Tokyo)
    const val DEFAULT_LATITUDE = "35.6762"
    const val DEFAULT_LONGITUDE = "139.6503"

    // Coordinate bounds
    const val MIN_LATITUDE = -90.0
    const val MAX_LATITUDE = 90.0
    const val MIN_LONGITUDE = -180.0
    const val MAX_LONGITUDE = 180.0

    // Location properties
    const val DEFAULT_ACCURACY = 5f
    const val DEFAULT_ALTITUDE = 0.0
    const val DEFAULT_SPEED = 0f
    const val DEFAULT_BEARING = 0f

    // Settings keys
    const val MOCK_LOCATION_SETTING = "mock_location"
}
