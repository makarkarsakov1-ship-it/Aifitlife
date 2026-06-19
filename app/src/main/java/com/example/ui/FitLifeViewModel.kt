package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiManager
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FitLifeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FitLifeRepository

    init {
        val database = FitLifeDatabase.getDatabase(application)
        repository = FitLifeRepository(database.dao())
    }

    private val _currentDate = MutableStateFlow(getTodayDateString())
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    val todayStats: StateFlow<DailyStats> = repository.observeStatsForDate(getTodayDateString())
        .map { it ?: DailyStats(date = getTodayDateString()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DailyStats(date = getTodayDateString())
        )

    val workouts: StateFlow<List<WorkoutLog>> = repository.allWorkouts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val foods: StateFlow<List<FoodLog>> = repository.allFoods
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val weeklyStats: StateFlow<List<DailyStats>> = repository.observeLast7DaysStats()
        .map { list -> list.reversed() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _aiAdvice = MutableStateFlow<String>("")
    val aiAdvice: StateFlow<String> = _aiAdvice.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun addSteps(amount: Int) {
        viewModelScope.launch {
            repository.addSteps(currentDate.value, amount)
        }
    }

    fun addWater(ml: Int) {
        viewModelScope.launch {
            repository.addWater(currentDate.value, ml)
        }
    }

    fun updateWeight(weight: Double) {
        viewModelScope.launch {
            repository.updateWeight(currentDate.value, weight)
        }
    }

    fun updateGoals(stepGoal: Int, waterGoalMl: Int) {
        viewModelScope.launch {
            repository.updateGoals(currentDate.value, stepGoal, waterGoalMl)
        }
    }

    fun addWorkout(name: String, minutes: Int, calories: Int, category: String) {
        viewModelScope.launch {
            val workout = WorkoutLog(
                name = name,
                durationMinutes = minutes,
                caloriesBurned = calories,
                category = category
            )
            repository.addWorkout(currentDate.value, workout)
        }
    }

    fun deleteWorkout(workout: WorkoutLog) {
        viewModelScope.launch {
            repository.deleteWorkout(currentDate.value, workout)
        }
    }

    fun addFood(name: String, calories: Int, mealType: String) {
        viewModelScope.launch {
            val food = FoodLog(
                name = name,
                calories = calories,
                mealType = mealType
            )
            repository.addFood(currentDate.value, food)
        }
    }

    fun deleteFood(food: FoodLog) {
        viewModelScope.launch {
            repository.deleteFood(currentDate.value, food)
        }
    }

    fun askAiCoach(customQuestion: String? = null) {
        val stats = todayStats.value
        val workoutsList = workouts.value
        val foodsList = foods.value

        val progressSummary = """
            - Дата: ${stats.date}
            - Мои шаги आज: ${stats.steps} (Цель: ${stats.stepGoal})
            - Выпито воды: ${stats.waterMl} мл (Цель: ${stats.waterGoalMl} мл)
            - Потреблено калорий: ${stats.caloriesConsumed} ккал
            - Сожжено калорий: ${stats.caloriesBurned} ккал
            - Мой вес: ${stats.weightKg} кг
            - Тренировки за сегодня: ${workoutsList.joinToString { "${it.name} (${it.durationMinutes} мин, -${it.caloriesBurned} ккал)" }}
            - Еда за сегодня: ${foodsList.joinToString { "${it.name} (${it.calories} ккал)" }}
        """.trimIndent()

        val prompt = if (!customQuestion.isNullOrBlank()) {
            "Вот данные профиля пользователя на сегодня:\n$progressSummary\n\nПользователь задал персональный вопрос фитнес-тренеру: \"$customQuestion\"\n\nДай очень подробный, дружелюбный, экспертный ответ на русском языке. Ответ должен быть мотивирующим, красиво оформленным, с использованием списков и советов, а также здоровых привычек."
        } else {
            "Вот данные фитнес-трекера пользователя на сегодня:\n$progressSummary\n\nПроанализируй эти показатели и составь отчет AI-тренера на русском языке: персональные рекомендации по активности, балансу воды, диете и тренировкам. Дай 3 практических совета на сегодня. Сделай текст мотивирующим, профессиональным, используй списки с буллетами."
        }

        viewModelScope.launch {
            _aiLoading.value = true
            _aiAdvice.value = "Связываемся с AI Coach... Анализируем ваши фитнес-показатели..."
            try {
                val advice = GeminiManager.generateAdvice(prompt)
                _aiAdvice.value = advice
            } catch (e: Exception) {
                _aiAdvice.value = "Ошибка подключения к AI Coach. Пожалуйста, проверьте интернет-соединение."
            } finally {
                _aiLoading.value = false
            }
        }
    }
}
