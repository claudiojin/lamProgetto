package com.example.progetto.utils

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.progetto.data.entity.GeofenceArea
import com.example.progetto.receivers.GeofenceBroadcastReceiver
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

/**
 * 地理围栏管理器
 *
 * 负责添加、删除和管理地理围栏
 */
class GeofenceManager(private val context: Context) {

    private val TAG = "GeofenceManager"

    // Geofencing客户端
    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    // PendingIntent（用于接收围栏事件）
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * 检查位置权限
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 添加地理围栏
     *
     * @param geofenceArea 围栏区域
     */
    fun addGeofence(geofenceArea: GeofenceArea) {
        if (!hasLocationPermission()) {
            Log.e(TAG, "❌ 没有位置权限，无法添加围栏")
            return
        }

        // 1. 创建Geofence对象
        val geofence = Geofence.Builder()
            .setRequestId(geofenceArea.id.toString())  // 唯一ID
            .setCircularRegion(
                geofenceArea.latitude,
                geofenceArea.longitude,
                geofenceArea.radius
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)  // 永不过期
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or  // 进入
                        Geofence.GEOFENCE_TRANSITION_EXIT      // 离开
            )
            .build()

        // 2. 创建GeofencingRequest
        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)  // 初始触发
            .addGeofence(geofence)
            .build()

        // 3. 添加到系统
        try {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    Log.d(TAG, "✅ 围栏添加成功: ${geofenceArea.name}")
                }
                addOnFailureListener { e ->
                    Log.e(TAG, "❌ 围栏添加失败: ${e.message}")
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ 安全异常: ${e.message}")
        }
    }

    /**
     * 移除地理围栏
     */
    fun removeGeofence(geofenceId: Long) {
        geofencingClient.removeGeofences(listOf(geofenceId.toString())).run {
            addOnSuccessListener {
                Log.d(TAG, "✅ 围栏移除成功: $geofenceId")
            }
            addOnFailureListener { e ->
                Log.e(TAG, "❌ 围栏移除失败: ${e.message}")
            }
        }
    }

    /**
     * 移除所有地理围栏
     */
    fun removeAllGeofences() {
        geofencingClient.removeGeofences(geofencePendingIntent).run {
            addOnSuccessListener {
                Log.d(TAG, "✅ 所有围栏已移除")
            }
            addOnFailureListener { e ->
                Log.e(TAG, "❌ 移除围栏失败: ${e.message}")
            }
        }
    }
}