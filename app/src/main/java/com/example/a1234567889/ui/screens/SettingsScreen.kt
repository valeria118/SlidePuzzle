package com.example.a1234567889.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a1234567889.models.*

enum class SettingsCategory(val title: String, val icon: ImageVector) {
    LANGUAGE("Язык и регион", Icons.Default.Language),
    APPEARANCE("Внешний вид", Icons.Default.Palette),
    SOUND("Звук и уведомления", Icons.AutoMirrored.Filled.VolumeUp),
    GAME("Игровые параметры", Icons.Default.Games),
    PROFILE("Профиль и аккаунт", Icons.Default.Person),
    STATS("Статистика", Icons.Default.BarChart),
    INTERFACE("Управление", Icons.Default.TouchApp),
    ABOUT("О программе", Icons.Default.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentSettings: GameSettings,
    onNavigateBack: () -> Unit,
    onSave: (GameSettings) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(SettingsCategory.LANGUAGE) }
    var tempSettings by remember { mutableStateOf(currentSettings) }
    var isSidebarExpanded by remember { mutableStateOf(false) }
    
    val sidebarWidth by animateDpAsState(
        targetValue = if (isSidebarExpanded) 280.dp else 0.dp,
        label = "SidebarWidth"
    )
    val arrowRotation by animateFloatAsState(
        targetValue = if (isSidebarExpanded) 180f else 0f,
        label = "ArrowRotation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { tempSettings = GameSettings() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Сбросить")
                    }
                    Button(
                        onClick = { onSave(tempSettings) },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Применить", maxLines = 1, softWrap = false)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Sidebar
                Surface(
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .width(sidebarWidth)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.type == PointerEventType.Exit) {
                                        isSidebarExpanded = false
                                    }
                                }
                            }
                        }
                ) {
                    if (sidebarWidth > 100.dp) { // Show content only when expanded
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            SettingsCategory.entries.forEach { category ->
                                NavigationDrawerItem(
                                    label = { 
                                        Text(
                                            text = category.title, 
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            softWrap = false
                                        ) 
                                    },
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category },
                                    icon = { Icon(category.icon, contentDescription = null) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            
                            TextButton(
                                onClick = { tempSettings = GameSettings() },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.DeleteSweep, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Сбросить всё", maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }

                // Content
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        when (selectedCategory) {
                            SettingsCategory.LANGUAGE -> LanguageSection(tempSettings) { tempSettings = it }
                            SettingsCategory.APPEARANCE -> AppearanceSection(tempSettings) { tempSettings = it }
                            SettingsCategory.SOUND -> SoundSection(tempSettings) { tempSettings = it }
                            SettingsCategory.GAME -> GameSection(tempSettings) { tempSettings = it }
                            SettingsCategory.PROFILE -> ProfileSection()
                            SettingsCategory.STATS -> StatsSection()
                            SettingsCategory.INTERFACE -> InterfaceSection(tempSettings) { tempSettings = it }
                            SettingsCategory.ABOUT -> AboutSection()
                        }
                    }
                }
            }

            // Sidebar Handle (The arrow indicator)
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
                tonalElevation = 6.dp,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .offset(x = sidebarWidth - 20.dp)
                    .size(40.dp)
                    .align(Alignment.CenterStart)
                    .clickable { isSidebarExpanded = !isSidebarExpanded }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Enter || event.type == PointerEventType.Move) {
                                    isSidebarExpanded = true
                                }
                            }
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (isSidebarExpanded) "Свернуть" else "Развернуть",
                        modifier = Modifier.rotate(arrowRotation)
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageSection(settings: GameSettings, onUpdate: (GameSettings) -> Unit) {
    SectionTitle("Язык и регион")
    
    var expanded by remember { mutableStateOf(false) }
    val languages = listOf("ru" to "Русский", "en" to "English", "de" to "Deutsch")
    
    Box {
        OutlinedCard(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            ListItem(
                headlineContent = { Text("Язык интерфейса") },
                trailingContent = { Text(languages.find { it.first == settings.language }?.second ?: "Unknown") }
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            languages.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onUpdate(settings.copy(language = code))
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AppearanceSection(settings: GameSettings, onUpdate: (GameSettings) -> Unit) {
    SectionTitle("Внешний вид и темы")
    
    // Theme
    Text("Тема оформления", style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
    OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(null to "Авто", false to "Светлая", true to "Тёмная").forEach { (valBool, label) ->
            FilterChip(
                selected = settings.isDarkTheme == valBool,
                onClick = { onUpdate(settings.copy(isDarkTheme = valBool)) },
                label = { Text(label, maxLines = 1, softWrap = false) },
                leadingIcon = if (settings.isDarkTheme == valBool) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null
            )
        }
    }

    // Color Schemes
    Text("Цветовая схема", style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
    val schemes = listOf("Classic", "Ocean", "Forest", "Space", "Pastel")
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        schemes.forEach { scheme ->
            FilterChip(
                selected = settings.colorScheme == scheme,
                onClick = { onUpdate(settings.copy(colorScheme = scheme)) },
                label = { Text(scheme, maxLines = 1, softWrap = false) },
                leadingIcon = if (settings.colorScheme == scheme) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null
            )
        }
    }

    // Animation Speed
    Text("Скорость анимации", style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AnimationSpeed.entries.forEach { speed ->
            val label = when(speed) {
                AnimationSpeed.SLOW -> "Медленная"
                AnimationSpeed.MEDIUM -> "Средняя"
                AnimationSpeed.FAST -> "Быстрая"
            }
            FilterChip(
                selected = settings.animationSpeed == speed,
                onClick = { onUpdate(settings.copy(animationSpeed = speed)) },
                label = { Text(label, maxLines = 1, softWrap = false) }
            )
        }
    }
}

@Composable
fun SoundSection(settings: GameSettings, onUpdate: (GameSettings) -> Unit) {
    val soundContext = androidx.compose.ui.platform.LocalContext.current
    val soundManager = remember { com.example.a1234567889.logic.SoundManager.getInstance(soundContext) }

    // Lab item 6 — keep SoundPool's volume/enabled flag synced with the saved settings.
    LaunchedEffect(settings.sfxVolume, settings.isSfxEnabled) {
        soundManager.volume = settings.sfxVolume
        soundManager.isEnabled = settings.isSfxEnabled
    }

    SectionTitle("Звук и уведомления")
    
    ListItem(
        headlineContent = { Text("Звуковые эффекты") },
        trailingContent = {
            Switch(checked = settings.isSfxEnabled, onCheckedChange = { onUpdate(settings.copy(isSfxEnabled = it)) })
        }
    )
    if (settings.isSfxEnabled) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Громкость эффектов: ${(settings.sfxVolume * 100).toInt()}%")
            Slider(
                value = settings.sfxVolume,
                onValueChange = { onUpdate(settings.copy(sfxVolume = it)) }
            )
            // Lab item 6 — "Воспроизведение, остановка, управление громкостью" demo controls.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { soundManager.playClick() }, modifier = Modifier.weight(1f)) {
                    Text("▶ Тест звука", maxLines = 1)
                }
                OutlinedButton(onClick = { soundManager.stopAll() }, modifier = Modifier.weight(1f)) {
                    Text("⏹ Стоп", maxLines = 1)
                }
            }
        }
    }

    ListItem(
        headlineContent = { Text("Музыка в меню") },
        trailingContent = {
            Switch(checked = settings.isMusicEnabled, onCheckedChange = { onUpdate(settings.copy(isMusicEnabled = it)) })
        }
    )
    if (settings.isMusicEnabled) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Громкость музыки: ${(settings.musicVolume * 100).toInt()}%")
            Slider(
                value = settings.musicVolume,
                onValueChange = { onUpdate(settings.copy(musicVolume = it)) }
            )
        }
    }

    ListItem(
        headlineContent = { Text("Вибрация") },
        supportingContent = { Text("Тактильная отдача при перемещении") },
        trailingContent = {
            Switch(checked = settings.isVibrationEnabled, onCheckedChange = { onUpdate(settings.copy(isVibrationEnabled = it)) })
        }
    )

    ListItem(
        headlineContent = { Text("Уведомления о достижениях") },
        trailingContent = {
            Switch(checked = settings.notifyAchievements, onCheckedChange = { onUpdate(settings.copy(notifyAchievements = it)) })
        }
    )

    ToastDemoSection()
}

