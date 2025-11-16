package com.example.progetto.data.dao

import androidx.room.*
import com.example.progetto.data.entity.Photo
import kotlinx.coroutines.flow.Flow


@Dao
interface PhotoDao {

    @Insert
    suspend fun insert(photo: Photo): Long

    @Update
    suspend fun update(photo: Photo)

    @Delete
    suspend fun delete(photo: Photo)


    @Query("SELECT * FROM photos WHERE tripId = :tripId ORDER BY timestamp DESC")
    fun getPhotosByTripId(tripId: Long): Flow<List<Photo>>


    @Query("SELECT COUNT(*) FROM photos WHERE tripId = :tripId")
    suspend fun getPhotoCount(tripId: Long): Int

    @Query("DELETE FROM photos WHERE tripId = :tripId")
    suspend fun deletePhotosByTripId(tripId: Long)
}