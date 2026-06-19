package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStats(
    @PrimaryKey val date: String, // "yyyy-MM-dd"
    val steps: Int = 0,
    val stepGoal: Int = 10000,
    val waterMl: Int = 0,
    val waterGoalMl: Int = 2500,
    val caloriesConsumed: Int = 0,
    val caloriesBurned: Int = 0,
    val weightKg: Double = 70.0
)

@Entity(tableName = "workout_logs")
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val category: String, // "Cardio", "Strength", "Yoga", "Stretching"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "food_logs")
data class FoodLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val calories: Int,
    val mealType: String, // "Breakfast", "Lunch", "Dinner", "Snack"
    val timestamp: Long = System.currentTimeMillis()
)
