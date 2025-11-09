package com.example.progetto.data.dao

import android.location.Location
import androidx.room.*
import com.example.progetto.data.entity.LocationPoint
import com.example.progetto.data.entity.Trip
import com.example.progetto.data.entity.TripWithLocations
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao{
    @Insert
    suspend fun insert(location: LocationPoint): Long

    //inserimento a batch per registare la trattoria
    @Insert
    suspend fun insertAll(location: List<LocationPoint>)

    //get tutti i location di un viaggio
    @Query("SELECT * FROM location_points WHERE tripId = :tripId ORDER BY timestamp ASC")
    fun getLocationsByTripId(tripId: Long): Flow<List<LocationPoint>>

    //cancella tutti i location di un viaggio
    @Query("DELETE FROM location_points WHERE  tripId= :tripId")
    suspend fun deleteLocationsByTripId(tripId: Long)

    @Transaction
    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripWithLocations(tripId: Long): TripWithLocations?
}

data class TripWithLocationsEntity(
    @Embedded val trip: Trip,

    @Relation(
        parentColumn = "id",
        entityColumn = "tripId"
    )
    val locations: List<LocationPoint>
)

