package com.example.progetto.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.progetto.data.database.TripDatabase
import com.example.progetto.R
import com.example.progetto.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit


class TripReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = TripDatabase.getDatabase(applicationContext)
            val tripDao = database.tripDao()

            val trips = tripDao.getAllTrips().first()

            if (trips.isEmpty()) {
                sendNotification(
                    title = "Inizia a registrare i viaggi!",
                    message = "Non hai ancora registrato alcun viaggio. Comincia ora!"
                )
            } else {
                val lastTrip = trips.firstOrNull()
                if (lastTrip != null) {
                    val daysSinceLastTrip = calculateDaysSince(lastTrip.endDate)

                    if (daysSinceLastTrip >= 7) {
                        sendNotification(
                            title = "È da un po' che non registri",
                            message = "Sono passati ${daysSinceLastTrip} giorni dall'ultima registrazione, esci e viaggia!"
                        )
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun sendNotification(title: String, message: String) {
        NotificationHelper.sendTripReminderNotification(
            context = applicationContext,
            title = title,
            message = message
        )
    }


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