/**
 * Lab item 3 — "Всплывающие сообщения Toast. Настройка положения и длительности".
 * Lets the user pick a Toast position and duration, then show one immediately.
 */
@Composable
private fun ToastDemoSection() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var position by remember { mutableStateOf(com.example.a1234567889.logic.ToastHelper.Position.BOTTOM) }
    var durationMs by remember { mutableStateOf(2000L) }

    SectionTitle("Демонстрация Toast")
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("Положение:", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            listOf(
                com.example.a1234567889.logic.ToastHelper.Position.TOP to "Сверху",
                com.example.a1234567889.logic.ToastHelper.Position.CENTER to "По центру",
                com.example.a1234567889.logic.ToastHelper.Position.BOTTOM to "Снизу"
            ).forEach { (pos, label) ->
                FilterChip(
                    selected = position == pos,
                    onClick = { position = pos },
                    label = { Text(label, maxLines = 1, softWrap = false) }
                )
            }
        }
        Text("Длительность: ${durationMs / 1000.0}с", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            listOf(1000L to "1с", 2000L to "2с", 4000L to "4с").forEach { (dur, label) ->
                FilterChip(
                    selected = durationMs == dur,
                    onClick = { durationMs = dur },
                    label = { Text(label, maxLines = 1, softWrap = false) }
                )
            }
        }
        Button(
            onClick = {
                com.example.a1234567889.logic.ToastHelper.show(context, "Привет! Это тестовое сообщение 👋", position, durationMs)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Показать Toast")
        }
    }
}

