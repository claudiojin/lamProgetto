package com.example.progetto.data.dao

import androidx.room.*
import com.example.progetto.data.entity.GeofenceArea
import com.example.progetto.data.entity.GeofenceEvent
import kotlinx.coroutines.flow.Flow


@Dao
interface GeofenceDao {

    // ========== GeofenceArea ==========

    @Insert
    suspend fun insert(geofence: GeofenceArea): Long

    @Update
    suspend fun update(geofence: GeofenceArea)

    @Delete
    suspend fun delete(geofence: GeofenceArea)

    @Query("SELECT * FROM geofences ORDER BY createdAt DESC")
    fun getAllGeofences(): Flow<List<GeofenceArea>>

    @Query("SELECT * FROM geofences WHERE isActive = 1")
    fun getActiveGeofences(): Flow<List<GeofenceArea>>

    @Query("SELECT * FROM geofences WHERE id = :id")
    suspend fun getGeofenceById(id: Long): GeofenceArea?

    // ========== GeofenceEvent ==========

    @Insert
    suspend fun insertEvent(event: GeofenceEvent): Long

    @Query("SELECT * FROM geofence_events ORDER BY timestamp DESC LIMIT 50")
    fun getRecentEvents(): Flow<List<GeofenceEvent>>

    @Query("SELECT * FROM geofence_events WHERE geofenceId = :geofenceId ORDER BY timestamp DESC")
    fun getEventsByGeofenceId(geofenceId: Long): Flow<List<GeofenceEvent>>
}