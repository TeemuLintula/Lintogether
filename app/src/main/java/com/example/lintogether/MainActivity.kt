package com.example.lintogether

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.lintogether.ui.theme.LintogetherTheme
import com.example.lintogether.sub_functions.CalendarScreen
import com.example.lintogether.sub_functions.StatsScreen
import com.example.lintogether.sub_functions.GymScreen
import com.example.lintogether.sub_functions.NotesScreen
import com.example.lintogether.sub_functions.GoalsScreen
import com.example.lintogether.sub_functions.MoneyScreen
import com.example.lintogether.sub_functions.Goal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.lintogether.api.RetrofitInstance
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Date
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val db by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "tasks-database"
        )
            .addMigrations(
                MIGRATION_5_8, MIGRATION_6_8, MIGRATION_7_8, MIGRATION_8_9,
                MIGRATION_9_10, MIGRATION_10_11, MIGRATION_9_11, MIGRATION_8_11, 
                MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15,
                MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19,
                MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22
            )
            .build()
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        } else {
            // Permission denied
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val taskDao = db.taskDao()

        // Initialize Notification Channel
        NotificationHelper.createNotificationChannel(this)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        scheduleGoalReminders()
        scheduleRelapseReminders()

        lifecycleScope.launch {
            val today = Calendar.getInstance()
            val todayStr = dateFormat.format(today.time)
            
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val lastResetDate = prefs.getString("last_reset_date", "")
            val isNewDay = lastResetDate != todayStr

            if (isNewDay) {
                prefs.edit().putString("last_reset_date", todayStr).apply()
            }
        }

        enableEdgeToEdge()
        setContent {
            LintogetherTheme {
                var showCalendar by remember { mutableStateOf(false) }

                if (showCalendar) {
                    BackHandler {
                        showCalendar = false
                    }
                    CalendarScreen(taskDao, lifecycleScope, onFlip = { showCalendar = false })
                } else {
                    val pagerState = rememberPagerState(initialPage = 2, pageCount = { 6 })
                    HorizontalPager(state = pagerState) { page ->
                        when (page) {
                            0 -> MoneyScreen()
                            1 -> GymScreen(taskDao)
                            2 -> MainScreen(taskDao, lifecycleScope, onShowCalendar = { showCalendar = true })
                            3 -> StatsScreen(taskDao)
                            4 -> NotesScreen(taskDao)
                            5 -> GoalsScreen(taskDao)
                        }
                    }
                }
            }
        }
    }

    private fun scheduleGoalReminders() {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }

        val initialDelay = dueDate.timeInMillis - currentDate.timeInMillis

        val reminderRequest = PeriodicWorkRequestBuilder<GoalReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "goal_reminder_work",
            ExistingPeriodicWorkPolicy.UPDATE,
            reminderRequest
        )
    }

    private fun scheduleRelapseReminders() {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }

        val initialDelay = dueDate.timeInMillis - currentDate.timeInMillis

        val reminderRequest = PeriodicWorkRequestBuilder<RelapseReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "relapse_reminder_work",
            ExistingPeriodicWorkPolicy.UPDATE,
            reminderRequest
        )
    }
}

