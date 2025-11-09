package com.example.progetto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.progetto.data.database.TripDatabase
import com.example.progetto.ui.screens.RecordingScreen
import com.example.progetto.ui.screens.StatisticsScreen
import com.example.progetto.ui.screens.TripDetailScreen
import com.example.progetto.ui.screens.TripListScreen
import com.example.progetto.ui.theme.ProgettoTheme
import androidx.activity.compose.BackHandler
import com.example.progetto.ui.screens.GeofenceManagementScreen

class MainActivity : ComponentActivity() {

    private lateinit var database: TripDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = TripDatabase.getDatabase(this)

        enableEdgeToEdge()
        setContent {
            ProgettoTheme {
                TravelCompanionApp(database)
            }
        }
    }
}

/**
 * 应用主入口
 * 简单的导航系统
 */
@Composable
fun TravelCompanionApp(database: TripDatabase) {
    // 导航状态
    var currentScreen by remember { mutableStateOf<Screen>(Screen.TripList) }
    var selectedTripId by remember { mutableStateOf<Long?>(null) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when (val screen = currentScreen) {
            is Screen.TripList -> {
                TripListScreen(
                    tripDao = database.tripDao(),
                    modifier = Modifier.padding(innerPadding),
                    onTripClick = { tripId ->
                        selectedTripId = tripId
                        currentScreen = Screen.TripDetail
                    },
                    onNavigateToStatistics = {  // ✅ 新增
                        currentScreen = Screen.Statistics
                    },
                    onNavigateToGeofence = {  // ✅ 新增
                        currentScreen = Screen.GeofenceManagement
                    }
                )
            }

            is Screen.TripDetail -> {
                selectedTripId?.let { tripId ->
                    TripDetailScreen(
                        tripId = tripId,
                        tripDao = database.tripDao(),
                        locationDao = database.locationDao(),
                        onNavigateBack = {
                            currentScreen = Screen.TripList
                        },
                        onStartRecording = { id ->
                            selectedTripId = id
                            currentScreen = Screen.Recording
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
                        onNavigateBack = {
                            currentScreen = Screen.TripDetail
                        }
                    )
                }
            }

            is Screen.Statistics -> {  // ✅ 新增
                StatisticsScreen(
                    tripDao = database.tripDao(),
                    onNavigateBack = {
                        currentScreen = Screen.TripList
                    }
                )
            }
            is Screen.GeofenceManagement -> {  // ✅ 新增
                GeofenceManagementScreen(
                    geofenceDao = database.geofenceDao(),
                    onNavigateBack = {
                        currentScreen = Screen.TripList
                    }
                )
            }
        }
    }
}

/**
 * 屏幕导航
 */
sealed class Screen {
    object TripList : Screen()
    object TripDetail : Screen()
    object Recording : Screen()
    object Statistics : Screen()
    object GeofenceManagement : Screen()
}