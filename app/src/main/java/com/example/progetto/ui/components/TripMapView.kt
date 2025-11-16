package com.example.progetto.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.progetto.data.entity.LocationPoint
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun TripMapView(
    locations: List<LocationPoint>,
    currentLocation: LocationPoint? = null,
    showRoute: Boolean = true,
    modifier: Modifier = Modifier,
    notePoints: List<Pair<Double, Double>> = emptyList()
) {
    val defaultPosition = LatLng(9.18646, 45.47825 )

    val centerPosition = remember(locations, currentLocation) {
        when {
            currentLocation != null -> LatLng(currentLocation.latitude, currentLocation.longitude)
            locations.isNotEmpty() -> LatLng(locations.last().latitude, locations.last().longitude)
            else -> defaultPosition
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(centerPosition, 15f)
    }

    LaunchedEffect(centerPosition) {
        cameraPositionState.animate(
            update = com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                centerPosition,
                15f
            ),
            durationMs = 1000
        )
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = false,  // 我们自己绘制位置
            mapType = MapType.NORMAL
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true,
            myLocationButtonEnabled = false,
            compassEnabled = true
        )
    ) {
        if (showRoute && locations.size >= 2) {
            Polyline(
                points = locations.map { LatLng(it.latitude, it.longitude) },
                color = Color(0xFF2196F3),  // 蓝色
                width = 10f,
                geodesic = true
            )
        }

        if (locations.isNotEmpty()) {
            val startPoint = locations.first()
            Marker(
                state = MarkerState(
                    position = LatLng(startPoint.latitude, startPoint.longitude)
                ),
                title = "Partenza",
                snippet = formatTimestamp(startPoint.timestamp),
                icon = com.google.android.gms.maps.model.BitmapDescriptorFactory
                    .defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN)
            )
        }

        if (currentLocation != null) {
            Marker(
                state = MarkerState(
                    position = LatLng(currentLocation.latitude, currentLocation.longitude)
                ),
                title = "Posizione corrente",
                icon = com.google.android.gms.maps.model.BitmapDescriptorFactory
                    .defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE)
            )
        } else if (locations.size >= 2) {
            val endPoint = locations.last()
            Marker(
                state = MarkerState(
                    position = LatLng(endPoint.latitude, endPoint.longitude)
                ),
                title = "Destinazione",
                snippet = formatTimestamp(endPoint.timestamp),
                icon = com.google.android.gms.maps.model.BitmapDescriptorFactory
                    .defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED)
            )
        }

        if (locations.size in 2..50) {
            locations.forEach { point ->
                Circle(
                    center = LatLng(point.latitude, point.longitude),
                    radius = 5.0,
                    fillColor = Color(0x442196F3),
                    strokeColor = Color(0xFF2196F3),
                    strokeWidth = 2f
                )
            }
        }

        notePoints.forEach { (lat, lng) ->
            Marker(
                state = MarkerState(position = LatLng(lat, lng)),
                title = "Foto/Note",
                snippet = "Note in questo momento",
                icon = com.google.android.gms.maps.model.BitmapDescriptorFactory
                    .defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_YELLOW)
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.ITALY)
    return sdf.format(java.util.Date(timestamp))
}
