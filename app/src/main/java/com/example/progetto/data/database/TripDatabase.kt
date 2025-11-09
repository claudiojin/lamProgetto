package com.example.progetto.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.progetto.data.dao.GeofenceDao
import com.example.progetto.data.dao.LocationDao
import com.example.progetto.data.dao.TripDao
import com.example.progetto.data.entity.LocationPoint
import com.example.progetto.data.entity.Trip
import com.example.progetto.data.entity.TripType
import com.example.progetto.data.entity.*

/***
 *
 */

@Database(
    entities = [
        Trip::class,
        LocationPoint::class,
        GeofenceArea::class,
        GeofenceEvent::class
    ],
    version = 4,
    exportSchema = false
)

@TypeConverters(Converters::class)
abstract class TripDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun locationDao(): LocationDao
    abstract fun geofenceDao(): GeofenceDao

    companion object {
        @Volatile
        private var INSTANCE: TripDatabase? = null
        fun getDatabase(context: Context): TripDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TripDatabase::class.java,
                    "trip_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromTripType(value: TripType): String {
        return value.name
    }

    @TypeConverter
    fun toTripType(value: String): TripType {
        return TripType.valueOf(value)
    }
}