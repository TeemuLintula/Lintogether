package com.example.lintogether.sub_functions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lintogether.RepeatUnit
import com.example.lintogether.Task
import com.example.lintogether.TaskDao
import com.example.lintogether.TaskEditor
import com.example.lintogether.TaskItem
import com.example.lintogether.WeekDay
import com.example.lintogether.XpManager
import com.example.lintogether.isTaskActiveOnWeek
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(taskDao: TaskDao, coroutineScope: CoroutineScope, onFlip: () -> Unit) {
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }
    val selectedDate = remember { mutableStateOf(Calendar.getInstance()) }
    val tasks by taskDao.getAllTasks().collectAsState(initial = emptyList())
    
    var showTaskEditor by remember { mutableStateOf(false) }
    var selectedTaskIndex by remember { mutableStateOf(-1) }

    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
    val year = calendar.get(Calendar.YEAR)

    val selectedDateStr = dateFormat.format(selectedDate.value.time)
    val today = Calendar.getInstance()
    val todayStr = dateFormat.format(today.time)
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val yesterdayStr = dateFormat.format(yesterday.time)

    val canToggle = selectedDateStr == todayStr || selectedDateStr == yesterdayStr

    val selectedWeekDay = when (selectedDate.value.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> WeekDay.Mon
        Calendar.TUESDAY -> WeekDay.Tue
        Calendar.WEDNESDAY -> WeekDay.Wed
        Calendar.THURSDAY -> WeekDay.Thu
        Calendar.FRIDAY -> WeekDay.Fri
        Calendar.SATURDAY -> WeekDay.Sat
        Calendar.SUNDAY -> WeekDay.Sun
        else -> WeekDay.Mon
    }

    val filteredTasks = tasks.filter { task ->
        val isActiveOnDay = if (task.repeatUnit == RepeatUnit.WEEK) {
            task.activeDays.contains(selectedWeekDay)
        } else {
            task.activeMonthDays.contains(selectedDate.value.get(Calendar.DAY_OF_MONTH))
        }
        
        isActiveOnDay && isTaskActiveOnWeek(task, selectedDate.value, dateFormat)
    }.sortedByDescending { it.priority }

    Scaffold(
        containerColor = Color(0xFF121212),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onFlip,
                containerColor = Color.White,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.List, contentDescription = "Switch to Todo List")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val newCal = calendar.clone() as Calendar
                        newCal.add(Calendar.MONTH, -1)
                        calendar = newCal
                    }) {
                        Text("<", fontSize = 20.sp, color = Color.White)
                    }

                    Text(
                        text = "$monthName $year",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    IconButton(onClick = {
                        val newCal = calendar.clone() as Calendar
                        newCal.add(Calendar.MONTH, 1)
                        calendar = newCal
                    }) {
                        Text(">", fontSize = 20.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
                Row(modifier = Modifier.fillMaxWidth()) {
                    daysOfWeek.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Cyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val daysInMonth = getDaysInMonth(calendar)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                ) {
                    items(daysInMonth) { day ->
                        DayCell(
                            day = day,
                            isSelected = day != null && isSameDay(calendar, day, selectedDate.value),
                            isToday = day != null && isSameDay(calendar, day, Calendar.getInstance()),
                            onClick = {
                                if (day != null) {
                                    val newSelected = calendar.clone() as Calendar
                                    newSelected.set(Calendar.DAY_OF_MONTH, day)
                                    selectedDate.value = newSelected
                                }
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Tasks for ${selectedDate.value.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E1E1E))
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        itemsIndexed(filteredTasks) { _, task ->
                            TaskItem(
                                task = task,
                                dateStr = selectedDateStr,
                                isToday = selectedDateStr == todayStr,
                                canToggle = canToggle,
                                onEditTask = {
                                    selectedTaskIndex = tasks.indexOf(task)
                                    showTaskEditor = true
                                },
                                onCompletedToggle = {
                                    val isNowCompleted = !task.completedDates.contains(selectedDateStr)
                                    val newCompletedDates = if (!isNowCompleted) {
                                        task.completedDates - selectedDateStr
                                    } else {
                                        task.completedDates + selectedDateStr
                                    }

                                    if (isNowCompleted) {
                                        XpManager.addXp(context, 1)
                                    } else {
                                        XpManager.subtractXp(context, 1)
                                    }

                                    coroutineScope.launch {
                                        taskDao.updateTask(task.copy(
                                            completedDates = newCompletedDates,
                                            skippedDates = if (isNowCompleted) task.skippedDates - selectedDateStr else task.skippedDates
                                        ))
                                    }
                                },
                                onSkipToggle = {
                                    val isCurrentlySkipped = task.skippedDates.contains(selectedDateStr)
                                    val newSkippedDates = if (isCurrentlySkipped) {
                                        task.skippedDates - selectedDateStr
                                    } else {
                                        task.skippedDates + selectedDateStr
                                    }
                                    coroutineScope.launch {
                                        taskDao.updateTask(task.copy(skippedDates = newSkippedDates))
                                    }
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF252525))
                                    .clickable { 
                                        selectedTaskIndex = -1
                                        showTaskEditor = true 
                                    }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Add task for this day", fontSize = 16.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            if (showTaskEditor) {
                val currentTask = tasks.getOrNull(selectedTaskIndex)
                TaskEditor(
                    task = currentTask,
                    onSave = { updatedTask ->
                        coroutineScope.launch {
                            if (selectedTaskIndex == -1) {
                                taskDao.insertTask(updatedTask)
                            } else {
                                taskDao.updateTask(updatedTask)
                            }
                        }
                        showTaskEditor = false
                    },
                    onCancel = { _ ->
                        if (currentTask != null) {
                            coroutineScope.launch {
                                taskDao.deleteTask(currentTask)
                            }
                        }
                        showTaskEditor = false
                    },
                )
            }
        }
    }
}

@Composable
fun DayCell(day: Int?, isSelected: Boolean, isToday: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> Color.Cyan
                    isToday -> Color(0xFF1E1E1E)
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = day != null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (day != null) {
            Text(
                text = day.toString(),
                color = if (isSelected) Color.Black else Color.White,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

private fun getDaysInMonth(calendar: Calendar): List<Int?> {
    val days = mutableListOf<Int?>()
    val tempCal = calendar.clone() as Calendar
    tempCal.set(Calendar.DAY_OF_MONTH, 1)

    val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed (Sun=0)
    val maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    repeat(firstDayOfWeek) { days.add(null) }
    for (i in 1..maxDays) { days.add(i) }
    return days
}

private fun isSameDay(cal: Calendar, day: Int, target: Calendar): Boolean {
    return cal.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
           cal.get(Calendar.MONTH) == target.get(Calendar.MONTH) &&
           day == target.get(Calendar.DAY_OF_MONTH)
}
