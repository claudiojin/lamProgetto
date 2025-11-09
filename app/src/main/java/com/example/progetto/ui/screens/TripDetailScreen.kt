package com.example.progetto.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.progetto.data.dao.LocationDao
import com.example.progetto.data.dao.PhotoDao
import com.example.progetto.data.dao.NoteDao
import com.example.progetto.data.entity.Note
import com.example.progetto.data.dao.TripDao
import com.example.progetto.data.entity.Trip
import com.example.progetto.ui.components.TripMapView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.progetto.data.entity.Photo
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    tripId: Long,
    tripDao: TripDao,
    locationDao: LocationDao,
    photoDao: PhotoDao,  // ✅ 照片
    noteDao: NoteDao,    // ✅ 位置笔记
    onNavigateBack: () -> Unit,
    onStartRecording: (Long) -> Unit,
    onNavigateToPhotos: (Long, String) -> Unit,  // ✅ 新增参数
    onNavigateToNotes: (Long) -> Unit  // ✅ 新增参数
) {
    var trip by remember { mutableStateOf<Trip?>(null) }
    val locations by locationDao.getLocationsByTripId(tripId)
        .collectAsState(initial = emptyList())

    // ✅ 获取照片数量
    var photoCount by remember { mutableStateOf(0) }
    // ✅ 订阅照片列表以用于地图标记
    val photos by photoDao.getPhotosByTripId(tripId).collectAsState(initial = emptyList())
    // ✅ 订阅位置笔记
    val notes by noteDao.getNotesByTripId(tripId).collectAsState(initial = emptyList())

    LaunchedEffect(tripId) {
        withContext(Dispatchers.IO) {
            trip = tripDao.getTripById(tripId)
            photoCount = photoDao.getPhotoCount(tripId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trip?.destination ?: "加载中...") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (trip == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TripInfoCard(trip = trip!!)

                    Spacer(modifier = Modifier.height(16.dp))

                    // ✅ 照片和笔记快捷入口
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 照片按钮
                        OutlinedButton(
                            onClick = {
                                onNavigateToPhotos(tripId, trip?.destination ?: "旅行")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Photo,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("照片 ($photoCount)")
                        }

                        // 笔记按钮
                        OutlinedButton(
                            onClick = {
                                onNavigateToNotes(tripId)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("笔记")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (locations.isNotEmpty()) {
                        GpsStatsCard(
                            locationCount = locations.size,
                            totalDistance = trip!!.distance
                        )
                    } else {
                        Text(
                            text = "还没有GPS记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onStartRecording(tripId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (locations.isEmpty()) "开始记录旅程" else "继续记录"
                        )
                    }
                }

                if (locations.isNotEmpty()) {
                    // 从带位置的照片构造地图标记点
                    val photoPoints: List<Pair<Double, Double>> = photos.mapNotNull { p ->
                        if (p.latitude != null && p.longitude != null) Pair(p.latitude!!, p.longitude!!) else null
                    }
                    val notePointsOnly: List<Pair<Double, Double>> = notes.mapNotNull { n ->
                        if (n.latitude != null && n.longitude != null) Pair(n.latitude!!, n.longitude!!) else null
                    }
                    val notePoints = photoPoints + notePointsOnly
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp)
                    ) {
                        Column {
                            Text(
                                text = "路线轨迹",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(16.dp)
                            )

                            TripMapView(
                                locations = locations,
                                showRoute = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                notePoints = notePoints
                            )
                        }
                    }
                }

                // 位置笔记列表（简版）
                if (notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "位置笔记",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)) {
                        notes.take(5).forEach { n ->
                            Text(text = n.text, style = MaterialTheme.typography.bodyMedium)
                            val timeStr = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
                                .format(java.util.Date(n.timestamp))
                            val posStr = if (n.latitude != null && n.longitude != null)
                                " @ ${"%.5f".format(n.latitude)}, ${"%.5f".format(n.longitude)}" else ""
                            Text(
                                text = "$timeStr$posStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Divider(modifier = Modifier.padding(vertical = 6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TripInfoCard(trip: Trip) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = trip.destination,
                    style = MaterialTheme.typography.headlineSmall
                )
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = trip.type.displayName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${trip.startDate} 至 ${trip.endDate}",
                style = MaterialTheme.typography.bodyMedium
            )

            // 起止时间与时长展示（若有）
            if (trip.startTimestamp != null || trip.endTimestamp != null || trip.durationSec != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val startStr = trip.startTimestamp?.let { ts -> formatDateTime(ts) } ?: "-"
                val endStr = trip.endTimestamp?.let { ts -> formatDateTime(ts) } ?: "-"
                val durStr = trip.durationSec?.let { ds -> formatDuration(ds) } ?: "-"
                Text(
                    text = "开始: $startStr",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "结束: $endStr",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "时长: $durStr",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (trip.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = trip.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDateTime(ts: Long): String {
    return try {
        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(ts))
    } catch (e: Exception) { "-" }
}

private fun formatDuration(sec: Long): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
}

@Composable
private fun GpsStatsCard(
    locationCount: Int,
    totalDistance: Double
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$locationCount",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "GPS点",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.2f", totalDistance),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "公里",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
