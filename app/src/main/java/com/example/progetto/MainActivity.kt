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
            println("✅ 通知权限已授予")
        } else {
            println("❌ 通知权限被拒绝")
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
                    println("✅ 已有通知权限")
                }
                else -> {
                    println("🔔 请求通知权限")
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
    data class PhotoGallery(val tripId: Long, val tripName: String) : Screen()  // ✅ 新增
    data class NotesEditor(val tripId: Long) : Screen()  // ✅ 新增
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
            is Screen.PhotoGallery -> Screen.TripDetail  // ✅ 新增
            is Screen.NotesEditor -> Screen.TripDetail  // ✅ 新增
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
                        label = { Text("旅行") }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.HistoryMap,
                        onClick = { currentScreen = Screen.HistoryMap },
                        icon = { Icon(Icons.Filled.Map, contentDescription = null) },
                        label = { Text("历史地图") }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.GeofenceManagement,
                        onClick = { currentScreen = Screen.GeofenceManagement },
                        icon = { Icon(Icons.Filled.Place, contentDescription = null) },
                        label = { Text("围栏") }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.Statistics,
                        onClick = { currentScreen = Screen.Statistics },
                        icon = { Icon(Icons.Filled.BarChart, contentDescription = null) },
                        label = { Text("统计") }
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
                        photoDao = database.photoDao(),  // ✅ 新增
                        noteDao = database.noteDao(),
                        onNavigateBack = {
                            currentScreen = Screen.TripList
                        },
                        onStartRecording = { id ->
                            selectedTripId = id
                            currentScreen = Screen.Recording
                        },
                        onNavigateToPhotos = { id, name ->  // ✅ 新增
                            currentScreen = Screen.PhotoGallery(id, name)
                        },
                        onNavigateToNotes = { id ->  // ✅ 新增
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
            is Screen.PhotoGallery -> {  // ✅ 新增
                PhotoGalleryScreen(
                    tripId = screen.tripId,
                    tripName = screen.tripName,
                    photoDao = database.photoDao(),
                    onNavigateBack = {
                        currentScreen = Screen.TripDetail
                    }
                )
            }
            is Screen.NotesEditor -> {  // ✅ 新增
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
