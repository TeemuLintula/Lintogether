package com.example.lintogether.sub_functions

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lintogether.Task
import com.example.lintogether.TaskDao
import com.example.lintogether.WeekDay
import com.example.lintogether.isTaskActiveOnWeek
import com.example.lintogether.RepeatUnit
import com.example.lintogether.XpManager
import com.example.lintogether.AllAchievements
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatsScreen(taskDao: TaskDao) {
    val tasks by taskDao.getAllTasks().collectAsState(initial = emptyList())
    val reflections by taskDao.getAllReflections().collectAsState(initial = emptyList())
    val goals by taskDao.getAllGoals().collectAsState(initial = emptyList())
    val gymExercises by taskDao.getAllGymExercises().collectAsState(initial = emptyList())
    
    var showRelapseDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val context = LocalContext.current
    
    var totalXp by remember { mutableIntStateOf(XpManager.getTotalXp(context)) }
    var unlockedAchievements by remember { mutableStateOf(XpManager.getUnlockedAchievements(context)) }
    val levelInfo = XpManager.getLevelInfo(totalXp)

    // Achievement logic
    LaunchedEffect(tasks, reflections, goals, gymExercises) {
        // Streak achievements
        val completedStreak = calculateCompletedDaysStreak(tasks)
        if (completedStreak >= 1) XpManager.unlockAchievement(context, "getting_started")
        if (completedStreak >= 7) XpManager.unlockAchievement(context, "real_commitment")
        if (completedStreak >= 30) XpManager.unlockAchievement(context, "monk_mode")
        if (completedStreak >= 90) XpManager.unlockAchievement(context, "actual_god")

        // Relapse achievements
        val maxRelapseStreak = tasks.filter { it.isRelapseTracker }
            .maxOfOrNull { calculateCurrentStreak(it) } ?: 0
            
        if (maxRelapseStreak >= 7) XpManager.unlockAchievement(context, "getting_better")
        if (maxRelapseStreak >= 30) XpManager.unlockAchievement(context, "getting_control")
        if (maxRelapseStreak >= 90) XpManager.unlockAchievement(context, "absolute_control_of_mind")

        // Net Worth achievements
        val moneyPrefs = context.getSharedPreferences("money_prefs", Context.MODE_PRIVATE)
        val cashVal = (moneyPrefs.getString("cash", "0") ?: "0").toDoubleOrNull() ?: 0.0
        val invVal = (moneyPrefs.getString("investments", "0") ?: "0").toDoubleOrNull() ?: 0.0
        val loansVal = (moneyPrefs.getString("loans", "0") ?: "0").toDoubleOrNull() ?: 0.0
        val otherVal = (moneyPrefs.getString("other", "0") ?: "0").toDoubleOrNull() ?: 0.0
        val netWorth = cashVal + invVal + otherVal - loansVal

        if (netWorth >= 50000) XpManager.unlockAchievement(context, "big_money")
        if (netWorth >= 100000) XpManager.unlockAchievement(context, "the_first_hundred")
        if (netWorth >= 200000) XpManager.unlockAchievement(context, "net_worth_200k")
        if (netWorth >= 500000) XpManager.unlockAchievement(context, "half_way_there")
        if (netWorth >= 1000000) XpManager.unlockAchievement(context, "one_million")

        // Reflection achievements
        if (reflections.isNotEmpty()) XpManager.unlockAchievement(context, "self_aware")
        if (reflections.size >= 10) XpManager.unlockAchievement(context, "deep_thinker")

        // Goal achievements
        val majorGoals = goals.filter { it.type == GoalType.MAIN }
        if (majorGoals.isNotEmpty()) XpManager.unlockAchievement(context, "visionary")
        if (majorGoals.any { it.isCompleted }) XpManager.unlockAchievement(context, "achiever")
        
        val hasNewYearGoal = goals.any { goal ->
            val cal = Calendar.getInstance().apply { timeInMillis = goal.timestamp }
            cal.get(Calendar.MONTH) == Calendar.JANUARY && cal.get(Calendar.DAY_OF_MONTH) == 1
        }
        if (hasNewYearGoal) XpManager.unlockAchievement(context, "new_years_promise")

        // Individual Task streak achievements
        val maxTaskStreak = tasks.filter { !it.isRelapseTracker }
            .maxOfOrNull { calculateCurrentStreak(it) } ?: 0
        if (maxTaskStreak >= 100) XpManager.unlockAchievement(context, "routine_is_set")
        if (maxTaskStreak >= 365) XpManager.unlockAchievement(context, "every_single_day")

        // Gym achievements
        var maxWeightIncrease = 0.0
        gymExercises.forEach { exercise ->
            if (exercise.weightHistory.size >= 2) {
                val weights = exercise.weightHistory.map { it.weight }
                val startWeight = weights.first()
                if (startWeight > 0) {
                    val currentMax = weights.maxOrNull() ?: 0.0
                    val increase = (currentMax - startWeight) / startWeight
                    if (increase > maxWeightIncrease) maxWeightIncrease = increase
                }
            }
        }
        
        if (maxWeightIncrease >= 0.1) XpManager.unlockAchievement(context, "good_gains")
        if (maxWeightIncrease >= 0.5) XpManager.unlockAchievement(context, "gym_rat")
        if (maxWeightIncrease >= 1.0) XpManager.unlockAchievement(context, "hulk")

        // Refresh UI state
        totalXp = XpManager.getTotalXp(context)
        unlockedAchievements = XpManager.getUnlockedAchievements(context)
    }

    val filteredAndSortedTasks = remember(tasks) {
        tasks.filter { (it.repeatInterval > 0 || it.isRelapseTracker) && it.repeatUnit != RepeatUnit.MONTH }
            .map { task ->
                task to calculateCurrentStreak(task)
            }.sortedByDescending { it.second }.map { it.first }
    }

    Scaffold(
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "STATISTICS",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            XpProgressBar(levelInfo)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(modifier = Modifier.weight(1f)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp, start = 8.dp, end = 8.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        CompletedDayCard(tasks)
                    }
                    items(filteredAndSortedTasks) { task ->
                        StatsCard(
                            task = task,
                            onReset = {
                                val todayStr = dateFormat.format(Date())
                                coroutineScope.launch {
                                    taskDao.updateTask(task.copy(
                                        completedDates = task.completedDates + todayStr
                                    ))
                                }
                            },
                            onRemove = {
                                coroutineScope.launch {
                                    taskDao.deleteTask(task)
                                }
                            }
                        )
                    }
                    
                    // Achievements section
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                        Column(modifier = Modifier.padding(vertical = 16.dp)) {
                            Text(
                                text = "Achievements",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            AllAchievements.forEach { achievement ->
                                AchievementItem(
                                    achievement = achievement,
                                    isUnlocked = unlockedAchievements.contains(achievement.id)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
                
                Button(
                    onClick = { showRelapseDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Text("Add relapse tracker", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showRelapseDialog) {
        var trackerName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRelapseDialog = false },
            containerColor = Color(0xFF1E1E1E),
            title = { Text("New Relapse Tracker", color = Color.White) },
            text = {
                TextField(
                    value = trackerName,
                    onValueChange = { trackerName = it },
                    label = { Text("Tracker Name") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF252525),
                        unfocusedContainerColor = Color(0xFF252525),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (trackerName.isNotBlank()) {
                            coroutineScope.launch {
                                taskDao.insertTask(Task(
                                    name = trackerName,
                                    activeDays = emptySet(),
                                    repeatInterval = 1,
                                    isRelapseTracker = true,
                                    startDate = dateFormat.format(Date())
                                ))
                            }
                            showRelapseDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) { Text("Add") }
            },
            dismissButton = {
                Button(
                    onClick = { showRelapseDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White)
                ) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AchievementItem(achievement: com.example.lintogether.Achievement, isUnlocked: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) Color(0xFF1E1E1E) else Color(0xFF252525).copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isUnlocked) Color.LightGray else Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isUnlocked) "🏆" else "🔒",
                    fontSize = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) Color.White else Color.Gray
                )
                Text(
                    text = achievement.description,
                    fontSize = 12.sp,
                    color = if (isUnlocked) Color.LightGray else Color.DarkGray
                )
            }
            if (isUnlocked) {
                Text(
                    text = "+${achievement.xpReward} XP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Cyan
                )
            }
        }
    }
}

@Composable
fun XpProgressBar(levelInfo: com.example.lintogether.LevelInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "Level ${levelInfo.level}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "${levelInfo.currentLevelXp.toInt()} / ${levelInfo.nextLevelXp.toInt()} XP",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { levelInfo.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = Color.Cyan,
            trackColor = Color(0xFF1E1E1E),
        )
    }
}

