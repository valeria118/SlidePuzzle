package com.example.a1234567889

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.drawToBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.res.stringResource
import com.example.a1234567889.models.*
import com.example.a1234567889.ui.LocaleHelper
import com.example.a1234567889.ui.PuzzleViewModel
import com.example.a1234567889.ui.components.*
import com.example.a1234567889.ui.screens.*
import com.example.a1234567889.ui.theme._1234567889Theme
import com.example.a1234567889.logic.NotificationHelper
import com.example.a1234567889.logic.ScreenshotUtil
import com.example.a1234567889.logic.ToastHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            var pendingPickedUri by remember { mutableStateOf<Uri?>(null) }
            var isEditingAvatar by remember { mutableStateOf(false) }
            
            // System photo picker launcher must be at the very top level
            val photoPickerLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                if (uri != null) {
                    pendingPickedUri = uri
                    if (isEditingAvatar) {
                        navController.navigate("avatar_editor")
                    } else {
                        navController.navigate("image_editor")
                    }
                }
            }

            val viewModel: PuzzleViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            val language = uiState?.settings?.language ?: "ru"
            
            val context = androidx.compose.ui.platform.LocalContext.current
            val localizedContext = remember(language) {
                LocaleHelper.setLocale(context, language)
            }

            // Lab item 5 — request the POST_NOTIFICATIONS runtime permission once on startup (API 33+).
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { /* result intentionally ignored: notifications degrade gracefully without it */ }
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalContext provides localizedContext,
                androidx.activity.compose.LocalActivityResultRegistryOwner provides this@MainActivity
            ) {
                val colorSchemeName = uiState?.settings?.colorScheme ?: "Classic"
                val darkThemeSetting = uiState?.settings?.isDarkTheme
                val darkTheme = darkThemeSetting ?: isSystemInDarkTheme()

                _1234567889Theme(
                    darkTheme = darkTheme,
                    colorSchemeName = colorSchemeName
                ) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        val newAchievement by viewModel.newAchievement.collectAsState(null)
                        val user by viewModel.currentUser.collectAsState(null)
                        val userStats by viewModel.userStats.collectAsState(UserStats())

                        // Lab item 5: mirror every newly unlocked achievement as a real system
                        // notification (high-priority channel with sound + vibration), in addition
                        // to the in-app overlay banner shown below.
                        LaunchedEffect(newAchievement) {
                            newAchievement?.let {
                                NotificationHelper.showAchievementNotification(context, it.title, it.description)
                            }
                        }

                        // Lab item 11 — "Меню-шторка" (Navigation Drawer): a single drawer shared
                        // across the whole app, opened from the hamburger icon on the home screen.
                        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                        val drawerScope = rememberCoroutineScope()

                        ModalNavigationDrawer(
                            drawerState = drawerState,
                            drawerContent = {
                                AppDrawerContent(
                                    isLoggedIn = user != null,
                                    onItemSelected = { route ->
                                        drawerScope.launch { drawerState.close() }
                                        when (route) {
                                            else -> navController.navigate(route) {
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                )
                            }
                        ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Shared state for the picture-selection flow
                            var pendingBoardSize by remember { mutableIntStateOf(3) }
                            var pendingImageCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }
                            var pendingCurrentUri by remember { mutableStateOf<String?>(null) }

                            NavHost(navController = navController, startDestination = "splash") {
                                composable("splash") {
                                    SplashScreen(
                                        onFinished = {
                                            navController.navigate("mode_selection") {
                                                popUpTo("splash") { inclusive = true }
                                            }
                                        }
                                    )
                                }
                                composable("mode_selection") {
                                    ModeSelectionScreen(
                                        user = user,
                                        onOpenDrawer = { drawerScope.launch { drawerState.open() } },
                                        stats = userStats,
                                        settings = uiState?.settings ?: GameSettings(),
                                        configuringMode = viewModel.configuringMode,
                                        configuringSettings = viewModel.configuringSettings,
                                        onStartConfiguring = { mode, base -> viewModel.startConfiguring(mode, base) },
                                        onUpdateConfiguringSettings = { viewModel.updateConfiguringSettings(it) },
                                        onStopConfiguring = { viewModel.stopConfiguring() },
                                        onModeSelected = { selectedSettings, resetStats ->
                                            when (selectedSettings.mode) {
                                                GameMode.CHALLENGE -> {
                                                    navController.navigate("challenge_selection")
                                                }
                                                GameMode.COMPETITION -> {
                                                    navController.navigate("competition_selection")
                                                }
                                                else -> {
                                                    viewModel.updateSettings(selectedSettings)
                                                    viewModel.startNewGame(resetStats = resetStats)
                                                    navController.navigate("game")
                                                }
                                            }
                                        },
                                        onSettingsChanged = { viewModel.updateSettings(it) },
                                        onNavigateToProfile = { navController.navigate("profile") },
                                        onNavigateToLeaderboard = {
                                            if (user != null) {
                                                navController.navigate("leaderboard")
                                            } else {
                                                android.widget.Toast.makeText(navController.context, "Войдите в аккаунт, чтобы видеть рейтинг", android.widget.Toast.LENGTH_SHORT).show()
                                                navController.navigate("profile")
                                            }
                                        },
                                        onNavigateToGallery = { navController.navigate("gallery") },
                                        onNavigateToSettings = { navController.navigate("settings") },
                                        onSelectPicture = { boardSize, currentUri, callback ->
                                            pendingBoardSize = boardSize
                                            pendingImageCallback = callback
                                            pendingCurrentUri = currentUri
                                            navController.navigate("picture_selection")
                                        }
                                    )
                                    
                                    // "Continue Game" button overlay if a game is in progress
                                    if (uiState != null && !uiState!!.isSolved && !uiState!!.isGameOver && uiState!!.moves > 0) {
                                        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
                                            ExtendedFloatingActionButton(
                                                onClick = { navController.navigate("game") },
                                                icon = { Icon(Icons.Default.PlayArrow, null) },
                                                text = { Text("Продолжить игру") },
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                        }
                                    }
                                }
                                composable("picture_selection") {
                                        PictureSelectionScreen(
                                            user = user,
                                            currentBoardSize = pendingBoardSize,
                                            selectedImageUri = pendingCurrentUri,
                                            onImageSelected = { item, startGame ->
                                                pendingImageCallback?.invoke(item.uri)
                                                pendingImageCallback = null
                                                if (startGame) {
                                                    viewModel.updateSettings(
                                                        uiState?.settings?.copy(imageUri = item.uri, size = pendingBoardSize, mode = GameMode.IMAGE)
                                                            ?: GameSettings(imageUri = item.uri, size = pendingBoardSize, mode = GameMode.IMAGE)
                                                    )
                                                    viewModel.startNewGame(resetStats = true)
                                                    navController.navigate("game") {
                                                        popUpTo("mode_selection") { inclusive = false }
                                                    }
                                                } else {
                                                    navController.popBackStack()
                                                }
                                            },
                                            onUploadOwn = {
                                                isEditingAvatar = false
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            onNavigateBack = { navController.popBackStack() },
                                        )
                                    }
                                    composable("image_editor") {
                                        val pickedUri = pendingPickedUri
                                        val scope = androidx.compose.runtime.rememberCoroutineScope()
                                        val ctx = androidx.compose.ui.platform.LocalContext.current
                                        if (pickedUri != null) {
                                            ImageEditorScreen(
                                                imageUri = pickedUri,
                                                onSave = { uri, scale, offsetX, offsetY, rotation, viewportPx ->
                                                    scope.launch {
                                                        val savedUri = com.example.a1234567889.logic.ImageCropUtil.cropAndSave(
                                                            ctx, uri, scale, offsetX, offsetY, rotation, viewportPx
                                                        )
                                                        if (savedUri != null) {
                                                            val uriStr = savedUri.toString()
                                                            viewModel.uploadToGallery(uriStr, "Моя картинка", "Мои")
                                                            pendingImageCallback?.invoke(uriStr)
                                                            pendingImageCallback = null
                                                            pendingPickedUri = null
                                                            navController.popBackStack("picture_selection", inclusive = true)
                                                        }
                                                    }
                                                },
                                                onCancel = {
                                                    pendingPickedUri = null
                                                    navController.popBackStack()
                                                }
                                            )
                                        }
                                    }
                                    composable("avatar_editor") {
                                        val pickedUri = pendingPickedUri
                                        val scope = androidx.compose.runtime.rememberCoroutineScope()
                                        val ctx = androidx.compose.ui.platform.LocalContext.current
                                        if (pickedUri != null) {
                                            ImageEditorScreen(
                                                imageUri = pickedUri,
                                                onSave = { uri, scale, offsetX, offsetY, rotation, viewportPx ->
                                                    scope.launch {
                                                        val savedUri = com.example.a1234567889.logic.ImageCropUtil.cropAndSave(
                                                            ctx, uri, scale, offsetX, offsetY, rotation, viewportPx
                                                        )
                                                        if (savedUri != null) {
                                                            viewModel.updateAvatar(savedUri.toString())
                                                            pendingPickedUri = null
                                                            navController.popBackStack("profile", inclusive = false)
                                                        }
                                                    }
                                                },
                                                onCancel = {
                                                    pendingPickedUri = null
                                                    navController.popBackStack()
                                                }
                                            )
                                        }
                                    }
                                composable("game") {
                                    PuzzleGameScreen(
                                        viewModel = viewModel,
                                        navController = navController,
                                        onNavigateToProfile = { navController.navigate("profile") },
                                        onNavigateToLeaderboard = {
                                            if (user != null) {
                                                navController.navigate("leaderboard")
                                            } else {
                                                android.widget.Toast.makeText(navController.context, "Войдите в аккаунт, чтобы видеть рейтинг", android.widget.Toast.LENGTH_SHORT).show()
                                                navController.navigate("profile")
                                            }
                                        },
                                        onNavigateBack = { navController.popBackStack() },
                                        onSelectPicture = {
                                            pendingBoardSize = uiState?.settings?.size ?: 3
                                            pendingImageCallback = { uri ->
                                                viewModel.updateSettings(uiState?.settings?.copy(imageUri = uri) ?: GameSettings(imageUri = uri))
                                            }
                                            pendingCurrentUri = uiState?.settings?.imageUri
                                            navController.navigate("picture_selection")
                                        }
                                    )
                                }
                                composable("profile") {
                                    val userStats by viewModel.userStats.collectAsState(com.example.a1234567889.models.UserStats())
                                    val achievements by viewModel.achievements.collectAsState(emptyList())
                                    
                                    if (user == null) {
                                        val error by viewModel.authError.collectAsState(null)
                                        AuthScreen(
                                            onLogin = { email, password -> viewModel.login(email, password) },
                                            onRegister = { email, nickname, password -> viewModel.register(email, nickname, password) },
                                            onGuest = { navController.popBackStack() },
                                            onForgotPassword = { email -> viewModel.requestPasswordReset(email) },
                                            errorMessage = error
                                        )
                                    } else {
                                        val profileError by viewModel.profileUpdateError.collectAsState(null)
                                        val profileSuccess by viewModel.profileUpdateSuccess.collectAsState(false)
                                        ProfileScreen(
                                            user = user,
                                            stats = userStats,
                                            achievements = achievements,
                                            onLogout = {
                                                viewModel.logout()
                                            },
                                            onNavigateToGallery = { navController.navigate("gallery") },
                                            onNavigateToCardGallery = { navController.navigate("card_gallery") },
                                            onNavigateToAchievements = { navController.navigate("achievements") },
                                            onNavigateBack = { navController.popBackStack() },
                                            onSaveProfile = { nick, email -> viewModel.updateProfile(nick, email) },
                                            onChangePassword = { cur, new -> viewModel.changePassword(cur, new) },
                                            profileError = profileError,
                                            profileSuccess = profileSuccess,
                                            onClearProfileStatus = { viewModel.clearProfileStatus() },
                                            onAvatarChanged = { uri -> viewModel.updateAvatar(uri) },
                                            onLaunchPhotoPicker = { 
                                                isEditingAvatar = true
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            onLaunchCamera = {
                                                isEditingAvatar = true
                                            }
                                        )
                                    }
                                }
                                composable("card_gallery") {
                                    CardGalleryScreen(
                                        cards = user?.cardCollection ?: emptyList(),
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                                composable("leaderboard") {
                                    LeaderboardScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                                composable("gallery") {
                                    GalleryScreen(
                                        gallery = user?.gallery ?: emptyList(),
                                        cards = user?.cardCollection ?: emptyList(),
                                        onPlayItem = { item ->
                                            viewModel.playGalleryItem(item)
                                            navController.navigate("game") {
                                                popUpTo("game") { inclusive = true }
                                            }
                                        },
                                        onUploadImage = { /* Open picker logic */ },
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                                composable("achievements") {
                                    val achievements by viewModel.achievements.collectAsState(emptyList())
                                    AchievementsScreen(
                                        achievements = achievements,
                                        onResetProgress = { viewModel.resetAchievements() },
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                                composable("settings") {
                                    SettingsScreen(
                                        currentSettings = uiState?.settings ?: com.example.a1234567889.models.GameSettings(),
                                        onNavigateBack = { navController.popBackStack() },
                                        onSave = { 
                                            viewModel.updateSettings(it)
                                            navController.popBackStack()
                                        }
                                    )
                                }
                                composable("challenge_selection") {
                                    val userStats by viewModel.userStats.collectAsState(com.example.a1234567889.models.UserStats())
                                    
                                    ChallengeSelectionScreen(
                                        userStats = userStats,
                                        onChallengeSelected = { config, size ->
                                            viewModel.updateSettings(
                                                uiState?.settings?.copy(
                                                    mode = GameMode.CHALLENGE,
                                                    challenge = config.type,
                                                    size = size
                                                ) ?: GameSettings(mode = GameMode.CHALLENGE, challenge = config.type, size = size)
                                            )
                                            navController.navigate("game")
                                        },
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                                composable("competition_selection") {
                                    CompetitionSelectionScreen(
                                        user = user,
                                        viewModel = viewModel,
                                        onStartMatch = { settings ->
                                            viewModel.updateSettings(settings)
                                            viewModel.startNewGame(settings = settings)
                                            navController.navigate("game")
                                        },
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                                composable("score_calculator") {
                                    ScoreCalculatorScreen(onNavigateBack = { navController.popBackStack() })
                                }
                                composable("friends") {
                                    FriendsScreen(
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("game_history") {
                                    GameHistoryScreen(onNavigateBack = { navController.popBackStack() })
                                }
                                composable("about") {
                                    AboutScreen(onNavigateBack = { navController.popBackStack() })
                                }
                            }
                        }

                        // Achievement Notification Overlay
                        newAchievement?.let { achievement ->
                            AchievementNotification(
                                achievement = achievement,
                                onDismiss = { viewModel.clearNewAchievement() }
                            )
                        }
                        } // close ModalNavigationDrawer content
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleGameScreen(
    viewModel: PuzzleViewModel,
    navController: androidx.navigation.NavController,
    onNavigateToProfile: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateBack: () -> Unit,
    onSelectPicture: () -> Unit
) {
    val uiStateNullable by viewModel.uiState.collectAsState()
    val uiState = uiStateNullable ?: return
    val user by viewModel.currentUser.collectAsState(null)

    var showSettings by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showWinDialog by remember { mutableStateOf(false) }
    var showGoal by remember { mutableStateOf(false) }

    // Lab item 16 — "Сохранение скриншотов": capture the root view of this screen.
    val rootView = LocalView.current
    val screenshotContext = androidx.compose.ui.platform.LocalContext.current
    val screenshotScope = rememberCoroutineScope()
    fun captureAndSaveScreenshot(thenShare: Boolean) {
        screenshotScope.launch {
            val bitmap = rootView.drawToBitmap()
            val uri = ScreenshotUtil.saveScreenshot(screenshotContext, bitmap)
            if (uri != null) {
                ToastHelper.show(screenshotContext, "Скриншот сохранён в галерею", ToastHelper.Position.TOP, 1800)
                if (thenShare) ScreenshotUtil.shareScreenshot(screenshotContext, uri)
            } else {
                ToastHelper.show(screenshotContext, "Не удалось сохранить скриншот")
            }
        }
    }

    // Observe win state
    LaunchedEffect(uiState.isSolved) {
        if (uiState.isSolved) {
            showWinDialog = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (uiState.settings.mode == GameMode.IMAGE || uiState.settings.visualMode == GameMode.IMAGE) {
                        IconButton(onClick = { 
                            viewModel.toggleHints()
                        }) {
                            Icon(
                                if (uiState.settings.showHints) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Подсказка"
                            )
                        }
                        IconButton(onClick = onSelectPicture) {
                            Icon(Icons.Default.AddPhotoAlternate, "Сменить картинку")
                        }
                    }
                    if (uiState.settings.mode == GameMode.CHALLENGE) {
                        IconButton(onClick = { showGoal = true }) {
                            Icon(Icons.Default.Star, "Показать цель")
                        }
                    }
                    IconButton(onClick = onNavigateToLeaderboard) {
                        Icon(Icons.Default.BarChart, stringResource(R.string.leaderboards))
                    }
                    IconButton(onClick = { captureAndSaveScreenshot(thenShare = false) }) {
                        Icon(Icons.Default.Photo, "Сделать скриншот")
                    }
                    if (uiState.settings.mode == GameMode.CHALLENGE) {
                        IconButton(onClick = { viewModel.toggleGoalPreview(true) }) {
                            Icon(Icons.Default.Star, "Цель")
                        }
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, stringResource(R.string.settings))
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        BadgedBox(badge = { if (user == null) Badge { Text("!") } }) {
                            Icon(Icons.Default.AccountCircle, stringResource(R.string.profile))
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomControlPanel(
                onShuffle = { viewModel.startNewGame() },
                onPause = { viewModel.togglePause() },
                isPaused = uiState.isPaused
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TopStatsPanel(
                moves = uiState.moves,
                seconds = uiState.timeSeconds,
                timeLeftSeconds = uiState.timeLeftSeconds,
                movesLeft = uiState.movesLeft,
                lives = if (uiState.settings.mode == GameMode.ENDLESS) uiState.lives else null,
                currentLevel = if (uiState.settings.mode == GameMode.ENDLESS || (uiState.settings.mode == GameMode.TIME_ATTACK && uiState.settings.isSurvivalMode)) uiState.currentLevel else null,
                progressPercent = if (uiState.settings.mode == GameMode.COLORS) {
                    val total = uiState.board.size * uiState.board.size
                    (uiState.correctTiles.toFloat() / total.toFloat() * 100).toInt()
                } else null,
                showCounters = uiState.settings.showCounters
            )
            
            PuzzleBoard(
                board = uiState.board,
                onTileClick = { viewModel.onTileClicked(it) },
                settings = uiState.settings,
                isPaused = uiState.isPaused || !uiState.isMatchStarted,
                modifier = Modifier.weight(1f)
            )

            if (uiState.settings.mode == GameMode.COMPETITION && !uiState.isPaused) {
                OpponentStatusPanel(
                    progress = uiState.opponentProgress,
                    nickname = uiState.opponentNickname,
                    isPublic = uiState.settings.isPublicMatch,
                    isTeam = uiState.settings.competitionType == CompetitionType.TEAM_2V2,
                    partnerProgress = uiState.teamPartnerProgress
                )
            }

            if (uiState.settings.usePowerUps && uiState.settings.mode == GameMode.COMPETITION) {
                PowerUpPanel(
                    available = uiState.availablePowerUps,
                    active = uiState.activePowerUps,
                    onUse = { viewModel.usePowerUp(it) }
                )
            }

            if (uiState.settings.isExperimentMode && !uiState.isPaused) {
                ExperimentStatsPanel(
                    inversions = uiState.inversions,
                    optimalMoves = uiState.optimalMoves ?: 0,
                    avgTime = uiState.averageMoveTime
                )
            }
        }
        
        if (uiState.settings.mode == GameMode.COMPETITION && !uiState.isMatchStarted) {
            MatchCountdownOverlay(countdown = uiState.matchCountdown ?: 0)
        }
    }

    if (uiState.isGameOver) {
        GameOverDialog(
            gameState = uiState,
            onRetry = { viewModel.startNewGame(resetStats = uiState.settings.mode == GameMode.ENDLESS) },
            onExit = { navController.popBackStack() }
        )
    }

    if (uiState.showGoalPreview) {
        val currentChallenge = ChallengeProvider.challenges.find { it.type == uiState.settings.challenge }
        GoalDialog(
            board = uiState.targetBoard,
            title = currentChallenge?.title ?: "Цель",
            description = currentChallenge?.goalText ?: "Соберите поле согласно эталону.",
            onDismiss = { viewModel.toggleGoalPreview(false) }
        )
    }

    if (showSettings) {
        navController.navigate("settings")
        showSettings = false
    }

    if (showStats) {
        StatsDialog(
            settings = uiState.settings,
            onDismiss = { showStats = false }
        )
    }

    if (showWinDialog) {
        WinDialog(
            gameState = uiState,
            onPlayAgain = {
                viewModel.retryCurrentGame()
                showWinDialog = false
            },
            onNewGame = {
                showWinDialog = false
                navController.popBackStack("mode_selection", inclusive = false)
            },
            onMainMenu = {
                showWinDialog = false
                navController.popBackStack("mode_selection", inclusive = false)
            },
            onShowStats = {
                showStats = true
            },
            onNextLevel = {
                if (uiState.settings.mode == GameMode.ENDLESS) {
                    viewModel.startNextEndlessLevel()
                } else if (uiState.settings.mode == GameMode.TIME_ATTACK && uiState.settings.isSurvivalMode) {
                    viewModel.startNextSurvivalLevel()
                }
                showWinDialog = false
            },
            onShare = {
                captureAndSaveScreenshot(thenShare = true)
            },
            onDismiss = { showWinDialog = false }
        )
    }

    if (showGoal) {
        val config = ChallengeProvider.challenges.find { it.type == uiState.settings.challenge }
        if (config != null) {
            GoalDialog(
                board = uiState.targetBoard,
                title = config.title,
                description = config.goalText,
                onDismiss = { showGoal = false }
            )
        }
    }
}
