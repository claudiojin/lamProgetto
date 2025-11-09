package com.example.progetto.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 地理围栏数据实体
 *
 * 存储用户设置的常去地点
 */
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

/**
 * 地理围栏事件记录
 * 记录进入/离开的时间
 */
@Entity(tableName = "geofence_events")
data class GeofenceEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val geofenceId: Long,          // 关联的围栏ID
    val geofenceName: String,      // 围栏名称（冗余存储，方便查询）
    val transitionType: String,    // 事件类型："ENTER" 或 "EXIT"
    val timestamp: Long = System.currentTimeMillis()
)
