package com.example.progetto.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.progetto.data.entity.LocationPoint
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

/**
 * 地图组件 - 显示GPS轨迹
 *
 * @param locations GPS点列表
 * @param currentLocation 当前位置（用于实时追踪）
 * @param showRoute 是否显示完整路线
 */
@Composable
fun TripMapView(
    locations: List<LocationPoint>,
    currentLocation: LocationPoint? = null,
    showRoute: Boolean = true,
    modifier: Modifier = Modifier,
    notePoints: List<Pair<Double, Double>> = emptyList()
) {
    // 如果没有位置数据，显示默认位置（北京）
    val defaultPosition = LatLng(39.9042, 116.4074)

    // 计算地图中心点
    val centerPosition = remember(locations, currentLocation) {
        when {
            currentLocation != null -> LatLng(currentLocation.latitude, currentLocation.longitude)
            locations.isNotEmpty() -> LatLng(locations.last().latitude, locations.last().longitude)
            else -> defaultPosition
        }
    }

    // 相机位置
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(centerPosition, 15f)
    }

    // 当位置更新时，移动相机
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
        // 1. 绘制路线（如果有多个点）
        if (showRoute && locations.size >= 2) {
            Polyline(
                points = locations.map { LatLng(it.latitude, it.longitude) },
                color = Color(0xFF2196F3),  // 蓝色
                width = 10f,
                geodesic = true  // 大地测量线（考虑地球曲率）
            )
        }

        // 2. 起点标记
        if (locations.isNotEmpty()) {
            val startPoint = locations.first()
            Marker(
                state = MarkerState(
                    position = LatLng(startPoint.latitude, startPoint.longitude)
                ),
                title = "起点",
                snippet = formatTimestamp(startPoint.timestamp),
                icon = com.google.android.gms.maps.model.BitmapDescriptorFactory
                    .defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN)
            )
        }

        // 3. 终点/当前位置标记
        if (currentLocation != null) {
            // 记录中 - 显示当前位置
            Marker(
                state = MarkerState(
                    position = LatLng(currentLocation.latitude, currentLocation.longitude)
                ),
                title = "当前位置",
                icon = com.google.android.gms.maps.model.BitmapDescriptorFactory
                    .defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE)
            )
        } else if (locations.size >= 2) {
            // 已完成 - 显示终点
            val endPoint = locations.last()
            Marker(
                state = MarkerState(
                    position = LatLng(endPoint.latitude, endPoint.longitude)
                ),
                title = "终点",
                snippet = formatTimestamp(endPoint.timestamp),
                icon = com.google.android.gms.maps.model.BitmapDescriptorFactory
                    .defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED)
            )
        }

        // 4. 显示所有GPS点（可选，点太多时可能影响性能）
        if (locations.size in 2..50) {  // 只在点不太多时显示
            locations.forEach { point ->
                Circle(
                    center = LatLng(point.latitude, point.longitude),
                    radius = 5.0,  // 5米半径
                    fillColor = Color(0x442196F3),  // 半透明蓝色
                    strokeColor = Color(0xFF2196F3),
                    strokeWidth = 2f
                )
            }
        }

        // 5. 附带位置的照片/笔记标记（黄色）
        notePoints.forEach { (lat, lng) ->
            Marker(
                state = MarkerState(position = LatLng(lat, lng)),
                title = "照片/笔记",
                snippet = "附加在这里的瞬间",
                icon = com.google.android.gms.maps.model.BitmapDescriptorFactory
                    .defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_YELLOW)
            )
        }
    }
}

/**
 * 格式化时间戳
 */
private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
