package com.example.progetto.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.progetto.MainActivity
import com.example.progetto.R

/**
 * 通知助手
 * 封装通知的创建和发送
 */
object NotificationHelper {

    // 通知渠道ID
    private const val CHANNEL_ID = "travel_companion_channel"
    private const val CHANNEL_NAME = "旅行提醒"
    private const val CHANNEL_DESCRIPTION = "旅行记录和提醒通知"

    // 通知ID
    const val TRIP_REMINDER_NOTIFICATION_ID = 1001
    const val GEOFENCE_NOTIFICATION_ID = 1002
    const val TRACKING_NOTIFICATION_ID = 1003

    /**
     * 创建通知渠道（Android 8.0+需要）
     *
     * 类比Web：这像是注册Service Worker
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                importance
            ).apply {
                description = CHANNEL_DESCRIPTION
                // 可选：设置通知声音、震动等
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 检查通知权限（Android 13+需要）
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true  // Android 13以下不需要权限
        }
    }

    /**
     * 发送旅行提醒通知
     *
     * @param context 上下文
     * @param title 通知标题
     * @param message 通知内容
     */
    fun sendTripReminderNotification(
        context: Context,
        title: String,
        message: String
    ) {
        if (!hasNotificationPermission(context)) {
            return  // 没有权限，不发送
        }

        // 创建点击通知后打开应用的Intent
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 构建通知
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)  // 通知图标
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)  // 点击后自动消失
            .build()

        // 发送通知
        try {
            with(NotificationManagerCompat.from(context)) {
                notify(TRIP_REMINDER_NOTIFICATION_ID, notification)
            }
        } catch (se: SecurityException) {
            // 权限可能被拒绝，避免崩溃
        }
    }

    /**
     * 发送地理围栏通知
     */
    fun sendGeofenceNotification(
        context: Context,
        title: String,
        message: String
    ) {
        if (!hasNotificationPermission(context)) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)  // 高优先级
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))  // 震动模式
            .build()

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(GEOFENCE_NOTIFICATION_ID, notification)
            }
        } catch (se: SecurityException) {
            // 权限可能被拒绝，避免崩溃
        }
    }

    /**
     * 构建前台服务的持续通知（用于后台定位）
     */
    fun buildTrackingNotification(context: Context, title: String = "正在记录旅程", message: String = "应用正在后台记录位置信息"):
            android.app.Notification {
        // 点击通知回到应用
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }
}
