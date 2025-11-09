package com.example.progetto.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.progetto.data.entity.Trip
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    //nuovo viaggio
    // con @Insert
    @Insert
    suspend fun insert(trip: Trip): Long

    //update viaggio
    @Update
    suspend fun update(trip: Trip)

    //delete viaggio
    @Delete
    suspend fun delete(trip: Trip)

    //delete by ID
    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteById(tripId: Long)

    //get tutti i viaggi con FLOW
    @Query("SELECT * FROM trips ORDER BY createdAt DESC")
    fun getAllTrips(): Flow<List<Trip>>

    //get viaggio by ID
    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripById(tripId: Long): Trip?

    //get viaggio con filtro di tipo
    @Query("SELECT * FROM trips WHERE type = :type ORDER BY createdAt DESC")
    fun getTripsByType(type: String): Flow<List<Trip>>

    //ricerca un viaggio(con destinazione)
    @Query("SELECT * FROM trips WHERE destination LIKE '%' || :searchQuery || '%'")
    fun searchTrips(searchQuery: String): Flow<List<Trip>>

    @Transaction
    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripWithLocations(tripId: Long): TripWithLocationsEntity?
}