@Composable
fun CompletedDayCard(tasks: List<Task>) {
    val streak = calculateCompletedDaysStreak(tasks)
    val highScore = calculateCompletedDaysHighScore(tasks)

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = streak.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Completed Days",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                lineHeight = 11.sp,
                textAlign = TextAlign.Center,
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "high score: $highScore",
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatsCard(
    task: Task, 
    onReset: () -> Unit = {}, 
    onRemove: () -> Unit = {}
) {
    val streak = calculateCurrentStreak(task)
    val highScore = calculateHighScore(task)
    var showActionDialog by remember { mutableStateOf(false) }
    
    val nameFontSize = when {
        task.name.length > 25 -> 7.sp
        task.name.length > 18 -> 8.sp
        task.name.length > 12 -> 10.sp
        else -> 12.sp
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (task.isRelapseTracker) Color(0xFF1E1E1E) else Color(0xFF252525))
            .combinedClickable(
                onClick = { },
                onLongClick = { if (task.isRelapseTracker) showActionDialog = true }
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = streak.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (task.isRelapseTracker) Color.Cyan else Color.White
            )
            Text(
                text = task.name,
                fontSize = nameFontSize,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                minLines = 2,
                lineHeight = nameFontSize * 1.1f,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = Color.White
            )
            Text(
                text = "high score: $highScore",
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }

    if (showActionDialog) {
        AlertDialog(
            onDismissRequest = { showActionDialog = false },
            containerColor = Color(0xFF1E1E1E),
            title = { Text(task.name, color = Color.White) },
            confirmButton = {
                Button(
                    onClick = {
                        onReset()
                        showActionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) { Text("Reset") }
            },
            dismissButton = {
                Button(
                    onClick = {
                        onRemove()
                        showActionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
                ) { Text("Remove") }
            }
        )
    }
}

private fun isTaskActiveOnDate(task: Task, date: Calendar, dateFormat: SimpleDateFormat): Boolean {
    if (task.isRelapseTracker) return true
    
    val weekDay = when (date.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> WeekDay.Mon
        Calendar.TUESDAY -> WeekDay.Tue
        Calendar.WEDNESDAY -> WeekDay.Wed
        Calendar.THURSDAY -> WeekDay.Thu
        Calendar.FRIDAY -> WeekDay.Fri
        Calendar.SATURDAY -> WeekDay.Sat
        Calendar.SUNDAY -> WeekDay.Sun
        else -> WeekDay.Mon
    }
    
    val isActiveOnDay = if (task.repeatUnit == RepeatUnit.WEEK) {
        task.activeDays.contains(weekDay)
    } else {
        task.activeMonthDays.contains(date.get(Calendar.DAY_OF_MONTH))
    }
    
    return isActiveOnDay && isTaskActiveOnWeek(task, date, dateFormat)
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

fun calculateCurrentStreak(task: Task): Int {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val today = cal.clone() as Calendar
    
    val startLimit = if (task.startDate.isNotEmpty()) {
        try {
            Calendar.getInstance().apply { 
                time = dateFormat.parse(task.startDate)!! 
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } catch (_: Exception) {
            null
        }
    } else null

    var streak = 0
    var daysChecked = 0
    
    while (daysChecked < 1000) {
        if (startLimit != null && cal.before(startLimit)) break
        
        val dateStr = dateFormat.format(cal.time)
        
        if (task.isRelapseTracker) {
            if (task.completedDates.contains(dateStr)) {
                if (isSameDay(cal, today)) return 0
                break
            } else {
                if (!isSameDay(cal, today)) {
                    streak++
                }
            }
        } else {
            val isCompleted = task.completedDates.contains(dateStr)
            val isSkipped = task.skippedDates.contains(dateStr)
            val isActive = isTaskActiveOnDate(task, cal, dateFormat)
            
            if (isCompleted) {
                streak++
            } else if (isActive && !isSkipped) {
                if (!isSameDay(cal, today)) {
                    break
                }
            }
        }
        
        cal.add(Calendar.DAY_OF_YEAR, -1)
        daysChecked++
        
        if (cal.get(Calendar.YEAR) < 2000) break
    }
    return streak
}

fun calculateHighScore(task: Task): Int {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sortedDates = task.completedDates.mapNotNull { 
        try { dateFormat.parse(it) } catch (_: Exception) { null }
    }.sorted()
    
    val startLimit = if (task.startDate.isNotEmpty()) {
        try {
            Calendar.getInstance().apply { 
                time = dateFormat.parse(task.startDate)!! 
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } catch (_: Exception) {
            null
        }
    } else if (!task.isRelapseTracker && sortedDates.isNotEmpty()) {
        Calendar.getInstance().apply { time = sortedDates.first() }
    } else null

    if (startLimit == null) return 0
    
    val cal = startLimit.clone() as Calendar
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    var maxStreak = 0
    var currentStreak = 0
    
    while (!cal.after(today)) {
        val dateStr = dateFormat.format(cal.time)
        
        if (task.isRelapseTracker) {
            if (task.completedDates.contains(dateStr)) {
                currentStreak = 0
            } else {
                if (!isSameDay(cal, today)) {
                    currentStreak++
                    maxStreak = maxOf(maxStreak, currentStreak)
                }
            }
        } else {
            val isCompleted = task.completedDates.contains(dateStr)
            val isSkipped = task.skippedDates.contains(dateStr)
            val isActive = isTaskActiveOnDate(task, cal, dateFormat)
            
            if (isCompleted) {
                currentStreak++
                maxStreak = maxOf(maxStreak, currentStreak)
            } else if (isActive && !isSkipped) {
                if (!isSameDay(cal, today)) {
                    currentStreak = 0
                }
            }
        }
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    
    return maxStreak
}

private fun isDayFullyCompleted(tasks: List<Task>, date: Calendar, dateFormat: SimpleDateFormat): Boolean {
    val dateStr = dateFormat.format(date.time)
    var activeTasksCount = 0
    var completedOrSkippedCount = 0
    var hasRegularTasks = false
    
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val isToday = isSameDay(date, today)

    // Filter out monthly tasks
    val relevantTasks = tasks.filter { it.repeatUnit != RepeatUnit.MONTH }

    for (task in relevantTasks) {
        if (isTaskActiveOnDate(task, date, dateFormat)) {
            activeTasksCount++
            if (!task.isRelapseTracker) {
                hasRegularTasks = true
                if (task.completedDates.contains(dateStr) || task.skippedDates.contains(dateStr)) {
                    completedOrSkippedCount++
                }
            } else {
                if (!task.completedDates.contains(dateStr)) {
                    // Only count today as completed for a relapse tracker if there are also 
                    // regular tasks being tracked. This prevents "auto-completing" 
                    // the whole day when only relapse trackers exist and it's currently today.
                    if (!isToday || hasRegularTasks) {
                        completedOrSkippedCount++
                    }
                }
            }
        }
    }
    
    if (activeTasksCount == 0) return false
    if (isToday && !hasRegularTasks) return false

    return activeTasksCount == completedOrSkippedCount
}

private fun hasActiveTasksOnDate(tasks: List<Task>, date: Calendar, dateFormat: SimpleDateFormat): Boolean {
    return tasks.filter { it.repeatUnit != RepeatUnit.MONTH }.any { isTaskActiveOnDate(it, date, dateFormat) }
}

fun calculateCompletedDaysStreak(tasks: List<Task>): Int {
    if (tasks.isEmpty()) return 0
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val today = cal.clone() as Calendar

    var streak = 0
    var daysChecked = 0
    
    while (daysChecked < 1000) {
        if (isDayFullyCompleted(tasks, cal, dateFormat)) {
            streak++
        } else if (hasActiveTasksOnDate(tasks, cal, dateFormat)) {
            if (!isSameDay(cal, today)) {
                break
            }
        }
        
        cal.add(Calendar.DAY_OF_YEAR, -1)
        daysChecked++
        if (cal.get(Calendar.YEAR) < 2000) break
    }
    return streak
}

fun calculateCompletedDaysHighScore(tasks: List<Task>): Int {
    if (tasks.isEmpty()) return 0
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    // Use only relevant tasks for start date calculation if we want the high score to be consistent
    val relevantTasksForStartDate = tasks.filter { it.repeatUnit != RepeatUnit.MONTH }
    val allStartDates = relevantTasksForStartDate.mapNotNull {
        try { dateFormat.parse(it.startDate) } catch (_: Exception) { null }
    }
    if (allStartDates.isEmpty()) return 0
    
    val cal = Calendar.getInstance().apply {
        time = allStartDates.minOrNull() ?: Date()
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    var maxStreak = 0
    var currentStreak = 0
    
    while (!cal.after(today)) {
        if (isDayFullyCompleted(tasks, cal, dateFormat)) {
            currentStreak++
            maxStreak = maxOf(maxStreak, currentStreak)
        } else if (hasActiveTasksOnDate(tasks, cal, dateFormat)) {
            if (!isSameDay(cal, today)) {
                currentStreak = 0
            }
        }
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    
    return maxStreak
}
