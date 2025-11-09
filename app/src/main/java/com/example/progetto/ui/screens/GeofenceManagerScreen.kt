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
import com.example.progetto.utils.GeofenceManager
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
        if (geofences.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌍", style = MaterialTheme.typography.displayLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("还没有设置地理围栏")
                    Text(
                        "点击右下角+按钮添加常去地点",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
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
    var name by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("200") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加地理围栏") },
        text = {
            Column {
                Text(
                    "提示：可以使用当前GPS位置，或手动输入坐标",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("地点名称") },
                    placeholder = { Text("如：家、公司") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    label = { Text("纬度") },
                    placeholder = { Text("39.9042") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text("经度") },
                    placeholder = { Text("116.4074") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = radius,
                    onValueChange = { radius = it },
                    label = { Text("半径(米)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val lat = latitude.toDoubleOrNull() ?: return@TextButton
                    val lng = longitude.toDoubleOrNull() ?: return@TextButton
                    val rad = radius.toFloatOrNull() ?: 200f
                    if (name.isNotBlank()) {
                        onConfirm(name, lat, lng, rad)
                    }
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