@Composable
fun MainScreen(taskDao: TaskDao, coroutineScope: CoroutineScope, onShowCalendar: () -> Unit) {
    var showAllTasks by remember { mutableStateOf(false) }
    var showTaskEditor by remember { mutableStateOf(false) }
    val tasks by taskDao.getAllTasks().collectAsState(initial = emptyList())
    val goals by taskDao.getAllGoals().collectAsState(initial = emptyList())
    var selectedTaskIndex by remember { mutableIntStateOf(-1) }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val calendar = Calendar.getInstance()
    val todayStr = dateFormat.format(calendar.time)
    val currentWeekDay = when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> WeekDay.Mon
        Calendar.TUESDAY -> WeekDay.Tue
        Calendar.WEDNESDAY -> WeekDay.Wed
        Calendar.THURSDAY -> WeekDay.Thu
        Calendar.FRIDAY -> WeekDay.Fri
        Calendar.SATURDAY -> WeekDay.Sat
        Calendar.SUNDAY -> WeekDay.Sun
        else -> WeekDay.Mon
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    var listOffset by remember { mutableStateOf(160.dp) }

    val displayTasks = if (showAllTasks) {
        tasks.sortedByDescending { it.priority }
    } else {
        tasks.filter { task ->
            if (task.isRelapseTracker) return@filter false
            val isActiveOnDay = if (task.repeatUnit == RepeatUnit.WEEK) {
                task.activeDays.contains(currentWeekDay)
            } else {
                task.activeMonthDays.contains(calendar.get(Calendar.DAY_OF_MONTH))
            }
            isActiveOnDay && !task.completedDates.contains(todayStr) && isTaskActiveOnWeek(task, calendar, dateFormat) && !task.skippedDates.contains(todayStr)
        }.sortedByDescending { it.priority }
    }

    val closestGoal = remember(goals) {
        val now = System.currentTimeMillis()
        goals.filter { !it.isCompleted && it.deadline != null }
            .mapNotNull { goal ->
                try {
                    val deadlineDate = dateFormat.parse(goal.deadline!!)
                    if (deadlineDate != null) goal to deadlineDate.time else null
                } catch (e: Exception) {
                    null
                }
            }
            .filter { it.second >= now - 86400000 } // Include goals whose deadline is today or in the future
            .minByOrNull { it.second }?.first
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color(0xFF121212),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onShowCalendar,
                    containerColor = Color.White,
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Switch to Calendar")
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                Greeting()
                Spacer(modifier = Modifier.height(24.dp))
                // Use a Box to allow TODOLIST to overlap Quote
                Box(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Quote()
                        if (closestGoal != null) {
                            Spacer(modifier = Modifier.height(48.dp))
                            Text(
                                text = "The goal: ${closestGoal.name}",
                                fontSize = 22.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    TODOLIST(
                        tasks = displayTasks,
                        dateStr = todayStr,
                        showAllTasks = showAllTasks,
                        onShowAllTasksChange = { showAllTasks = it },
                        showCompletedToggle = !showAllTasks,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = listOffset),
                        onHeaderDrag = { delta ->
                            listOffset = (listOffset + with(density) { delta.toDp() }).coerceIn(0.dp, 600.dp)
                        },
                        onEditTask = { index ->
                            selectedTaskIndex = if (index == -1) -1 else tasks.indexOf(displayTasks[index])
                            showTaskEditor = true
                        },
                        onCompletedToggle = { index ->
                            val clickedTask = displayTasks[index]
                            val isNowCompleted = !clickedTask.completedDates.contains(todayStr)
                            val newCompletedDates = if (!isNowCompleted) clickedTask.completedDates - todayStr else clickedTask.completedDates + todayStr
                            
                            if (isNowCompleted) {
                                XpManager.addXp(context, 1)
                            } else {
                                XpManager.subtractXp(context, 1)
                            }

                            coroutineScope.launch {
                                taskDao.updateTask(clickedTask.copy(
                                    completedDates = newCompletedDates,
                                    skippedDates = if (isNowCompleted) clickedTask.skippedDates - todayStr else clickedTask.skippedDates
                                ))
                            }
                        },
                        onSkipToggle = { index ->
                            val clickedTask = displayTasks[index]
                            val isCurrentlySkipped = clickedTask.skippedDates.contains(todayStr)
                            val newSkippedDates = if (isCurrentlySkipped) clickedTask.skippedDates - todayStr else clickedTask.skippedDates + todayStr
                            coroutineScope.launch { 
                                taskDao.updateTask(clickedTask.copy(skippedDates = newSkippedDates)) 
                            }
                        }
                    )
                }
            }
        }
        if (showTaskEditor) {
            TaskEditor(
                task = tasks.getOrNull(selectedTaskIndex),
                initialDays = setOf(currentWeekDay),
                onSave = { updatedTask ->
                    coroutineScope.launch {
                        if (selectedTaskIndex == -1) taskDao.insertTask(updatedTask) else taskDao.updateTask(updatedTask)
                    }
                    showTaskEditor = false
                },
                onCancel = { taskToDelete ->
                    if (selectedTaskIndex != -1) coroutineScope.launch { taskDao.deleteTask(taskToDelete) }
                    showTaskEditor = false
                }
            )
        }
    }
}

