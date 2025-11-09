package com.example.progetto.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.progetto.data.database.TripDatabase
import com.example.progetto.data.entity.LocationPoint
import com.example.progetto.utils.NotificationHelper
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LocationTrackingService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var fusedClient: FusedLocationProviderClient
    private var tripId: Long = -1L
    private var isTracking = false

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            val id = tripId
            if (id <= 0L) return
            scope.launch {
                try {
                    val db = TripDatabase.getDatabase(applicationContext)
                    val point = LocationPoint(
                        tripId = id,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        altitude = location.altitude,
                        accuracy = location.accuracy,
                        speed = location.speed,
                        timestamp = System.currentTimeMillis()
                    )
                    db.locationDao().insert(point)
                } catch (e: Exception) {
                    Log.e(TAG, "Insert location failed: ${e.message}", e)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        NotificationHelper.createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                tripId = intent.getLongExtra(EXTRA_TRIP_ID, -1L)
                startForeground(NotificationHelper.TRACKING_NOTIFICATION_ID,
                    NotificationHelper.buildTrackingNotification(this))
                startLocationUpdates()
            }
            ACTION_STOP -> {
                stopLocationUpdates()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> Unit
        }
        return START_STICKY
    }

    private fun startLocationUpdates() {
        if (isTracking) return
        val hasFine = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        ).setMinUpdateIntervalMillis(2500L)
            .setWaitForAccurateLocation(false)
            .build()

        try {
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
            isTracking = true
        } catch (se: SecurityException) {
            Log.e(TAG, "No location permission for background tracking")
        }
    }

    private fun stopLocationUpdates() {
        if (!isTracking) return
        fusedClient.removeLocationUpdates(callback)
        isTracking = false
    }

    override fun onDestroy() {
        stopLocationUpdates()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "LocationTrackingSvc"
        const val ACTION_START = "com.example.progetto.action.START_TRACKING"
        const val ACTION_STOP = "com.example.progetto.action.STOP_TRACKING"
        const val EXTRA_TRIP_ID = "extra_trip_id"
    }
}

