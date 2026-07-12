package com.example.a1234567889.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.a1234567889.data.GamePreferences
import com.example.a1234567889.data.UserRepository
import com.example.a1234567889.logic.GameEngine
import com.example.a1234567889.logic.CardRewardLogic
import com.example.a1234567889.logic.PictureProvider
import com.example.a1234567889.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class PuzzleViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = GamePreferences(application)
    private val userRepository = UserRepository(application)

    private val ENDLESS_BASE_SHUFFLE = mapOf(
        ChallengeDifficulty.EASY to 20,
        ChallengeDifficulty.MEDIUM to 35,
        ChallengeDifficulty.HARD to 50,
        ChallengeDifficulty.EXPERT to 60
    )

    private val ENDLESS_BASE_MOVES = mapOf(
        ChallengeDifficulty.EASY to 30,
        ChallengeDifficulty.MEDIUM to 20,
        ChallengeDifficulty.HARD to 12
    )

    private val ENDLESS_BASE_TIME = mapOf(
        ChallengeDifficulty.EASY to 60,
        ChallengeDifficulty.MEDIUM to 45,
        ChallengeDifficulty.HARD to 30
    )

    private val ENDLESS_GROWTH_FACTOR = mapOf(
        ChallengeDifficulty.EASY to 1.2f,
        ChallengeDifficulty.MEDIUM to 1.4f,
        ChallengeDifficulty.HARD to 1.6f,
        ChallengeDifficulty.EXPERT to 1.8f
    )

    private val ENDLESS_MIN_MOVES = mapOf(
        3 to 6,
        4 to 15,
        5 to 30,
        6 to 50,
        7 to 80,
        8 to 120
    )

    private val ENDLESS_MIN_TIME = mapOf(
        3 to 15,
        4 to 25,
        5 to 40,
        6 to 60,
        7 to 90,
        8 to 130
    )

    private val _uiState = MutableStateFlow<GameState?>(null)
    val uiState: StateFlow<GameState?> = _uiState.asStateFlow()

    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard.asStateFlow()

    val currentUser = userRepository.currentUser
    val userStats = userRepository.userStats
    val achievements = userRepository.achievements
    val newAchievement = userRepository.newAchievements
    val authError = userRepository.authError

    private var timerJob: Job? = null
    private var aiJob: Job? = null
    private var matchJob: Job? = null

    private var isProcessingDefeat = false

    /** Friendly Russian label for a game mode, used in notification text. */
    private fun modeDisplayName(mode: GameMode): String = when (mode) {
        GameMode.NUMBERS -> "Классика"
        GameMode.IMAGE -> "Картинка"
        GameMode.COLORS -> "Цвета"
        GameMode.CHALLENGE -> "Вызов"
        GameMode.COMPETITION -> "Соревнование"
        GameMode.TIME_ATTACK -> "На время"
        GameMode.ENDLESS -> "Бесконечный"
        GameMode.CUSTOM -> "Свой режим"
        GameMode.IMAGE_FOLDER -> "Папка изображений"
    }

    init {
        viewModelScope.launch {
            userRepository.autoLogin()
        }

        viewModelScope.launch {
            prefs.gameSettings.collect { settings ->
                if (_uiState.value == null) {
                    startNewGame(settings, resetStats = true)
                } else {
                    _uiState.update { current ->
                        current?.copy(settings = settings)
                    }
                }
            }
        }
        
        // Update current user in uiState
        viewModelScope.launch {
            currentUser.collect { user ->
                _uiState.update { it?.copy(currentUser = user) }
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            userRepository.login(email, password)
        }
    }

    fun register(email: String, nickname: String, password: String) {
        viewModelScope.launch {
            userRepository.register(email, nickname, password)
        }
    }

    fun clearAuthError() {
        userRepository.clearError()
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
        }
    }

    private var lastEmptyPos: Position? = null
    private var hasMadeReturnMove = false
    private var undosUsed = 0

    // Persistence for Competition selection
    var competitionSettings by mutableStateOf(GameSettings(mode = GameMode.COMPETITION))
    var teamName by mutableStateOf("")
    var teamPartner by mutableStateOf<String?>(null)
    var selectedOpponent by mutableStateOf<Pair<String, String>?>(null)

    // Mode Configuration Persistence (keeps dialog open after picking image)
    var configuringMode by mutableStateOf<GameMode?>(null)
    var configuringSettings by mutableStateOf<GameSettings?>(null)

    fun startConfiguring(mode: GameMode, baseSettings: GameSettings) {
        configuringMode = mode
        configuringSettings = baseSettings.copy(mode = mode)
    }

    fun updateConfiguringSettings(settings: GameSettings) {
        configuringSettings = settings
    }

    fun stopConfiguring() {
        configuringMode = null
        configuringSettings = null
    }

    fun retryCurrentGame() {
        _uiState.value?.settings?.let { currentSettings ->
            startNewGame(currentSettings, resetStats = true)
        }
    }

    fun startNewGame(settings: GameSettings? = null, resetStats: Boolean = false) {
        isProcessingDefeat = false
        var currentSettings = settings ?: _uiState.value?.settings ?: GameSettings()
        
        // Endless mode initialization
        if (currentSettings.mode == GameMode.ENDLESS && resetStats) {
            val initialShuffle = ENDLESS_BASE_SHUFFLE[currentSettings.difficulty] ?: 35
            
            val moveLimit = if (currentSettings.endlessLimitType == EndlessLimitType.MOVES) {
                ENDLESS_BASE_MOVES[currentSettings.difficulty] ?: 20
            } else null
            
            val timeLimit = if (currentSettings.endlessLimitType == EndlessLimitType.TIME) {
                ENDLESS_BASE_TIME[currentSettings.difficulty] ?: 45
            } else null
            
            currentSettings = currentSettings.copy(
                size = 3,
                customShuffleMoves = initialShuffle,
                moveLimit = moveLimit,
                timeAttackLimit = timeLimit ?: 60,
                paletteType = if (currentSettings.endlessVisualMode == GameMode.COLORS) ColorPaletteType.RAINBOW else currentSettings.paletteType,
                colorPattern = if (currentSettings.endlessVisualMode == GameMode.COLORS) ColorPattern.SPECTRUM else currentSettings.colorPattern
            )
        }

        // Survival mode initialization
        if (currentSettings.mode == GameMode.TIME_ATTACK && currentSettings.isSurvivalMode && resetStats) {
            currentSettings = currentSettings.copy(
                timeAttackLimit = currentSettings.survivalInitialTime,
                customShuffleMoves = 30 // Reset shuffle for new run
            )
        }

        // Determine the actual mode to use for board creation
        val actualMode = currentSettings.visualMode

        val gameSeed = System.currentTimeMillis()

        val targetBoard = GameEngine.createTargetBoard(
            currentSettings.size,
            currentSettings.challenge,
            actualMode,
            currentSettings.paletteType,
            currentSettings.colorPattern,
            currentSettings.customPaletteColors,
            seed = gameSeed
        )
        val shuffledBoard = GameEngine.shuffle(
            size = currentSettings.size,
            challenge = currentSettings.challenge,
            mode = actualMode,
            paletteType = currentSettings.paletteType,
            patternType = currentSettings.colorPattern,
            customPalette = currentSettings.customPaletteColors,
            allowMatches = currentSettings.allowMatches,
            customShuffleMoves = if (currentSettings.mode == GameMode.CUSTOM || currentSettings.mode == GameMode.ENDLESS) currentSettings.customShuffleMoves else null,
            seed = gameSeed
        )
        
        lastEmptyPos = shuffledBoard.findEmpty()
        hasMadeReturnMove = false
        undosUsed = 0

        val initialDist = GameEngine.getManhattanDistance(shuffledBoard, targetBoard)

        _uiState.update { prev ->
            val startingLives = when(currentSettings.difficulty) {
                ChallengeDifficulty.EASY -> 5
                ChallengeDifficulty.MEDIUM -> 3
                ChallengeDifficulty.HARD -> 2
                ChallengeDifficulty.EXPERT -> 1
            }

            GameState(
                board = shuffledBoard,
                targetBoard = targetBoard,
                moves = 0,
                timeSeconds = 0,
                timeLeftSeconds = if (currentSettings.mode == GameMode.TIME_ATTACK) currentSettings.timeAttackLimit 
                                 else if (currentSettings.mode == GameMode.ENDLESS && currentSettings.endlessLimitType == EndlessLimitType.TIME) currentSettings.timeAttackLimit
                                 else null,
                movesLeft = if (currentSettings.mode == GameMode.ENDLESS && currentSettings.endlessLimitType == EndlessLimitType.MOVES) currentSettings.moveLimit else null,
                lives = if (resetStats) {
                    startingLives
                } else {
                    prev?.lives ?: startingLives
                },
                score = if (resetStats) 0 else (prev?.score ?: 0),
                isPaused = false,
                isSolved = false,
                currentLevel = if (resetStats) 1 else (prev?.currentLevel ?: 1),
                correctTiles = calculateCorrectTiles(shuffledBoard, targetBoard),
                history = listOf(shuffledBoard),
                settings = currentSettings,
                availablePowerUps = if (currentSettings.mode == GameMode.COMPETITION && currentSettings.usePowerUps) {
                    listOf(PowerUpType.ACCELERATION, PowerUpType.EXTRA_TIME)
                } else emptyList(),
                opponentProgress = 0f,
                opponentNickname = when (currentSettings.competitionType) {
                    CompetitionType.DUEL_AI -> "ИИ Противник"
                    CompetitionType.DUEL_FRIEND -> selectedOpponent?.second ?: "Друг"
                    CompetitionType.RANKED -> "Соперник"
                    CompetitionType.TEAM_2V2 -> "Команда Соперника"
                },
                opponentDistance = if (currentSettings.isAiMatch || currentSettings.mode == GameMode.COMPETITION) initialDist else null,
                initialDistance = initialDist,
                isMatchStarted = currentSettings.mode != GameMode.COMPETITION,
                matchCountdown = if(currentSettings.mode == GameMode.COMPETITION) 3 else null,
                totalEndlessMoves = if (resetStats) 0 else (prev?.totalEndlessMoves ?: 0),
                totalEndlessTime = if (resetStats) 0 else (prev?.totalEndlessTime ?: 0),
                totalSurvivalMoves = if (resetStats) 0 else (prev?.totalSurvivalMoves ?: 0),
                totalSurvivalTime = if (resetStats) 0 else (prev?.totalSurvivalTime ?: 0),
                showGoalPreview = currentSettings.mode == GameMode.CHALLENGE && resetStats
            )
        }
        stopTimer()
        if (currentSettings.mode == GameMode.COMPETITION) {
            startMatchCountdown()
        }
    }

    private fun startMatchCountdown() {
        matchJob?.cancel()
        matchJob = viewModelScope.launch {
            for (count in 3 downTo 0) {
                _uiState.update { it?.copy(matchCountdown = count) }
                delay(1000)
            }
            _uiState.update { it?.copy(matchCountdown = null, isMatchStarted = true) }
            startTimer()
        }
    }

    fun clearNewAchievement() {
        userRepository.clearNewAchievement()
    }

    fun usePowerUp(type: PowerUpType) {
        _uiState.update { state ->
            if (state == null) return@update null
            val active = state.activePowerUps.toMutableMap()
            active[type] = System.currentTimeMillis() + 10000 // 10s duration
            
            // Effect logic
            when(type) {
                PowerUpType.EXTRA_TIME -> {
                    val current = state.timeLeftSeconds ?: 0
                    state.copy(timeLeftSeconds = current + 15, activePowerUps = active, availablePowerUps = state.availablePowerUps - type)
                }
                PowerUpType.AUTO_SOLVE_3 -> {
                    // Just a visual mock for now
                    state.copy(activePowerUps = active, availablePowerUps = state.availablePowerUps - type)
                }
                PowerUpType.RESET_OPPONENT_MOVES -> {
                    val newProgress = (state.opponentProgress - 0.15f).coerceAtLeast(0f)
                    state.copy(opponentProgress = newProgress, activePowerUps = active, availablePowerUps = state.availablePowerUps - type)
                }
                PowerUpType.FREEZE_OPPONENT -> {
                    state.copy(activePowerUps = active, availablePowerUps = state.availablePowerUps - type)
                }
                else -> state.copy(activePowerUps = active, availablePowerUps = state.availablePowerUps - type)
            }
        }
    }

    fun resetAchievements() {
        viewModelScope.launch {
            userRepository.resetAchievements()
        }
    }

    fun onTileClicked(pos: Position) {
        val state = _uiState.value ?: return
        if (state.isPaused || state.isSolved || state.isGameOver) return

        // Survival mode complexity: Level 16-20 Blockages (random chance to ignore click)
        if (state.settings.mode == GameMode.TIME_ATTACK && state.settings.isSurvivalMode && state.currentLevel in 16..20) {
            if ((1..10).random() == 1) return // 10% chance to be "blocked"
        }

        val actualMode = state.settings.visualMode

        val emptyPos = state.board.findEmpty()
        val newBoard = GameEngine.moveTile(state.board, pos)
        if (newBoard != null) {
            // Lab item 6: short SoundPool click for every successful tile move.
            com.example.a1234567889.logic.SoundManager.getInstance(getApplication()).playClick()
            if (state.settings.isVibrationEnabled) {
                com.example.a1234567889.logic.SoundManager.vibrateTick(getApplication())
            }

            // Track "No Returns" achievement
            if (lastEmptyPos == pos) {
                hasMadeReturnMove = true
            }
            lastEmptyPos = emptyPos

            if (state.moves == 0 && state.timeSeconds == 0L) {
                startTimer()
                viewModelScope.launch { userRepository.unlockAchievement("welcome") }
            }
            
            // Chromatic Adaptation Logic (visual only shift, doesn't change IDs)
            val finalBoard = if (actualMode == GameMode.COLORS && state.settings.isChromaticAdaptation && (state.moves + 1) % 10 == 0) {
                applyChromaticAdaptation(newBoard)
            } else {
                newBoard
            }

            val isSolved = GameEngine.isSolved(finalBoard, state.targetBoard)
            val correctTiles = calculateCorrectTiles(finalBoard, state.targetBoard)
            
            // Experiment Mode Stats
            var inversions = state.inversions
            var optimalMoves = state.optimalMoves
            var avgMoveTime = state.averageMoveTime
            
            if (state.settings.isExperimentMode) {
                inversions = GameEngine.countInversions(finalBoard)
                optimalMoves = GameEngine.getManhattanDistance(finalBoard, state.targetBoard)
                val totalTime = state.timeSeconds.toDouble()
                avgMoveTime = if (state.moves + 1 > 0) totalTime / (state.moves + 1) else 0.0
            }

            // Survival mode complexity: Level 21+ reduction per move
            var nextTimeLeft = state.timeLeftSeconds
            if (state.settings.mode == GameMode.TIME_ATTACK && state.settings.isSurvivalMode && state.currentLevel >= 21) {
                nextTimeLeft = (nextTimeLeft ?: 0) - 2
            }

            _uiState.update { current ->
                current?.copy(
                    board = finalBoard,
                    moves = current.moves + 1,
                    movesLeft = current.movesLeft?.minus(1),
                    timeLeftSeconds = nextTimeLeft,
                    isSolved = isSolved,
                    correctTiles = correctTiles,
                    history = current.history + finalBoard,
                    inversions = inversions,
                    optimalMoves = optimalMoves,
                    averageMoveTime = avgMoveTime
                )
            }

            if (_uiState.value?.movesLeft != null && _uiState.value!!.movesLeft!! <= 0 && !isSolved) {
                handleDefeat()
            } else if (isSolved) {
                stopTimer()
                handleWin()
            }
        }
    }

    private fun handleDefeat() {
        if (isProcessingDefeat) return
        isProcessingDefeat = true
        stopTimer()
        
        // Immediately pause UI to prevent further triggers
        _uiState.update { it?.copy(isPaused = true) }
        
        // Use a background job for heavy state processing to keep UI responsive
        viewModelScope.launch(Dispatchers.Default) {
            val currentState = _uiState.value ?: return@launch
            
            // Finalize survival/endless stats if it's game over
            val isSurvival = currentState.settings.mode == GameMode.TIME_ATTACK && currentState.settings.isSurvivalMode
            val isEndless = currentState.settings.mode == GameMode.ENDLESS
            
            val newLives = (currentState.lives - 1).coerceAtLeast(0)
            
            if (newLives == 0) {
                // Record the loss in repository (Disk I/O)
                userRepository.recordLoss()
                
                if (isSurvival) {
                    prefs.updateSurvivalRecord(currentState.currentLevel - 1)
                }

                // Update UI state to Terminal state
                _uiState.update { state ->
                    state?.copy(
                        lives = 0, 
                        isGameOver = true,
                        totalSurvivalMoves = if (isSurvival) state.totalSurvivalMoves + state.moves else state.totalSurvivalMoves,
                        totalSurvivalTime = if (isSurvival) state.totalSurvivalTime + state.timeSeconds else state.totalSurvivalTime,
                        totalEndlessMoves = if (isEndless) state.totalEndlessMoves + state.moves else state.totalEndlessMoves,
                        totalEndlessTime = if (isEndless) state.totalEndlessTime + state.timeSeconds else state.totalEndlessTime
                    )
                }
                
                // Competition achievement check
                if (currentState.settings.mode == GameMode.COMPETITION) {
                    userRepository.unlockAchievement("comp_first_duel")
                }

                // Lab item 15: persist the lost match into the local SQLite database.
                recordGameHistory(currentState, won = false)

                // Lab item 6 & 5: a short "defeat" sound plus a system notification about the game over.
                com.example.a1234567889.logic.SoundManager.getInstance(getApplication()).playError()
                com.example.a1234567889.logic.NotificationHelper.showGameResultNotification(
                    getApplication(),
                    won = false,
                    modeName = modeDisplayName(currentState.settings.mode),
                    moves = currentState.moves,
                    timeSeconds = currentState.timeSeconds
                )
            } else {
                // Deduct heart and restart level
                _uiState.update { it?.copy(lives = newLives, isPaused = true) }
                delay(500)
                startNewGame(currentState.settings)
            }
        }
    }

    private fun applyChromaticAdaptation(board: Board): Board {
        val intensity = _uiState.value?.settings?.adaptationIntensity ?: 0.5f
        // Change brightness/saturation slightly to simulate "lighting change"
        val newTiles = board.tiles.map { row ->
            row.map { tile ->
                if (tile.isEmpty || tile.color == null) tile
                else {
                    val hsv = tile.color.toHsv()
                    val variance = (Random.nextFloat() * 2f - 1f) * intensity * 0.2f
                    hsv[2] = (hsv[2] + variance).coerceIn(0f, 1f)
                    tile.copy(color = androidx.compose.ui.graphics.Color.hsv(hsv[0], hsv[1], hsv[2]))
                }
            }
        }
        return board.copy(tiles = newTiles)
    }

    private fun androidx.compose.ui.graphics.Color.toHsv(): FloatArray {
        val hsv = FloatArray(3)
        val max = maxOf(red, maxOf(green, blue))
        val min = minOf(red, minOf(green, blue))
        val delta = max - min
        hsv[0] = when {
            delta == 0f -> 0f
            max == red -> 60f * (((green - blue) / delta) % 6f)
            max == green -> 60f * (((blue - red) / delta) + 2f)
            else -> 60f * (((red - green) / delta) + 4f)
        }
        if (hsv[0] < 0f) hsv[0] += 360f
        hsv[1] = if (max == 0f) 0f else delta / max
        hsv[2] = max
        return hsv
    }

    private fun calculateCorrectTiles(board: Board, targetBoard: Board): Int {
        var count = 0
        for (r in 0 until board.size) {
            for (c in 0 until board.size) {
                if (board.tiles[r][c].id == targetBoard.tiles[r][c].id) count++
            }
        }
        return count
    }

    fun startNextSurvivalLevel() {
        val currentState = _uiState.value ?: return
        val nextLevel = currentState.currentLevel + 1
        
        // Progression logic
        val timeStep = currentState.settings.survivalTimeStep
        val sizeInterval = currentState.settings.survivalSizeIncreaseInterval
        
        // 1. Reduce time limit
        val nextTimeLimit = (currentState.settings.timeAttackLimit - timeStep).coerceAtLeast(5)
        
        // 2. Increase size if interval reached
        var nextSize = currentState.settings.size
        if (sizeInterval > 0 && (nextLevel - 1) % sizeInterval == 0) {
            nextSize = (nextSize + 1).coerceAtMost(8)
        }
        
        // 3. Increase shuffle intensity
        val nextShuffle = currentState.settings.customShuffleMoves + 5

        // 4. Move limit for levels 11+
        var nextMoveLimit: Int? = null
        if (nextLevel >= 11) {
            val baseMoves = nextSize * nextSize * 4 
            nextMoveLimit = (baseMoves * (1 + (nextLevel - 10) * 0.05f)).toInt()
        }

        val nextSettings = currentState.settings.copy(
            size = nextSize,
            timeAttackLimit = nextTimeLimit,
            customShuffleMoves = nextShuffle,
            moveLimit = nextMoveLimit
        )

        _uiState.update { it?.copy(
            currentLevel = nextLevel,
            totalSurvivalMoves = it.totalSurvivalMoves + currentState.moves,
            totalSurvivalTime = it.totalSurvivalTime + currentState.timeSeconds
        ) }
        startNewGame(nextSettings)
    }

    fun startNextEndlessLevel() {
        val currentState = _uiState.value ?: return
        val nextLevel = currentState.currentLevel + 1
        
        // Progression logic: 3->4->5->6->7->8 (Cycle length 6)
        val cycleNum = (nextLevel - 1) / 6
        val sizeIndex = (nextLevel - 1) % 6
        val nextSize = 3 + sizeIndex
        
        val difficulty = currentState.settings.difficulty
        val growthFactor = ENDLESS_GROWTH_FACTOR[difficulty] ?: 1.4f
        
        // Shuffle moves: Fixed by difficulty for 3x3, then grows?
        // Technical description says: "Количество ходов перемешивания ... определяется только сложностью"
        // But for larger sizes it should probably grow to ensure non-triviality.
        // Let's use a base shuffle and grow it.
        val baseShuffleInitial = ENDLESS_BASE_SHUFFLE[difficulty] ?: 35
        val nextShuffle = (baseShuffleInitial * (1 + (nextLevel - 1) * 0.1f)).toInt()

        var nextMoveLimit: Int? = null
        var nextTimeLimit: Int = 60

        if (currentState.settings.endlessLimitType == EndlessLimitType.MOVES) {
            val baseInitial = ENDLESS_BASE_MOVES[difficulty] ?: 20
            val baseForCycle = baseInitial * (1 + cycleNum * 0.2f)
            
            val calculated = if (sizeIndex == 0) {
                baseForCycle.toInt()
            } else {
                ((currentState.settings.moveLimit ?: baseInitial) * growthFactor).toInt()
            }
            val minLimit = ENDLESS_MIN_MOVES[nextSize] ?: 6
            nextMoveLimit = maxOf(calculated, minLimit)
        } else {
            val baseInitial = ENDLESS_BASE_TIME[difficulty] ?: 45
            val baseForCycle = baseInitial * (1 + cycleNum * 0.2f)
            
            val calculated = if (sizeIndex == 0) {
                baseForCycle.toInt()
            } else {
                (currentState.settings.timeAttackLimit * growthFactor).toInt()
            }
            val minLimit = ENDLESS_MIN_TIME[nextSize] ?: 15
            nextTimeLimit = maxOf(calculated, minLimit)
        }

        val nextSettings = currentState.settings.copy(
            size = nextSize,
            customShuffleMoves = nextShuffle,
            moveLimit = nextMoveLimit,
            timeAttackLimit = nextTimeLimit,
            paletteType = if (currentState.settings.endlessVisualMode == GameMode.COLORS) currentState.settings.paletteType else currentState.settings.paletteType,
            colorPattern = if (currentState.settings.endlessVisualMode == GameMode.COLORS) currentState.settings.colorPattern else currentState.settings.colorPattern
        )

        // Calculate score for previous level
        val levelScore = calculateLevelScore(currentState)

        _uiState.update { it?.copy(
            currentLevel = nextLevel, 
            score = it.score + levelScore,
            totalEndlessMoves = it.totalEndlessMoves + currentState.moves,
            totalEndlessTime = it.totalEndlessTime + currentState.timeSeconds
        ) }
        startNewGame(nextSettings)
    }

    private fun calculateLevelScore(state: GameState): Int {
        val base = state.settings.size * 100
        val timeBonus = if (state.timeLeftSeconds != null) state.timeLeftSeconds * 10 else 0
        val moveBonus = if (state.movesLeft != null) state.movesLeft * 20 else 0
        return base + timeBonus + moveBonus
    }

    private fun handleWin() {
        val state = _uiState.value ?: return
        viewModelScope.launch {
            // Check for new record before updating
            val oldBestTime = prefs.getBestTime(state.settings.size, state.settings.mode, state.settings.challenge).first() ?: Long.MAX_VALUE
            val oldBestMoves = prefs.getBestMoves(state.settings.size, state.settings.mode, state.settings.challenge).first() ?: Int.MAX_VALUE
            val isNewRecord = state.timeSeconds < oldBestTime || state.moves < oldBestMoves

            // Capture achievements state before
            val beforeAchievements = userRepository.achievements.first().filter { it.isUnlocked }.map { it.id }.toSet()

            // General
            userRepository.unlockAchievement("first_win")
            userRepository.updateAchievementProgress("novice")
            userRepository.updateAchievementProgress("experienced")
            userRepository.updateAchievementProgress("veteran")
            userRepository.updateAchievementProgress("legend")
            
            // Global progress for Endless and Challenge achievements
            userRepository.updateAchievementProgress("enduring")
            userRepository.updateAchievementProgress("marathoner")
            userRepository.updateAchievementProgress("iron_player")
            userRepository.updateAchievementProgress("endless_wanderer")
            userRepository.updateAchievementProgress("inventor")
            userRepository.updateAchievementProgress("universal")
            
            if (!state.isPaused) userRepository.unlockAchievement("no_pause")

            // Mode-specific
            when (state.settings.mode) {
                GameMode.IMAGE -> {
                    state.settings.imageUri?.let { uri ->
                        userRepository.addToGallery(uri, state.timeSeconds, state.moves, state.settings.size)
                        userRepository.unlockAchievement("photographer")
                    }
                    userRepository.updateAchievementProgress("collector")
                    userRepository.updateAchievementProgress("artist")
                    userRepository.updateAchievementProgress("puzzle_master")
                }
                GameMode.COLORS -> {
                    userRepository.updateAchievementProgress("chromatic")
                    userRepository.updateAchievementProgress("color_genius")
                    userRepository.updateAchievementProgress("symbolist")
                }
                GameMode.NUMBERS -> {
                    userRepository.incrementClassicWins()
                    userRepository.updateAchievementProgress("classic_marathon")
                    userRepository.updateAchievementProgress("counter", state.moves)
                    
                    if (!hasMadeReturnMove) userRepository.unlockAchievement("no_returns")

                    val size = state.settings.size
                    if (size == 4) {
                        if (state.moves <= 16) userRepository.unlockAchievement("mathematician")
                        if (state.timeSeconds <= 30) userRepository.unlockAchievement("fast_30")
                    } else if (size == 3) {
                        if (state.moves <= 4) userRepository.unlockAchievement("perfect_3x3")
                    }
                    userRepository.updateAchievementProgress("follower") // Should be complex logic but simplified here
                }
                GameMode.CHALLENGE -> {
                    userRepository.recordChallenge(state.settings.challenge, state.timeSeconds)
                    userRepository.unlockAchievement("random_hero")
                    
                    // Challenge progression achievements - updated to use latest count immediately
                    val count = userRepository.userStats.first().completedChallengesCount
                    if (count >= 5) userRepository.unlockAchievement("novice_challenges")
                    if (count >= 15) userRepository.unlockAchievement("experienced_challenges")
                    if (count >= 30) userRepository.unlockAchievement("master_challenges")
                }
                GameMode.COMPETITION -> {
                    userRepository.unlockAchievement("comp_first_duel")
                    userRepository.updateAchievementProgress("duelist")
                    userRepository.updateAchievementProgress("champion")
                    
                    if (state.settings.isAiMatch) {
                        when(state.settings.aiDifficulty) {
                            ChallengeDifficulty.EASY -> userRepository.unlockAchievement("comp_ai_easy")
                            ChallengeDifficulty.MEDIUM -> userRepository.unlockAchievement("comp_ai_medium")
                            ChallengeDifficulty.HARD -> userRepository.unlockAchievement("comp_ai_hard")
                            ChallengeDifficulty.EXPERT -> userRepository.unlockAchievement("comp_ai_expert")
                        }
                    }

                    val oppTime = state.opponentTime ?: Long.MAX_VALUE
                    val oppMoves = state.opponentMoves ?: Int.MAX_VALUE
                    
                    var ratingChange: Int? = null
                    if (state.settings.competitionType == CompetitionType.RANKED) {
                        ratingChange = if (state.timeSeconds < oppTime) (15..25).random() else -(10..15).random()
                    }

                    if (state.timeSeconds < oppTime) {
                        userRepository.unlockAchievement("comp_faster")
                    }
                    if (state.moves < oppMoves) {
                        userRepository.unlockAchievement("comp_efficient")
                    }

                    _uiState.update { it?.copy(ratingChange = ratingChange) }
                }
                GameMode.TIME_ATTACK -> {
                    userRepository.updateAchievementProgress("time_master")
                    userRepository.updateAchievementProgress("time_manager")

                    if (state.settings.isSurvivalMode) {
                        val level = state.currentLevel
                        if (level == 1) userRepository.unlockAchievement("survival_first")
                        userRepository.updateAchievementProgress("survival_sprinter")
                        userRepository.updateAchievementProgress("survival_master")
                        userRepository.updateAchievementProgress("survival_marathoner")
                        userRepository.updateAchievementProgress("survival_endless")

                        val initialLimit = state.settings.timeAttackLimit
                        if (state.timeSeconds < initialLimit / 2) {
                            userRepository.unlockAchievement("survival_lightning")
                        }

                        if (state.timeLeftSeconds != null && state.timeLeftSeconds!! <= 3) {
                            userRepository.unlockAchievement("survival_iron_nerve")
                        }
                    }
                }
                GameMode.ENDLESS -> {
                    val currentLevel = state.currentLevel
                    val cycle = (currentLevel - 1) / 6
                    
                    if (currentLevel == 1) userRepository.unlockAchievement("first_steps")
                    
                    if (currentLevel >= 5) userRepository.unlockAchievement("size_conqueror")
                    // Specific level-based unlocks are kept for Endless mode itself
                    if (currentLevel >= 15) userRepository.unlockAchievement("marathoner")
                    if (currentLevel >= 25) userRepository.unlockAchievement("iron_player")
                    if (currentLevel >= 50) userRepository.unlockAchievement("endless_wanderer")
                    
                    if (currentLevel == 5 && state.lives == 3) userRepository.unlockAchievement("no_losses")
                    
                    if (state.settings.size == 5 && state.timeSeconds <= 30) userRepository.unlockAchievement("speed_run")
                    if (state.settings.size == 8) {
                        userRepository.unlockAchievement("adaptation_master")
                        if (state.settings.difficulty == ChallengeDifficulty.HARD) {
                            userRepository.unlockAchievement("difficulty_master")
                        }
                    }
                }
                GameMode.CUSTOM -> {
                    if (state.settings.isExperimentMode) {
                        userRepository.updateAchievementProgress("experimenter")
                    }
                }
                else -> {}
            }

            // Card reward: only the "Картинка" (IMAGE) mode awards collectible cards.
            if (state.settings.mode == GameMode.IMAGE) {
                val boardSize = state.settings.size
                val rank = CardRewardLogic.rollRank(boardSize)
                val photo = CardRewardLogic.randomCardPhoto(rank)

                // Try to find a source title for the card
                val builtIn = PictureProvider.builtInImages.find { it.uri == state.settings.imageUri }
                val sourceTitle = builtIn?.title ?: "Моя картинка"

                val card = com.example.a1234567889.models.CollectedCard(
                    rank = rank,
                    photoRes = photo,
                    sourceImageId = builtIn?.id ?: "custom_image_${System.currentTimeMillis()}",
                    sourceTitle = sourceTitle,
                    boardSize = boardSize
                )
                val savedCard = userRepository.addCollectedCard(card)
                _uiState.update { it?.copy(newCard = savedCard) }
            }

            userRepository.recordGame(state.timeSeconds)
            userRepository.recordLeaderboardResult(state.timeSeconds, state.moves)
            prefs.incrementGames()
            prefs.updateBestScore(
                state.settings.size,
                state.settings.mode,
                state.settings.challenge,
                state.timeSeconds,
                state.moves
            )

            // Lab item 15: persist this finished match into the local SQLite database.
            recordGameHistory(state, won = true)

            // Lab item 6: short victory sound effect via SoundPool.
            com.example.a1234567889.logic.SoundManager.getInstance(getApplication()).playWin()

            // Lab item 5: system notification about the finished game (channel/priority/sound/vibration).
            com.example.a1234567889.logic.NotificationHelper.showGameResultNotification(
                getApplication(),
                won = true,
                modeName = modeDisplayName(state.settings.mode),
                moves = state.moves,
                timeSeconds = state.timeSeconds
            )

            // Capture achievements state after
            val afterAchievements = userRepository.achievements.first().filter { it.isUnlocked }
            val newUnlocked = afterAchievements.filter { it.id !in beforeAchievements }

            _uiState.update { it?.copy(isNewRecord = isNewRecord, newAchievements = newUnlocked, newCard = it.newCard) }
        }
    }

    fun rematch() {
        _uiState.value?.let { state ->
            if (state.settings.mode == GameMode.COMPETITION) {
                startNewGame(state.settings, resetStats = true)
            }
        }
    }

    fun undo() {
        val state = _uiState.value ?: return
        if (state.settings.undoLimit != -1 && undosUsed >= state.settings.undoLimit) {
            // Undo limit reached
            return
        }

        if (state.history.size > 1) {
            undosUsed++
            _uiState.update { current ->
                if (current != null && current.history.size > 1) {
                    val newHistory = current.history.dropLast(1)
                    current.copy(
                        board = newHistory.last(),
                        moves = current.moves + 1,
                        history = newHistory
                    )
                } else current
            }
        }
    }

    fun togglePause() {
        _uiState.update { it?.copy(isPaused = !it.isPaused) }
        val state = _uiState.value ?: return
        if (state.isPaused) stopTimer() else if (state.moves > 0) startTimer()
    }

    fun updateSettings(newSettings: GameSettings) {
        viewModelScope.launch {
            val oldSettings = _uiState.value?.settings
            prefs.updateSettings(newSettings)
            
            val shouldReset = oldSettings == null || 
                oldSettings.size != newSettings.size ||
                oldSettings.mode != newSettings.mode ||
                oldSettings.challenge != newSettings.challenge ||
                oldSettings.imageUri != newSettings.imageUri ||
                oldSettings.difficulty != newSettings.difficulty ||
                oldSettings.paletteType != newSettings.paletteType ||
                oldSettings.colorPattern != newSettings.colorPattern ||
                oldSettings.allowMatches != newSettings.allowMatches ||
                oldSettings.customShuffleMoves != newSettings.customShuffleMoves

            if (shouldReset) {
                startNewGame(newSettings, resetStats = true)
            }
        }
    }

    fun toggleHints() {
        val currentState = _uiState.value ?: return
        val newSettings = currentState.settings.copy(showHints = !currentState.settings.showHints)
        updateSettings(newSettings)
    }

    fun uploadToGallery(uri: String, title: String, category: String) {
        viewModelScope.launch {
            userRepository.uploadToGallery(uri, title, category)
        }
    }

    fun savePreset(name: String, settings: GameSettings, presetId: String? = null, isPublic: Boolean = false) {
        viewModelScope.launch {
            userRepository.savePreset(name, settings, presetId, isPublic)
            userRepository.updateAchievementProgress("preset_collector")
        }
    }

    fun deletePreset(presetId: String) {
        viewModelScope.launch {
            userRepository.deletePreset(presetId)
        }
    }

    fun refreshLeaderboard(
        mode: GameMode,
        size: Int,
        sortBy: LeaderboardSortBy,
        onlyFriends: Boolean,
        period: LeaderboardPeriod,
        searchQuery: String = ""
    ) {
        viewModelScope.launch {
            val entries = userRepository.getLeaderboard(mode, size, sortBy, onlyFriends, period, searchQuery)
            _leaderboard.value = entries
        }
    }

    // ---- Friends ----

    fun getFriends(): List<Pair<String, String>> = userRepository.getFriends()

    suspend fun searchUsers(query: String): List<Pair<String, String>> = userRepository.searchRegisteredUsers(query)

    fun addFriend(userId: String) {
        viewModelScope.launch { userRepository.addFriend(userId) }
    }

    fun removeFriend(userId: String) {
        viewModelScope.launch { userRepository.removeFriend(userId) }
    }

    /** My own QR payload — show this on screen so a friend can scan it. */
    fun myFriendQrPayload(): String? = userRepository.myFriendQrPayload()

    /** Handle a scanned QR code's text content: try to add the encoded user as a friend. */
    suspend fun addFriendFromQrPayload(payload: String): com.example.a1234567889.models.AddFriendResult =
        userRepository.addFriendFromQrPayload(payload)

    // ---- Tournaments ----

    val tournaments = userRepository.tournaments

    fun createTournament(name: String, config: GameSettings): Tournament {
        return userRepository.createTournament(name, config)
    }

    fun joinTournament(tournamentId: String) {
        userRepository.joinTournament(tournamentId)
    }

    fun getOpenTournaments(): List<Tournament> = userRepository.getOpenTournaments()

    fun getMyTournaments(): List<Tournament> = userRepository.getMyTournaments()

    fun createChallenge(title: String, description: String?, settings: GameSettings) {
        viewModelScope.launch {
            userRepository.createChallenge(title, description, settings)
            userRepository.updateAchievementProgress("chef")
        }
    }

    fun toggleGoalPreview(show: Boolean) {
        _uiState.update { it?.copy(showGoalPreview = show) }
    }

    fun startChallenge(config: ChallengeConfig) {
        val newSettings = _uiState.value?.settings?.copy(
            mode = GameMode.CHALLENGE,
            challenge = config.type,
            difficulty = config.difficulty
        ) ?: GameSettings(mode = GameMode.CHALLENGE, challenge = config.type, difficulty = config.difficulty)
        
        startNewGame(newSettings, resetStats = true)
        toggleGoalPreview(true)
    }

    fun playGalleryItem(item: ImageGalleryItem) {
        val newSettings = _uiState.value?.settings?.copy(
            mode = GameMode.IMAGE,
            imageUri = item.uri,
            size = if (item.boardSize > 0) item.boardSize else (_uiState.value?.settings?.size ?: 4)
        ) ?: GameSettings(
            mode = GameMode.IMAGE,
            imageUri = item.uri,
            size = if (item.boardSize > 0) item.boardSize else 4
        )
        updateSettings(newSettings)
    }

    private fun startTimer() {
        stopTimer()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { state ->
                    if (state == null) return@update null
                    
                    val newTime = state.timeSeconds + 1
                    val nextTimeLeftRaw = state.timeLeftSeconds?.minus(1)
                    var nextTimeLeft = nextTimeLeftRaw
                    var isGameOver = state.isGameOver
                    
                    if (nextTimeLeftRaw != null && nextTimeLeftRaw <= 0) {
                        nextTimeLeft = 0
                        stopTimer()
                        if (state.settings.mode == GameMode.ENDLESS || (state.settings.mode == GameMode.TIME_ATTACK && state.settings.isSurvivalMode)) {
                            handleDefeat()
                        } else {
                            isGameOver = true
                        }
                    }
                    
                    var opponentProgress = state.opponentProgress
                    var teamPartnerProgress = state.teamPartnerProgress
                    if (state.settings.mode == GameMode.COMPETITION && !state.isSolved && !state.isGameOver) {
                        val isFrozen = state.activePowerUps[PowerUpType.FREEZE_OPPONENT]?.let { it > System.currentTimeMillis() } ?: false

                        // Simulation for non-AI competition opponent (the real AI duel opponent
                        // is driven separately by startAiSimulation())
                        if (!state.settings.isAiMatch) {
                            if (!isFrozen) {
                                val increment = (5..15).random() / 1000f
                                opponentProgress = (opponentProgress + increment).coerceAtMost(1f)
                            }
                        }

                        // Team partner progress is simulated independently of the opponent,
                        // so it advances in both AI-backed and non-AI team matches.
                        if (state.settings.competitionType == CompetitionType.TEAM_2V2 && !isFrozen) {
                            teamPartnerProgress = (teamPartnerProgress + (3..8).random() / 1000f).coerceAtMost(1f)
                        }
                        
                        if (opponentProgress >= 1f) {
                            isGameOver = true
                        }
                        
                        // Randomly give a power-up every 15 seconds
                        if (state.settings.usePowerUps && newTime % 15 == 0L) {
                            val newPowerUp = PowerUpType.entries.random()
                            val currentList = state.availablePowerUps.toMutableList()
                            if (currentList.size < 4) {
                                currentList.add(newPowerUp)
                            }
                            return@update state.copy(
                                timeSeconds = newTime,
                                timeLeftSeconds = nextTimeLeft,
                                isGameOver = isGameOver,
                                opponentProgress = opponentProgress,
                                teamPartnerProgress = teamPartnerProgress,
                                availablePowerUps = currentList
                            )
                        }
                    }

                    state.copy(
                        timeSeconds = newTime,
                        timeLeftSeconds = nextTimeLeft,
                        isGameOver = isGameOver,
                        opponentProgress = opponentProgress,
                        teamPartnerProgress = teamPartnerProgress
                    )
                }
            }
        }

        val state = _uiState.value ?: return
        if (state.settings.mode == GameMode.COMPETITION && state.settings.isAiMatch) {
            startAiSimulation()
        }
    }

    private fun startAiSimulation() {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            while (true) {
                val state = _uiState.value ?: break
                if (state.isPaused || state.isSolved || state.isGameOver || !state.isMatchStarted) {
                    delay(500)
                    continue
                }

                val (moveDelay, errorRate) = when (state.settings.aiDifficulty) {
                    ChallengeDifficulty.EASY -> 2500L to 0.40f
                    ChallengeDifficulty.MEDIUM -> 1250L to 0.15f
                    ChallengeDifficulty.HARD -> 650L to 0.05f
                    ChallengeDifficulty.EXPERT -> 300L to 0.0f
                }

                delay(moveDelay)
                
                _uiState.update { current ->
                    if (current == null || current.isPaused || current.isSolved || current.isGameOver) return@update current
                    
                    // Check for freeze power-up
                    val isFrozen = current.activePowerUps[PowerUpType.FREEZE_OPPONENT]?.let { it > System.currentTimeMillis() } ?: false
                    if (isFrozen) return@update current

                    val currentDist = current.opponentDistance ?: return@update current
                    val isError = Random.nextFloat() < errorRate
                    val nextMoves = (current.opponentMoves ?: 0) + 1
                    
                    val nextDist = if (isError) {
                        currentDist + 1 // Made a mistake
                    } else {
                        (currentDist - 1).coerceAtLeast(0)
                    }
                    
                    val initial = current.initialDistance ?: 1
                    val progress = ((initial - nextDist).toFloat() / initial.toFloat()).coerceIn(0f, 1f)
                    
                    val aiWon = nextDist == 0
                    
                    current.copy(
                        opponentDistance = nextDist,
                        opponentProgress = progress,
                        opponentTime = if (aiWon) current.timeSeconds else current.opponentTime,
                        opponentMoves = nextMoves,
                        isGameOver = if (aiWon) true else current.isGameOver
                    )
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        aiJob?.cancel()
    }

    // ---- Profile editing ----

    private val _profileUpdateError = MutableStateFlow<String?>(null)
    val profileUpdateError: StateFlow<String?> = _profileUpdateError.asStateFlow()

    private val _profileUpdateSuccess = MutableStateFlow(false)
    val profileUpdateSuccess: StateFlow<Boolean> = _profileUpdateSuccess.asStateFlow()

    fun updateProfile(newNickname: String, newEmail: String) {
        viewModelScope.launch {
            val result = userRepository.updateProfile(newNickname, newEmail)
            when (result) {
                is com.example.a1234567889.models.ProfileUpdateResult.Success -> {
                    _profileUpdateError.value = null
                    _profileUpdateSuccess.value = true
                }
                is com.example.a1234567889.models.ProfileUpdateResult.Error -> {
                    _profileUpdateError.value = result.message
                    _profileUpdateSuccess.value = false
                }
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            val result = userRepository.changePassword(currentPassword, newPassword)
            when (result) {
                is com.example.a1234567889.models.ProfileUpdateResult.Success -> {
                    _profileUpdateError.value = null
                    _profileUpdateSuccess.value = true
                }
                is com.example.a1234567889.models.ProfileUpdateResult.Error -> {
                    _profileUpdateError.value = result.message
                    _profileUpdateSuccess.value = false
                }
            }
        }
    }

    fun requestPasswordReset(email: String): com.example.a1234567889.models.ForgotPasswordResult {
        return userRepository.requestPasswordReset(email)
    }

    fun clearProfileStatus() {
        _profileUpdateError.value = null
        _profileUpdateSuccess.value = false
    }

    /** Updates the avatar after a photo was taken with the camera or picked from the gallery (lab item 17). */
    fun updateAvatar(uri: String) {
        viewModelScope.launch {
            val result = userRepository.updateAvatar(uri)
            when (result) {
                is com.example.a1234567889.models.ProfileUpdateResult.Success -> {
                    _profileUpdateError.value = null
                    _profileUpdateSuccess.value = true
                }
                is com.example.a1234567889.models.ProfileUpdateResult.Error -> {
                    _profileUpdateError.value = result.message
                }
            }
        }
    }

    // ---- Game history (SQLite) — lab item 15 ----

    /** Lazily-created handle to the local SQLite database used for match history & chat (item 15). */
    val gameHistoryDb: com.example.a1234567889.data.db.AppDatabaseHelper by lazy {
        com.example.a1234567889.data.db.AppDatabaseHelper(getApplication())
    }

    private fun recordGameHistory(state: GameState, won: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            gameHistoryDb.insertGameRecord(
                mode = state.settings.mode.name,
                size = state.settings.size,
                moves = state.moves,
                timeSeconds = state.timeSeconds,
                won = won
            )
        }
    }
}
