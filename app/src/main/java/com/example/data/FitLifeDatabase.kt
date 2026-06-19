package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DailyStats::class, WorkoutLog::class, FoodLog::class],
    version = 1,
    exportSchema = false
)
abstract class FitLifeDatabase : RoomDatabase() {
    abstract fun dao(): FitLifeDao

    companion object {
        @Volatile
        private var INSTANCE: FitLifeDatabase? = null

        fun getDatabase(context: Context): FitLifeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FitLifeDatabase::class.java,
                    "fitlife_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
