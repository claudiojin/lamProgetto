package com.example.progetto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.progetto.R
import com.example.progetto.data.dao.LocationDao
import com.example.progetto.data.dao.TripDao
import com.example.progetto.data.entity.LocationPoint
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryMapScreen(
    tripDao: TripDao,
    locationDao: LocationDao,
    onNavigateBack: () -> Unit
) {
    val now = remember { System.currentTimeMillis() }
    var range by remember { mutableStateOf(RangeOption.MONTH) }
    var showRoutes by remember { mutableStateOf(true) }
    var selectedTripId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    val (start, end) = remember(range, now) {
        val s = range.days?.let { d -> now - d * 24L * 3600L * 1000L } ?: 0L
        s to now
    }

    val trips by tripDao.getAllTrips().collectAsState(initial = emptyList())
    val tripMap = remember(trips) { trips.associateBy { it.id } }
    val locations by locationDao.getLocationsInRange(start, end).collectAsState(initial = emptyList())

    val groups = remember(locations) { locations.groupBy { it.tripId } }
    val allPoints = remember(locations) { locations.map { LatLng(it.latitude, it.longitude) } }
    val centerLatLng = remember(allPoints) { allPoints.lastOrNull() ?: LatLng(39.9042, 116.4074) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(centerLatLng, 12f)
    }

    LaunchedEffect(allPoints) {
        if (selectedTripId != null) return@LaunchedEffect
        if (allPoints.isNotEmpty()) {
            try {
                if (allPoints.size >= 2) {
                    val builder = LatLngBounds.Builder()
                    allPoints.forEach { builder.include(it) }
                    val bounds = builder.build()
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(bounds, 80),
                        durationMs = 800
                    )
                } else {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(allPoints.last(), 13f),
                        durationMs = 600
                    )
                }
            } catch (_: Exception) {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(allPoints.last(), 13f)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_map)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RangeOption.values().forEach { opt ->
                        FilterChip(
                            selected = range == opt,
                            onClick = { range = opt },
                            label = { Text(stringResource(opt.labelResId)) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.show_routes), style = MaterialTheme.typography.bodySmall)
                    Switch(checked = showRoutes, onCheckedChange = { showRoutes = it })
                }
            }

            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(mapType = MapType.NORMAL),
                uiSettings = MapUiSettings(zoomControlsEnabled = true)
            ) {
                val palette = remember {
                    listOf(
                        Color(0xFFE91E63), Color(0xFF3F51B5), Color(0xFF009688),
                        Color(0xFFFF5722), Color(0xFF795548), Color(0xFF607D8B),
                        Color(0xFF8BC34A), Color(0xFFFFC107)
                    )
                }

                groups.forEach { (tripId, points) ->
                    val baseColor = palette[(tripId % palette.size).toInt().coerceAtLeast(0)]
                    val isSelected = selectedTripId == tripId
                    val color = if (isSelected) baseColor else baseColor.copy(alpha = 0.6f)

                    if (showRoutes && points.size >= 2) {
                        Polyline(
                            points = points.map { LatLng(it.latitude, it.longitude) },
                            color = color,
                            width = if (isSelected) 10f else 6f,
                            geodesic = true
                        )
                    }
                    if (!showRoutes) {
                        points.forEach { p ->
                            Circle(
                                center = LatLng(p.latitude, p.longitude),
                                radius = 6.0,
                                fillColor = color.copy(alpha = if (isSelected) 0.45f else 0.3f),
                                strokeColor = color,
                                strokeWidth = if (isSelected) 1.5f else 1f
                            )
                        }
                    }
                }
            }

            if (groups.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val palette = listOf(
                        Color(0xFFE91E63), Color(0xFF3F51B5), Color(0xFF009688),
                        Color(0xFFFF5722), Color(0xFF795548), Color(0xFF607D8B),
                        Color(0xFF8BC34A), Color(0xFFFFC107)
                    )
                    groups.keys.forEach { tripId ->
                        val color = palette[(tripId % palette.size).toInt().coerceAtLeast(0)]
                        val selected = selectedTripId == tripId
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedTripId = if (selected) null else tripId
                                val points = groups[tripId].orEmpty()
                                if (points.isNotEmpty()) {
                                    scope.launch {
                                        try {
                                            if (points.size >= 2) {
                                                val builder = LatLngBounds.Builder()
                                                points.forEach { builder.include(LatLng(it.latitude, it.longitude)) }
                                                val bounds = builder.build()
                                                cameraPositionState.animate(
                                                    CameraUpdateFactory.newLatLngBounds(bounds, 80),
                                                    durationMs = 800
                                                )
                                            } else {
                                                cameraPositionState.animate(
                                                    CameraUpdateFactory.newLatLngZoom(LatLng(points.last().latitude, points.last().longitude), 13f),
                                                    durationMs = 600
                                                )
                                            }
                                        } catch (_: Exception) {
                                            // ignore
                                        }
                                    }
                                }
                            },
                            label = { Text(tripMap[tripId]?.destination ?: "Trip $tripId") },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(color, shape = MaterialTheme.shapes.small)
                                )
                            }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private enum class RangeOption(val labelResId: Int, val days: Int?) {
    WEEK(R.string.last_7_days, 7), MONTH(R.string.last_30_days, 30), ALL(R.string.all, null)
}
