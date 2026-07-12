package com.example.a1234567889.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.a1234567889.models.*
import com.example.a1234567889.logic.GameEngine

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    currentSettings: GameSettings,
    onDismiss: () -> Unit,
    onSave: (GameSettings) -> Unit,
    onNavigateToAchievements: () -> Unit = {}
) {
    var size by remember { mutableIntStateOf(currentSettings.size) }
    var mode by remember { mutableStateOf(currentSettings.mode) }
    var challenge by remember { mutableStateOf(currentSettings.challenge) }
    var imageUri by remember { mutableStateOf(currentSettings.imageUri) }
    var language by remember { mutableStateOf(currentSettings.language) }

    // Color Mode Specific State
    var paletteType by remember { mutableStateOf(currentSettings.paletteType) }
    var colorPattern by remember { mutableStateOf(currentSettings.colorPattern) }
    var isColorBlindMode by remember { mutableStateOf(currentSettings.isColorBlindMode) }
    var showColorLabels by remember { mutableStateOf(currentSettings.showColorLabels) }
    var isChromaticAdaptation by remember { mutableStateOf(currentSettings.isChromaticAdaptation) }
    var adaptationIntensity by remember { mutableFloatStateOf(currentSettings.adaptationIntensity) }
    var isDynamicPalette by remember { mutableStateOf(currentSettings.isDynamicPalette) }
    var dynamicPaletteSpeed by remember { mutableStateOf(currentSettings.dynamicPaletteSpeed) }
    var endPaletteType by remember { mutableStateOf(currentSettings.endPaletteType) }
    var customPaletteColors by remember { mutableStateOf(currentSettings.customPaletteColors) }
    var showPaletteEditor by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                imageUri = uri.toString()
            }
        }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Game Settings") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Size
                Text("Grid Size: ${size}x${size}", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = size.toFloat(),
                    onValueChange = { size = it.toInt() },
                    valueRange = 3f..8f,
                    steps = 4
                )

                // Mode
                Text("Game Mode", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    GameMode.entries.forEach { m ->
                        FilterChip(
                            selected = mode == m,
                            onClick = { mode = m },
                            label = { 
                                Text(
                                    text = m.name,
                                    maxLines = 1,
                                    softWrap = false
                                ) 
                            }
                        )
                    }
                }

                if (mode == GameMode.IMAGE) {
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (imageUri == null) "Select Image" else "Change Image",
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    imageUri?.let { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = "Selected image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                if (mode == GameMode.COLORS) {
                    HorizontalDivider()
                    Text("Colors Mode Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                    // 1. Palette Selection
                    Text("1. Выбор палитры", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ColorPaletteType.entries.forEach { pt ->
                            FilterChip(
                                selected = paletteType == pt,
                                onClick = { paletteType = pt },
                                label = { Text(text = pt.name, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                    Button(onClick = { showPaletteEditor = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Создать свою палитру", maxLines = 1, softWrap = false)
                    }

                    // 2. Pattern Selection
                    Text("2. Выбор паттерна", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ColorPattern.entries.forEach { cp ->
                            FilterChip(
                                selected = colorPattern == cp,
                                onClick = { colorPattern = cp },
                                label = { Text(text = cp.name, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    // 3 & 4. Switches
                    ListItem(
                        headlineContent = { Text("3. Режим для дальтоников") },
                        trailingContent = { Switch(checked = isColorBlindMode, onCheckedChange = { isColorBlindMode = it }) }
                    )
                    ListItem(
                        headlineContent = { Text("4. Показывать номер") },
                        trailingContent = { Switch(checked = showColorLabels, onCheckedChange = { showColorLabels = it }) }
                    )

                    // 5. Chromatic Adaptation
                    ListItem(
                        headlineContent = { Text("5. Хроматическая адаптация") },
                        trailingContent = { 
                            Switch(
                                checked = isChromaticAdaptation, 
                                onCheckedChange = { 
                                    isChromaticAdaptation = it
                                    if (it) isDynamicPalette = false 
                                }
                            ) 
                        }
                    )
                    if (isChromaticAdaptation) {
                        Text("Интенсивность: ${(adaptationIntensity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        Slider(value = adaptationIntensity, onValueChange = { adaptationIntensity = it })
                    }

                    // 6. Dynamic Palette
                    ListItem(
                        headlineContent = { Text("6. Динамическая палитра") },
                        trailingContent = { 
                            Switch(
                                checked = isDynamicPalette, 
                                onCheckedChange = { 
                                    isDynamicPalette = it 
                                    if (it) isChromaticAdaptation = false // mutually exclusive as per guide
                                }
                            ) 
                        }
                    )
                    if (isDynamicPalette) {
                        Text("Конечная палитра", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ColorPaletteType.entries.forEach { pt ->
                                FilterChip(
                                    selected = endPaletteType == pt,
                                    onClick = { endPaletteType = pt },
                                    label = { Text(text = pt.name, maxLines = 1, softWrap = false) }
                                )
                            }
                        }
                        Text("Скорость изменений", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AnimationSpeed.entries.forEach { speed ->
                                FilterChip(
                                    selected = dynamicPaletteSpeed == speed,
                                    onClick = { dynamicPaletteSpeed = speed },
                                    label = { Text(text = speed.name, maxLines = 1, softWrap = false) }
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                }

                // Challenge
                Text("Challenge Mode", style = MaterialTheme.typography.labelLarge)
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChallengeMode.entries.forEach { c ->
                        FilterChip(
                            selected = challenge == c,
                            onClick = { challenge = c },
                            label = { Text(text = c.name, maxLines = 1, softWrap = false) }
                        )
                    }
                }

                // Language
                Text("Language", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("ru" to "Русский", "en" to "English").forEach { (code, label) ->
                        FilterChip(
                            selected = language == code,
                            onClick = { language = code },
                            label = { Text(text = label, maxLines = 1, softWrap = false) }
                        )
                    }
                }

                // Achievements Button in Settings
                OutlinedButton(
                    onClick = { 
                        onDismiss()
                        onNavigateToAchievements() 
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Star, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Achievements", maxLines = 1, softWrap = false)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(currentSettings.copy(
                    size = size,
                    mode = mode,
                    challenge = challenge,
                    imageUri = imageUri,
                    language = language,
                    
                    // Colors
                    paletteType = paletteType,
                    colorPattern = colorPattern,
                    isColorBlindMode = isColorBlindMode,
                    showColorLabels = showColorLabels,
                    isChromaticAdaptation = isChromaticAdaptation,
                    adaptationIntensity = adaptationIntensity,
                    isDynamicPalette = isDynamicPalette,
                    dynamicPaletteSpeed = dynamicPaletteSpeed,
                    endPaletteType = endPaletteType,
                    customPaletteColors = customPaletteColors
                ))
            }) {
                Text("Save & Restart", maxLines = 1, softWrap = false)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", maxLines = 1, softWrap = false)
            }
        }
    )

    if (showPaletteEditor) {
        PaletteEditorDialog(
            initialColors = if (customPaletteColors.isEmpty()) GameEngine.generatePalette(size * size, paletteType) else customPaletteColors,
            onDismiss = { showPaletteEditor = false },
            onSave = { 
                customPaletteColors = it
                showPaletteEditor = false
            }
        )
    }
}
