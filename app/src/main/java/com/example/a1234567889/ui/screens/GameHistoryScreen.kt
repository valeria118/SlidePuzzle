package com.example.a1234567889.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.a1234567889.data.db.AppDatabaseHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lab item 15 — "Создание БД": this screen reads/deletes rows from the local
 * SQLite [AppDatabaseHelper.TABLE_GAMES] table built up by PuzzleViewModel after
 * every finished match.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameHistoryScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val dbHelper = remember { AppDatabaseHelper(context) }
    var records by remember { mutableStateOf(dbHelper.getAllGames()) }
    var showClearConfirm by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    fun refresh() {
        records = dbHelper.getAllGames()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История партий") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (records.isNotEmpty()) {
                        IconButton(onClick = { showClearConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Очистить историю")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (records.isEmpty()) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Пока нет завершённых партий", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(12.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (record.won) Icons.Default.CheckCircle else Icons.Default.HighlightOff,
                                contentDescription = null,
                                tint = if (record.won) Color(0xFF2ECC71) else Color(0xFFE74C3C)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${record.mode} · ${record.size}×${record.size}",
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Ходов: ${record.moves} · Время: ${record.timeSeconds / 60}:${(record.timeSeconds % 60).toString().padStart(2, '0')}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    dateFormat.format(Date(record.dateMillis)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                dbHelper.deleteGameRecord(record.id)
                                refresh()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить запись")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
            title = { Text("Очистить историю?") },
            text = { Text("Все ${records.size} записей о партиях будут удалены без возможности восстановления.") },
            confirmButton = {
                TextButton(onClick = {
                    dbHelper.clearGameHistory()
                    refresh()
                    showClearConfirm = false
                }) { Text("Очистить") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Отмена") }
            }
        )
    }
}
