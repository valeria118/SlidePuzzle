package com.example.a1234567889.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a1234567889.models.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PresetDetailsDialog(
    preset: CustomPreset,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onEdit: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(preset.name, style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BoardPreview(
                    settings = preset.settings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                DetailItem("Размер поля", "${preset.settings.size}×${preset.settings.size}")
                DetailItem("Тип плиток", when(preset.settings.mode) {
                    GameMode.NUMBERS -> "Числа"
                    GameMode.IMAGE -> "Картинка"
                    GameMode.COLORS -> "Цвета"
                    else -> preset.settings.mode.name
                })
                DetailItem("Цель сборки", preset.settings.challenge.name)
                DetailItem("Лимит времени", if (preset.settings.timeAttackLimit > 0) "${preset.settings.timeAttackLimit} сек" else "Нет")
                
                DetailItem("Статус", if (preset.isPublic) "Опубликован" else "Приватный")
                DetailItem("Запусков", preset.playCount.toString())
                
                DetailItem("Создан", dateFormat.format(Date(preset.createdAt)))
                DetailItem("Обновлен", dateFormat.format(Date(preset.updatedAt)))
            }
        },
        confirmButton = {
            Button(onClick = onPlay) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("Играть")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onEdit) { Text("Редактировать") }
                TextButton(onClick = onDismiss) { Text("Закрыть") }
            }
        }
    )
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
