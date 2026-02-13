package com.learning.mockgps

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MockServiceState(
    val isMocking: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val errorMessage: String? = null
)

class MockLocationService : Service() {

    private var mockLocationProvider: MockLocationProvider? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var mockJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MOCKING -> {
                val lat = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
                val lng = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)
                startMocking(lat, lng)
            }
            ACTION_STOP_MOCKING -> {
                stopMocking()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Do NOT stop — the whole point is to survive app removal from recents
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanUp()
        serviceScope.cancel()
    }

    private fun startMocking(latitude: Double, longitude: Double) {
        val provider = MockLocationProvider(this)
        val startResult = provider.start()

        if (startResult.isFailure) {
            val errorMsg = startResult.exceptionOrNull()?.message
                ?: getString(R.string.error_start_provider)
            _serviceState.value = MockServiceState(errorMessage = errorMsg)
            stopSelf()
            return
        }

        val locationResult = provider.setLocation(latitude, longitude)
        if (locationResult.isFailure) {
            provider.stop()
            val errorMsg = locationResult.exceptionOrNull()?.message
                ?: getString(R.string.error_set_location)
            _serviceState.value = MockServiceState(errorMessage = errorMsg)
            stopSelf()
            return
        }

        mockLocationProvider = provider

        val notification = buildNotification(latitude, longitude)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        _serviceState.value = MockServiceState(
            isMocking = true,
            latitude = latitude,
            longitude = longitude
        )

        // Re-set the mock location periodically so Android doesn't discard stale locations
        mockJob?.cancel()
        mockJob = serviceScope.launch {
            while (true) {
                delay(LOCATION_REFRESH_INTERVAL_MS)
                val result = provider.setLocation(latitude, longitude)
                if (result.isFailure) {
                    Log.w(TAG, "Failed to refresh mock location", result.exceptionOrNull())
                }
            }
        }
    }

    private fun stopMocking() {
        cleanUp()
        _serviceState.value = MockServiceState(isMocking = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanUp() {
        mockJob?.cancel()
        mockJob = null
        mockLocationProvider?.stop()
        mockLocationProvider = null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(latitude: Double, longitude: Double): Notification {
        // Tap notification -> open app
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Stop action button
        val stopIntent = Intent(this, MockLocationService::class.java).apply {
            action = ACTION_STOP_MOCKING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text, latitude.toString(), longitude.toString()))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(0, getString(R.string.notification_action_stop), stopPendingIntent)
            .build()
    }

    companion object {
        private const val TAG = "MockLocationService"
        private const val CHANNEL_ID = "mock_location_channel"
        private const val NOTIFICATION_ID = 1
        private const val LOCATION_REFRESH_INTERVAL_MS = 2000L

        const val ACTION_START_MOCKING = "com.learning.mockgps.ACTION_START_MOCKING"
        const val ACTION_STOP_MOCKING = "com.learning.mockgps.ACTION_STOP_MOCKING"
        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"

        private val _serviceState = MutableStateFlow(MockServiceState())
        val serviceState: StateFlow<MockServiceState> = _serviceState.asStateFlow()
    }
}
