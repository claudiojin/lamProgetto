package com.example.progetto.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.progetto.R
import com.example.progetto.data.dao.LocationDao
import com.example.progetto.data.dao.NoteDao
import com.example.progetto.data.dao.TripDao
import com.example.progetto.data.entity.LocationPoint
import com.example.progetto.data.entity.Note
import com.example.progetto.data.entity.Trip
import com.example.progetto.ui.components.TripMapView
import com.example.progetto.utils.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import android.content.Intent
import com.example.progetto.services.LocationTrackingService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    tripId: Long,
    tripDao: TripDao,
    locationDao: LocationDao,
    noteDao: NoteDao,
    onNavigateBack: () -> Unit
) {
    var trip by remember { mutableStateOf<Trip?>(null) }
    var isRecording by remember { mutableStateOf(true) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var recordedLocations by remember { mutableStateOf<List<LocationPoint>>(emptyList()) }
    var elapsedTime by remember { mutableStateOf(0L) }
    var totalDistance by remember { mutableStateOf(0.0) }

    val context = LocalContext.current
    val locationManager = remember { LocationManager(context) }
    val scope = rememberCoroutineScope()

    var showNoteDialog by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }

    LaunchedEffect(tripId) {
        val loaded = tripDao.getTripById(tripId)
        if (loaded != null && loaded.startTimestamp == null) {
            val startTs = System.currentTimeMillis()
            tripDao.update(loaded.copy(startTimestamp = startTs))
            trip = loaded.copy(startTimestamp = startTs)
        } else {
            trip = loaded
        }


        locationDao.getLocationsByTripId(tripId).collect { locations ->
            recordedLocations = locations
            totalDistance = calculateDistance(locations)
        }
    }

    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(1000)
            elapsedTime++
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {  }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            val hasPermission = locationManager.hasLocationPermission()
            if (!hasPermission) return@LaunchedEffect
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_START
                putExtra(LocationTrackingService.EXTRA_TRIP_ID, tripId)
            }
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }
    }

    fun stopRecording() {
        scope.launch {
            isRecording = false
            val stopIntent = Intent(context, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_STOP
            }
            context.startService(stopIntent)

            trip?.let { t ->
                val now = System.currentTimeMillis()
                val firstTs = recordedLocations.firstOrNull()?.timestamp
                val lastTs = recordedLocations.lastOrNull()?.timestamp
                val startTs = (t.startTimestamp ?: firstTs) ?: now
                val endTs = (lastTs ?: now).coerceAtLeast(startTs)
                val durationSec = ((endTs - startTs) / 1000).coerceAtLeast(0)

                val updatedTrip = t.copy(
                    distance = totalDistance,
                    startTimestamp = startTs,
                    endTimestamp = endTs,
                    durationSec = durationSec
                )
                tripDao.update(updatedTrip)
            }

            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recording)) },
                navigationIcon = {
                    IconButton(onClick = {
                        stopRecording()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val hasLocationPermission = locationManager.hasLocationPermission()
            if (!hasLocationPermission) {
                PermissionRequestBlock(
                    onRequest = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
                return@Column
            }

            RecordingIndicator()

            Spacer(modifier = Modifier.height(24.dp))

            StatsCard(
                elapsedTime = elapsedTime,
                distance = totalDistance,
                speed = currentLocation?.speed ?: 0f,
                locationCount = recordedLocations.size
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (currentLocation != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.real_time_location),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(8.dp)
                        )

                        TripMapView(
                            locations = recordedLocations,
                            currentLocation = LocationPoint(
                                tripId = tripId,
                                latitude = currentLocation!!.latitude,
                                longitude = currentLocation!!.longitude,
                                altitude = currentLocation!!.altitude,
                                accuracy = currentLocation!!.accuracy,
                                speed = currentLocation!!.speed,
                                timestamp = currentLocation!!.time
                            ),
                            showRoute = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ElevatedButton(
                onClick = {
                    noteText = ""
                    showNoteDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.add_location_note))
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { stopRecording() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.stop_recording), style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    RecordingNoteDialog(
        show = showNoteDialog,
        noteText = noteText,
        onTextChange = { noteText = it },
        onDismiss = { showNoteDialog = false },
        onSave = {
            val lastPoint = recordedLocations.lastOrNull()
            val lat = lastPoint?.latitude ?: currentLocation?.latitude
            val lng = lastPoint?.longitude ?: currentLocation?.longitude
            val note = Note(
                tripId = tripId,
                locationPointId = lastPoint?.id,
                text = noteText,
                latitude = lat,
                longitude = lng
            )
            scope.launch { noteDao.insert(note) }
            showNoteDialog = false
        }
    )
}


@Composable
private fun RecordingIndicator() {
    var isVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            isVisible = !isVisible
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(12.dp),
            shape = MaterialTheme.shapes.small,
            color = if (isVisible) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant
        ) {}

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = stringResource(R.string.recording_gps_track),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun StatsCard(
    elapsedTime: Long,
    distance: Double,
    speed: Float,
    locationCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format("%.2f", distance),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.kilometers),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(
                    label = stringResource(R.string.time_duration),
                    value = formatTime(elapsedTime)
                )
                StatItem(
                    label = stringResource(R.string.speed),
                    value = String.format(stringResource(R.string.km_h), speed * 3.6)
                )
                StatItem(
                    label = stringResource(R.string.gps_points),
                    value = "$locationCount"
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}


//@Composable
//private fun CurrentLocationCard(location: Location) {
//    Card(modifier = Modifier.fillMaxWidth()) {
//        Column(modifier = Modifier.padding(16.dp)) {
//            Text(
//                text = stringResource(R.string.current_location),
//                style = MaterialTheme.typography.titleSmall,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//            Text("纬度: ${String.format("%.6f", location.latitude)}")
//            Text("经度: ${String.format("%.6f", location.longitude)}")
//            Text("精度: ${String.format("%.1f", location.accuracy)} 米")
//            Text(
//                "${SimpleDateFormat("HH:mm:ss", Locale.ITALY)
//                    .format(Date(location.time))}"
//            )
//        }
//    }
//}

private fun calculateDistance(locations: List<LocationPoint>): Double {
    if (locations.size < 2) return 0.0

    var totalDistance = 0.0
    for (i in 1 until locations.size) {
        val results = FloatArray(1)
        Location.distanceBetween(
            locations[i - 1].latitude,
            locations[i - 1].longitude,
            locations[i].latitude,
            locations[i].longitude,
            results
        )
        totalDistance += results[0]
    }

    return totalDistance / 1000.0
}

private fun formatTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, secs)
        else -> String.format("%d:%02d", minutes, secs)
    }
}


@Composable
private fun PermissionRequestBlock(onRequest: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.need_location_permission),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRequest) { Text(stringResource(R.string.grant_location_permission)) }
        }
    }
}

@Composable
private fun RecordingNoteDialog(
    show: Boolean,
    noteText: String,
    onTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_note)) },
        text = {
            OutlinedTextField(
                value = noteText,
                onValueChange = onTextChange,
                label = { Text(stringResource(R.string.content)) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = onSave) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