@Composable
fun Greeting() {
    Text(
        text = "Welcome to\n\nLintogether",
        textAlign = TextAlign.Center,
        fontSize = 42.sp,
        fontWeight = FontWeight.Black,
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    )
}

@Composable
fun Quote() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time) }

    var quote by remember {
        mutableStateOf(prefs.getString("daily_quote", "Loading quote...") ?: "Loading quote...")
    }
    var author by remember {
        mutableStateOf(prefs.getString("daily_author", "") ?: "")
    }

    LaunchedEffect(Unit) {
        val lastQuoteDate = prefs.getString("last_quote_date", "")
        if (lastQuoteDate != todayStr) {
            try {
                val response = RetrofitInstance.api.getRandomQuote()
                val fetchedQuote = response[0].q
                val fetchedAuthor = response[0].a

                prefs.edit()
                    .putString("daily_quote", fetchedQuote)
                    .putString("daily_author", fetchedAuthor)
                    .putString("last_quote_date", todayStr)
                    .apply()

                quote = fetchedQuote
                author = fetchedAuthor
            } catch (e: Exception) {
                if (quote == "Loading quote...") {
                    quote = "Failed to load quote"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "\"$quote\"",
            textAlign = TextAlign.Center,
            fontSize = 22.sp,
            color = Color.White,
            fontStyle = FontStyle.Italic
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "- $author",
            fontSize = 16.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun TODOLIST(
    tasks: List<Task>,
    dateStr: String,
    showAllTasks: Boolean,
    onShowAllTasksChange: (Boolean) -> Unit,
    showCompletedToggle: Boolean = true,
    modifier: Modifier = Modifier,
    onHeaderDrag: (Float) -> Unit = {},
    onEditTask: (Int) -> Unit,
    onCompletedToggle: (Int) -> Unit,
    onSkipToggle: (Int) -> Unit
) {
    Box(
        modifier = modifier
            .padding(horizontal = 24.dp)
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .background(Color(0xFF1E1E1E))
    ) {
        Column {
            // Drag Handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            onHeaderDrag(dragAmount)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(6.6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.Gray.copy(alpha = 0.6f))
                )
            }

            // Show all tasks toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text("Show all tasks", fontSize = 14.sp, color = Color.White)
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = showAllTasks,
                    onCheckedChange = onShowAllTasksChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color.Cyan,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.DarkGray
                    )
                )
            }
            
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                itemsIndexed(tasks) { index, task ->
                    TaskItem(
                        task = task,
                        dateStr = dateStr,
                        isToday = true,
                        canToggle = showCompletedToggle,
                        onEditTask = { onEditTask(index) },
                        onCompletedToggle = { onCompletedToggle(index) },
                        onSkipToggle = { onSkipToggle(index) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF252525))
                            .clickable { onEditTask(-1) }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Add task", fontSize = 18.sp, color = Color.White)
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskItem(
    task: Task,
    dateStr: String,
    onEditTask: () -> Unit,
    onCompletedToggle: () -> Unit,
    onSkipToggle: () -> Unit,
    isToday: Boolean = false,
    canToggle: Boolean = true
) {
    val isCompleted = task.completedDates.contains(dateStr)
    val isSkipped = task.skippedDates.contains(dateStr)
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF252525))
            .combinedClickable(
                onClick = { onEditTask() },
                onLongClick = { if (!isCompleted && isToday) showMenu = true }
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (canToggle) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isCompleted) Color.Green else Color.Gray)
                        .clickable { onCompletedToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) Text("✔", fontSize = 14.sp, color = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
            Text(
                task.name,
                fontSize = 18.sp,
                color = Color.White,
                textDecoration = if (isSkipped && !isCompleted && isToday) TextDecoration.Underline else TextDecoration.None
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(if (isSkipped) "Unskip" else "Skip") },
                onClick = {
                    onSkipToggle()
                    showMenu = false
                }
            )
        }
    }
}

