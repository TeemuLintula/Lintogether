package com.example.lintogether.sub_functions

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.Lock
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lintogether.TaskDao
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Entity(tableName = "reflections")
data class Reflection(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun ReflectionScreen(taskDao: TaskDao, onBack: () -> Unit) {
    val reflections by taskDao.getAllReflections().collectAsState(initial = emptyList())
    var editingReflection by remember { mutableStateOf<Reflection?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    if (showEditor) {
        ReflectionView(
            reflection = editingReflection,
            onSave = { updatedReflection ->
                coroutineScope.launch {
                    if (updatedReflection.id == 0) {
                        val newId = taskDao.insertReflection(updatedReflection)
                        editingReflection = updatedReflection.copy(id = newId.toInt())
                    } else {
                        taskDao.insertReflection(updatedReflection)
                    }
                }
            },
            onBack = { showEditor = false }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                floatingActionButton = {
                    Column(horizontalAlignment = Alignment.End) {
                        FloatingActionButton(
                            onClick = { onBack() },
                            containerColor = Color.White,
                            contentColor = Color.Black,
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = "Notes")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        FloatingActionButton(
                            onClick = {
                                editingReflection = null
                                showEditor = true
                            },
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Reflection")
                        }
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
                        text = "REFLECTIONS",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(reflections) { reflection ->
                            ReflectionItem(
                                reflection = reflection,
                                onClick = {
                                    editingReflection = reflection
                                    showEditor = true
                                },
                                onDelete = {
                                    coroutineScope.launch {
                                        taskDao.deleteReflection(reflection)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReflectionItem(reflection: Reflection, onClick: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
            .padding(16.dp)
    ) {
        Column {
            if (reflection.title.isNotBlank()) {
                Text(
                    text = reflection.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Delete", color = Color.Red) },
                onClick = {
                    onDelete()
                    showMenu = false
                }
            )
        }
    }
}

@Composable
fun ReflectionView(
    reflection: Reflection?,
    onSave: (Reflection) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()) }
    var title by remember { mutableStateOf(reflection?.title ?: sdf.format(Date())) }
    var content by remember { mutableStateOf(reflection?.content ?: "") }
    var timestamp by remember { mutableLongStateOf(reflection?.timestamp ?: System.currentTimeMillis()) }

    BackHandler {
        onBack()
    }

    // Debounced Auto-save logic
    LaunchedEffect(title, content, timestamp) {
        if (content.isNotBlank() || title.isNotBlank()) {
            delay(500)
            onSave(
                Reflection(
                    id = reflection?.id ?: 0,
                    title = title,
                    content = content,
                    timestamp = timestamp
                )
            )
        }
    }

    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, top = 45.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "← Back",
                    color = Color.Cyan,
                    modifier = Modifier.clickable { onBack() },
                    fontSize = 18.sp
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val calendar = Calendar.getInstance()
                        try {
                            val current = sdf.parse(title)
                            if (current != null) calendar.time = current
                        } catch (_: Exception) {}

                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val newDate = Calendar.getInstance()
                                newDate.set(year, month, dayOfMonth)
                                title = sdf.format(newDate.time)
                                timestamp = newDate.timeInMillis
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    .padding(vertical = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            TextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("Start typing...", color = Color.Gray) },
                modifier = Modifier.fillMaxSize(),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 18.sp
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
    }
}
