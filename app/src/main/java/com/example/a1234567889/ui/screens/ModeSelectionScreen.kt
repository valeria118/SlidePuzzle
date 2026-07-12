package com.example.a1234567889.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.a1234567889.R
import com.example.a1234567889.logic.PictureProvider
import com.example.a1234567889.models.*

data class GameModeItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val mode: GameMode,
    val progress: String? = null,
    val goal: String,
    val rules: List<String>,
    val tip: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSelectionScreen(
    user: User?,
    stats: UserStats,
    settings: GameSettings,
    configuringMode: GameMode?,
    configuringSettings: GameSettings?,
    onStartConfiguring: (GameMode, GameSettings) -> Unit,
    onUpdateConfiguringSettings: (GameSettings) -> Unit,
    onStopConfiguring: () -> Unit,
    onModeSelected: (GameSettings, Boolean) -> Unit,
    onSettingsChanged: (GameSettings) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSelectPicture: (boardSize: Int, currentUri: String?, onSelected: (String) -> Unit) -> Unit = { _, _, _ -> },
    onOpenDrawer: () -> Unit = {}
) {
    var showRulesForMode by remember { mutableStateOf<GameModeItem?>(null) }
    var showLockDialogForMode by remember { mutableStateOf<GameModeItem?>(null) }

    val actualClassicWins = stats.classicGamesSolved

    val modes = listOf(
        GameModeItem(
            stringResource(R.string.mode_classic),
            stringResource(R.string.mode_classic_desc),
            Icons.Default.Pin,
            Color(0xFF2196F3),
            GameMode.NUMBERS,
            goal = "Расположить плитки с числами в порядке возрастания слева направо, сверху вниз.",
            rules = listOf(
                "Поле представляет собой квадрат N×N.",
                "Плитки пронумерованы от 1 до N²–1.",
                "Перемещать можно только соседние с пустой ячейкой плитки.",
                "Нельзя перемещать плитки по диагонали.",
                "Пустая ячейка должна оказаться в правом нижнем углу."
            ),
            tip = "Можно включить режим «Без возврата» для усложнения."
        ),
        GameModeItem(
            stringResource(R.string.mode_image),
            stringResource(R.string.mode_image_desc),
            Icons.Default.Image,
            Color(0xFF4CAF50),
            GameMode.IMAGE,
            progress = "${user?.gallery?.count { it.isCollected } ?: 0}/${user?.gallery?.size ?: 0}",
            goal = "Собрать из фрагментов целостное изображение.",
            rules = listOf(
                "Изображение разрезается на N×N фрагментов.",
                "Один фрагмент удаляется (становится пустой ячейкой).",
                "Перемещение фрагментов происходит по классическим правилам.",
                "Победа — когда все фрагменты на своих местах."
            ),
            tip = "Используйте режим «Подсказка-призрак» для облегчения сборки."
        ),
        GameModeItem(
            stringResource(R.string.mode_colors),
            stringResource(R.string.mode_colors_desc),
            Icons.Default.Palette,
            Color(0xFFFF9800),
            GameMode.COLORS,
            goal = "Расположить цветные плитки в заданной цветовой последовательности.",
            rules = listOf(
                "Каждая плитка имеет уникальный цвет.",
                "Задача — упорядочить их согласно выбранной схеме (радуга, градиент).",
                "Перемещение происходит по классическим правилам."
            ),
            tip = "Используйте различные палитры для разнообразия."
        ),
        GameModeItem(
            "Вызов",
            "Нестандартные цели: обратный порядок, спираль",
            Icons.Default.FlashOn,
            Color(0xFFFF5722),
            GameMode.CHALLENGE,
            goal = "Выполнить нестандартное задание с изменённой целью сборки.",
            rules = listOf(
                "Обратный порядок — числа в убывающем порядке.",
                "Спираль — плитки выстраиваются от центра к краям.",
                "Диагональ — специальные условия для главной диагонали.",
                "За каждый уникальный вызов выдаётся бейдж."
            )
        ),
        GameModeItem(
            "Соревнование",
            "Соревнуйся с друзьями в реальном времени",
            Icons.Default.People,
            Color(0xFF9C27B0),
            GameMode.COMPETITION,
            goal = "Собрать головоломку быстрее друга.",
            rules = listOf(
                "Генерируется одинаковое поле для обоих игроков.",
                "Победитель тот, кто соберет первым или за меньше ходов.",
                "Результат сохраняется в статистику дуэлей."
            ),
            tip = "Необходимо иметь аккаунт и хотя бы одного друга."
        ),
        GameModeItem(
            "На время",
            "Собери пазл за ограниченное время",
            Icons.Default.Timer,
            Color(0xFFF44336),
            GameMode.TIME_ATTACK,
            goal = "Собрать головоломку до истечения времени.",
            rules = listOf(
                "Устанавливается лимит времени (например, 60 сек для 4×4).",
                "Таймер начинает отсчёт с первого хода.",
                "В режиме выживания время урезается после каждой победы."
            )
        ),
        GameModeItem(
            "Бесконечный",
            "Бесконечная серия пазлов",
            Icons.Default.AllInclusive,
            Color(0xFF607D8B),
            GameMode.ENDLESS,
            goal = "Пройти как можно больше уровней.",
            rules = listOf(
                "Размер поля может увеличиваться каждые несколько уровней.",
                "Количество перемешивающих ходов постоянно растёт.",
                "Игра продолжается до первой ошибки или нажатия «Стоп»."
            )
        )
    )

    var showOptionsMenu by remember { mutableStateOf(false) }
    val optionsMenuContext = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                navigationIcon = {
                    // Lab item 11 — "Меню-шторка" (Navigation Drawer) entry point.
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Открыть меню")
                    }
                },
                actions = {
                    if (user == null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(end = 8.dp).clickable { onNavigateToProfile() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.NoAccounts, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Гость", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    } else {
                        IconButton(onClick = onNavigateToProfile) {
                            Icon(Icons.Default.AccountCircle, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Lab item 11 — "Главное меню" (Options Menu): contextual quick actions.
                    Box {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Дополнительные действия")
                        }
                        DropdownMenu(expanded = showOptionsMenu, onDismissRequest = { showOptionsMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Поделиться приложением") },
                                leadingIcon = { Icon(Icons.Default.Share, null) },
                                onClick = {
                                    showOptionsMenu = false
                                    com.example.a1234567889.logic.IntentHelper.shareText(
                                        optionsMenuContext,
                                        "Попробуй Slide Puzzle — отличная игра-пятнашки! https://play.google.com/store/apps/details?id=${optionsMenuContext.packageName}",
                                        "Поделиться приложением"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Связаться с разработчиком") },
                                leadingIcon = { Icon(Icons.Default.Email, null) },
                                onClick = {
                                    showOptionsMenu = false
                                    com.example.a1234567889.logic.IntentHelper.sendEmail(
                                        optionsMenuContext, "support@slidepuzzle.example", "Slide Puzzle"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Оценить в Google Play") },
                                leadingIcon = { Icon(Icons.Default.Star, null) },
                                onClick = {
                                    showOptionsMenu = false
                                    com.example.a1234567889.logic.IntentHelper.openPlayStore(optionsMenuContext)
                                }
                            )
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
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Выбери режим игры",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Text(
                text = "Начни свое путешествие в мире пазлов",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(modes) { modeItem ->
                    val lockInfo = getLockInfo(modeItem.mode, actualClassicWins, user != null)
                    ModeCard(
                        item = modeItem, 
                        isLocked = lockInfo.isLocked,
                        lockLabel = lockInfo.shortReason,
                        onClick = { 
                            if (lockInfo.isLocked) {
                                showLockDialogForMode = modeItem
                            } else {
                                if (modeItem.mode == GameMode.COMPETITION || modeItem.mode == GameMode.CHALLENGE) {
                                    onModeSelected(settings.copy(mode = modeItem.mode), true)
                                } else {
                                    // Use current settings as base for the dialog's initial state
                                    onStartConfiguring(modeItem.mode, settings)
                                }
                            }
                        },
                        onInfoClick = { showRulesForMode = modeItem }
                    )
                }
            }
        }

        if (configuringMode != null && configuringSettings != null) {
            val modeItem = modes.find { it.mode == configuringMode } ?: modes[0]
            ModeSettingsDialog(
                modeItem = modeItem,
                currentSettings = configuringSettings,
                user = user,
                onDismiss = onStopConfiguring,
                onStart = { updatedSettings ->
                    onModeSelected(updatedSettings, true)
                    onStopConfiguring()
                },
                onShowRules = { showRulesForMode = modeItem },
                onSelectPicture = { boardSize, currentUri, onSelected ->
                    onSelectPicture(boardSize, currentUri) { uri ->
                        onSelected(uri)
                    }
                },
                onSettingsChanged = onUpdateConfiguringSettings
            )
        }

        showRulesForMode?.let { modeItem ->
            RulesDialog(
                modeItem = modeItem,
                onDismiss = { showRulesForMode = null }
            )
        }

        showLockDialogForMode?.let { modeItem ->
            val lockInfo = getLockInfo(modeItem.mode, actualClassicWins, user != null)
            RequirementDialog(
                modeTitle = modeItem.title,
                reason = lockInfo.fullReason,
                canNavigateToProfile = modeItem.mode == GameMode.COMPETITION && user == null,
                onDismiss = { showLockDialogForMode = null },
                onNavigateToProfile = {
                    showLockDialogForMode = null
                    onNavigateToProfile()
                }
            )
        }
    }
}

data class ModeLockInfo(
    val isLocked: Boolean,
    val shortReason: String? = null,
    val fullReason: String = ""
)

private fun getLockInfo(mode: GameMode, classicWins: Int, isLoggedIn: Boolean): ModeLockInfo {
    return when (mode) {
        GameMode.CHALLENGE -> {
            if (classicWins < 3) ModeLockInfo(true, "Нужно 3 победы", "Для открытия режима вызовов нужно собрать минимум 3 классические партии (Цифры). У вас побед: $classicWins")
            else ModeLockInfo(false)
        }
        GameMode.COMPETITION -> {
            if (!isLoggedIn) ModeLockInfo(true, "Нужен аккаунт", "Соревнования доступны только для зарегистрированных пользователей. Пожалуйста, войдите в свой профиль.")
            else ModeLockInfo(false)
        }
        GameMode.ENDLESS -> {
            if (classicWins < 1) ModeLockInfo(true, "Нужна 1 победа", "Сначала соберите хотя бы одну классическую партию, чтобы открыть бесконечный режим!")
            else ModeLockInfo(false)
        }
        else -> ModeLockInfo(false)
    }
}

@Composable
fun RequirementDialog(
    modeTitle: String,
    reason: String,
    canNavigateToProfile: Boolean,
    onDismiss: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Режим заблокирован") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Чтобы играть в режим «$modeTitle», необходимо выполнить условия:", style = MaterialTheme.typography.bodyMedium)
                Text(reason, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            if (canNavigateToProfile) {
                Button(onClick = onNavigateToProfile) { Text("Войти", maxLines = 1, softWrap = false) }
            } else {
                Button(onClick = onDismiss) { Text("Понятно", maxLines = 1, softWrap = false) }
            }
        },
        dismissButton = {
            if (canNavigateToProfile) {
                TextButton(onClick = onDismiss) { Text("Отмена", maxLines = 1, softWrap = false) }
            }
        }
    )
}

@Composable
fun RulesDialog(
    modeItem: GameModeItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Понятно", maxLines = 1, softWrap = false) }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(modeItem.icon, null, tint = modeItem.color, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Правила: ${modeItem.title}")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Цель:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(modeItem.goal)
                
                Text(text = "Правила:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                modeItem.rules.forEach { rule ->
                    Row {
                        Text("• ", fontWeight = FontWeight.Bold)
                        Text(rule)
                    }
                }

                modeItem.tip?.let { tip ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tip, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun ModeSettingsDialog(
    modeItem: GameModeItem,
    currentSettings: GameSettings,
    user: User?,
    onDismiss: () -> Unit,
    onStart: (GameSettings) -> Unit,
    onShowRules: () -> Unit,
    onSelectPicture: (boardSize: Int, currentUri: String?, onSelected: (String) -> Unit) -> Unit = { _, _, _ -> },
    onSettingsChanged: (GameSettings) -> Unit = {}
) {
    var settings by remember(currentSettings) { mutableStateOf(currentSettings) }
    var showPaletteEditor by remember { mutableStateOf(false) }

    LaunchedEffect(settings) {
        if (settings != currentSettings) {
            onSettingsChanged(settings)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(modeItem.title)
                IconButton(onClick = onShowRules) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, "Правила", tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        text = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(modeItem.description, style = MaterialTheme.typography.bodyMedium)

                    if (modeItem.mode != GameMode.ENDLESS) {
                        SizeSelector(
                            selectedSize = settings.size,
                            onSizeSelected = { newSize ->
                                var newSettings = settings.copy(size = newSize)
                                
                                // If in Image mode, ensure the selected image is valid for the new size
                                if (settings.mode == GameMode.IMAGE && settings.imageUri != null) {
                                    if (!PictureProvider.isValidForSize(settings.imageUri!!, newSize)) {
                                        // Auto-switch to a default image for the new size
                                        val defaultImg = PictureProvider.defaultImageForSize(newSize)
                                        newSettings = newSettings.copy(imageUri = defaultImg?.uri)
                                    }
                                }
                                
                                settings = newSettings
                            }
                        )
                    }

                    // Mode Specific Settings
                    when (modeItem.mode) {
                        GameMode.NUMBERS -> {
                            Text("Выберите расположение (паттерн):", style = MaterialTheme.typography.labelLarge)
                            OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val patterns = listOf(
                                    ChallengeMode.NORMAL to "По порядку",
                                    ChallengeMode.REVERSE to "Обратно",
                                    ChallengeMode.SPIRAL_IN to "Спираль",
                                    ChallengeMode.DIAGONAL to "Диагональ",
                                    ChallengeMode.MIRROR_H to "Зеркало (Г)",
                                    ChallengeMode.MIRROR_V to "Зеркало (В)",
                                    ChallengeMode.RANDOM to "Случайный"
                                )
                                patterns.forEach { (mode, label) ->
                                    FilterChip(
                                        selected = settings.challenge == mode,
                                        onClick = { settings = settings.copy(challenge = mode) },
                                        label = { Text(label, maxLines = 1, softWrap = false) }
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            com.example.a1234567889.ui.components.BoardPreview(
                                settings = settings,
                                modifier = Modifier
                                    .size(150.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                        }
                        GameMode.IMAGE -> {
                            // Selected image preview
                            if (settings.imageUri != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = coil.compose.rememberAsyncImagePainter(settings.imageUri),
                                        contentDescription = null,
                                        modifier = Modifier.size(72.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp)),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Картинка выбрана", style = MaterialTheme.typography.labelMedium, color = Color(0xFF4CAF50))
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        OutlinedButton(
                                            onClick = {
                                                onSelectPicture(settings.size, settings.imageUri) { uri ->
                                                    settings = settings.copy(imageUri = uri)
                                                    onSettingsChanged(settings)
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(androidx.compose.material.icons.Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Изменить", fontSize = 12.sp)
                                        }
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        onSelectPicture(settings.size, settings.imageUri) { uri ->
                                            settings = settings.copy(imageUri = uri)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(androidx.compose.material.icons.Icons.Default.Image, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Выбрать картинку")
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { settings = settings.copy(showHints = !settings.showHints) }) {
                                Checkbox(
                                    checked = settings.showHints,
                                    onCheckedChange = { settings = settings.copy(showHints = it) }
                                )
                                Text("Показать подсказку")
                            }
                        }
                        GameMode.COLORS -> {
                            Text("Выберите палитру:", style = MaterialTheme.typography.labelLarge)
                            OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val palettes = listOf(
                                    ColorPaletteType.RAINBOW to "Радуга",
                                    ColorPaletteType.GRADIENT to "Градиент",
                                    ColorPaletteType.MONOCHROME to "Монохром",
                                    ColorPaletteType.PASTEL to "Пастель",
                                    ColorPaletteType.BRIGHT to "Яркая",
                                    ColorPaletteType.CHESSBOARD to "Шахматы",
                                    ColorPaletteType.RANDOM to "Случайная"
                                )
                                palettes.forEach { (palette, label) ->
                                    FilterChip(
                                        selected = settings.paletteType == palette,
                                        onClick = { settings = settings.copy(paletteType = palette) },
                                        label = { Text(label, maxLines = 1, softWrap = false) }
                                    )
                                }
                            }
                            
                            Button(
                                onClick = { showPaletteEditor = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.Add, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Создать свою", maxLines = 1, softWrap = false)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Выберите паттерн:", style = MaterialTheme.typography.labelLarge)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val patterns = listOf(
                                    ColorPattern.SPECTRUM to "Спектр",
                                    ColorPattern.LINEAR to "Линейный",
                                    ColorPattern.CHESSBOARD to "Шахматы",
                                    ColorPattern.SPIRAL to "Спираль",
                                    ColorPattern.DIAGONAL to "Диагональ",
                                    ColorPattern.CONCENTRIC to "Кольца",
                                    ColorPattern.RANDOM to "Случайный"
                                )
                                patterns.forEach { (pattern, label) ->
                                    FilterChip(
                                        selected = settings.colorPattern == pattern,
                                        onClick = { settings = settings.copy(colorPattern = pattern) },
                                        label = { Text(label, maxLines = 1, softWrap = false) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            com.example.a1234567889.ui.components.BoardPreview(
                                settings = settings,
                                modifier = Modifier
                                    .size(150.dp)
                                    .align(Alignment.CenterHorizontally)
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { settings = settings.copy(isColorBlindMode = !settings.isColorBlindMode) }) {
                                    Checkbox(
                                        checked = settings.isColorBlindMode,
                                        onCheckedChange = { settings = settings.copy(isColorBlindMode = it) }
                                    )
                                    Text("Режим для дальтоников", style = MaterialTheme.typography.bodyMedium)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { settings = settings.copy(showColorLabels = !settings.showColorLabels) }) {
                                    Checkbox(
                                        checked = settings.showColorLabels,
                                        onCheckedChange = { settings = settings.copy(showColorLabels = it) }
                                    )
                                    Text("Показывать номера", style = MaterialTheme.typography.bodyMedium)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { settings = settings.copy(isChromaticAdaptation = !settings.isChromaticAdaptation) }) {
                                    Checkbox(
                                        checked = settings.isChromaticAdaptation,
                                        onCheckedChange = { settings = settings.copy(isChromaticAdaptation = it) }
                                    )
                                    Text("Хроматическая адаптация", style = MaterialTheme.typography.bodyMedium)
                                }
                                if (settings.isChromaticAdaptation) {
                                    Text("Интенсивность: ${(settings.adaptationIntensity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                                    Slider(value = settings.adaptationIntensity, onValueChange = { settings = settings.copy(adaptationIntensity = it) })
                                }
                            }
                        }
                        GameMode.TIME_ATTACK -> {
                            Text("Базовый режим:", style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(GameMode.NUMBERS, GameMode.IMAGE, GameMode.COLORS).forEach { base ->
                                    FilterChip(
                                        selected = settings.timeAttackBaseMode == base,
                                        onClick = { settings = settings.copy(timeAttackBaseMode = base) },
                                        label = { Text(
                                            text = when(base) {
                                                GameMode.NUMBERS -> "Числа"
                                                GameMode.IMAGE -> "Картинка"
                                                GameMode.COLORS -> "Цвета"
                                                else -> ""
                                            },
                                            maxLines = 1,
                                            softWrap = false
                                        ) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            if (settings.timeAttackBaseMode == GameMode.NUMBERS) {
                                Text("Выберите расположение (паттерн):", style = MaterialTheme.typography.labelLarge)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val patterns = listOf(
                                        ChallengeMode.NORMAL to "По порядку",
                                        ChallengeMode.REVERSE to "Обратно",
                                        ChallengeMode.SPIRAL_IN to "Спираль",
                                        ChallengeMode.DIAGONAL to "Диагональ",
                                        ChallengeMode.MIRROR_H to "Зеркало (Г)",
                                        ChallengeMode.MIRROR_V to "Зеркало (В)",
                                        ChallengeMode.RANDOM to "Случайный"
                                    )
                                    patterns.forEach { (mode, label) ->
                                        FilterChip(
                                            selected = settings.challenge == mode,
                                            onClick = { settings = settings.copy(challenge = mode) },
                                            label = { Text(label, maxLines = 1, softWrap = false) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                com.example.a1234567889.ui.components.BoardPreview(
                                    settings = settings.copy(mode = GameMode.NUMBERS),
                                    modifier = Modifier
                                        .size(120.dp)
                                        .align(Alignment.CenterHorizontally)
                                )
                            } else if (settings.timeAttackBaseMode == GameMode.IMAGE) {
                                if (settings.imageUri != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        androidx.compose.foundation.Image(
                                            painter = coil.compose.rememberAsyncImagePainter(settings.imageUri),
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                onSelectPicture(settings.size, settings.imageUri) { uri ->
                                                    settings = settings.copy(imageUri = uri)
                                                    onSettingsChanged(settings)
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Изменить картинку")
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            onSelectPicture(settings.size, settings.imageUri) { uri ->
                                                settings = settings.copy(imageUri = uri)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(androidx.compose.material.icons.Icons.Default.Image, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Выбрать картинку")
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { settings = settings.copy(showHints = !settings.showHints) }) {
                                    Checkbox(
                                        checked = settings.showHints,
                                        onCheckedChange = { settings = settings.copy(showHints = it) }
                                    )
                                    Text("Показать подсказку", style = MaterialTheme.typography.bodyMedium)
                                }
                            } else if (settings.timeAttackBaseMode == GameMode.COLORS) {
                                Text("Выберите палитру:", style = MaterialTheme.typography.labelLarge)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val palettes = listOf(
                                        ColorPaletteType.RAINBOW to "Радуга",
                                        ColorPaletteType.GRADIENT to "Градиент",
                                        ColorPaletteType.MONOCHROME to "Монохром",
                                        ColorPaletteType.PASTEL to "Пастель",
                                        ColorPaletteType.BRIGHT to "Яркая",
                                        ColorPaletteType.CHESSBOARD to "Шахматы",
                                        ColorPaletteType.RANDOM to "Случайная"
                                    )
                                    palettes.forEach { (palette, label) ->
                                        FilterChip(
                                            selected = settings.paletteType == palette,
                                            onClick = { settings = settings.copy(paletteType = palette) },
                                            label = { Text(label, maxLines = 1, softWrap = false) }
                                        )
                                    }
                                }

                                Text("Выберите паттерн:", style = MaterialTheme.typography.labelLarge)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val patterns = listOf(
                                        ColorPattern.SPECTRUM to "Спектр",
                                        ColorPattern.LINEAR to "Линейный",
                                        ColorPattern.CHESSBOARD to "Шахматы",
                                        ColorPattern.SPIRAL to "Спираль",
                                        ColorPattern.DIAGONAL to "Диагональ",
                                        ColorPattern.CONCENTRIC to "Кольца",
                                        ColorPattern.RANDOM to "Случайный"
                                    )
                                    patterns.forEach { (pattern, label) ->
                                        FilterChip(
                                            selected = settings.colorPattern == pattern,
                                            onClick = { settings = settings.copy(colorPattern = pattern) },
                                            label = { Text(label, maxLines = 1, softWrap = false) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                com.example.a1234567889.ui.components.BoardPreview(
                                    settings = settings.copy(mode = GameMode.COLORS),
                                    modifier = Modifier
                                        .size(120.dp)
                                        .align(Alignment.CenterHorizontally)
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { settings = settings.copy(isColorBlindMode = !settings.isColorBlindMode) }) {
                                        Checkbox(checked = settings.isColorBlindMode, onCheckedChange = { settings = settings.copy(isColorBlindMode = it) })
                                        Text("Режим для дальтоников", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { settings = settings.copy(showColorLabels = !settings.showColorLabels) }) {
                                        Checkbox(checked = settings.showColorLabels, onCheckedChange = { settings = settings.copy(showColorLabels = it) })
                                        Text("Показывать номера", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Лимит времени: ${settings.timeAttackLimit} сек")
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(30, 45, 60, 90).forEach { t ->
                                    FilterChip(
                                        selected = settings.timeAttackLimit == t,
                                        onClick = { 
                                            settings = settings.copy(
                                                timeAttackLimit = t,
                                                survivalInitialTime = t
                                            ) 
                                        },
                                        label = { Text("${t}с", maxLines = 1, softWrap = false) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Slider(
                                value = settings.timeAttackLimit.toFloat(),
                                onValueChange = { 
                                    settings = settings.copy(
                                        timeAttackLimit = it.toInt(),
                                        survivalInitialTime = it.toInt()
                                    ) 
                                },
                                valueRange = 15f..120f,
                                steps = 6
                            )
                            
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { settings = settings.copy(isSurvivalMode = !settings.isSurvivalMode) }) {
                                Checkbox(
                                    checked = settings.isSurvivalMode,
                                    onCheckedChange = { settings = settings.copy(isSurvivalMode = it) }
                                )
                                Text("Режим выживания")
                            }

                            if (settings.isSurvivalMode) {
                                Text("Шаг уменьшения времени: ${settings.survivalTimeStep} сек", style = MaterialTheme.typography.labelMedium)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(3, 5, 7, 10).forEach { step ->
                                        FilterChip(
                                            selected = settings.survivalTimeStep == step,
                                            onClick = { settings = settings.copy(survivalTimeStep = step) },
                                            label = { Text("-${step}с", maxLines = 1, softWrap = false) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Text("Увеличение размера через: ${settings.survivalSizeIncreaseInterval} ур.", style = MaterialTheme.typography.labelMedium)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(2, 3, 5, 0).forEach { interval ->
                                        FilterChip(
                                            selected = settings.survivalSizeIncreaseInterval == interval,
                                            onClick = { settings = settings.copy(survivalSizeIncreaseInterval = interval) },
                                            label = { 
                                                Text(
                                                    text = if (interval == 0) "Никогда" else "${interval} ур.",
                                                    maxLines = 1,
                                                    softWrap = false
                                                ) 
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                        GameMode.ENDLESS -> {
                            Text("Базовый режим:", style = MaterialTheme.typography.labelLarge)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(GameMode.NUMBERS, GameMode.COLORS).forEach { visual ->
                                    FilterChip(
                                        selected = settings.endlessVisualMode == visual,
                                        onClick = { settings = settings.copy(endlessVisualMode = visual) },
                                        label = { 
                                            Text(
                                                text = if (visual == GameMode.NUMBERS) "Классический" else "Цветной",
                                                maxLines = 1,
                                                softWrap = false
                                            ) 
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            if (settings.endlessVisualMode == GameMode.COLORS) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Выберите палитру:", style = MaterialTheme.typography.labelLarge)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val palettes = listOf(
                                        ColorPaletteType.RAINBOW to "Радуга",
                                        ColorPaletteType.GRADIENT to "Градиент",
                                        ColorPaletteType.MONOCHROME to "Монохром",
                                        ColorPaletteType.PASTEL to "Пастель",
                                        ColorPaletteType.BRIGHT to "Яркая",
                                        ColorPaletteType.CHESSBOARD to "Шахматы",
                                        ColorPaletteType.RANDOM to "Случайная"
                                    )
                                    palettes.forEach { (palette, label) ->
                                        FilterChip(
                                            selected = settings.paletteType == palette,
                                            onClick = { settings = settings.copy(paletteType = palette) },
                                            label = { Text(label, maxLines = 1, softWrap = false) }
                                        )
                                    }
                                }

                                Text("Выберите паттерн:", style = MaterialTheme.typography.labelLarge)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val patterns = listOf(
                                        ColorPattern.SPECTRUM to "Спектр",
                                        ColorPattern.LINEAR to "Линейный",
                                        ColorPattern.CHESSBOARD to "Шахматы",
                                        ColorPattern.SPIRAL to "Спираль",
                                        ColorPattern.DIAGONAL to "Диагональ",
                                        ColorPattern.CONCENTRIC to "Кольца",
                                        ColorPattern.RANDOM to "Случайный"
                                    )
                                    patterns.forEach { (pattern, label) ->
                                        FilterChip(
                                            selected = settings.colorPattern == pattern,
                                            onClick = { settings = settings.copy(colorPattern = pattern) },
                                            label = { Text(label, maxLines = 1, softWrap = false) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                com.example.a1234567889.ui.components.BoardPreview(
                                    settings = settings.copy(mode = GameMode.COLORS),
                                    modifier = Modifier
                                        .size(120.dp)
                                        .align(Alignment.CenterHorizontally)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                                com.example.a1234567889.ui.components.BoardPreview(
                                    settings = settings.copy(mode = GameMode.NUMBERS, challenge = ChallengeMode.NORMAL),
                                    modifier = Modifier
                                        .size(120.dp)
                                        .align(Alignment.CenterHorizontally)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Тип ограничения:", style = MaterialTheme.typography.labelLarge)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(EndlessLimitType.MOVES, EndlessLimitType.TIME).forEach { type ->
                                    FilterChip(
                                        selected = settings.endlessLimitType == type,
                                        onClick = { settings = settings.copy(endlessLimitType = type) },
                                        label = { 
                                            Text(
                                                text = if (type == EndlessLimitType.MOVES) "По ходам" else "По времени",
                                                maxLines = 1,
                                                softWrap = false
                                            ) 
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Выберите сложность:", style = MaterialTheme.typography.labelLarge)
                            
                            val isTime = settings.endlessLimitType == EndlessLimitType.TIME
                            val endlessDifficulties = listOf(
                                ChallengeDifficulty.EASY to "Лёгкий (❤️×5, ${if(isTime) "60с" else "30х"}, ×1.2)",
                                ChallengeDifficulty.MEDIUM to "Средний (❤️×3, ${if(isTime) "45с" else "20х"}, ×1.4)",
                                ChallengeDifficulty.HARD to "Сложный (❤️×2, ${if(isTime) "30с" else "12х"}, ×1.6)"
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                endlessDifficulties.forEach { (diff, label) ->
                                    FilterChip(
                                        selected = settings.difficulty == diff,
                                        onClick = { settings = settings.copy(difficulty = diff) },
                                        label = { Text(label, maxLines = 1, softWrap = false) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Старт с 3×3. Размер растёт до 8×8, затем цикл повторяется с усложнениями.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {}
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (settings.mode == GameMode.ENDLESS) {
                        // Always start simplified marathon at 3x3
                        onStart(settings.copy(size = 3))
                    } else {
                        onStart(settings) 
                    }
                },
                enabled = if (settings.visualMode == GameMode.IMAGE) settings.imageUri != null else true
            ) {
                Text(if (modeItem.mode == GameMode.ENDLESS) "Начать забег" else "Играть", maxLines = 1, softWrap = false)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", maxLines = 1, softWrap = false)
            }
        }
    )

    if (showPaletteEditor) {
        com.example.a1234567889.ui.components.PaletteEditorDialog(
            initialColors = if (settings.customPaletteColors.isNotEmpty()) {
                settings.customPaletteColors
            } else {
                com.example.a1234567889.logic.GameEngine.generatePalette(
                    settings.size * settings.size,
                    settings.paletteType
                )
            },
            onDismiss = { showPaletteEditor = false },
            onSave = { newColors ->
                settings = settings.copy(customPaletteColors = newColors)
                showPaletteEditor = false
            }
        )
    }
}


@Composable
fun ModeCard(
    item: GameModeItem, 
    isLocked: Boolean = false,
    lockLabel: String? = null,
    onClick: () -> Unit, 
    onInfoClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) Color.Gray.copy(alpha = 0.1f) else item.color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else item.icon,
                    contentDescription = null,
                    tint = if (isLocked) Color.Gray else item.color,
                    modifier = Modifier.size(40.dp)
                )
                if (!isLocked) {
                    IconButton(onClick = onInfoClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Правила",
                            tint = item.color.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Column {
                Text(
                    text = item.title, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 18.sp,
                    color = if (isLocked) Color.Gray else Color.Unspecified,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = if (isLocked) (lockLabel ?: "Заблокировано") else item.description,
                    fontSize = 12.sp,
                    maxLines = 2,
                    lineHeight = 16.sp,
                    color = if (isLocked) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isLocked && item.progress != null) {
                Text(
                    text = item.progress,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = item.color
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ModeSelectionScreenPreview() {
    MaterialTheme {
        ModeSelectionScreen(
            user = null,
            stats = UserStats(classicGamesSolved = 1),
            settings = GameSettings(),
            configuringMode = null,
            configuringSettings = null,
            onStartConfiguring = { _, _ -> },
            onUpdateConfiguringSettings = {},
            onStopConfiguring = {},
            onModeSelected = { _, _ -> },
            onSettingsChanged = {},
            onNavigateToProfile = {},
            onNavigateToLeaderboard = {},
            onNavigateToGallery = {},
            onNavigateToSettings = {}
        )
    }
}

