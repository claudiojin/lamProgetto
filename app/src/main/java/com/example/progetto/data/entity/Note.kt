package com.example.progetto.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 位置笔记（文本，按时刻/位置）
 */
@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Trip::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LocationPoint::class,
            parentColumns = ["id"],
            childColumns = ["locationPointId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("tripId"), Index("locationPointId")]
)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tripId: Long,
    val locationPointId: Long? = null,
    val text: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Long = System.currentTimeMillis()
)

