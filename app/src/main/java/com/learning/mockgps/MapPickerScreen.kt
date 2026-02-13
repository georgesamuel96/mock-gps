package com.learning.mockgps

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

@Composable
fun MapPickerScreen(
    initialLatitude: Double,
    initialLongitude: Double,
    onLocationSelected: (Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val initialPosition = LatLng(initialLatitude, initialLongitude)
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 10f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
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
