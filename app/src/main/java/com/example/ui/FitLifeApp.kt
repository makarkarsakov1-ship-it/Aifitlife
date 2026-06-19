package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DailyStats
import com.example.data.FoodLog
import com.example.data.WorkoutLog
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FitLifeApp(viewModel: FitLifeViewModel) {
    val stats by viewModel.todayStats.collectAsStateWithLifecycle()
    val workouts by viewModel.workouts.collectAsStateWithLifecycle()
    val foods by viewModel.foods.collectAsStateWithLifecycle()
    val weeklyStats by viewModel.weeklyStats.collectAsStateWithLifecycle()
    val aiAdvice by viewModel.aiAdvice.collectAsStateWithLifecycle()
    val aiLoading by viewModel.aiLoading.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Dashboard, 1: Workouts, 2: Diet/Water, 3: AI Coach
    var showGoalDialog by remember { mutableStateOf(false) }
    var activeWorkoutName by remember { mutableStateOf<String?>(null) }
    var activeWorkoutCategory by remember { mutableStateOf("") }
    var isWorkoutSessionRunning by remember { mutableStateOf(false) }

    // Edge-to-edge content container with standard bottom M3 NavigationBar
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                val tabs = listOf(
                    NavigationTabItem("Обзор", Icons.Default.Dashboard, "dashboard_tab"),
                    NavigationTabItem("Фир-трени", Icons.Default.DirectionsRun, "workouts_tab"),
                    NavigationTabItem("Вода и Еда", Icons.Default.Restaurant, "diet_tab"),
                    NavigationTabItem("AI Коуч", Icons.Default.AutoAwesome, "coach_tab")
                )
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentPrimary,
                            selectedTextColor = AccentPrimary,
                            indicatorColor = DarkCard,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkBackground,
                            DarkBackground.copy(alpha = 0.95f),
                            DarkSurface
                        )
                    )
                )
        ) {
            // Screen routing transition
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220, easing = EaseOutQuad)) with
                            fadeOut(animationSpec = tween(180, easing = EaseInQuad))
                },
                label = "TabTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> DashboardScreen(
                        stats = stats,
                        weeklyStats = weeklyStats,
                        onAddStepsClick = { viewModel.addSteps(1000) },
                        onDailyGreetingClick = { activeTab = 3; viewModel.askAiCoach() },
                        onConfigureGoalsClick = { showGoalDialog = true }
                    )
                    1 -> WorkoutsScreen(
                        workouts = workouts,
                        onAddWorkout = { name, mins, cals, cat ->
                            viewModel.addWorkout(name, mins, cals, cat)
                        },
                        onDeleteWorkout = { viewModel.deleteWorkout(it) },
                        onStartActiveWorkout = { name, cat ->
                            activeWorkoutName = name
                            activeWorkoutCategory = cat
                            isWorkoutSessionRunning = true
                        }
                    )
                    2 -> DietAndWaterScreen(
                        stats = stats,
                        foods = foods,
                        onAddWater = { amount -> viewModel.addWater(amount) },
                        onAddFood = { name, calories, mealType ->
                            viewModel.addFood(name, calories, mealType)
                        },
                        onDeleteFood = { viewModel.deleteFood(it) }
                    )
                    3 -> CoachScreen(
                        aiAdvice = aiAdvice,
                        aiLoading = aiLoading,
                        onAskCoach = { question -> viewModel.askAiCoach(question) }
                    )
                }
            }

            // Interactive Live Workout Timer Modal
            if (isWorkoutSessionRunning && activeWorkoutName != null) {
                ActiveWorkoutSessionDialog(
                    workoutName = activeWorkoutName!!,
                    category = activeWorkoutCategory,
                    onFinishWorkout = { elapsedSeconds ->
                        val durationMins = maxOf(1, elapsedSeconds / 60)
                        val multiplier = when (activeWorkoutCategory) {
                            "Cardio" -> 10
                            "Strength" -> 7
                            "Yoga" -> 4
                            else -> 5
                        }
                        val calculatedCalories = durationMins * multiplier
                        viewModel.addWorkout(activeWorkoutName!!, durationMins, calculatedCalories, activeWorkoutCategory)
                        isWorkoutSessionRunning = false
                        activeWorkoutName = null
                    },
                    onDismiss = {
                        isWorkoutSessionRunning = false
                        activeWorkoutName = null
                    }
                )
            }

            // Goal editor modal
            if (showGoalDialog) {
                GoalConfigDialog(
                    currentStepGoal = stats.stepGoal,
                    currentWaterGoal = stats.waterGoalMl,
                    onSave = { stepGoal, waterGoal ->
                        viewModel.updateGoals(stepGoal, waterGoal)
                        showGoalDialog = false
                    },
                    onDismiss = { showGoalDialog = false }
                )
            }
        }
    }
}