@Composable
fun TaskEditor(
    task: Task?,
    initialDays: Set<WeekDay> = emptySet(),
    onSave: (Task) -> Unit,
    onCancel: (Task) -> Unit,
) {
    val context = LocalContext.current
    var taskName by remember(task) { mutableStateOf(task?.name ?: "") }
    var selectedDays by remember(task) { mutableStateOf(task?.activeDays ?: initialDays) }
    var selectedMonthDays by remember(task) { mutableStateOf(task?.activeMonthDays ?: emptySet()) }
    var priority by remember(task) { mutableIntStateOf(task?.priority ?: 1) }
    var repeatInterval by remember(task) { mutableIntStateOf(task?.repeatInterval ?: 1) }
    var repeatUnit by remember(task) { mutableStateOf(task?.repeatUnit ?: RepeatUnit.WEEK) }
    var startDate by remember(task) {
        mutableStateOf(task?.startDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time))
    }

    BackHandler {
        if (taskName.isNotBlank() || task?.isRelapseTracker == true) {
            onSave(Task(
                id = task?.id ?: 0,
                name = if (task?.isRelapseTracker == true) task.name else taskName,
                activeDays = selectedDays,
                priority = priority,
                completedDates = task?.completedDates ?: emptySet(),
                skippedDates = task?.skippedDates ?: emptySet(),
                repeatInterval = repeatInterval,
                startDate = startDate,
                repeatUnit = repeatUnit,
                activeMonthDays = selectedMonthDays,
                skippedDay = task?.skippedDay ?: false,
                isRelapseTracker = task?.isRelapseTracker ?: false
            ))
        } else {
            onCancel(task ?: Task(name = taskName, activeDays = selectedDays))
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)).padding(24.dp)) {
        Column {
            Text(if (task == null) "New Task" else "Edit Task", fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            if (task?.isRelapseTracker == true) {
                Text("Relapse Tracker: ${task.name}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            } else {
                TextField(
                    value = taskName, 
                    onValueChange = { taskName = it }, 
                    label = { Text("Task name") }, 
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color.Cyan,
                        unfocusedLabelColor = Color.Gray
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (task?.isRelapseTracker != true) {
                Text("Priority", fontSize = 18.sp, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    (1..5).forEach { level ->
                        PriorityButton(level, priority == level) { priority = level }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                // Start Date Picker
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Starts on:", fontSize = 18.sp, color = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E1E1E))
                            .clickable {
                                val calendar = Calendar.getInstance()
                                try {
                                    val current = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(startDate)
                                    if (current != null) calendar.time = current
                                } catch (_: Exception) {}

                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val newDate = Calendar.getInstance()
                                        newDate.set(year, month, dayOfMonth)
                                        startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(newDate.time)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(text = startDate, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                val itemHeight = 48.dp
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Repeat", fontSize = 18.sp, color = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier.weight(1f).height(itemHeight).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1E1E1E)),
                        contentAlignment = Alignment.Center
                    ) {
                        val listState = rememberLazyListState(initialFirstVisibleItemIndex = repeatInterval)
                        val coroutineScope = rememberCoroutineScope()
                        LaunchedEffect(listState.isScrollInProgress) {
                            if (!listState.isScrollInProgress) { repeatInterval = listState.firstVisibleItemIndex }
                        }
                        LazyColumn(
                            state = listState,
                            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            items(11) { index ->
                                val unitStr = if (repeatUnit == RepeatUnit.WEEK) "week" else "month"
                                Box(modifier = Modifier.height(itemHeight).fillMaxWidth().clickable { coroutineScope.launch { listState.animateScrollToItem(index) } }, contentAlignment = Alignment.Center) {
                                    Text(
                                        text = when (index) {
                                            0 -> "does not repeat"
                                            1 -> "every $unitStr"
                                            else -> "every $index ${unitStr}s"
                                        },
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Repeat by:", fontSize = 18.sp, color = Color.White)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Week", modifier = Modifier.clickable { repeatUnit = RepeatUnit.WEEK }, color = if (repeatUnit == RepeatUnit.WEEK) Color.Cyan else Color.Gray, fontWeight = if (repeatUnit == RepeatUnit.WEEK) FontWeight.Bold else FontWeight.Normal)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Month", modifier = Modifier.clickable { repeatUnit = RepeatUnit.MONTH }, color = if (repeatUnit == RepeatUnit.MONTH) Color.Cyan else Color.Gray, fontWeight = if (repeatUnit == RepeatUnit.MONTH) FontWeight.Bold else FontWeight.Normal)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (repeatUnit == RepeatUnit.WEEK) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        WeekDay.entries.forEach { day ->
                            DayToggle(day, selectedDays.contains(day)) {
                                selectedDays = if (selectedDays.contains(day)) selectedDays - day else selectedDays + day
                            }
                        }
                    }
                } else {
                    Column {
                        (1..31).chunked(7).forEach { week ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                week.forEach { day ->
                                    DayNumberToggle(
                                        day,
                                        selectedMonthDays.contains(day)
                                    ) {
                                        selectedMonthDays = if (selectedMonthDays.contains(day)) selectedMonthDays - day else selectedMonthDays + day
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).clickable {
                    if (taskName.isNotBlank() || task?.isRelapseTracker == true) {
                        onSave(Task(
                            id = task?.id ?: 0,
                            name = if (task?.isRelapseTracker == true) task.name else taskName,
                            activeDays = selectedDays,
                            priority = priority,
                            completedDates = task?.completedDates ?: emptySet(),
                            skippedDates = task?.skippedDates ?: emptySet(),
                            repeatInterval = repeatInterval,
                            startDate = startDate,
                            repeatUnit = repeatUnit,
                            activeMonthDays = selectedMonthDays,
                            skippedDay = task?.skippedDay ?: false,
                            isRelapseTracker = task?.isRelapseTracker ?: false
                        ))
                    }
                }.padding(16.dp),
                contentAlignment = Alignment.Center
            ) { Text("Save Task", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(12.dp))
            Text(if (task == null) "Cancel" else "Delete Task", modifier = Modifier.align(Alignment.CenterHorizontally).clickable { onCancel(task ?: Task(name = taskName, activeDays = selectedDays)) }, color = Color.Gray)
        }
    }
}

@Composable
fun PriorityButton(level: Int, isSelected: Boolean, onSelect: () -> Unit) {
    Box(modifier = Modifier.size(45.dp).clip(RoundedCornerShape(8.dp)).background(if (isSelected) Color.Cyan else Color(0xFF1E1E1E)).clickable { onSelect() }, contentAlignment = Alignment.Center) {
        Text(text = level.toString(), color = if (isSelected) Color.Black else Color.White, fontSize = 16.sp)
    }
}

@Composable
fun DayToggle(day: WeekDay, selected: Boolean, onToggle: () -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (selected) Color.Cyan else Color(0xFF1E1E1E)).size(40.dp).clickable { onToggle() }, contentAlignment = Alignment.Center) {
        Text(text = day.name.take(1), color = if (selected) Color.Black else Color.White)
    }
}

@Composable
fun DayNumberToggle(day: Int, selected: Boolean, onToggle: () -> Unit) {
    Box(modifier = Modifier.padding(2.dp).clip(RoundedCornerShape(8.dp)).background(if (selected) Color.Cyan else Color(0xFF1E1E1E)).size(36.dp).clickable { onToggle() }, contentAlignment = Alignment.Center) {
        Text(text = day.toString(), color = if (selected) Color.Black else Color.White, fontSize = 12.sp)
    }
}
