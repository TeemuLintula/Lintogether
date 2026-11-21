package com.example.lintogether.sub_functions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import com.example.lintogether.R
import com.example.lintogether.TaskDao
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun GoalsScreen(taskDao: TaskDao) {
    val goals by taskDao.getAllGoals().collectAsState(initial = emptyList())
    var showEditor by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<Goal?>(null) }
    var filterType by remember { mutableStateOf(GoalType.MAIN) }
    val coroutineScope = rememberCoroutineScope()

    // Whiteboard state per filter type
    val viewStates = remember { mutableStateMapOf<GoalType, Pair<Float, Offset>>() }
    val currentViewState = viewStates.getOrPut(filterType) { 0.7f to Offset.Zero }

    var scale by remember(filterType) { mutableFloatStateOf(currentViewState.first) }
    var offset by remember(filterType) { mutableStateOf(currentViewState.second) }

    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.2f, 3f)
        offset += offsetChange
        viewStates[filterType] = scale to offset
    }

    if (showEditor) {
        GoalEditor(
            goal = editingGoal,
            goals = goals,
            onSave = { name, description, type, parentId, isCompleted, deadline ->
                coroutineScope.launch {
                    if (editingGoal != null) {
                        taskDao.updateGoal(editingGoal!!.copy(
                            name = name,
                            description = description,
                            type = type,
                            parentId = parentId,
                            isCompleted = isCompleted,
                            deadline = deadline
                        ))
                    } else {
                        // Place new goals near the center of the current view
                        val centerX = -offset.x / scale
                        val centerY = -offset.y / scale
                        taskDao.insertGoal(Goal(
                            name = name, 
                            description = description, 
                            type = type, 
                            parentId = parentId,
                            x = centerX,
                            y = centerY,
                            isCompleted = isCompleted,
                            deadline = deadline
                        ))
                    }
                    showEditor = false
                    editingGoal = null
                }
            },
            onCancel = { 
                showEditor = false
                editingGoal = null
            },
            onDelete = {
                editingGoal?.let { goal ->
                    coroutineScope.launch {
                        taskDao.deleteGoal(goal)
                        showEditor = false
                        editingGoal = null
                    }
                }
            },
            onEditGoal = { goalToEdit ->
                editingGoal = goalToEdit
            },
            onToggleSubGoal = { subGoal ->
                coroutineScope.launch {
                    taskDao.updateGoal(subGoal.copy(isCompleted = !subGoal.isCompleted))
                }
            }
        )
    } else {
        Scaffold(
            containerColor = Color(0xFF121212),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { 
                        editingGoal = null
                        showEditor = true 
                    },
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    modifier = Modifier.zIndex(100f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Goal")
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // 1. Interactive Panning Layer (Background)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 48.dp)
                        .zIndex(0f)
                        .transformable(state = state)
                )

                // 2. Content Layer (Foreground)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                ) {
                    goals.filter { it.type == filterType }.forEach { goal ->
                        val subGoals = goals.filter { it.parentId == goal.id }
                        GoalStickyNote(
                            goal = goal,
                            subGoals = subGoals,
                            onDelete = { coroutineScope.launch { taskDao.deleteGoal(goal) } },
                            onToggleComplete = {
                                coroutineScope.launch {
                                    taskDao.updateGoal(goal.copy(isCompleted = !goal.isCompleted))
                                }
                            },
                            onEdit = {
                                editingGoal = goal
                                showEditor = true
                            },
                            onPositionChange = { newX, newY ->
                                coroutineScope.launch {
                                    taskDao.updateGoal(goal.copy(x = newX, y = newY))
                                }
                            }
                        )
                    }
                }

                // 3. Fixed UI Overlay (Always on top)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(10f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "GOALS",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFF1E1E1E).copy(alpha = 0.9f),
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .zIndex(11f),
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(GoalType.MAIN, GoalType.YEAR, GoalType.MINOR).forEach { type ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (filterType == type) Color.White else Color.Transparent)
                                            .clickable { filterType = type }
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = type.name,
                                            color = if (filterType == type) Color.Black else Color.White.copy(alpha = 0.6f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoalStickyNote(
    goal: Goal,
    subGoals: List<Goal>,
    onDelete: () -> Unit,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onPositionChange: (Float, Float) -> Unit
) {
    var offsetX by remember(goal.id) { mutableFloatStateOf(goal.x) }
    var offsetY by remember(goal.id) { mutableFloatStateOf(goal.y) }

    val noteColor = if (goal.isCompleted) Color(0xFF1B5E20)
    else when (goal.type) {
        GoalType.MAIN -> Color(0xFFFBC02D) // Yellow
        GoalType.YEAR -> Color(0xFF039BE5) // Blue
        GoalType.MINOR -> Color(0xFF8E24AA) // Purple
        GoalType.SUB -> Color(0xFF43A047) // Green
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(160.dp)
            .pointerInput(goal.id) {
                detectDragGestures(
                    onDragEnd = {
                        onPositionChange(offsetX, offsetY)
                    }
                ) { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .clickable { onEdit() }
    ) {
        Image(
            painter = painterResource(R.drawable.stickynote),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.tint(noteColor, BlendMode.Multiply)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            val textColor = if (goal.type == GoalType.MAIN) Color.Black else Color.White
            
            Text(
                text = goal.name,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textDecoration = if (goal.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (goal.deadline != null) {
                Text(
                    text = "By: ${goal.deadline}",
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (subGoals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                // Only show a few subgoals to fit the sticky note size
                subGoals.take(4).forEach { sub ->
                    Text(
                        text = "• ${sub.name}",
                        color = textColor.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                }
                if (subGoals.size > 4) {
                    Text(
                        text = "...",
                        color = textColor.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalEditor(
    goal: Goal? = null,
    goals: List<Goal>,
    onSave: (String, String, GoalType, Int?, Boolean, String?) -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit = {},
    onEditGoal: (Goal) -> Unit = {},
    onToggleSubGoal: (Goal) -> Unit = {}
) {
    var name by remember(goal?.id) { mutableStateOf(goal?.name ?: "") }
    var description by remember(goal?.id) { mutableStateOf(goal?.description ?: "") }
    var selectedType by remember(goal?.id) { mutableStateOf(goal?.type ?: GoalType.MAIN) }
    var selectedParentId by remember(goal?.id) { mutableStateOf<Int?>(goal?.parentId) }
    var isCompleted by remember(goal?.id) { mutableStateOf(goal?.isCompleted ?: false) }
    var deadline by remember(goal?.id) { mutableStateOf(goal?.deadline) }
    var showParentMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    BackHandler {
        if (name.isNotBlank()) {
            onSave(name, description, selectedType, selectedParentId, isCompleted, deadline)
        } else {
            onCancel()
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        deadline = sdf.format(Date(it))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (goal == null) "New Goal" else "Edit Goal", 
                            color = Color.White, 
                            fontSize = 32.sp, 
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (goal != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = onDelete) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Goal",
                                    tint = Color.Red.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Done", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isCompleted,
                            onCheckedChange = { isCompleted = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF43A047),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color(0xFF1E1E1E)
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E),
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.Gray,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E),
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.Gray,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Deadline Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E1E))
                        .clickable { showDatePicker = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Deadline", color = Color.Gray, fontSize = 12.sp)
                        Text(deadline ?: "No deadline set", color = Color.White, fontSize = 16.sp)
                    }
                    if (deadline != null) {
                        IconButton(onClick = { deadline = null }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear deadline", tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Goal Type", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf(GoalType.MAIN, GoalType.YEAR, GoalType.MINOR, GoalType.SUB)
                    types.forEach { type ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedType == type) Color.White else Color(0xFF1E1E1E))
                                .clickable { 
                                    selectedType = type 
                                    selectedParentId = null
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val typeLabel = when(type) {
                                GoalType.MAIN -> "Main"
                                GoalType.YEAR -> "Year"
                                GoalType.MINOR -> "Minor"
                                GoalType.SUB -> "Sub"
                            }
                            Text(
                                text = typeLabel, 
                                color = if (selectedType == type) Color.Black else Color.White, 
                                fontSize = 14.sp,
                                fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                if (selectedType != GoalType.MAIN) {
                    Spacer(modifier = Modifier.height(24.dp))
                    val availableParents = goals.filter { it.id != goal?.id && it.type.ordinal < selectedType.ordinal }

                    Text("Parent Goal", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        val parent = goals.find { it.id == selectedParentId }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1E1E))
                                .clickable { showParentMenu = true }
                                .padding(16.dp)
                        ) {
                            Text(
                                text = parent?.name ?: "No Parent (Floating)",
                                color = Color.White,
                                modifier = Modifier.clickable(enabled = parent != null) {
                                    parent?.let { onEditGoal(it) }
                                }
                            )
                        }

                        DropdownMenu(
                            expanded = showParentMenu,
                            onDismissRequest = { showParentMenu = false },
                            modifier = Modifier
                                .background(Color(0xFF252525))
                                .fillMaxWidth(0.85f)
                                .heightIn(max = 300.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("No Parent (Floating)", color = Color.White) },
                                onClick = {
                                    selectedParentId = null
                                    showParentMenu = false
                                }
                            )
                            availableParents.forEach { parent ->
                                DropdownMenuItem(
                                    text = { Text(parent.name, color = Color.White) },
                                    onClick = {
                                        selectedParentId = parent.id
                                        showParentMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (goal != null) {
                    val subGoals = goals.filter { it.parentId == goal.id }
                    if (subGoals.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Text("Sub-goals", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        subGoals.forEach { subGoal ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E1E1E))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (subGoal.isCompleted) Color(0xFF43A047) else Color(0xFF333333))
                                        .clickable { onToggleSubGoal(subGoal) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (subGoal.isCompleted) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onEditGoal(subGoal) }
                                ) {
                                    Text(
                                        text = subGoal.name,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        textDecoration = if (subGoal.isCompleted) TextDecoration.LineThrough else null
                                    )
                                    if (subGoal.deadline != null) {
                                        Text(
                                            text = "By: ${subGoal.deadline}",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1E1E))
                        .clickable { onCancel() }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (name.isNotBlank()) Color.White else Color.DarkGray)
                        .clickable(enabled = name.isNotBlank()) { 
                            onSave(name, description, selectedType, selectedParentId, isCompleted, deadline) 
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Save Goal",
                        color = if (name.isNotBlank()) Color.Black else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
