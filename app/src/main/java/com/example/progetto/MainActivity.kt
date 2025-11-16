package com.example.progetto

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.example.progetto.data.database.TripDatabase
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.BarChart
import com.example.progetto.ui.screens.*
import com.example.progetto.ui.theme.ProgettoTheme

class MainActivity : ComponentActivity() {

    private lateinit var database: TripDatabase

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            println("✅ Permesso di notifica ottenuto")
        } else {
            println("❌ Permesso di notifica non ottenuto")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = TripDatabase.getDatabase(this)

        requestNotificationPermission()

        enableEdgeToEdge()
        setContent {
            ProgettoTheme {
                TravelCompanionApp(database)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    println("✅ Permesso di notifica ottenuto")
                }
                else -> {
                    println("🔔 Richiesta di permesso di notifica")
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}

sealed class Screen {
    object TripList : Screen()
    object TripDetail : Screen()
    object Recording : Screen()
    object Statistics : Screen()
    object GeofenceManagement : Screen()
    object HistoryMap : Screen()
    data class PhotoGallery(val tripId: Long, val tripName: String) : Screen()
    data class NotesEditor(val tripId: Long) : Screen()
}

@Composable
fun TravelCompanionApp(database: TripDatabase) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.TripList) }
    var selectedTripId by remember { mutableStateOf<Long?>(null) }

    BackHandler(enabled = currentScreen != Screen.TripList) {
        currentScreen = when (currentScreen) {
            Screen.Statistics -> Screen.TripList
            Screen.TripDetail -> Screen.TripList
            Screen.Recording -> Screen.TripDetail
            Screen.GeofenceManagement -> Screen.TripList
            Screen.HistoryMap -> Screen.TripList
            is Screen.PhotoGallery -> Screen.TripDetail
            is Screen.NotesEditor -> Screen.TripDetail
            else -> Screen.TripList
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            val showBottomBar = when (currentScreen) {
                Screen.TripList, Screen.Statistics, Screen.GeofenceManagement, Screen.HistoryMap -> true
                else -> false
            }
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentScreen == Screen.TripList,
                        onClick = { currentScreen = Screen.TripList },
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_trips)) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.HistoryMap,
                        onClick = { currentScreen = Screen.HistoryMap },
                        icon = { Icon(Icons.Filled.Map, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_history_map)) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.GeofenceManagement,
                        onClick = { currentScreen = Screen.GeofenceManagement },
                        icon = { Icon(Icons.Filled.Place, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_geofence)) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.Statistics,
                        onClick = { currentScreen = Screen.Statistics },
                        icon = { Icon(Icons.Filled.BarChart, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_statistics)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (val screen = currentScreen) {
            is Screen.TripList -> {
                TripListScreen(
                    tripDao = database.tripDao(),
                    modifier = Modifier.padding(innerPadding),
                    onTripClick = { tripId ->
                        selectedTripId = tripId
                        currentScreen = Screen.TripDetail
                    }
                )
            }
            is Screen.TripDetail -> {
                selectedTripId?.let { tripId ->
                    TripDetailScreen(
                        tripId = tripId,
                        tripDao = database.tripDao(),
                        locationDao = database.locationDao(),
                        photoDao = database.photoDao(),
                        noteDao = database.noteDao(),
                        onNavigateBack = {
                            currentScreen = Screen.TripList
                        },
                        onStartRecording = { _ ->
                            currentScreen = Screen.Recording
                        },
                        onNavigateToPhotos = { id, name ->
                            currentScreen = Screen.PhotoGallery(id, name)
                        },
                        onNavigateToNotes = { id ->
                            currentScreen = Screen.NotesEditor(id)
                        }
                    )
                }
            }
            is Screen.Recording -> {
                selectedTripId?.let { tripId ->
                    RecordingScreen(
                        tripId = tripId,
                        tripDao = database.tripDao(),
                        locationDao = database.locationDao(),
                        noteDao = database.noteDao(),
                        onNavigateBack = {
                            currentScreen = Screen.TripDetail
                        }
                    )
                }
            }
            is Screen.Statistics -> {
                Box(modifier = Modifier.padding(innerPadding)) {
                    StatisticsScreen(
                        tripDao = database.tripDao(),
                        onNavigateBack = { currentScreen = Screen.TripList }
                    )
                }
            }
            is Screen.GeofenceManagement -> {
                Box(modifier = Modifier.padding(innerPadding)) {
                    GeofenceManagementScreen(
                        geofenceDao = database.geofenceDao(),
                        onNavigateBack = { currentScreen = Screen.TripList }
                    )
                }
            }
            is Screen.HistoryMap -> {
                Box(modifier = Modifier.padding(innerPadding)) {
                    HistoryMapScreen(
                        tripDao = database.tripDao(),
                        locationDao = database.locationDao(),
                        onNavigateBack = { currentScreen = Screen.TripList }
                    )
                }
            }
            is Screen.PhotoGallery -> {
                PhotoGalleryScreen(
                    tripId = screen.tripId,
                    tripName = screen.tripName,
                    photoDao = database.photoDao(),
                    onNavigateBack = {
                        currentScreen = Screen.TripDetail
                    }
                )
            }
            is Screen.NotesEditor -> {
                NotesEditorScreen(
                    tripId = screen.tripId,
                    tripDao = database.tripDao(),
                    onNavigateBack = {
                        currentScreen = Screen.TripDetail
                    }
                )
            }
        }
    }
}
