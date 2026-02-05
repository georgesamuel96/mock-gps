package com.learning.mockgps

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.learning.mockgps.ui.theme.MockGPSTheme

@Composable
fun MockGpsApp(
    uiState: MockLocationUiState,
    actions: MockLocationActions
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showPermissionRationale by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            actions.onStartMocking()
        } else {
            // Check if we should show rationale
            val activity = context as? ComponentActivity
            if (activity != null &&
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                showPermissionRationale = true
            } else {
                showSettingsDialog = true
            }
        }
    }

    // Show error messages in snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            val result = snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
            when (result) {
                SnackbarResult.Dismissed, SnackbarResult.ActionPerformed -> actions.onClearMessages()
            }
        }
    }

    // Show success messages in snackbar
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            val result = snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            when (result) {
                SnackbarResult.Dismissed, SnackbarResult.ActionPerformed -> actions.onClearMessages()
            }
        }
    }

    // Permission rationale dialog
    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text(stringResource(R.string.dialog_title_location_permission)) },
            text = {
                Text(stringResource(R.string.dialog_message_location_permission))
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionRationale = false
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }) {
                    Text(stringResource(R.string.button_grant_permission))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        )
    }

    // Settings redirect dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text(stringResource(R.string.dialog_title_permission_required)) },
            text = {
                Text(stringResource(R.string.dialog_message_permission_denied))
            },
            confirmButton = {
                TextButton(onClick = {
                    showSettingsDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text(stringResource(R.string.button_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (uiState.errorMessage != null)
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (uiState.errorMessage != null)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Status indicator
            StatusCard(isMocking = uiState.isMocking)

            Spacer(modifier = Modifier.height(24.dp))

            // Coordinate inputs
            Text(
                text = stringResource(R.string.label_coordinates),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.latitude,
                    onValueChange = { actions.onLatitudeChange(it) },
                    label = { Text(stringResource(R.string.label_latitude)) },
                    placeholder = { Text(stringResource(R.string.hint_latitude)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    enabled = !uiState.isMocking && !uiState.isLoading,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = uiState.longitude,
                    onValueChange = { actions.onLongitudeChange(it) },
                    label = { Text(stringResource(R.string.label_longitude)) },
                    placeholder = { Text(stringResource(R.string.hint_longitude)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    enabled = !uiState.isMocking && !uiState.isLoading,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            val loadingDescription = stringResource(R.string.accessibility_loading)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    enabled = !uiState.isMocking && !uiState.isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.isLoading && !uiState.isMocking) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .semantics { contentDescription = loadingDescription },
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.button_mock_now))
                }

                Button(
                    onClick = { actions.onStopMocking() },
                    enabled = uiState.isMocking && !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.isLoading && uiState.isMocking) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .semantics { contentDescription = loadingDescription },
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.button_disable_mock))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Help text
            Text(
                text = stringResource(R.string.help_developer_options),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun StatusCard(isMocking: Boolean) {
    val statusDescription = if (isMocking) {
        stringResource(R.string.status_mock_active)
    } else {
        stringResource(R.string.status_mock_inactive)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = statusDescription },
        colors = CardDefaults.cardColors(
            containerColor = if (isMocking)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = statusDescription,
                style = MaterialTheme.typography.titleMedium,
                color = if (isMocking)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
private fun MockGPSAppPreview() {
    MockGPSTheme {
        MockGpsApp(
            uiState = MockLocationUiState(),
            actions = MockLocationActions(
                onLatitudeChange = {},
                onLongitudeChange = {},
                onStartMocking = {},
                onStopMocking = {},
                onClearMessages = {}
            )
        )
    }
}
