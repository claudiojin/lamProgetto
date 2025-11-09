package com.example.progetto.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 旅行照片实体
 *
 * 存储照片的文件路径和元数据
 */
@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = Trip::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE  // 删除旅行时自动删除照片
        )
    ],
    indices = [Index("tripId")]
)
data class Photo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val tripId: Long,              // 关联的旅行ID
    val filePath: String,          // 照片文件路径
    val caption: String = "",      // 照片说明（可选）
    val timestamp: Long = System.currentTimeMillis(),  // 拍摄/添加时间
    val latitude: Double? = null,  // 拍摄位置（可选）
    val longitude: Double? = null
)