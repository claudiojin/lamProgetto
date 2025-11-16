package com.example.progetto.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "geofences")
data class GeofenceArea(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,              // 地点名称（如"家"、"公司"）
    val latitude: Double,          // 中心点纬度
    val longitude: Double,         // 中心点经度
    val radius: Float = 500f,      // 半径（米）
    val isActive: Boolean = true,  // 是否启用
    val createdAt: Long = System.currentTimeMillis()
)


@Entity(tableName = "geofence_events")
data class GeofenceEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val geofenceId: Long,
    val geofenceName: String,
    val transitionType: String,
    val timestamp: Long = System.currentTimeMillis()
)
