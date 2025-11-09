package com.example.progetto.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
/**
 * 位置管理器
 *
 * 类比Web：这是Service层
 * 封装了GPS操作的业务逻辑，类似WebSocket实时推送服务
 */
class LocationManager(private val context: Context) {

    // FusedLocationProviderClient（Google的位置服务客户端）
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * 检查位置权限
     * 类比：检查JWT token是否有效
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 获取最后已知位置（快速）
     * 类比：从缓存获取数据（Redis）
     */
    suspend fun getLastLocation(): Location? {
        if (!hasLocationPermission()) return null

        return try {
            fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取实时位置流（持续更新）
     *
     * 类比Web：这是WebSocket连接，持续推送位置数据
     *
     * Flow vs WebSocket：
     * - Flow: Kotlin的响应式数据流
     * - WebSocket: 双向实时通信
     * - 相似点：都是持续推送数据
     */
    fun getLocationUpdates(intervalMillis: Long = 5000): Flow<Location> = callbackFlow {
        if (!hasLocationPermission()) {
            close(Exception("位置权限未授予"))
            return@callbackFlow
        }

        // 位置请求配置
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,  // 高精度
            intervalMillis                     // 更新间隔
        ).apply {
            setMinUpdateIntervalMillis(intervalMillis / 2)  // 最小间隔
            setWaitForAccurateLocation(false)
        }.build()

        // 位置回调
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    // 发送位置到Flow
                    trySend(location)
                }
            }
        }

        // 开始请求位置更新
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        // 当Flow被取消时，停止位置更新
        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    /**
     * 停止位置更新
     */
    fun stopLocationUpdates(callback: LocationCallback) {
        fusedLocationClient.removeLocationUpdates(callback)
    }
}

