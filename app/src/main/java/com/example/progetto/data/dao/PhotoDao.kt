package com.example.progetto.data.dao

import androidx.room.*
import com.example.progetto.data.entity.Photo
import kotlinx.coroutines.flow.Flow

/**
 * 照片数据访问对象
 */
@Dao
interface PhotoDao {

    @Insert
    suspend fun insert(photo: Photo): Long

    @Update
    suspend fun update(photo: Photo)

    @Delete
    suspend fun delete(photo: Photo)

    /**
     * 获取某个旅行的所有照片
     */
    @Query("SELECT * FROM photos WHERE tripId = :tripId ORDER BY timestamp DESC")
    fun getPhotosByTripId(tripId: Long): Flow<List<Photo>>

    /**
     * 获取照片数量
     */
    @Query("SELECT COUNT(*) FROM photos WHERE tripId = :tripId")
    suspend fun getPhotoCount(tripId: Long): Int

    /**
     * 删除某个旅行的所有照片
     */
    @Query("DELETE FROM photos WHERE tripId = :tripId")
    suspend fun deletePhotosByTripId(tripId: Long)
}