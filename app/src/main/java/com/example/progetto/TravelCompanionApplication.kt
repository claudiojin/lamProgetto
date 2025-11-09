package com.example.progetto

import android.app.Application
import androidx.work.*
import com.example.progetto.utils.NotificationHelper
import com.example.progetto.workers.TripReminderWorker
import java.util.concurrent.TimeUnit


class TravelCompanionApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. 创建通知渠道
        NotificationHelper.createNotificationChannel(this)

        // 2. 启动周期性任务
        schedulePeriodicTripReminder()
    }

    private fun schedulePeriodicTripReminder() {
        // 任务约束（只在有网络、电量充足时执行）
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)  // 电量不低时执行
            .build()

        // 创建周期任务（每24小时执行一次）
        val tripReminderRequest = PeriodicWorkRequestBuilder<TripReminderWorker>(
            24, TimeUnit.HOURS,  // 重复间隔：24小时
            15, TimeUnit.MINUTES  // 弹性窗口：±15分钟
        )
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.MINUTES)  // 首次延迟1分钟（测试用）
            .build()

        // 提交到WorkManager
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "trip_reminder_work",  // 唯一名称
            ExistingPeriodicWorkPolicy.KEEP,  // 如果已存在，保持原有任务
            tripReminderRequest
        )
    }
}