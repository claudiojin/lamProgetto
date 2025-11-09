package com.example.progetto.ui.screens

import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.progetto.utils.LocationManager
import com.example.progetto.utils.PermissionsHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * GPS测试界面
 *
 * 类比Web：这是React/Vue组件
 * 负责UI渲染和用户交互
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GpsTestScreen() {
    // State（类似React的useState）
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var isTracking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Context和工具类
    val context = LocalContext.current
    val locationManager = remember { LocationManager(context) }
    val permissionState = PermissionsHelper.rememberLocationPermissionState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "GPS测试",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 权限状态显示
        when {
            permissionState.allPermissionsGranted -> {
                PermissionGrantedContent(
                    currentLocation = currentLocation,
                    isTracking = isTracking,
                    errorMessage = errorMessage,
                    onGetLocation = {
                        scope.launch {
                            val location = locationManager.getLastLocation()
                            if (location != null) {
                                currentLocation = location
                                errorMessage = null
                            } else {
                                errorMessage = "无法获取位置，请确保GPS已开启"
                            }
                        }
                    },
                    onToggleTracking = {
                        isTracking = !isTracking
                        if (isTracking) {
                            // 开始实时追踪
                            scope.launch {
                                locationManager.getLocationUpdates(2000).collect { location ->
                                    currentLocation = location
                                    errorMessage = null
                                }
                            }
                        }
                    }
                )
            }
            else -> {
                PermissionDeniedContent(
                    onRequestPermission = {
                        permissionState.launchMultiplePermissionRequest()
                    }
                )
            }
        }
    }
}

/**
 * 权限已授予的内容
 */
@Composable
private fun PermissionGrantedContent(
    currentLocation: Location?,
    isTracking: Boolean,
    errorMessage: String?,
    onGetLocation: () -> Unit,
    onToggleTracking: () -> Unit
) {
    Text(
        text = "✅ 位置权限已授予",
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    // 位置信息卡片
    if (currentLocation != null) {
        LocationInfoCard(location = currentLocation)
    }

    // 操作按钮
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onGetLocation,
            modifier = Modifier.weight(1f),
            enabled = !isTracking
        ) {
            Text("获取位置")
        }

        Button(
            onClick = onToggleTracking,
            modifier = Modifier.weight(1f),
            colors = if (isTracking) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            } else {
                ButtonDefaults.buttonColors()
            }
        ) {
            Text(if (isTracking) "停止追踪" else "开始追踪")
        }
    }

    // 错误信息
    if (errorMessage != null) {
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    // 追踪状态提示
    if (isTracking) {
        Text(
            text = "🔴 正在实时追踪位置（每2秒更新）",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * 位置信息卡片
 */
@Composable
private fun LocationInfoCard(location: Location) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "当前位置",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text("纬度: ${location.latitude}")
            Text("经度: ${location.longitude}")
            Text("精度: ${location.accuracy} 米")
            Text("海拔: ${location.altitude} 米")
            Text("速度: ${location.speed} 米/秒")
            Text(
                "时间: ${
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        .format(Date(location.time))
                }"
            )
        }
    }
}

/**
 * 权限被拒绝的内容
 */
@Composable
private fun PermissionDeniedContent(
    onRequestPermission: () -> Unit
) {
    Text(
        text = "❌ 需要位置权限",
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    Text(
        text = "此应用需要访问您的位置来记录旅程路线",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    Button(
        onClick = onRequestPermission,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("请求位置权限")
    }
}