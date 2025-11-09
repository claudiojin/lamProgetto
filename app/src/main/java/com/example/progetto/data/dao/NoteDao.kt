package com.example.progetto.data.dao

import androidx.room.*
import com.example.progetto.data.entity.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("SELECT * FROM notes WHERE tripId = :tripId ORDER BY timestamp DESC")
    fun getNotesByTripId(tripId: Long): Flow<List<Note>>
}

