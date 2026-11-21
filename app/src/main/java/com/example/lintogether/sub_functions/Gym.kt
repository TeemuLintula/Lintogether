package com.example.lintogether.sub_functions

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.lintogether.TaskDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun GymScreen(taskDao: TaskDao) {
    val exercises by taskDao.getAllGymExercises().collectAsState(initial = emptyList())
    val gymDays by taskDao.getAllGymDays().collectAsState(initial = emptyList())
    var showEditor by remember { mutableStateOf(false) }
    var editingExercise by remember { mutableStateOf<GymExercise?>(null) }
    
    var showDayEditor by remember { mutableStateOf(false) }
    var dayToEdit by remember { mutableStateOf("") }
    
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // sorted days by position
    val sortedDays = gymDays.sortedBy { it.position }
    
    // Create a flat list of items for the LazyColumn
    val listItems = remember(exercises, gymDays) {
        val items = mutableListOf<Any>()
        
        // Named days first
        sortedDays.forEach { day ->
            items.add(day.name) // Header
            items.addAll(exercises.filter { it.gymDay == day.name }.sortedBy { it.position })
        }

        // Unassigned always at the bottom
        val unassigned = exercises.filter { it.gymDay.isBlank() }.sortedBy { it.position }
        if (unassigned.isNotEmpty() || gymDays.isEmpty()) {
            items.add("") // Header for unassigned
            items.addAll(unassigned)
        }
        items
    }

    var currentList by remember(listItems) { mutableStateOf(listItems) }
    var draggingRange by remember { mutableStateOf<IntRange?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { 
                        editingExercise = null
                        showEditor = true 
                    },
                    containerColor = Color.White,
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Exercise")
                }
            },
            containerColor = Color(0xFF121212)
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "GYM",
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                val lazyListState = rememberLazyListState()

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(currentList, key = { _, item -> 
                        when(item) {
                            is String -> if (item.isEmpty()) "header_unassigned" else "header_$item"
                            is GymExercise -> "exercise_${item.id}"
                            else -> item.hashCode()
                        }
                    }) { _, item ->
                        val index = currentList.indexOf(item)
                        val isDragging = draggingRange?.contains(index) == true
                        val scale by animateFloatAsState(if (isDragging) 1.05f else 1f, label = "scale")
                        val elevation by animateDpAsState(if (isDragging) 12.dp else 0.dp, label = "elevation")

                        val offsetModifier = if (isDragging) {
                            Modifier
                                .zIndex(1f)
                                .offset { IntOffset(0, dragOffset.roundToInt()) }
                        } else Modifier

                        Box(
                            modifier = Modifier
                                .animateItem()
                                .then(offsetModifier)
                                .fillMaxWidth()
                                .scale(scale)
                                .shadow(elevation, RoundedCornerShape(12.dp))
                                .pointerInput(item) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { 
                                            val actualIndex = currentList.indexOf(item)
                                            if (actualIndex == -1 || item == "") return@detectDragGesturesAfterLongPress
                                            
                                            val start = actualIndex
                                            var end = actualIndex
                                            if (item is String) {
                                                for (i in actualIndex + 1 until currentList.size) {
                                                    if (currentList[i] is GymExercise) end = i else break
                                                }
                                            }
                                            draggingRange = start..end
                                        },
                                        onDragEnd = {
                                            saveNewPositions(currentList, taskDao, coroutineScope)
                                            draggingRange = null
                                            dragOffset = 0f
                                        },
                                        onDragCancel = {
                                            draggingRange = null
                                            dragOffset = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffset += dragAmount.y
                                            
                                            draggingRange?.let { range ->
                                                val avgItemHeightPx = 72f * density.density
                                                val itemsJumped = (dragOffset / avgItemHeightPx).roundToInt()
                                                
                                                if (itemsJumped != 0) {
                                                    val newList = currentList.toMutableList()
                                                    val itemsToMove = newList.subList(range.first, range.last + 1).toList()
                                                    
                                                    repeat(itemsToMove.size) { newList.removeAt(range.first) }
                                                    
                                                    val unassignedHeaderIndex = newList.indexOf("")
                                                    var targetIndex = (range.first + itemsJumped).coerceIn(0, newList.size)
                                                    
                                                    // Categories can't go inside another
                                                    if (item is String) {
                                                        // Snap to a header position
                                                        val headerIndices = newList.mapIndexedNotNull { i, it -> if (it is String) i else null }.toMutableList()
                                                        headerIndices.add(newList.size)
                                                        val limit = if (unassignedHeaderIndex != -1) unassignedHeaderIndex else newList.size
                                                        val validIndices = headerIndices.filter { it <= limit }
                                                        
                                                        targetIndex = validIndices.minByOrNull { abs(it - targetIndex) } ?: targetIndex
                                                    }

                                                    if (targetIndex != range.first) {
                                                        newList.addAll(targetIndex, itemsToMove)
                                                        currentList = newList
                                                        draggingRange = targetIndex until (targetIndex + itemsToMove.size)
                                                        dragOffset -= (targetIndex - range.first) * avgItemHeightPx
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                        ) {
                            if (item is String) {
                                val nameDisplay = if (item.isBlank()) "Unassigned" else item
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isDragging) Color(0xFF252525) else Color(0xFF121212))
                                ) {
                                    Text(
                                        text = nameDisplay,
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = item.isNotBlank()) {
                                                dayToEdit = item
                                                showDayEditor = true
                                            }
                                            .padding(vertical = 12.dp)
                                    )
                                }
                            } else if (item is GymExercise) {
                                ExerciseRow(item) {
                                    editingExercise = item
                                    showEditor = true
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showEditor) {
             GymExerciseEditor(
                exercise = editingExercise,
                availableDays = gymDays.map { it.name }.filter { it.isNotBlank() },
                onSave = { name, day, weight, marker ->
                    val original = editingExercise
                    coroutineScope.launch {
                        if (day.isNotBlank()) {
                            val exists = gymDays.any { it.name == day }
                            if (!exists) taskDao.insertGymDay(GymDay(name = day, position = gymDays.size))
                        }
                        
                        val newWeightHistory = if (original != null && weight.isNotBlank()) {
                            val doubleWeight = weight.toDoubleOrNull() ?: 0.0
                            if (original.weightHistory.isEmpty() || original.weightHistory.last().weight != doubleWeight) {
                                original.weightHistory + WeightEntry(System.currentTimeMillis(), doubleWeight)
                            } else {
                                original.weightHistory
                            }
                        } else if (original == null && weight.isNotBlank()) {
                            listOf(WeightEntry(System.currentTimeMillis(), weight.toDoubleOrNull() ?: 0.0))
                        } else {
                            original?.weightHistory ?: emptyList()
                        }

                        if (original == null) {
                            taskDao.insertGymExercise(GymExercise(name = name, gymDay = day, weight = weight, weightMarker = marker, weightHistory = newWeightHistory, position = exercises.size))
                        } else {
                            taskDao.updateGymExercise(original.copy(name = name, gymDay = day, weight = weight, weightMarker = marker, weightHistory = newWeightHistory))
                        }
                    }
                    showEditor = false
                    editingExercise = null
                },
                onDelete = { ex ->
                    coroutineScope.launch { taskDao.deleteGymExercise(ex) }
                    showEditor = false
                    editingExercise = null
                },
                onCancel = { showEditor = false; editingExercise = null }
            )
        }
        
        if (showDayEditor) {
            val originalDayName = dayToEdit
            DayNameEditor(
                initialName = originalDayName.ifBlank { "Unassigned" },
                onSave = { newName ->
                    coroutineScope.launch {
                        val trimmed = newName.trim()
                        if (trimmed.isNotBlank() && trimmed != originalDayName) {
                            val existing = gymDays.find { it.name == trimmed }
                            if (existing == null) {
                                val oldDay = gymDays.find { it.name == originalDayName }
                                taskDao.insertGymDay(GymDay(name = trimmed, position = oldDay?.position ?: 0))
                            }
                            exercises.filter { it.gymDay == originalDayName }.forEach {
                                taskDao.updateGymExercise(it.copy(gymDay = trimmed))
                            }
                            if (originalDayName.isNotBlank()) {
                                taskDao.deleteGymDay(GymDay(name = originalDayName))
                            }
                        }
                    }
                    showDayEditor = false
                },
                onDelete = {
                    coroutineScope.launch {
                        exercises.filter { it.gymDay == originalDayName }.forEach {
                            taskDao.updateGymExercise(it.copy(gymDay = ""))
                        }
                        if (originalDayName.isNotBlank()) {
                            taskDao.deleteGymDay(GymDay(name = originalDayName))
                        }
                    }
                    showDayEditor = false
                },
                onCancel = { showDayEditor = false }
            )
        }
    }
}

private fun saveNewPositions(currentList: List<Any>, taskDao: TaskDao, coroutineScope: CoroutineScope) {
    coroutineScope.launch {
        var dayPos = 0
        var curDay = ""
        var exPos = 0
        currentList.forEach { item ->
            if (item is String) {
                curDay = item
                if (item.isNotBlank()) taskDao.updateGymDay(GymDay(name = item, position = dayPos++))
                exPos = 0
            } else if (item is GymExercise) {
                taskDao.updateGymExercise(item.copy(gymDay = curDay, position = exPos++))
            }
        }
    }
}

@Composable
fun ExerciseRow(exercise: GymExercise, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = exercise.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (exercise.weight.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = exercise.weight, color = Color.White, fontSize = 18.sp)
                    if (exercise.weightMarker.isNotBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = exercise.weightMarker, color = if (exercise.weightMarker == "+") Color.Green else Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun WeightGraph(history: List<WeightEntry>) {
    if (history.isEmpty()) return
    
    val sortedHistory = remember(history) {
        history.sortedBy { it.timestamp }
            .groupBy { 
                val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                sdf.format(Date(it.timestamp))
            }
            .map { it.value.last() }
            .sortedBy { it.timestamp }
    }
    val maxWeight = (sortedHistory.maxOfOrNull { it.weight } ?: 0.0).toFloat()
    val minWeight = (sortedHistory.minOfOrNull { it.weight } ?: 0.0).toFloat()
    val weightRange = (maxWeight - minWeight).coerceAtLeast(1f)
    
    val dateFormat = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Weight Progress", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF252525))
            .padding(start = 40.dp, end = 16.dp, top = 16.dp, bottom = 32.dp) // Leave space for labels
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val points = sortedHistory.size
                
                // Draw weight references (Y axis)
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 10.dp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
                
                drawContext.canvas.nativeCanvas.drawText("${maxWeight.toInt()}kg", -10.dp.toPx(), 5.dp.toPx(), paint)
                drawContext.canvas.nativeCanvas.drawText("${minWeight.toInt()}kg", -10.dp.toPx(), height, paint)
                
                if (points > 1) {
                    val minTime = sortedHistory.first().timestamp
                    val maxTime = sortedHistory.last().timestamp
                    val timeRange = (maxTime - minTime).coerceAtLeast(1L).toFloat()
                    
                    val path = Path()
                    
                    sortedHistory.forEachIndexed { index, entry ->
                        val x = ((entry.timestamp - minTime).toFloat() / timeRange) * width
                        val y = height - ((entry.weight.toFloat() - minWeight) / weightRange) * height
                        
                        if (index == 0) path.moveTo(x, y)
                        else path.lineTo(x, y)
                        
                        // Draw date reference (X axis) every few points or first/last
                        if (index == 0 || index == points - 1 || points < 5) {
                            val dateStr = dateFormat.format(Date(entry.timestamp))
                            drawContext.canvas.nativeCanvas.drawText(
                                dateStr,
                                x,
                                height + 20.dp.toPx(),
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.GRAY
                                    textSize = 10.dp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                }
                            )
                        }
                        
                        drawCircle(color = Color.Cyan, radius = 4.dp.toPx(), center = Offset(x, y))
                    }
                    
                    drawPath(path = path, color = Color.Cyan, style = Stroke(width = 2.dp.toPx()))
                } else if (points == 1) {
                    val x = width / 2
                    val y = height / 2
                    drawCircle(color = Color.Cyan, radius = 4.dp.toPx(), center = Offset(x, y))
                    val dateStr = dateFormat.format(Date(sortedHistory[0].timestamp))
                    drawContext.canvas.nativeCanvas.drawText(
                        dateStr,
                        x,
                        height + 20.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 10.dp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DayNameEditor(initialName: String, onSave: (String) -> Unit, onDelete: () -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf(initialName) }

    BackHandler {
        onCancel()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E1E1E))
                .padding(24.dp)
        ) {
            Column {
                Text("Rename Day", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Day Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedContainerColor = Color(0xFF252525),
                        unfocusedContainerColor = Color(0xFF252525)
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    if (initialName != "Unassigned") {
                        Text(
                            text = "Delete",
                            color = Color.Red,
                            modifier = Modifier
                                .clickable { onDelete() }
                                .padding(8.dp)
                        )
                    } else {
                        Text(
                            text = "Cancel",
                            color = Color.Gray,
                            modifier = Modifier
                                .clickable { onCancel() }
                                .padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .clickable { if (name.isNotBlank()) onSave(name) }
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GymExerciseEditor(
    exercise: GymExercise? = null,
    availableDays: List<String>,
    onSave: (String, String, String, String) -> Unit,
    onDelete: (GymExercise) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember(exercise) { mutableStateOf(exercise?.name ?: "") }
    var weight by remember(exercise) { mutableStateOf(exercise?.weight ?: "") }
    var marker by remember(exercise) { mutableStateOf(exercise?.weightMarker ?: "") }
    var selectedDay by remember(exercise) { mutableStateOf(exercise?.gymDay ?: "") }
    var customDay by remember { mutableStateOf("") }
    var isCreatingNewDay by remember { mutableStateOf(false) }

    BackHandler {
        onCancel()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.fillMaxWidth(0.9f).clip(RoundedCornerShape(24.dp)).background(Color(0xFF1E1E1E)).padding(24.dp)) {
            Column {
                Text(text = if (exercise == null) "New Exercise" else "Edit Exercise", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                
                if (exercise != null) {
                    Spacer(Modifier.height(16.dp))
                    WeightGraph(exercise.weightHistory)
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedContainerColor = Color(0xFF252525),
                        unfocusedContainerColor = Color(0xFF252525)
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Weight (kg)") },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = Color(0xFF252525),
                            unfocusedContainerColor = Color(0xFF252525)
                        )
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Row {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (marker == "+") Color.Green else Color.DarkGray)
                                .clickable { marker = if (marker == "+") "" else "+" },
                            contentAlignment = Alignment.Center
                        ) { Text("+", color = if (marker == "+") Color.Black else Color.White, fontSize = 24.sp) }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (marker == "-") Color.Red else Color.DarkGray)
                                .clickable { marker = if (marker == "-") "" else "-" },
                            contentAlignment = Alignment.Center
                        ) { Text("-", color = if (marker == "-") Color.Black else Color.White, fontSize = 24.sp) }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Select Day", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp) ) {
                    val unassignedSel = selectedDay.isBlank() && !isCreatingNewDay
                    Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (unassignedSel) Color.White else Color.DarkGray).clickable { selectedDay = ""; isCreatingNewDay = false }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(text = "Unassigned", color = if (unassignedSel) Color.Black else Color.White)
                    }
                    availableDays.forEach { day ->
                        val sel = selectedDay == day && !isCreatingNewDay
                        Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (sel) Color.White else Color.DarkGray).clickable { selectedDay = day; isCreatingNewDay = false }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(text = day, color = if (sel) Color.Black else Color.White)
                        }
                    }
                    Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (isCreatingNewDay) Color.Cyan else Color.DarkGray).clickable { isCreatingNewDay = true }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(text = "+ New Day", color = if (isCreatingNewDay) Color.Black else Color.Cyan)
                    }
                }
                if (isCreatingNewDay) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = customDay,
                        onValueChange = { customDay = it },
                        label = { Text("Enter Day Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            focusedContainerColor = Color(0xFF252525),
                            unfocusedContainerColor = Color(0xFF252525)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    if (exercise != null) {
                        Text(
                            text = "Delete",
                            color = Color.Red,
                            modifier = Modifier
                                .clickable { onDelete(exercise) }
                                .padding(8.dp)
                        )
                    } else {
                        Text(
                            text = "Cancel",
                            color = Color.Gray,
                            modifier = Modifier
                                .clickable { onCancel() }
                                .padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .clickable {
                                val finalDay = if (isCreatingNewDay) customDay.trim() else selectedDay
                                if (name.isNotBlank()) {
                                    onSave(name, finalDay, weight, marker)
                                }
                            }
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