data class NavigationTabItem(val label: String, val icon: ImageVector, val testTag: String)

// ==========================================
// 1. DASHBOARD SCREEN (OBZOR)
// ==========================================
@Composable
fun DashboardScreen(
    stats: DailyStats,
    weeklyStats: List<DailyStats>,
    onAddStepsClick: () -> Unit,
    onDailyGreetingClick: () -> Unit,
    onConfigureGoalsClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    val stepsFraction = remember(stats.steps, stats.stepGoal) {
        if (stats.stepGoal > 0) minOf(1f, stats.steps.toFloat() / stats.stepGoal) else 0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = stepsFraction,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "StepsRingProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming Card Header
        DashboardHeader(stats = stats, onDailyGreetingClick = onDailyGreetingClick)

        // Circle tracker with pulse animation & animated lines
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Активность за сегодня",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(
                        onClick = onConfigureGoalsClick,
                        modifier = Modifier.testTag("configure_goals_btn")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Настроить цели", tint = AccentPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Accent dynamic neon progress circle canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 14.dp.toPx()
                        // Base trail
                        drawCircle(
                            color = DarkCard,
                            style = Stroke(width = strokeWidth)
                        )
                        // Progress Arc
                        drawArc(
                            color = StepsMint,
                            startAngle = -90f,
                            sweepAngle = animatedProgress * 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // Numeric values
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.DirectionsWalk,
                            contentDescription = "Шаги",
                            tint = StepsMint,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            "${stats.steps}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(
                            "Цель: ${stats.stepGoal}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onAddStepsClick,
                    colors = ButtonDefaults.buttonColors(containerColor = StepsMint),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_1000_steps_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Симулировать +1000 шагов", fontWeight = FontWeight.Bold, color = DarkBackground)
                }
            }
        }

        // Row of mini trackers (Hydration wave fraction & calorie indicators)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MiniMetricCard(
                title = "Потреблено",
                value = "${stats.caloriesConsumed} ккал",
                subText = "Баланс калорий",
                icon = Icons.Default.LocalFireDepartment,
                accentColor = CalorieOrange,
                modifier = Modifier.weight(1f)
            )

            MiniMetricCard(
                title = "Сожжено",
                value = "${stats.caloriesBurned} ккал",
                subText = "С тренировками",
                icon = Icons.Default.FitnessCenter,
                accentColor = CardioRed,
                modifier = Modifier.weight(1f)
            )
        }

        // Custom drawn, interactive step tracker analytics chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Динамика за неделю",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    "Анализ шагов за последние дни",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (weeklyStats.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Нет данных статистики за неделю", color = TextMuted)
                    }
                } else {
                    InteractiveWeeklyChart(
                        weeklyStats = weeklyStats,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardHeader(stats: DailyStats, onDailyGreetingClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_header_card"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "С возвращением!",
                    fontSize = 13.sp,
                    color = AccentPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Ваш FitLife статус сегодня",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Отличный день для фитнес-целей! Вы выпили ${stats.waterMl} мл воды и прошли ${stats.steps} шагов.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onDailyGreetingClick,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                shape = CircleShape,
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier.size(54.dp)
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "Анализ AI-советником",
                    tint = DarkBackground
                )
            }
        }
    }
}

@Composable
fun MiniMetricCard(
    title: String,
    value: String,
    subText: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .padding(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subText, fontSize = 11.sp, color = TextMuted)
        }
    }
}

