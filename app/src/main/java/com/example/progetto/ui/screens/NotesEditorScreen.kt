package com.example.progetto.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.progetto.data.dao.TripDao
import com.example.progetto.data.entity.Trip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 笔记编辑界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesEditorScreen(
    tripId: Long,
    tripDao: TripDao,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var trip by remember { mutableStateOf<Trip?>(null) }
    var notes by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    // 加载旅行数据
    LaunchedEffect(tripId) {
        withContext(Dispatchers.IO) {
            trip = tripDao.getTripById(tripId)
            notes = trip?.detailedNotes ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑笔记") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                withContext(Dispatchers.IO) {
                                    trip?.let {
                                        tripDao.update(it.copy(detailedNotes = notes))
                                    }
                                }
                                isSaving = false
                                onNavigateBack()
                            }
                        },
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = "保存")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = trip?.destination ?: "加载中...",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("旅行笔记") },
                placeholder = { Text("记录你的旅行故事...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                maxLines = Int.MAX_VALUE
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "字数：${notes.length}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}