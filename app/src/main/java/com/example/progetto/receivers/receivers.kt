package com.example.progetto.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.progetto.data.database.TripDatabase
import com.example.progetto.R
import com.example.progetto.data.entity.GeofenceEvent
import com.example.progetto.utils.NotificationHelper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


class GeofenceBroadcastReceiver : BroadcastReceiver() {

    private val TAG = "GeofenceReceiver"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "📡 Riceuvto evento di recinto")

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            Log.e(TAG, "❌ Evento vuoto")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "❌ Errore: ${geofencingEvent.errorCode}")
            return
        }


        val triggeringGeofences = geofencingEvent.triggeringGeofences
        if (triggeringGeofences == null || triggeringGeofences.isEmpty()) {
            Log.w(TAG, "⚠️ Non ci sono ")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        triggeringGeofences.forEach { geofence ->
            handleGeofenceTransition(context, geofence, geofenceTransition)
        }
    }


    private fun handleGeofenceTransition(
        context: Context,
        geofence: Geofence,
        transitionType: Int
    ) {
        val geofenceId = geofence.requestId.toLongOrNull() ?: return

        scope.launch {
            try {
                val database = TripDatabase.getDatabase(context)
                val geofenceDao = database.geofenceDao()
                val geofenceArea = geofenceDao.getGeofenceById(geofenceId)

                if (geofenceArea == null) {
                    Log.w(TAG, "⚠️ Non trovato: $geofenceId")
                    return@launch
                }

                val (eventType, title, message) = when (transitionType) {
                    Geofence.GEOFENCE_TRANSITION_ENTER -> {
                        Triple(
                            "ENTER",
                            context.getString(R.string.geofence_enter_title, geofenceArea.name),
                            context.getString(R.string.geofence_enter_message, geofenceArea.name)
                        )
                    }
                    Geofence.GEOFENCE_TRANSITION_EXIT -> {
                        Triple(
                            "EXIT",
                            context.getString(R.string.geofence_exit_title, geofenceArea.name),
                            context.getString(R.string.geofence_exit_message, geofenceArea.name)
                        )
                    }
                    else -> {
                        Log.w(TAG, "⚠️ Evento tipo sconosciuto: $transitionType")
                        return@launch
                    }
                }

                Log.d(TAG, "🚪 $eventType: ${geofenceArea.name}")

                val event = GeofenceEvent(
                    geofenceId = geofenceId,
                    geofenceName = geofenceArea.name,
                    transitionType = eventType
                )
                geofenceDao.insertEvent(event)

                NotificationHelper.sendGeofenceNotification(
                    context = context,
                    title = title,
                    message = message
                )

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore: ${e.message}", e)
            }
        }
    }
}
