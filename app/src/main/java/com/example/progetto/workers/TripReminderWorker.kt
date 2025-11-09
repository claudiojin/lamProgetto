package com.example.progetto.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.progetto.data.database.TripDatabase
import com.example.progetto.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 旅行提醒Worker
 *
 * 功能：检查用户最近是否有旅行记录，如果超过7天没有记录，发送提醒
 *
 * 类比Web：这是定时任务（Cron Job）
 */
class TripReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 获取数据库
            val database = TripDatabase.getDatabase(applicationContext)
            val tripDao = database.tripDao()

            // 获取所有旅行
            val trips = tripDao.getAllTrips().first()

            if (trips.isEmpty()) {
                // 没有任何旅行记录
                sendNotification(
                    title = "开始记录旅行吧！",
                    message = "还没有记录任何旅行，现在就开始吧！"
                )
            } else {
                // 检查最近的旅行
                val lastTrip = trips.firstOrNull()  // 已按时间降序排列
                if (lastTrip != null) {
                    val daysSinceLastTrip = calculateDaysSince(lastTrip.endDate)

                    if (daysSinceLastTrip >= 7) {
                        // 超过7天没有记录
                        sendNotification(
                            title = "好久没旅行了",
                            message = "已经${daysSinceLastTrip}天没有记录旅行，出去走走吧！"
                        )
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()  // 失败后重试
        }
    }

    /**
     * 发送通知
     */
    private fun sendNotification(title: String, message: String) {
        NotificationHelper.sendTripReminderNotification(
            context = applicationContext,
            title = title,
            message = message
        )
    }

    /**
     * 计算距离某个日期过了多少天
     */
    private fun calculateDaysSince(dateString: String): Long {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val date = sdf.parse(dateString)
            val diffInMillis = System.currentTimeMillis() - (date?.time ?: 0)
            TimeUnit.MILLISECONDS.toDays(diffInMillis)
        } catch (e: Exception) {
            0
        }
    }
}