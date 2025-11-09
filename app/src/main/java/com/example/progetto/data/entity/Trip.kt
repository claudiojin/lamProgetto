package com.example.progetto.data.entity
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val destination: String,
    val startDate: String,
    val endDate: String,
    val type: TripType,
    var notes: String = "",
    var distance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val detailedNotes: String = "",

    // 新增：旅程起止时间与总时长（秒）
    val startTimestamp: Long? = null,
    val endTimestamp: Long? = null,
    val durationSec: Long? = null

)


enum class TripType(val displayName: String) {
    LOCAL("Viaggio in citta"),
    DAY_TRIP("Viaggio da un giorno"),
    MULTI_DAY("Viaggio con piu' giorni")
}