@Composable
fun GameSection(settings: GameSettings, onUpdate: (GameSettings) -> Unit) {
    SectionTitle("Игровые параметры")

    Text("Размер поля по умолчанию", style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
    OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        (3..8).forEach { size ->
            FilterChip(
                selected = settings.size == size,
                onClick = { onUpdate(settings.copy(size = size)) },
                label = { Text("${size}×${size}", maxLines = 1, softWrap = false) }
            )
        }
    }
    
    // Add selected size description
    Text(
        text = when(settings.size) {
            3 -> "3×3 — 8 плиток, идеальный уровень для новичков"
            4 -> "4×4 — 15 плиток, классический вариант игры"
            5 -> "5×5 — 24 плитки, для опытных игроков"
            6 -> "6×6 — 35 плиток, для любителей сложных головоломок"
            7 -> "7×7 — 48 плиток, экстремальный уровень сложности"
            8 -> "8×8 — 63 плитки, настоящий хардкор"
            else -> ""
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
        maxLines = 2
    )

    Spacer(modifier = Modifier.height(16.dp))

    ListItem(
        headlineContent = { Text("Показывать подсказки") },
        supportingContent = { Text("Подсветка возможных ходов") },
        trailingContent = {
            Switch(checked = settings.showHints, onCheckedChange = { onUpdate(settings.copy(showHints = it)) })
        }
    )

    ListItem(
        headlineContent = { Text("Автозапуск таймера") },
        supportingContent = { Text("Запускать таймер сразу при загрузке поля") },
        trailingContent = {
            Switch(checked = settings.autoTimerStart, onCheckedChange = { onUpdate(settings.copy(autoTimerStart = it)) })
        }
    )

    ListItem(
        headlineContent = { Text("Разрешить совпадения") },
        supportingContent = { Text("Разрешить плиткам оставаться на своих местах после перемешивания") },
        trailingContent = {
            Switch(checked = settings.allowMatches, onCheckedChange = { onUpdate(settings.copy(allowMatches = it)) })
        }
    )
}

@Composable
fun InterfaceSection(settings: GameSettings, onUpdate: (GameSettings) -> Unit) {
    SectionTitle("Управление и интерфейс")
    
    Text("Способ управления", style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
    OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ControlMethod.entries.forEach { method ->
            val label = when(method) {
                ControlMethod.CLICK -> "Клик"
                ControlMethod.SWIPE -> "Свайп"
                ControlMethod.KEYBOARD -> "Клавиатура"
            }
            FilterChip(
                selected = settings.controlMethod == method,
                onClick = { onUpdate(settings.copy(controlMethod = method)) },
                label = { Text(label, maxLines = 1, softWrap = false) }
            )
        }
    }

    ListItem(
        headlineContent = { Text("Отображение счётчиков", maxLines = 1, softWrap = false) },
        supportingContent = { Text("Показывать таймер и количество ходов") },
        trailingContent = {
            Switch(checked = settings.showCounters, onCheckedChange = { onUpdate(settings.copy(showCounters = it)) })
        }
    )

    Text("Размер плиток", style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TileSize.entries.forEach { size ->
            val label = when(size) {
                TileSize.AUTO -> "Авто"
                TileSize.SMALL -> "Маленький"
                TileSize.MEDIUM -> "Средний"
                TileSize.LARGE -> "Большой"
            }
            FilterChip(
                selected = settings.tileSize == size,
                onClick = { onUpdate(settings.copy(tileSize = size)) },
                label = { Text(label, maxLines = 1, softWrap = false) }
            )
        }
    }
}

@Composable
fun ProfileSection() {
    SectionTitle("Профиль и аккаунт")
    
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Управление аккаунтом", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Text("Редактировать профиль", maxLines = 1, softWrap = false)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Text("Привязать социальные сети", maxLines = 1, softWrap = false)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    
    Text("Конфиденциальность", style = MaterialTheme.typography.titleMedium)
    ListItem(
        headlineContent = { Text("Видимость профиля") },
        trailingContent = {
            Text("Только друзья")
        },
        modifier = Modifier.clickable {  }
    )
}

@Composable
fun StatsSection() {
    SectionTitle("Статистика и рекорды")
    
    ListItem(
        headlineContent = { Text("Отображение статистики") },
        supportingContent = { Text("Показывать краткую информацию на главном экране") },
        trailingContent = {
            Switch(checked = true, onCheckedChange = { })
        }
    )

    Spacer(modifier = Modifier.height(16.dp))
    
    Text("Данные", style = MaterialTheme.typography.titleMedium)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Download, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Экспорт", maxLines = 1, softWrap = false)
        }
        OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Upload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Импорт", maxLines = 1, softWrap = false)
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    
    Button(
        onClick = {},
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Сбросить всю статистику", maxLines = 1, softWrap = false)
    }
}

@Composable
fun AboutSection() {
    SectionTitle("О программе")
    Text("Версия 1.0.0")
    Text("Разработано с любовью")
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp),
        maxLines = 1,
        softWrap = false
    )
    HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))
}

@Preview(showBackground = true, widthDp = 800, heightDp = 600)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(
        currentSettings = GameSettings(),
        onNavigateBack = {},
        onSave = {}
    )
}
