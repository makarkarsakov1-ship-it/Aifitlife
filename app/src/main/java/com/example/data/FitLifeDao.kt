package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FitLifeDao {
    @Query("SELECT * FROM daily_stats WHERE date = :date LIMIT 1")
    suspend fun getStatsForDate(date: String): DailyStats?

    @Query("SELECT * FROM daily_stats WHERE date = :date LIMIT 1")
    fun observeStatsForDate(date: String): Flow<DailyStats?>

    @Query("SELECT * FROM daily_stats ORDER BY date DESC LIMIT 7")
    fun observeLast7DaysStats(): Flow<List<DailyStats>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyStats(stats: DailyStats)

    // Workouts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(log: WorkoutLog)

    @Query("SELECT * FROM workout_logs ORDER BY timestamp DESC")
    fun observeAllWorkouts(): Flow<List<WorkoutLog>>

    @Delete
    suspend fun deleteWorkout(log: WorkoutLog)

    // Food
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(log: FoodLog)

    @Query("SELECT * FROM food_logs ORDER BY timestamp DESC")
    fun observeAllFoods(): Flow<List<FoodLog>>

    @Delete
    suspend fun deleteFood(log: FoodLog)
}
