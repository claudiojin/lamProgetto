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
    val createdAt: Long = System.currentTimeMillis()
)


enum class TripType(val displayName: String) {
    LOCAL("Viaggio in citta"),
    DAY_TRIP("Viaggio da un giorno"),
    MULTI_DAY("Viaggio con piu' giorni")
}


