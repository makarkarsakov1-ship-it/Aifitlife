package com.example.data

import kotlinx.coroutines.flow.Flow

class FitLifeRepository(private val dao: FitLifeDao) {
    
    fun observeStatsForDate(date: String): Flow<DailyStats?> = dao.observeStatsForDate(date)

    fun observeLast7DaysStats(): Flow<List<DailyStats>> = dao.observeLast7DaysStats()

    suspend fun getStatsForDate(date: String): DailyStats {
        return dao.getStatsForDate(date) ?: DailyStats(date = date)
    }

    suspend fun updateSteps(date: String, steps: Int) {
        val current = getStatsForDate(date)
        dao.insertOrUpdateDailyStats(current.copy(steps = steps))
    }

    suspend fun addSteps(date: String, amount: Int) {
        val current = getStatsForDate(date)
        dao.insertOrUpdateDailyStats(current.copy(steps = current.steps + amount))
    }

    suspend fun addWater(date: String, ml: Int) {
        val current = getStatsForDate(date)
        dao.insertOrUpdateDailyStats(current.copy(waterMl = current.waterMl + ml))
    }

    suspend fun updateWeight(date: String, weight: Double) {
        val current = getStatsForDate(date)
        dao.insertOrUpdateDailyStats(current.copy(weightKg = weight))
    }

    suspend fun updateGoals(date: String, stepGoal: Int, waterGoalMl: Int) {
        val current = getStatsForDate(date)
        dao.insertOrUpdateDailyStats(current.copy(
            stepGoal = stepGoal,
            waterGoalMl = waterGoalMl
        ))
    }

    // Workouts
    val allWorkouts: Flow<List<WorkoutLog>> = dao.observeAllWorkouts()

    suspend fun addWorkout(date: String, workout: WorkoutLog) {
        dao.insertWorkout(workout)
        val current = getStatsForDate(date)
        dao.insertOrUpdateDailyStats(current.copy(
            caloriesBurned = current.caloriesBurned + workout.caloriesBurned
        ))
    }

    suspend fun deleteWorkout(date: String, workout: WorkoutLog) {
        dao.deleteWorkout(workout)
        val current = getStatsForDate(date)
        dao.insertOrUpdateDailyStats(current.copy(
            caloriesBurned = maxOf(0, current.caloriesBurned - workout.caloriesBurned)
        ))
    }

    // Food
    val allFoods: Flow<List<FoodLog>> = dao.observeAllFoods()

    suspend fun addFood(date: String, food: FoodLog) {
        dao.insertFood(food)
        val current = getStatsForDate(date)
        dao.insertOrUpdateDailyStats(current.copy(
            caloriesConsumed = current.caloriesConsumed + food.calories
        ))
    }

    suspend fun deleteFood(date: String, food: FoodLog) {
        dao.deleteFood(food)
        val current = getStatsForDate(date)
        dao.insertOrUpdateDailyStats(current.copy(
            caloriesConsumed = maxOf(0, current.caloriesConsumed - food.calories)
        ))
    }
}
