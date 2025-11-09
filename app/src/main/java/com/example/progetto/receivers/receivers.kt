package com.example.progetto.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.progetto.data.database.TripDatabase
import com.example.progetto.data.entity.GeofenceEvent
import com.example.progetto.utils.NotificationHelper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 地理围栏广播接收器
 *
 * 接收围栏进入/离开事件
 *
 * 类比Web：这是WebSocket消息处理器
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    private val TAG = "GeofenceReceiver"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "📡 收到地理围栏事件")

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            Log.e(TAG, "❌ 事件为空")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "❌ 围栏错误: ${geofencingEvent.errorCode}")
            return
        }

        // 获取触发的围栏列表
        val triggeringGeofences = geofencingEvent.triggeringGeofences
        if (triggeringGeofences == null || triggeringGeofences.isEmpty()) {
            Log.w(TAG, "⚠️ 没有触发的围栏")
            return
        }

        // 获取事件类型
        val geofenceTransition = geofencingEvent.geofenceTransition

        // 处理每个触发的围栏
        triggeringGeofences.forEach { geofence ->
            handleGeofenceTransition(context, geofence, geofenceTransition)
        }
    }

    /**
     * 处理围栏事件
     */
    private fun handleGeofenceTransition(
        context: Context,
        geofence: Geofence,
        transitionType: Int
    ) {
        val geofenceId = geofence.requestId.toLongOrNull() ?: return

        scope.launch {
            try {
                // 1. 从数据库获取围栏信息
                val database = TripDatabase.getDatabase(context)
                val geofenceDao = database.geofenceDao()
                val geofenceArea = geofenceDao.getGeofenceById(geofenceId)

                if (geofenceArea == null) {
                    Log.w(TAG, "⚠️ 找不到围栏: $geofenceId")
                    return@launch
                }

                // 2. 确定事件类型和消息
                val (eventType, title, message) = when (transitionType) {
                    Geofence.GEOFENCE_TRANSITION_ENTER -> {
                        Triple(
                            "ENTER",
                            "到达${geofenceArea.name}",
                            "欢迎回到${geofenceArea.name}！"
                        )
                    }
                    Geofence.GEOFENCE_TRANSITION_EXIT -> {
                        Triple(
                            "EXIT",
                            "离开${geofenceArea.name}",
                            "要开始新的旅程吗？"
                        )
                    }
                    else -> {
                        Log.w(TAG, "⚠️ 未知事件类型: $transitionType")
                        return@launch
                    }
                }

                Log.d(TAG, "🚪 $eventType: ${geofenceArea.name}")

                // 3. 保存事件到数据库
                val event = GeofenceEvent(
                    geofenceId = geofenceId,
                    geofenceName = geofenceArea.name,
                    transitionType = eventType
                )
                geofenceDao.insertEvent(event)

                // 4. 发送通知
                NotificationHelper.sendGeofenceNotification(
                    context = context,
                    title = title,
                    message = message
                )

            } catch (e: Exception) {
                Log.e(TAG, "❌ 处理围栏事件失败: ${e.message}", e)
            }
        }
    }
}