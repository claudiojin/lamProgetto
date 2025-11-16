package com.example.progetto

import android.app.Application
import androidx.work.*
import com.example.progetto.utils.NotificationHelper
import com.example.progetto.workers.TripReminderWorker
import java.util.concurrent.TimeUnit
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat


class TravelCompanionApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        try {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("it"))
        } catch (_: Exception) {}

        NotificationHelper.createNotificationChannel(this)

        schedulePeriodicTripReminder()
    }

    private fun schedulePeriodicTripReminder() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val tripReminderRequest = PeriodicWorkRequestBuilder<TripReminderWorker>(
            24, TimeUnit.HOURS,
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "trip_reminder_work",
            ExistingPeriodicWorkPolicy.KEEP,
            tripReminderRequest
        )
    }
}
