package com.example.a1234567889.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.a1234567889.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class GamePreferences(private val context: Context) {

    companion object {
        val GRID_SIZE = intPreferencesKey("grid_size")
        val GAME_MODE = stringPreferencesKey("game_mode")
        val CHALLENGE_MODE = stringPreferencesKey("challenge_mode")
        val IMAGE_URI = stringPreferencesKey("image_uri")
        val LANGUAGE = stringPreferencesKey("language")
        val DARK_THEME = booleanPreferencesKey("dark_theme") // null would mean it's not set
        val HAS_DARK_THEME_VAL = booleanPreferencesKey("has_dark_theme_val")

        // Appearance
        val COLOR_SCHEME = stringPreferencesKey("color_scheme")
        val ANIMATION_SPEED = stringPreferencesKey("animation_speed")
        val FONT_STYLE = stringPreferencesKey("font_style")
        val FONT_SIZE = floatPreferencesKey("font_size")
        val BOARD_BACKGROUND = stringPreferencesKey("board_background")

        // Sound
        val SFX_VOLUME = floatPreferencesKey("sfx_volume")
        val MUSIC_VOLUME = floatPreferencesKey("music_volume")
        val SFX_ENABLED = booleanPreferencesKey("sfx_enabled")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val NOTIFY_ACHIEVEMENTS = booleanPreferencesKey("notify_achievements")
        val NOTIFY_FRIENDS = booleanPreferencesKey("notify_friends")

        // Game
        val AUTO_TIMER_START = booleanPreferencesKey("auto_timer_start")
        val SHOW_HINTS = booleanPreferencesKey("show_hints")
        val UNDO_LIMIT = intPreferencesKey("undo_limit")
        val SAVE_ON_EXIT = booleanPreferencesKey("save_on_exit")
        val ALLOW_MATCHES = booleanPreferencesKey("allow_matches")
        val CUSTOM_SHUFFLE_MOVES = intPreferencesKey("custom_shuffle_moves")
        val DIFFICULTY = stringPreferencesKey("difficulty")

        // Interface
        val CONTROL_METHOD = stringPreferencesKey("control_method")
        val SHOW_COUNTERS = booleanPreferencesKey("show_counters")
        val TILE_SIZE = stringPreferencesKey("tile_size")
        val TIME_ATTACK_BASE_MODE = stringPreferencesKey("time_attack_base_mode")
        val ENDLESS_VISUAL_MODE = stringPreferencesKey("endless_visual_mode")
        val ENDLESS_LIMIT_TYPE = stringPreferencesKey("endless_limit_type")

        val SURVIVAL_RECORD = intPreferencesKey("survival_record")
        val SURVIVAL_INITIAL_TIME = intPreferencesKey("survival_initial_time")
        val SURVIVAL_TIME_STEP = intPreferencesKey("survival_time_step")
        val SURVIVAL_SIZE_INTERVAL = intPreferencesKey("survival_size_interval")
        val IS_SURVIVAL_MODE = booleanPreferencesKey("is_survival_mode")

        val LOGGED_IN_EMAIL = stringPreferencesKey("logged_in_email")

        val TOTAL_GAMES = intPreferencesKey("total_games")

        // Color Mode
        val COLOR_PATTERN = stringPreferencesKey("color_pattern")
        val PALETTE_TYPE = stringPreferencesKey("palette_type")
        val COLOR_BLIND_MODE = booleanPreferencesKey("color_blind_mode")
        val CHROMATIC_ADAPTATION = booleanPreferencesKey("chromatic_adaptation")
        val ADAPTATION_INTENSITY = floatPreferencesKey("adaptation_intensity")
        val SHOW_COLOR_LABELS = booleanPreferencesKey("show_color_labels")
        
        fun bestTimeKey(size: Int, mode: GameMode, challenge: ChallengeMode) = 
            longPreferencesKey("best_time_${size}_${mode.name}_${challenge.name}")
        
        fun bestMovesKey(size: Int, mode: GameMode, challenge: ChallengeMode) = 
            intPreferencesKey("best_moves_${size}_${mode.name}_${challenge.name}")
    }

    val gameSettings: Flow<GameSettings> = context.dataStore.data.map { prefs ->
        GameSettings(
            size = prefs[GRID_SIZE] ?: 4,
            mode = GameMode.valueOf(prefs[GAME_MODE] ?: GameMode.NUMBERS.name),
            challenge = ChallengeMode.valueOf(prefs[CHALLENGE_MODE] ?: ChallengeMode.NORMAL.name),
            imageUri = prefs[IMAGE_URI],
            isDarkTheme = if (prefs[HAS_DARK_THEME_VAL] == true) prefs[DARK_THEME] else null,
            language = prefs[LANGUAGE] ?: "ru",
            
            colorScheme = prefs[COLOR_SCHEME] ?: "Classic",
            animationSpeed = AnimationSpeed.valueOf(prefs[ANIMATION_SPEED] ?: AnimationSpeed.MEDIUM.name),
            fontStyle = FontStyle.valueOf(prefs[FONT_STYLE] ?: FontStyle.DEFAULT.name),
            fontSize = prefs[FONT_SIZE] ?: 1f,
            boardBackground = prefs[BOARD_BACKGROUND] ?: "Default",
            
            sfxVolume = prefs[SFX_VOLUME] ?: 0.8f,
            musicVolume = prefs[MUSIC_VOLUME] ?: 0.5f,
            isSfxEnabled = prefs[SFX_ENABLED] ?: true,
            isMusicEnabled = prefs[MUSIC_ENABLED] ?: true,
            isVibrationEnabled = prefs[VIBRATION_ENABLED] ?: true,
            notifyAchievements = prefs[NOTIFY_ACHIEVEMENTS] ?: true,
            notifyFriends = prefs[NOTIFY_FRIENDS] ?: true,
            
            autoTimerStart = prefs[AUTO_TIMER_START] ?: false,
            showHints = prefs[SHOW_HINTS] ?: true,
            undoLimit = prefs[UNDO_LIMIT] ?: -1,
            saveOnExit = prefs[SAVE_ON_EXIT] ?: true,
            allowMatches = prefs[ALLOW_MATCHES] ?: false,
            customShuffleMoves = prefs[CUSTOM_SHUFFLE_MOVES] ?: 30,
            difficulty = ChallengeDifficulty.valueOf(prefs[DIFFICULTY] ?: ChallengeDifficulty.MEDIUM.name),
            
            controlMethod = ControlMethod.valueOf(prefs[CONTROL_METHOD] ?: ControlMethod.CLICK.name),
            showCounters = prefs[SHOW_COUNTERS] ?: true,
            tileSize = TileSize.valueOf(prefs[TILE_SIZE] ?: TileSize.AUTO.name),
            timeAttackBaseMode = GameMode.valueOf(prefs[TIME_ATTACK_BASE_MODE] ?: GameMode.NUMBERS.name),
            endlessVisualMode = GameMode.valueOf(prefs[ENDLESS_VISUAL_MODE] ?: GameMode.NUMBERS.name),
            endlessLimitType = EndlessLimitType.valueOf(prefs[ENDLESS_LIMIT_TYPE] ?: EndlessLimitType.MOVES.name),
            isSurvivalMode = prefs[IS_SURVIVAL_MODE] ?: false,
            survivalInitialTime = prefs[SURVIVAL_INITIAL_TIME] ?: 60,
            survivalTimeStep = prefs[SURVIVAL_TIME_STEP] ?: 5,
            survivalSizeIncreaseInterval = prefs[SURVIVAL_SIZE_INTERVAL] ?: 3,

            colorPattern = ColorPattern.valueOf(prefs[COLOR_PATTERN] ?: ColorPattern.SPECTRUM.name),
            paletteType = ColorPaletteType.valueOf(prefs[PALETTE_TYPE] ?: ColorPaletteType.RAINBOW.name),
            isColorBlindMode = prefs[COLOR_BLIND_MODE] ?: false,
            isChromaticAdaptation = prefs[CHROMATIC_ADAPTATION] ?: false,
            adaptationIntensity = prefs[ADAPTATION_INTENSITY] ?: 0.5f,
            showColorLabels = prefs[SHOW_COLOR_LABELS] ?: false
        )
    }

    suspend fun updateSettings(settings: GameSettings) {
        context.dataStore.edit { prefs ->
            prefs[GRID_SIZE] = settings.size
            prefs[GAME_MODE] = settings.mode.name
            prefs[CHALLENGE_MODE] = settings.challenge.name
            prefs[LANGUAGE] = settings.language
            settings.imageUri?.let { prefs[IMAGE_URI] = it } ?: prefs.remove(IMAGE_URI)
            
            settings.isDarkTheme?.let {
                prefs[DARK_THEME] = it
                prefs[HAS_DARK_THEME_VAL] = true
            } ?: run {
                prefs.remove(DARK_THEME)
                prefs[HAS_DARK_THEME_VAL] = false
            }

            prefs[COLOR_SCHEME] = settings.colorScheme
            prefs[ANIMATION_SPEED] = settings.animationSpeed.name
            prefs[FONT_STYLE] = settings.fontStyle.name
            prefs[FONT_SIZE] = settings.fontSize
            prefs[BOARD_BACKGROUND] = settings.boardBackground

            prefs[SFX_VOLUME] = settings.sfxVolume
            prefs[MUSIC_VOLUME] = settings.musicVolume
            prefs[SFX_ENABLED] = settings.isSfxEnabled
            prefs[MUSIC_ENABLED] = settings.isMusicEnabled
            prefs[VIBRATION_ENABLED] = settings.isVibrationEnabled
            prefs[NOTIFY_ACHIEVEMENTS] = settings.notifyAchievements
            prefs[NOTIFY_FRIENDS] = settings.notifyFriends

            prefs[AUTO_TIMER_START] = settings.autoTimerStart
            prefs[SHOW_HINTS] = settings.showHints
            prefs[UNDO_LIMIT] = settings.undoLimit
            prefs[SAVE_ON_EXIT] = settings.saveOnExit
            prefs[ALLOW_MATCHES] = settings.allowMatches
            prefs[CUSTOM_SHUFFLE_MOVES] = settings.customShuffleMoves
            prefs[DIFFICULTY] = settings.difficulty.name

            prefs[CONTROL_METHOD] = settings.controlMethod.name
            prefs[SHOW_COUNTERS] = settings.showCounters
            prefs[TILE_SIZE] = settings.tileSize.name
            prefs[TIME_ATTACK_BASE_MODE] = settings.timeAttackBaseMode.name
            prefs[ENDLESS_VISUAL_MODE] = settings.endlessVisualMode.name
            prefs[ENDLESS_LIMIT_TYPE] = settings.endlessLimitType.name
            prefs[IS_SURVIVAL_MODE] = settings.isSurvivalMode
            prefs[SURVIVAL_INITIAL_TIME] = settings.survivalInitialTime
            prefs[SURVIVAL_TIME_STEP] = settings.survivalTimeStep
            prefs[SURVIVAL_SIZE_INTERVAL] = settings.survivalSizeIncreaseInterval

            prefs[COLOR_PATTERN] = settings.colorPattern.name
            prefs[PALETTE_TYPE] = settings.paletteType.name
            prefs[COLOR_BLIND_MODE] = settings.isColorBlindMode
            prefs[CHROMATIC_ADAPTATION] = settings.isChromaticAdaptation
            prefs[ADAPTATION_INTENSITY] = settings.adaptationIntensity
            prefs[SHOW_COLOR_LABELS] = settings.showColorLabels
        }
    }

    fun getBestTime(size: Int, mode: GameMode, challenge: ChallengeMode): Flow<Long?> = 
        context.dataStore.data.map { it[bestTimeKey(size, mode, challenge)] }

    fun getBestMoves(size: Int, mode: GameMode, challenge: ChallengeMode): Flow<Int?> = 
        context.dataStore.data.map { it[bestMovesKey(size, mode, challenge)] }

    val survivalRecord: Flow<Int> = context.dataStore.data.map { it[SURVIVAL_RECORD] ?: 0 }

    suspend fun updateSurvivalRecord(levels: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[SURVIVAL_RECORD] ?: 0
            if (levels > current) {
                prefs[SURVIVAL_RECORD] = levels
            }
        }
    }

    suspend fun updateBestScore(size: Int, mode: GameMode, challenge: ChallengeMode, time: Long, moves: Int) {
        context.dataStore.edit { prefs ->
            val currentTime = prefs[bestTimeKey(size, mode, challenge)] ?: Long.MAX_VALUE
            if (time < currentTime) {
                prefs[bestTimeKey(size, mode, challenge)] = time
            }
            val currentMoves = prefs[bestMovesKey(size, mode, challenge)] ?: Int.MAX_VALUE
            if (moves < currentMoves) {
                prefs[bestMovesKey(size, mode, challenge)] = moves
            }
        }
    }

    val totalGames: Flow<Int> = context.dataStore.data.map { it[TOTAL_GAMES] ?: 0 }
    
    suspend fun incrementGames() {
        context.dataStore.edit { 
            val current = it[TOTAL_GAMES] ?: 0
            it[TOTAL_GAMES] = current + 1
        }
    }
}
