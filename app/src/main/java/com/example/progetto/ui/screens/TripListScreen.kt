package com.example.progetto.ui.screens
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.example.progetto.data.entity.TripType
import com.example.progetto.ui.components.DeleteConfirmDialog
import com.example.progetto.ui.components.TripCard
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun TripListScreen(
    tripDao: TripDao,
    modifier: Modifier = Modifier,
    onTripClick: (Long) -> Unit = {}
) {
    // 从数据库获取数据（Flow自动更新）
    val trips by tripDao.getAllTrips().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<TripType?>(null) }
    val coroutineScope = rememberCoroutineScope()

    var tripToDelete by remember { mutableStateOf<Trip?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var tripToEdit by remember { mutableStateOf<Trip?>(null) }
    var showEditScreen by remember { mutableStateOf(false) }
    var showAddScreen by remember { mutableStateOf(false) }


    if (showAddScreen) {
        TripFormScreen(
            tripDao = tripDao,
            initialTrip = null,
            onSaved = {
                showAddScreen = false
            },
            onCancel = { showAddScreen = false },
            modifier = modifier
        )
    } else if (showEditScreen) {
        TripFormScreen(
            tripDao = tripDao,
            initialTrip = tripToEdit,
            onSaved = {
                showEditScreen = false
                tripToEdit = null
            },
            onCancel = {
                showEditScreen = false
                tripToEdit = null
            },
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(0.dp)
            ) {
                // 官方 API 请求通知权限（Android 13+）
                NotificationPermissionBanner()

                // 标题
                Text(
                    text = "我的旅行",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                )

                // 搜索与筛选
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("搜索目的地") },
                    singleLine = true,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    TripType.values().forEach { type ->
                        val selected = selectedType == type
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedType = if (selected) null else type
                            },
                            label = { Text(type.displayName) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                val filtered = trips.filter { trip ->
                    (searchQuery.isBlank() || trip.destination.contains(searchQuery, ignoreCase = true)) &&
                    (selectedType == null || trip.type == selectedType)
                }

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
                        items(filtered) { trip ->
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

            // 悬浮添加按钮（真正悬浮，不占用布局）
            FloatingActionButton(
                onClick = { showAddScreen = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Trip"
                )
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
