package com.learning.mockgps

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Composable
fun MapPickerScreen(
    initialLatitude: Double,
    initialLongitude: Double,
    onLocationSelected: (Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    val initialPosition = LatLng(initialLatitude, initialLongitude)
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var initialCameraSet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 10f)
    }

    // Fetch device location on open
    LaunchedEffect(Unit) {
        if (initialCameraSet) return@LaunchedEffect
        initialCameraSet = true

        if (hasLocationPermission) {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val location = getLastLocation(fusedClient)
            if (location != null) {
                val latLng = LatLng(location.latitude, location.longitude)
                userLocation = latLng
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(latLng, 15f),
                    durationMs = 600
                )
                return@LaunchedEffect
            }
        }
        // Fallback: animate to initial position
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngZoom(initialPosition, 10f),
            durationMs = 600
        )
    }

    val mapProperties = remember(hasLocationPermission) {
        MapProperties(isMyLocationEnabled = hasLocationPermission)
    }
    val mapUiSettings = remember {
        MapUiSettings(myLocationButtonEnabled = false)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings,
            onMapClick = { latLng -> selectedLocation = latLng }
        ) {
            selectedLocation?.let { location ->
                val markerState = rememberMarkerState(position = location)
                LaunchedEffect(location) {
                    markerState.position = location
                }
                Marker(
                    state = markerState,
                    title = stringResource(R.string.map_marker_title)
                )
            }
        }

        // Coordinate display chip at top
        selectedLocation?.let { location ->
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 4.dp
            ) {
                Text(
                    text = stringResource(
                        R.string.map_coordinates_display,
                        "%.6f".format(location.latitude),
                        "%.6f".format(location.longitude)
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // My Location FAB
        AnimatedVisibility(
            visible = userLocation != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    userLocation?.let { location ->
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(location, 15f),
                                durationMs = 600
                            )
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = stringResource(R.string.accessibility_my_location)
                )
            }
        }

        // Confirm button at bottom
        selectedLocation?.let { location ->
            Button(
                onClick = { onLocationSelected(location.latitude, location.longitude) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(stringResource(R.string.button_use_this_location))
            }
        }
    }
}

@SuppressLint("MissingPermission")
private suspend fun getLastLocation(
    client: com.google.android.gms.location.FusedLocationProviderClient
): Location? = suspendCancellableCoroutine { cont ->
    client.lastLocation
        .addOnSuccessListener { location -> cont.resume(location) }
        .addOnFailureListener { cont.resume(null) }
}
