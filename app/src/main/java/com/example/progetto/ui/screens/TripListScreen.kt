package com.example.progetto.ui.screens

import EditTripScreen
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.progetto.data.dao.TripDao
import com.example.progetto.data.entity.Trip
import com.example.progetto.ui.components.DeleteConfirmDialog
import com.example.progetto.ui.components.TripCard
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.core.content.ContextCompat

@Composable
fun TripListScreen(
    tripDao: TripDao,
    modifier: Modifier = Modifier,
    onTripClick: (Long) -> Unit = {},
    onNavigateToStatistics: () -> Unit = {},
    onNavigateToGeofence: () -> Unit
) {
    // 从数据库获取数据（Flow自动更新）
    val trips by tripDao.getAllTrips().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    var tripToDelete by remember { mutableStateOf<Trip?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var tripToEdit by remember { mutableStateOf<Trip?>(null) }
    var showEditScreen by remember { mutableStateOf(false) }


    if (showEditScreen) {
        EditTripScreen(
            trip = tripToEdit,
            onSave = { trip ->
                coroutineScope.launch {
                    if (tripToEdit == null) {
                        tripDao.insert(trip)
                    } else {
                        tripDao.update(trip)

                    }
                    showEditScreen = false
                    tripToEdit = null
                }

            },
            onCancel = {
                showEditScreen = false
                tripToEdit = null
            }
        )
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            floatingActionButton = {
                Column { // ✅ 地理围栏按钮
                    FloatingActionButton(
                        onClick = onNavigateToGeofence,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Place,
                            contentDescription = "地理围栏"
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    FloatingActionButton(
                        onClick = onNavigateToStatistics,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.BarChart,
                            contentDescription = "统计"
                        )
                    }
                    FloatingActionButton(
                        onClick = {
                            tripToEdit = null
                            showEditScreen = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Trip"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

            }
        ) { innerPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 官方 API 请求通知权限（Android 13+）
                NotificationPermissionBanner()

                // 标题
                Text(
                    text = "我的旅行 (共${trips.size}个)",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(16.dp)
                )

                // 旅行列表
                if (trips.isEmpty()) {
                    // 空状态
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无旅行记录")
                    }
                } else {
                    LazyColumn {
                        items(trips) { trip ->
                            TripCard(
                                trip = trip,
                                onDeleted = {
                                    tripToDelete = it
                                    showDeleteDialog = true
                                },
                                onEdit = {
                                    tripToEdit = it
                                    showEditScreen = true
                                },
                                onClick = { onTripClick(trip.id) }
                            )
                        }
                    }
                }
            }
        }
    }


    if (showDeleteDialog && tripToDelete != null) {
        DeleteConfirmDialog(
            trip = tripToDelete!!,
            onConfirm = {
                coroutineScope.launch {
                    tripDao.delete(tripToDelete!!)
                    showDeleteDialog = false
                    tripToDelete = null

                }
            },
            onDismiss = {
                showDeleteDialog = false
                tripToDelete = null
            }
        )
    }
}

@Composable
private fun NotificationPermissionBanner() {
    val context = LocalContext.current

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!isGranted) {
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { /* 可选：根据授权结果提示 */ }
            )

            // 简单按钮提示授权（不在服务或后台触发请求）
            Button(
                onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "通知"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "启用通知")
            }
        }
    }
}

