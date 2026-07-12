package com.example.a1234567889.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.toColorInt
import androidx.compose.ui.unit.dp

@Composable
fun PaletteEditorDialog(
    initialColors: List<Color>,
    onDismiss: () -> Unit,
    onSave: (List<Color>) -> Unit
) {
    var colors by remember { mutableStateOf(initialColors.toMutableList()) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактор палитры") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Настройте цвета для вашей палитры (${colors.size})")
                
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(40.dp),
                    modifier = Modifier.heightIn(max = 300.dp),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(colors) { index, color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { editingIndex = index }
                        )
                    }
                    item {
                        IconButton(
                            onClick = { colors.add(Color.Gray) },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.Add, null)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(colors) }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )

    editingIndex?.let { index ->
        ColorPickerDialog(
            initialColor = colors[index],
            onDismiss = { editingIndex = null },
            onColorSelected = { 
                colors = colors.toMutableList().apply { set(index, it) }
                editingIndex = null
            },
            onDelete = {
                colors = colors.toMutableList().apply { removeAt(index) }
                editingIndex = null
            }
        )
    }
}

@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
    onDelete: () -> Unit
) {
    var red by remember { mutableFloatStateOf(initialColor.red) }
    var green by remember { mutableFloatStateOf(initialColor.green) }
    var blue by remember { mutableFloatStateOf(initialColor.blue) }
    var hex by remember { mutableStateOf(colorToHex(initialColor)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Настройка цвета")
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(red, green, blue))
                )

                Text("Красный: ${(red * 255).toInt()}")
                Slider(value = red, onValueChange = { 
                    red = it
                    hex = colorToHex(Color(red, green, blue))
                })

                Text("Зеленый: ${(green * 255).toInt()}")
                Slider(value = green, onValueChange = { 
                    green = it
                    hex = colorToHex(Color(red, green, blue))
                })

                Text("Синий: ${(blue * 255).toInt()}")
                Slider(value = blue, onValueChange = { 
                    blue = it
                    hex = colorToHex(Color(red, green, blue))
                })

                OutlinedTextField(
                    value = hex,
                    onValueChange = { 
                        hex = it
                        hexToColor(it)?.let { c ->
                            red = c.red
                            green = c.green
                            blue = c.blue
                        }
                    },
                    label = { Text("HEX") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onColorSelected(Color(red, green, blue)) }) {
                Text("ОК")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

fun colorToHex(color: Color): String {
    return String.format("#%06X", (0xFFFFFF and color.toArgb()))
}

fun hexToColor(hex: String): Color? {
    return try {
        Color((if (hex.startsWith("#")) hex else "#$hex").toColorInt())
    } catch (_: Exception) {
        null
    }
}
