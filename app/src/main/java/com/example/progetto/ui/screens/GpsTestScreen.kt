package com.example.progetto.ui.screens

import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.progetto.utils.LocationManager
import com.example.progetto.utils.PermissionsHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GpsTestScreen() {
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var isTracking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val locationManager = remember { LocationManager(context) }
    val permissionState = PermissionsHelper.rememberLocationPermissionState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "GPS test",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when {
            permissionState.allPermissionsGranted -> {
                PermissionGrantedContent(
                    currentLocation = currentLocation,
                    isTracking = isTracking,
                    errorMessage = errorMessage,
                    onGetLocation = {
                        scope.launch {
                            val location = locationManager.getLastLocation()
                            if (location != null) {
                                currentLocation = location
                                errorMessage = null
                            } else {
                                errorMessage = "Controlla i permessi e riprova"
                            }
                        }
                    },
                    onToggleTracking = {
                        isTracking = !isTracking
                        if (isTracking) {
                            scope.launch {
                                locationManager.getLocationUpdates(2000).collect { location ->
                                    currentLocation = location
                                    errorMessage = null
                                }
                            }
                        }
                    }
                )
            }
            else -> {
                PermissionDeniedContent(
                    onRequestPermission = {
                        permissionState.launchMultiplePermissionRequest()
                    }
                )
            }
        }
    }
}


@Composable
private fun PermissionGrantedContent(
    currentLocation: Location?,
    isTracking: Boolean,
    errorMessage: String?,
    onGetLocation: () -> Unit,
    onToggleTracking: () -> Unit
) {
    Text(
        text = "✅ Gps permesso",
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    if (currentLocation != null) {
        LocationInfoCard(location = currentLocation)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onGetLocation,
            modifier = Modifier.weight(1f),
            enabled = !isTracking
        ) {
            Text("Ottieni posisione")
        }

        Button(
            onClick = onToggleTracking,
            modifier = Modifier.weight(1f),
            colors = if (isTracking) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            } else {
                ButtonDefaults.buttonColors()
            }
        ) {
            Text(if (isTracking) "停止追踪" else "开始追踪")
        }
    }


    if (errorMessage != null) {
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp)
        )
    }


    if (isTracking) {
        Text(
            text = "🔴 In monitoraggio (ogni 2 secondi)",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}


@Composable
private fun LocationInfoCard(location: Location) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Posizione locale",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text("latitudine: ${location.latitude}")
            Text("longitudine: ${location.longitude}")
            Text("precisione: ${location.accuracy} metri")
            Text("altitudine: ${location.altitude} metri")
            Text("velocità: ${location.speed} m/s")
            Text(
                "tempo: ${
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        .format(Date(location.time))
                }"
            )
        }
    }
}


@Composable
private fun PermissionDeniedContent(
    onRequestPermission: () -> Unit
) {
    Text(
        text = "❌ Serve permesso di GPS",
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    Text(
        text = "Questa app richiede accesso alla posizione",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    Button(
        onClick = onRequestPermission,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Richiesta permesso")
    }
}