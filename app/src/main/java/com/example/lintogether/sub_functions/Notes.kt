package com.example.lintogether.sub_functions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lintogether.TaskDao
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NotesScreen(taskDao: TaskDao) {
    val notes by taskDao.getAllNotes().collectAsState(initial = emptyList())
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showReflection by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    if (showReflection) {
        ReflectionScreen(taskDao = taskDao, onBack = { showReflection = false })
    } else if (showEditor) {
        NoteView(
            note = editingNote,
            onSave = { updatedNote ->
                coroutineScope.launch {
                    if (updatedNote.id == 0) {
                        val newId = taskDao.insertNote(updatedNote)
                        editingNote = updatedNote.copy(id = newId.toInt())
                    } else {
                        taskDao.updateNote(updatedNote)
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
                            onClick = { showReflection = true },
                            containerColor = Color.White,
                            contentColor = Color.Black,
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Reflection")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        FloatingActionButton(
                            onClick = {
                                editingNote = null
                                showEditor = true
                            },
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Note")
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
                        text = "NOTES",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(notes) { note ->
                            NoteItem(
                                note = note,
                                onClick = {
                                    editingNote = note
                                    showEditor = true
                                },
                                onDelete = {
                                    coroutineScope.launch {
                                        taskDao.deleteNote(note)
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
fun NoteItem(note: Note, onClick: () -> Unit, onDelete: () -> Unit) {
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
            if (note.title.isNotBlank()) {
                Text(
                    text = note.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = note.content,
                color = Color.Gray,
                fontSize = 14.sp,
                maxLines = 2
            )
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
fun NoteView(
    note: Note?,
    onSave: (Note) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }

    BackHandler {
        onBack()
    }

    // Debounced Auto-save logic
    LaunchedEffect(title, content) {
        if (content.isNotBlank() || title.isNotBlank()) {
            delay(500) // Wait for half a second of inactivity
            onSave(
                Note(
                    id = note?.id ?: 0,
                    title = title,
                    content = content
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
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title", color = Color.Gray, fontSize = 24.sp) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
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
