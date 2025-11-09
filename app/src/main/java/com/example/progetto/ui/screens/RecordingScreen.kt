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
import androidx.compose.ui.unit.dp
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
    // 状态
    var trip by remember { mutableStateOf<Trip?>(null) }
    var isRecording by remember { mutableStateOf(true) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var recordedLocations by remember { mutableStateOf<List<LocationPoint>>(emptyList()) }
    var elapsedTime by remember { mutableStateOf(0L) }  // 秒
    var totalDistance by remember { mutableStateOf(0.0) }  // 公里

    val context = LocalContext.current
    val locationManager = remember { LocationManager(context) }
    val scope = rememberCoroutineScope()

    var showNoteDialog by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }

    // 加载旅行数据
    LaunchedEffect(tripId) {
        // 加载Trip，并在首次进入录制时写入开始时间
        val loaded = tripDao.getTripById(tripId)
        if (loaded != null && loaded.startTimestamp == null) {
            val startTs = System.currentTimeMillis()
            tripDao.update(loaded.copy(startTimestamp = startTs))
            trip = loaded.copy(startTimestamp = startTs)
        } else {
            trip = loaded
        }

        // 加载已有的GPS点
        locationDao.getLocationsByTripId(tripId).collect { locations ->
            recordedLocations = locations
            totalDistance = calculateDistance(locations)
        }
    }

    // 计时器
    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(1000)
            elapsedTime++
        }
    }

    // 官方权限请求 launcher（定位）
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* 结果交由下次重组检查 */ }

    // 后台持续定位：使用前台服务进行追踪（包含前台与后台）
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

    // 停止记录
    fun stopRecording() {
        scope.launch {
            isRecording = false
            // 停止前台服务
            val stopIntent = Intent(context, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_STOP
            }
            context.startService(stopIntent)

            // 更新Trip的距离
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
                title = { Text("记录中...") },
                navigationIcon = {
                    IconButton(onClick = {
                        // 显示确认对话框
                        stopRecording()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
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
            // 若无定位权限，显示请求入口并返回，避免进入定位逻辑
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

            // 记录状态指示
            RecordingIndicator()

            Spacer(modifier = Modifier.height(24.dp))

            // 统计卡片
            StatsCard(
                elapsedTime = elapsedTime,
                distance = totalDistance,
                speed = currentLocation?.speed ?: 0f,
                locationCount = recordedLocations.size
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 当前位置信息
            if (currentLocation != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    Column {
                        Text(
                            text = "实时位置",
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

            // 添加位置笔记
            ElevatedButton(
                onClick = {
                    noteText = ""
                    showNoteDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("添加位置笔记")
            }

            Spacer(modifier = Modifier.weight(1f))

            // 停止按钮
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
                Text("停止记录", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    // 笔记输入对话框（内联）
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

/**
 * 记录中指示器
 */
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
            text = "正在记录GPS轨迹",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

/**
 * 统计卡片
 */
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
            // 距离（主要显示）
            Text(
                text = String.format("%.2f", distance),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "公里",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 其他统计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(
                    label = "时长",
                    value = formatTime(elapsedTime)
                )
                StatItem(
                    label = "速度",
                    value = String.format("%.1f km/h", speed * 3.6)
                )
                StatItem(
                    label = "GPS点",
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

/**
 * 当前位置卡片
 */
@Composable
private fun CurrentLocationCard(location: Location) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "当前位置",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("纬度: ${String.format("%.6f", location.latitude)}")
            Text("经度: ${String.format("%.6f", location.longitude)}")
            Text("精度: ${String.format("%.1f", location.accuracy)} 米")
            Text(
                "时间: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(Date(location.time))}"
            )
        }
    }
}

/**
 * 计算总距离
 */
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

    return totalDistance / 1000.0  // 转为公里
}

/**
 * 格式化时间
 */
private fun formatTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, secs)
        else -> String.format("%d:%02d", minutes, secs)
    }
}

/**
 * 无权限时的提示块
 */
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
                text = "需要定位权限以开始记录",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRequest) { Text("授予定位权限") }
        }
    }
}

// 内联弹窗：添加位置笔记
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
        title = { Text("添加笔记") },
        text = {
            OutlinedTextField(
                value = noteText,
                onValueChange = onTextChange,
                label = { Text("内容") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = onSave) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
