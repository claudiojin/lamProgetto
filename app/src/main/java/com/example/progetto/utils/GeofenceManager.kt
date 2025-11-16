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


class GeofenceManager(private val context: Context) {

    private val TAG = "GeofenceManager"

    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }


    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }


    fun addGeofence(geofenceArea: GeofenceArea) {
        if (!hasLocationPermission()) {
            Log.e(TAG, "❌ Non hai i permessi per la geolocalizzazione")
            return
        }

        val geofence = Geofence.Builder()
            .setRequestId(geofenceArea.id.toString())
            .setCircularRegion(
                geofenceArea.latitude,
                geofenceArea.longitude,
                geofenceArea.radius
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or
                        Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        try {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    Log.d(TAG, "✅ Aggiunta con successo: ${geofenceArea.name}")
                }
                addOnFailureListener { e ->
                    Log.e(TAG, "❌ Aggiunta fallita: ${e.message}")
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Errore di sicurezza: ${e.message}")
        }
    }


    fun removeGeofence(geofenceId: Long) {
        geofencingClient.removeGeofences(listOf(geofenceId.toString())).run {
            addOnSuccessListener {
                Log.d(TAG, "✅ Rimozione avvenuta con successo: $geofenceId")
            }
            addOnFailureListener { e ->
                Log.e(TAG, "❌ Rimozione fallita: ${e.message}")
            }
        }
    }


    fun removeAllGeofences() {
        geofencingClient.removeGeofences(geofencePendingIntent).run {
            addOnSuccessListener {
                Log.d(TAG, "✅ Tutti sono rimossi")
            }
            addOnFailureListener { e ->
                Log.e(TAG, "❌ Rimozione fallita: ${e.message}")
            }
        }
    }
}