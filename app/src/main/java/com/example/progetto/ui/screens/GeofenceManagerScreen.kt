package com.example.progetto.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.progetto.data.dao.GeofenceDao
import com.example.progetto.data.entity.GeofenceArea
import com.example.progetto.data.entity.GeofenceEvent
import com.example.progetto.utils.GeofenceManager
import com.example.progetto.utils.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.firstOrNull
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Circle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.CameraPosition
import kotlinx.coroutines.launch

/**
 * 地理围栏管理界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeofenceManagementScreen(
    geofenceDao: GeofenceDao,
    onNavigateBack: () -> Unit
) {
    val geofences by geofenceDao.getAllGeofences().collectAsState(initial = emptyList())
    val recentEvents by geofenceDao.getRecentEvents().collectAsState(initial = emptyList())
    val context = LocalContext.current
    val geofenceManager = remember { GeofenceManager(context) }
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("地理围栏管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加围栏")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Geofence section header
            item {
                Text(
                    text = "我的围栏 (${geofences.size})",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
            }

            if (geofences.isEmpty()) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("🌍", style = MaterialTheme.typography.displayLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("还没有设置地理围栏")
                        Text(
                            "点击右下角+按钮添加常去地点",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            } else {
                items(geofences) { geofence ->
                    GeofenceCard(
                        geofence = geofence,
                        onDelete = {
                            scope.launch {
                                geofenceManager.removeGeofence(geofence.id)
                                geofenceDao.delete(geofence)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Recent events section
            item {
                Spacer(Modifier.height(16.dp))
                Divider()
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "最近事件",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
                if (recentEvents.isEmpty()) {
                    Text(
                        text = "暂无围栏进入/离开记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(recentEvents) { ev ->
                GeofenceEventRow(
                    name = ev.geofenceName,
                    type = ev.transitionType,
                    timestamp = ev.timestamp
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showAddDialog) {
        AddGeofenceDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, lat, lng, radius ->
                scope.launch {
                    val geofence = GeofenceArea(
                        name = name,
                        latitude = lat,
                        longitude = lng,
                        radius = radius
                    )
                    val id = geofenceDao.insert(geofence)
                    geofenceManager.addGeofence(geofence.copy(id = id))
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
private fun GeofenceCard(
    geofence: GeofenceArea,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = geofence.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "半径: ${geofence.radius.toInt()}米",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = String.format(
                        "位置: %.4f, %.4f",
                        geofence.latitude,
                        geofence.longitude
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AddGeofenceDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double, Float) -> Unit
) {
    val context = LocalContext.current
    val locationManager = remember { LocationManager(context) }
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf<Double?>(null) }
    var lng by remember { mutableStateOf<Double?>(null) }
    var radius by remember { mutableStateOf("500") }
    var error by remember { mutableStateOf<String?>(null) }
    var locating by remember { mutableStateOf(false) }
    // 地图中心，初始北京
    var mapCenter by remember { mutableStateOf(LatLng(39.9042, 116.4074)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { /* handled next time */ }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加地理围栏") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("地点名称") },
                    placeholder = { Text("如：家、公司") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 定位与结果展示
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (lat != null && lng != null)
                                String.format("选择位置：%.5f, %.5f", lat, lng)
                            else "未选择位置",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            error = null
                            val fine = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            val coarse = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!fine && !coarse) {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                                return@Button
                            }
                            locating = true
                            scope.launch {
                                try {
                                    val last = locationManager.getLastLocation()
                                    val loc = last ?: locationManager.getLocationUpdates(2000).firstOrNull()
                                    if (loc != null) {
                                        lat = loc.latitude
                                        lng = loc.longitude
                                        // 更新地图中心
                                        mapCenter = LatLng(loc.latitude, loc.longitude)
                                    } else {
                                        error = "无法获取当前位置，请稍后重试"
                                    }
                                } finally {
                                    locating = false
                                }
                            }
                        },
                        enabled = !locating
                    ) {
                        if (locating) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("使用当前位置")
                    }
                }

                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 地图点选位置
                var mapInitialized by remember { mutableStateOf(false) }
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(mapCenter, 15f)
                }

                // 当用户选择位置时，平滑移动相机
                LaunchedEffect(lat, lng) {
                    val lt = lat; val lg = lng
                    if (lt != null && lg != null) {
                        val newCenter = LatLng(lt, lg)
                        cameraPositionState.animate(
                            com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(newCenter, 16f),
                            durationMs = 500
                        )
                    }
                }

                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(),
                    uiSettings = MapUiSettings(zoomControlsEnabled = true),
                    onMapClick = { ll ->
                        lat = ll.latitude
                        lng = ll.longitude
                    }
                ) {
                    val rMeters = radius.toFloatOrNull() ?: 500f
                    if (lat != null && lng != null) {
                        val pos = LatLng(lat!!, lng!!)
                        Marker(state = com.google.maps.android.compose.MarkerState(pos), title = name.ifBlank { "围栏中心" })
                        Circle(
                            center = pos,
                            radius = rMeters.toDouble(),
                            fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            strokeColor = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2f
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = radius,
                    onValueChange = { radius = it },
                    label = { Text("半径(米)") },
                    supportingText = { Text("默认500米，可自定义") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val latV = lat ?: return@TextButton
                    val lngV = lng ?: return@TextButton
                    val rad = radius.toFloatOrNull() ?: 500f
                    if (name.isNotBlank()) onConfirm(name, latV, lngV, rad)
                }
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun GeofenceEventRow(
    name: String,
    type: String,
    timestamp: Long
) {
    val isEnter = type.equals("ENTER", ignoreCase = true)
    val typeLabel = if (isEnter) "进入" else if (type.equals("EXIT", true)) "离开" else type
    val typeColor = if (isEnter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    val timeStr = remember(timestamp) {
        try {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(timestamp))
        } catch (e: Exception) { "-" }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(text = timeStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                color = typeColor.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = typeLabel,
                    color = typeColor,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