// Custom-drawn canvas interactive Weekly Chart
@Composable
fun InteractiveWeeklyChart(
    weeklyStats: List<DailyStats>,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf(-1) }
    val maxSteps = remember(weeklyStats) {
        val max = weeklyStats.maxOfOrNull { it.steps } ?: 10000
        if (max == 0) 10000 else max
    }

    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(weeklyStats) {
                    detectTapGestures { offset ->
                        val widthBetweenPoints = size.width / (weeklyStats.size + 1)
                        weeklyStats.forEachIndexed { index, _ ->
                            val x = (index + 1) * widthBetweenPoints
                            if (Math.abs(offset.x - x) < widthBetweenPoints / 2) {
                                selectedIndex = index
                            }
                        }
                    }
                }
        ) {
            val widthBetweenPoints = size.width / (weeklyStats.size + 1)
            val path = Path()
            val fillPath = Path()
            
            val points = weeklyStats.mapIndexed { index, daily ->
                val x = (index + 1) * widthBetweenPoints
                val fraction = daily.steps.toFloat() / maxSteps
                val y = size.height - 40.dp.toPx() - (fraction * (size.height - 80.dp.toPx()))
                Offset(x, y)
            }

            // Draw baseline & guide lines
            drawLine(
                color = DarkCard,
                start = Offset(0f, size.height - 30.dp.toPx()),
                end = Offset(size.width, size.height - 30.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )

            if (points.isNotEmpty()) {
                // Generate path curves smoothly
                path.moveTo(points.first().x, points.first().y)
                fillPath.moveTo(points.first().x, size.height - 30.dp.toPx())
                fillPath.lineTo(points.first().x, points.first().y)
                
                for (i in 1 until points.size) {
                    val prevPoint = points[i - 1]
                    val point = points[i]
                    path.cubicTo(
                        (prevPoint.x + point.x) / 2f, prevPoint.y,
                        (prevPoint.x + point.x) / 2f, point.y,
                        point.x, point.y
                    )
                    fillPath.cubicTo(
                        (prevPoint.x + point.x) / 2f, prevPoint.y,
                        (prevPoint.x + point.x) / 2f, point.y,
                        point.x, point.y
                    )
                }
                
                fillPath.lineTo(points.last().x, size.height - 30.dp.toPx())
                fillPath.close()

                // Draw neon area fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(StepsMint.copy(alpha = 0.35f), StepsMint.copy(alpha = 0.0f))
                    )
                )

                // Draw progress curve line
                drawPath(
                    path = path,
                    color = StepsMint,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw data nodules & labels
                weeklyStats.forEachIndexed { index, stats ->
                    val point = points[index]
                    val isSelected = index == selectedIndex
                    
                    // Draw node
                    drawCircle(
                        color = if (isSelected) AccentPrimary else StepsMint,
                        radius = if (isSelected) 7.dp.toPx() else 4.dp.toPx(),
                        center = point
                    )

                    // Draw day text label
                    val dateLabel = try {
                        val parts = stats.date.split("-")
                        if (parts.size >= 3) "${parts[2]}.${parts[1]}" else stats.date
                    } catch (e: Exception) {
                        stats.date
                    }

                    // A simple baseline text indicator or custom drawings could go here
                }
            }
        }

        // Custom Tooltip Bubble overlay for interactions
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (selectedIndex != -1 && selectedIndex < weeklyStats.size) {
                val selectedDay = weeklyStats[selectedIndex]
                Card(
                    modifier = Modifier.padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(StepsMint)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "${selectedDay.steps} шагов от ${selectedDay.date}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            } else {
                Text(
                    "Нажмите вкладку дня на графике для отображения деталей",
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            // High contract text dates array
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weeklyStats.forEachIndexed { index, stats ->
                    val isSelected = index == selectedIndex
                    val dateLabel = try {
                        val parts = stats.date.split("-")
                        if (parts.size >= 3) "${parts[2]}" else stats.date
                    } catch (e: Exception) {
                        stats.date
                    }
                    Text(
                        text = dateLabel,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) AccentPrimary else TextSecondary,
                        modifier = Modifier
                            .clickable { selectedIndex = index }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// 2. WORKOUTS SCREEN (TRENIKOVKI)
// ==========================================
@Composable
fun WorkoutsScreen(
    workouts: List<WorkoutLog>,
    onAddWorkout: (String, Int, Int, String) -> Unit,
    onDeleteWorkout: (WorkoutLog) -> Unit,
    onStartActiveWorkout: (String, String) -> Unit
) {
    var customWorkoutName by remember { mutableStateOf("") }
    var customWorkoutMinutes by remember { mutableStateOf("") }
    var customWorkoutCategory by remember { mutableStateOf("Cardio") } // Cardio, Strength, Yoga, Stretch
    var showCustomLogPanel by remember { mutableStateOf(false) }

    val presetWorkouts = listOf(
        PresetWorkout("Легкая пробежка", 25, 250, "Cardio"),
        PresetWorkout("Кроссфит-сессия", 40, 410, "Cardio"),
        PresetWorkout("Силовая круговая", 45, 320, "Strength"),
        PresetWorkout("Суставная разминка", 15, 80, "Stretching"),
        PresetWorkout("Хатха-йога для спины", 30, 140, "Yoga")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Тренировки и активность",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                "Запускайте активные сессии с таймером или логируйте готовые",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        // Section: Starts Live Animated Timer Workouts
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Активная сессия",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentPrimary
                    )
                    Text(
                        "Запустите таймер во время тренировки. AI автоматически рассчитает сожженные калории на основе вашей текущей сессии.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onStartActiveWorkout("Кардио тренировка", "Cardio") },
                            colors = ButtonDefaults.buttonColors(containerColor = CardioRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("start_cardio_session_btn")
                        ) {
                            Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Старт Кардио", fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        Button(
                            onClick = { onStartActiveWorkout("Силовая тонизация", "Strength") },
                            colors = ButtonDefaults.buttonColors(containerColor = StrengthPurple),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("start_strength_session_btn")
                        ) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Старт Сила", fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }
            }
        }

        // Section: Presets quick-logging
        item {
            Text(
                "Быстрое добавление тренировок",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        items(presetWorkouts) { preset ->
            PresetWorkoutItem(
                preset = preset,
                onLog = {
                    onAddWorkout(preset.name, preset.durationMinutes, preset.calories, preset.category)
                }
            )
        }

        // Section: Custom Log Toggle
        item {
            OutlinedButton(
                onClick = { showCustomLogPanel = !showCustomLogPanel },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentPrimary),
                border = BorderStroke(1.dp, AccentPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("toggle_custom_workout_pnl")
            ) {
                Icon(if (showCustomLogPanel) Icons.Default.Close else Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (showCustomLogPanel) "Закрыть ручной ввод" else "Залогировать уникальную тренировку",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (showCustomLogPanel) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Укажите параметры тренировки", fontWeight = FontWeight.Bold, color = TextPrimary)

                        OutlinedTextField(
                            value = customWorkoutName,
                            onValueChange = { customWorkoutName = it },
                            label = { Text("Название (например: бассейн)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_workout_name"),
                            singleLine = true
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = customWorkoutMinutes,
                                onValueChange = { customWorkoutMinutes = it },
                                label = { Text("Время (мин)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_workout_mins"),
                                singleLine = true
                            )

                            // Simple Category selector in manual input
                            Box(modifier = Modifier.weight(1f)) {
                                val categories = listOf("Cardio" to "Кардио", "Strength" to "Силовая", "Yoga" to "Йога", "Stretching" to "Растяжка")
                                var expanded by remember { mutableStateOf(false) }
                                
                                Button(
                                    onClick = { expanded = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(categories.find { it.first == customWorkoutCategory }?.second ?: "Тип")
                                }
                                
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.background(DarkSurface)
                                ) {
                                    categories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat.second, color = TextPrimary) },
                                            onClick = {
                                                customWorkoutCategory = cat.first
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val minutes = customWorkoutMinutes.toIntOrNull() ?: 30
                                val multiplier = when (customWorkoutCategory) {
                                    "Cardio" -> 9
                                    "Strength" -> 7
                                    "Yoga" -> 35
                                    else -> 5
                                }
                                val calBase = minutes * multiplier
                                if (customWorkoutName.isNotBlank()) {
                                    onAddWorkout(customWorkoutName, minutes, calBase, customWorkoutCategory)
                                    customWorkoutName = ""
                                    customWorkoutMinutes = ""
                                    showCustomLogPanel = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentSecondary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("submit_custom_workout"),
                            enabled = customWorkoutName.isNotBlank() && customWorkoutMinutes.isNotBlank()
                        ) {
                            Text("Добавить в дневник", fontWeight = FontWeight.Bold, color = DarkBackground)
                        }
                    }
                }
            }
        }

        // Section: History log
        item {
            Text(
                "История активности сегодня",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        if (workouts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Вы еще не делали тренировок сегодня", color = TextMuted, fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            items(workouts) { log ->
                LoggedWorkoutItem(log = log, onDelete = { onDeleteWorkout(log) })
            }
        }
    }
}

data class PresetWorkout(val name: String, val durationMinutes: Int, val calories: Int, val category: String)

@Composable
fun PresetWorkoutItem(preset: PresetWorkout, onLog: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (preset.category) {
                "Cardio" -> Icons.Default.DirectionsRun
                "Strength" -> Icons.Default.FitnessCenter
                "Yoga" -> Icons.Default.SelfImprovement
                else -> Icons.Default.Accessibility
            }
            val color = when (preset.category) {
                "Cardio" -> CardioRed
                "Strength" -> StrengthPurple
                "Yoga" -> StretchYellow
                else -> StepsMint
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f))
                    .padding(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(preset.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("${preset.durationMinutes} минут • -${preset.calories} ккал сожжено", fontSize = 12.sp, color = TextSecondary)
            }

            IconButton(
                onClick = onLog,
                modifier = Modifier.testTag("add_preset_workout_${preset.name.replace(" ", "_").lowercase()}")
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = "Добавить тренировку", tint = AccentPrimary, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
fun LoggedWorkoutItem(log: WorkoutLog, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (log.category) {
                "Cardio" -> Icons.Default.DirectionsRun
                "Strength" -> Icons.Default.FitnessCenter
                "Yoga" -> Icons.Default.SelfImprovement
                else -> Icons.Default.DirectionsWalk
            }

            Icon(
                icon,
                contentDescription = null,
                tint = AccentPrimary,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 8.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(log.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("${log.durationMinutes} минут • сожжено ${log.caloriesBurned} ккал", fontSize = 12.sp, color = TextSecondary)
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_workout_btn")
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = AccentTertiary)
            }
        }
    }
}

// Active workout session overlay with ticks
@Composable
fun ActiveWorkoutSessionDialog(
    workoutName: String,
    category: String,
    onFinishWorkout: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var secondsElapsed by remember { mutableStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }

    LaunchedEffect(isPaused) {
        while (!isPaused) {
            delay(1000)
            secondsElapsed++
        }
    }

    // Dynamic glowing pulsing circle configuration
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (!isPaused) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Dialog(onDismissRequest = { /* Force explicit cancel */ }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Активная сессия",
                    fontSize = 14.sp,
                    color = AccentPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    workoutName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Beautiful Concentric Pulsing Timer Indicator
                Box(
                    modifier = Modifier.size(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulsing Glow halo
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .drawBehind {
                                drawCircle(
                                    color = if (category == "Cardio") CardioRed.copy(alpha = 0.15f) else StrengthPurple.copy(alpha = 0.15f),
                                    radius = (size.width / 2f) * if (!isPaused) pulseScale else 1f
                                )
                            }
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 8.dp.toPx()
                        val col = if (category == "Cardio") CardioRed else StrengthPurple
                        drawCircle(
                            color = DarkCard,
                            style = Stroke(width = strokeWidth)
                        )
                        // Elapsed arc representation (e.g. 60 seconds progress)
                        drawArc(
                            color = col,
                            startAngle = -90f,
                            sweepAngle = (secondsElapsed % 60) * 6f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // Ticking timer formatter MM:SS
                    val minutes = secondsElapsed / 60
                    val seconds = secondsElapsed % 60
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { isPaused = !isPaused },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pause_timer_btn")
                    ) {
                        Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null, tint = TextPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPaused) "Продолжить" else "Пауза", color = TextPrimary)
                    }

                    Button(
                        onClick = { onFinishWorkout(secondsElapsed) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentSecondary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("finish_workout_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = DarkBackground)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Завершить", fontWeight = FontWeight.Bold, color = DarkBackground)
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = TextMuted),
                    modifier = Modifier.testTag("cancel_session_btn")
                ) {
                    Text("Отменить сессию")
                }
            }
        }
    }
}

// ==========================================
// 3. DIET AND WATER SCREEN (VODA I EDA)
// ==========================================
@Composable
fun DietAndWaterScreen(
    stats: DailyStats,
    foods: List<FoodLog>,
    onAddWater: (Int) -> Unit,
    onAddFood: (String, Int, String) -> Unit,
    onDeleteFood: (FoodLog) -> Unit
) {
    var foodName by remember { mutableStateOf("") }
    var foodCalories by remember { mutableStateOf("") }
    var foodMealType by remember { mutableStateOf("Breakfast") } // Breakfast, Lunch, Dinner, Snack
    var showFoodLoggingForm by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val waterFraction = remember(stats.waterMl, stats.waterGoalMl) {
        if (stats.waterGoalMl > 0) minOf(1f, stats.waterMl.toFloat() / stats.waterGoalMl) else 0f
    }
    val animatedWaterFill by animateFloatAsState(
        targetValue = waterFraction,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "WaterFractionHeightProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Питание и Гидратация",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        // WaveHydrationCard (Smooth, undulating Sine waves inside Canvas)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Баланс воды в организме", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Выпито: ${stats.waterMl} / ${stats.waterGoalMl} мл", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 12.dp))

                // Smooth loop-animated wave container
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        .background(DarkBackground),
                    contentAlignment = Alignment.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition()
                    // Wave shifts left/right
                    val waveShift by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 2 * Math.PI.toFloat(),
                        animationSpec = infiniteRepeatable(
                            animation = tween(2500, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        )
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val waterHeight = size.height * animatedWaterFill
                        val waterY = size.height - waterHeight

                        val path = Path()
                        path.moveTo(0f, size.height)
                        path.lineTo(0f, waterY)

                        // Compute cubic Bezier or sine waves along the screen width
                        val steps = 40
                        val stepWidth = size.width / steps
                        for (i in 0..steps) {
                            val x = i * stepWidth
                            val angle = (x / size.width) * 3 * Math.PI + waveShift
                            val y = waterY + Math.sin(angle).toFloat() * 10.dp.toPx()
                            path.lineTo(x, y)
                        }
                        path.lineTo(size.width, size.height)
                        path.close()

                        // Fill canvas with wave content gradient
                        drawPath(
                            path = path,
                            brush = Brush.verticalGradient(
                                colors = listOf(WaterBlue, WaterBlue.copy(alpha = 0.6f))
                            )
                        )
                    }

                    // Height level percentages overlay text
                    Text(
                        "${(waterFraction * 100).toInt()}%",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onAddWater(250) },
                        colors = ButtonDefaults.buttonColors(containerColor = WaterBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("add_250_btn")
                    ) {
                        Icon(Icons.Default.LocalDrink, contentDescription = null, tint = TextPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+250 мл", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onAddWater(500) },
                        colors = ButtonDefaults.buttonColors(containerColor = WaterBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("add_500_btn")
                    ) {
                        Icon(Icons.Default.LocalDrink, contentDescription = null, tint = TextPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+500 мл", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section: Calorie consumption progress indicator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Дневной рацион питания", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Калорийность рациона", fontSize = 13.sp, color = TextSecondary)
                    Text("${stats.caloriesConsumed} ккал", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CalorieOrange)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Modern Linear Progress Indicator with dynamic limits
                val targetLimit = 2200
                val ratio = minOf(1f, stats.caloriesConsumed.toFloat() / targetLimit)
                val animatedRatio by animateFloatAsState(targetValue = ratio)
                
                LinearProgressIndicator(
                    progress = { animatedRatio },
                    color = CalorieOrange,
                    trackColor = DarkCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Рекомендуемая норма: $targetLimit ккал", fontSize = 11.sp, color = TextMuted)
            }
        }

        // Add custom food toggle buttons
        OutlinedButton(
            onClick = { showFoodLoggingForm = !showFoodLoggingForm },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentPrimary),
            border = BorderStroke(1.dp, AccentPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("toggle_food_form_btn")
        ) {
            Icon(if (showFoodLoggingForm) Icons.Default.Close else Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (showFoodLoggingForm) "Закрыть ручной ввод" else "Залогировать съеденное блюдо", fontWeight = FontWeight.Bold)
        }

        if (showFoodLoggingForm) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Добавление приема пищи", fontWeight = FontWeight.Bold, color = TextPrimary)

                    OutlinedTextField(
                        value = foodName,
                        onValueChange = { foodName = it },
                        label = { Text("Название (например: банан)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("food_name_input"),
                        singleLine = true
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = foodCalories,
                            onValueChange = { foodCalories = it },
                            label = { Text("Калории (ккал)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("food_calories_input"),
                            singleLine = true
                        )

                        // Meal Category Dropdown selector
                        Box(modifier = Modifier.weight(1f)) {
                            val meals = listOf("Breakfast" to "Завтрак", "Lunch" to "Обед", "Dinner" to "Ужин", "Snack" to "Перекус")
                            var expanded by remember { mutableStateOf(false) }

                            Button(
                                onClick = { expanded = true },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(meals.find { it.first == foodMealType }?.second ?: "Тип")
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(DarkSurface)
                            ) {
                                meals.forEach { m ->
                                        DropdownMenuItem(
                                            text = { Text(m.second, color = TextPrimary) },
                                            onClick = {
                                                foodMealType = m.first
                                                expanded = false
                                            }
                                        )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val kcal = foodCalories.toIntOrNull() ?: 100
                            if (foodName.isNotBlank()) {
                                onAddFood(foodName, kcal, foodMealType)
                                foodName = ""
                                foodCalories = ""
                                showFoodLoggingForm = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentSecondary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_food_btn"),
                        enabled = foodName.isNotBlank() && foodCalories.isNotBlank()
                    ) {
                        Text("Добавить прием пищи", fontWeight = FontWeight.Bold, color = DarkBackground)
                    }
                }
            }
        }

        // Meal logging history List
        Text("Блюда, залогированные сегодня", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        if (foods.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.RestaurantMenu, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Сегодня вы еще не добавляли еду", color = TextMuted, fontSize = 13.sp)
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                foods.forEach { food ->
                    LoggedFoodItem(food = food, onDelete = { onDeleteFood(food) })
                }
            }
        }
    }
}

@Composable
fun LoggedFoodItem(food: FoodLog, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val indicatorText = when (food.mealType) {
                "Breakfast" -> "Завтрак"
                "Lunch" -> "Обед"
                "Dinner" -> "Ужин"
                else -> "Перекус"
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CalorieOrange.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(indicatorText, color = CalorieOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(food.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("+${food.calories} ккал", fontSize = 12.sp, color = TextSecondary)
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_food_btn")
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Улить", tint = AccentTertiary)
            }
        }
    }
}

// ==========================================
// 4. AI FIT COACH SCREEN (AI KOUCH)
// ==========================================
@Composable
fun CoachScreen(
    aiAdvice: String,
    aiLoading: Boolean,
    onAskCoach: (String?) -> Unit
) {
    var questionText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1450, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Персональный AI-Тренер",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        // Banner Avatar Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pulsing AI Brain avatar
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(AccentPrimary.copy(alpha = if (aiLoading) pulseAlpha else 0.15f))
                    )
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AccentPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text("Ваш FitLife AI Ассистент", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        "Задавайте любые вопросы по диетам, упражнениям или попросите проанализировать вашу активность.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Text input question
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Задать уникальный вопрос", fontWeight = FontWeight.Bold, color = TextPrimary)

                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    placeholder = { Text("Например: Как питаться для набора мышц?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("coach_question_input"),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onAskCoach(null)
                            focusManager.clearFocus()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ask_preset_analysis")
                    ) {
                        Text("Анализ дня", color = TextPrimary)
                    }

                    Button(
                        onClick = {
                            if (questionText.isNotBlank()) {
                                onAskCoach(questionText)
                                questionText = ""
                                focusManager.clearFocus()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("submit_coach_question"),
                        enabled = questionText.isNotBlank() && !aiLoading
                    ) {
                        Text("Спросить", fontWeight = FontWeight.Bold, color = DarkBackground)
                    }
                }
            }
        }

        // Advice Result Container
        if (aiAdvice.isNotBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("coach_advice_container"),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Советы фитнес-коуча", fontWeight = FontWeight.Bold, color = AccentSecondary)
                        if (aiLoading) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp), color = AccentSecondary)
                        } else {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = AccentSecondary, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Markdown-styled text block
                    Text(
                        text = aiAdvice,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 22.sp
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SmartToy, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Нажмите 'Анализ дня' или напишите свой вопрос тренеру для получения полезных диетических отчетов.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// Dialog: Configure goal targets
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalConfigDialog(
    currentStepGoal: Int,
    currentWaterGoal: Int,
    onSave: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var stepText by remember { mutableStateOf(currentStepGoal.toString()) }
    var waterText by remember { mutableStateOf(currentWaterGoal.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Настройка дневных целей", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                OutlinedTextField(
                    value = stepText,
                    onValueChange = { stepText = it },
                    label = { Text("Цель по шагам") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("step_goal_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = waterText,
                    onValueChange = { waterText = it },
                    label = { Text("Цель по гидратации (мл)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("water_goal_input"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("dismiss_goal_btn")) {
                        Text("Отмена", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val steps = stepText.toIntOrNull() ?: currentStepGoal
                            val water = waterText.toIntOrNull() ?: currentWaterGoal
                            onSave(steps, water)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                        modifier = Modifier.testTag("save_goal_btn")
                    ) {
                        Text("Сохранить", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
