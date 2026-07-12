package com.example.a1234567889.models

import androidx.compose.ui.graphics.Color

data class Tile(
    val id: Int, // Permanent unique identifier
    val number: Int, // The number displayed on the tile (1 to size*size - 1), 0 for empty
    val isEmpty: Boolean = false,
    val color: Color? = null
)

data class Position(val row: Int, val col: Int)

data class Board(
    val size: Int,
    val tiles: List<List<Tile>>
) {
    fun getTile(pos: Position): Tile = tiles[pos.row][pos.col]
    
    fun findEmpty(): Position {
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (tiles[r][c].isEmpty) return Position(r, c)
            }
        }
        throw IllegalStateException("No empty tile found")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Board) return false
        if (size != other.size) return false
        return tiles == other.tiles
    }

    override fun hashCode(): Int {
        var result = size
        result = 31 * result + tiles.hashCode()
        return result
    }
}

enum class ImageRarity { COMMON, RARE, LEGENDARY }

// Rank of a collectible card awarded for completing an "Image" puzzle.
// Higher board sizes have a better chance of rolling a higher rank.
enum class CardRank { BRONZE, SILVER, GOLD }

data class ImageGalleryItem(
    val id: String,
    val title: String,
    val uri: String,
    val isCollected: Boolean = false,
    val bestTime: Long? = null,
    val bestMoves: Int? = null,
    val category: String = "General",
    val rarity: ImageRarity = ImageRarity.COMMON,
    val dateCollected: Long? = null,
    val timesSolved: Int = 0,
    val isCustom: Boolean = false,
    val complexity: Float = 0.5f, // 0.0 to 1.0
    val boardSize: Int = 0, // 0 for custom/uploaded images; 3..8 for built-in puzzles tied to a fixed board size
    val bestCardRank: CardRank? = null // best card rank ever earned for this picture
)

// A card collected by completing a built-in "Image" puzzle. Visually it is the
// photo from kart/<rank> (bundled as drawable-nodpi/<photoRes>.jpg) with the
// matching frame (drawable-nodpi/<rank>.png, with its black background
// stripped) drawn on top of it.
data class CollectedCard(
    val id: String = java.util.UUID.randomUUID().toString(),
    val rank: CardRank,
    val photoRes: String, // drawable resource name, e.g. "a2"
    val sourceImageId: String, // id of the ImageGalleryItem that was solved to earn this card
    val sourceTitle: String,
    val boardSize: Int,
    val obtainedAt: Long = System.currentTimeMillis(),
    val isDuplicate: Boolean = false // true if this exact card art was already in the collection
)

data class ColorPalette(
    val id: String,
    val name: String,
    val colors: List<Color>,
    val isCustom: Boolean = false
)

data class ChallengeRecord(
    val type: ChallengeMode,
    val count: Int = 0,
    val bestTime: Long? = null
)

data class CustomPreset(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val settings: GameSettings,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPublic: Boolean = false,
    val playCount: Int = 0
)

data class ChallengeInstance(
    val id: String = java.util.UUID.randomUUID().toString(),
    val code: String,
    val title: String,
    val description: String? = null,
    val settings: GameSettings,
    val creatorId: String,
    val bestTime: Long? = null,
    val bestMoves: Int? = null,
    val acceptedCount: Int = 0
)

enum class CompetitionLeague {
    BRONZE, SILVER, GOLD, PLATINA, DIAMOND, MASTER, GRANDMASTER
}

enum class CompetitionType {
    DUEL_FRIEND, DUEL_AI, RANKED, TEAM_2V2
}

enum class TeamMatchVariant {
    SPLIT_FIELD, SYNC_ASSEMBLY, RELAY
}

data class RankedStats(
    val mmr: Int = 1200,
    val league: CompetitionLeague = CompetitionLeague.BRONZE,
    val division: Int = 5, // 1 to 5
    val seasonWins: Int = 0,
    val seasonLosses: Int = 0,
    val bestLeague: CompetitionLeague = CompetitionLeague.BRONZE
)

data class TournamentMatch(
    val id: String = java.util.UUID.randomUUID().toString(),
    val player1Id: String,
    val player2Id: String,
    val winnerId: String? = null,
    val p1Score: Int = 0,
    val p2Score: Int = 0,
    val isFinished: Boolean = false
)

data class Tournament(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val organizerId: String,
    val participants: List<String>,
    val matches: List<TournamentMatch>,
    val winnerId: String? = null,
    val isStarted: Boolean = false,
    val isFinished: Boolean = false,
    val config: GameSettings
)

enum class PowerUpType {
    ACCELERATION, EXTRA_TIME, AUTO_SOLVE_3, RESET_OPPONENT_MOVES, FREEZE_OPPONENT
}

data class PowerUp(
    val type: PowerUpType,
    val color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Yellow
)

data class User(
    val id: String,
    val nickname: String,
    val avatarUrl: String? = null,
    val email: String? = null,
    val friends: List<String> = emptyList(),
    val gallery: List<ImageGalleryItem> = emptyList(),
    val cardCollection: List<CollectedCard> = emptyList(),
    val palettes: List<ColorPalette> = emptyList(),
    val challenges: List<ChallengeRecord> = emptyList(),
    val presets: List<CustomPreset> = emptyList(),
    val myChallenges: List<ChallengeInstance> = emptyList(),
    val rankedStats: RankedStats = RankedStats(),
    val joinedTournaments: List<String> = emptyList(),
    val winStreak: Int = 0,
    val stats: UserStats = UserStats(),
    val achievements: List<Achievement> = emptyList()
)

data class Score(
    val timeSeconds: Long,
    val moves: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class UserStats(
    val totalGames: Int = 0,
    val totalWins: Int = 0,
    val totalLosses: Int = 0,
    val totalTime: Long = 0,
    val classicGamesSolved: Int = 0,
    val completedChallengesCount: Int = 0,
    val completedChallengeIds: Set<String> = emptySet(),
    val bestScores: Map<String, Score> = emptyMap() // Key format: "size_mode_challenge"
)

enum class AchievementRarity { COMMON, BRONZE, SILVER, GOLD, PLATINUM }
enum class AchievementCategory { 
    GENERAL, 
    CLASSIC, 
    IMAGE, 
    COLORS, 
    CHALLENGE, 
    COMPETITION, 
    TIME_ATTACK, 
    ENDLESS
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val category: AchievementCategory,
    val rarity: AchievementRarity = AchievementRarity.COMMON,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val progress: Int = 0,
    val maxProgress: Int = 1,
    val isHidden: Boolean = false,
    val isNew: Boolean = false
)

enum class LeaderboardSortBy { TIME, MOVES }
enum class LeaderboardPeriod { ALL_TIME, SEASON, WEEK }

data class LeaderboardEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val userId: String,
    val nickname: String,
    val avatarUrl: String? = null,
    val timeSeconds: Long,
    val moves: Int,
    val date: Long = System.currentTimeMillis(),
    val isCurrentUser: Boolean = false,
    val rank: Int = 0,
    val rankChange: Int = 0 // Positive for up, negative for down
)

enum class GameMode {
    NUMBERS, IMAGE, COLORS, CHALLENGE, COMPETITION, TIME_ATTACK, ENDLESS, CUSTOM, IMAGE_FOLDER
}

enum class EndlessLimitType { MOVES, TIME }

enum class ChallengeDifficulty { EASY, MEDIUM, HARD, EXPERT }

enum class ChallengeMode {
    NORMAL, REVERSE, SPIRAL_IN, SPIRAL_OUT, MIRROR_V, MIRROR_H, TRANSPOSE, CHESSBOARD, MAGIC_SQUARE, DIAGONAL, MODULO, RANDOM, SNAKE
}

data class ChallengeConfig(
    val type: ChallengeMode,
    val title: String,
    val goalText: String,
    val difficulty: ChallengeDifficulty,
    val requiredSolved: Int = 0,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null
)

enum class FontStyle { DEFAULT, DECORATIVE, KIDS, SERIF, SANS_SERIF }
enum class AnimationSpeed { SLOW, MEDIUM, FAST }
enum class ControlMethod { CLICK, SWIPE, KEYBOARD }
enum class TileSize { AUTO, SMALL, MEDIUM, LARGE }

enum class ColorPattern {
    SPECTRUM, LINEAR, CHESSBOARD, SPIRAL, DIAGONAL, CONCENTRIC, RANDOM
}

enum class ColorPaletteType {
    RAINBOW, GRADIENT, MONOCHROME, PASTEL, BRIGHT, CHESSBOARD, RANDOM
}

data class GameSettings(
    val size: Int = 4,
    val mode: GameMode = GameMode.NUMBERS,
    val timeAttackBaseMode: GameMode = GameMode.NUMBERS,
    val challenge: ChallengeMode = ChallengeMode.NORMAL,
    val difficulty: ChallengeDifficulty = ChallengeDifficulty.MEDIUM,
    val colorPattern: ColorPattern = ColorPattern.SPECTRUM,
    val paletteType: ColorPaletteType = ColorPaletteType.RAINBOW,
    val imageUri: String? = null,
    val isDarkTheme: Boolean? = null, // null means follow system
    val language: String = "ru",
    
    // Appearance
    val colorScheme: String = "Classic",
    val animationSpeed: AnimationSpeed = AnimationSpeed.MEDIUM,
    val fontStyle: FontStyle = FontStyle.DEFAULT,
    val fontSize: Float = 1f, // Multiplier
    val boardBackground: String = "Default",
    
    // Sound & Notifications
    val sfxVolume: Float = 0.8f,
    val musicVolume: Float = 0.5f,
    val isSfxEnabled: Boolean = true,
    val isMusicEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val notifyAchievements: Boolean = true,
    val notifyFriends: Boolean = true,
    
    // Game Parameters
    val autoTimerStart: Boolean = false,
    val showHints: Boolean = true,
    val undoLimit: Int = -1, // -1 for unlimited
    val saveOnExit: Boolean = true,
    val timeAttackLimit: Int = 60,
    val isSurvivalMode: Boolean = false,
    val isExperimentMode: Boolean = false,
    val allowMatches: Boolean = false,
    val customShuffleMoves: Int = 30,

    // Endless Mode Specific
    val endlessVisualMode: GameMode = GameMode.NUMBERS,
    val endlessLimitType: EndlessLimitType = EndlessLimitType.MOVES,
    
    // Survival Mode Specific
    val survivalInitialTime: Int = 60,
    val survivalTimeStep: Int = 5,
    val survivalSizeIncreaseInterval: Int = 3,
    
    // Color Mode Specific
    val isColorBlindMode: Boolean = false,
    val isChromaticAdaptation: Boolean = false,
    val adaptationIntensity: Float = 0.5f,
    val showColorLabels: Boolean = false,
    val isDynamicPalette: Boolean = false,
    val dynamicPaletteSpeed: AnimationSpeed = AnimationSpeed.MEDIUM,
    val endPaletteType: ColorPaletteType = ColorPaletteType.RAINBOW,
    val customPaletteColors: List<androidx.compose.ui.graphics.Color> = emptyList(),
    
    // Competition
    val competitionType: CompetitionType = CompetitionType.DUEL_AI,
    val teamVariant: TeamMatchVariant = TeamMatchVariant.SPLIT_FIELD,
    val isAiMatch: Boolean = false,
    val aiDifficulty: ChallengeDifficulty = ChallengeDifficulty.MEDIUM,
    val winnerCriteria: String = "TIME", // "TIME" or "MOVES"
    val handicapSeconds: Int = 0,
    val usePowerUps: Boolean = false,
    val isPublicMatch: Boolean = true,
    val moveLimit: Int? = null,
    val matchSeed: Long? = null,
    val competitionVisualMode: GameMode = GameMode.NUMBERS,
    
    // Interface
    val controlMethod: ControlMethod = ControlMethod.CLICK,
    val showCounters: Boolean = true,
    val tileSize: TileSize = TileSize.AUTO
)

val GameSettings.visualMode: GameMode
    get() = when (mode) {
        GameMode.TIME_ATTACK -> timeAttackBaseMode
        GameMode.ENDLESS -> endlessVisualMode
        GameMode.COMPETITION -> competitionVisualMode
        else -> mode
    }

data class GameState(
    val board: Board,
    val targetBoard: Board,
    val opponentBoard: Board? = null,
    val moves: Int = 0,
    val timeSeconds: Long = 0,
    val timeLeftSeconds: Int? = null,
    val movesLeft: Int? = null,
    val lives: Int = 3,
    val score: Int = 0,
    val currentLevel: Int = 1,
    val correctTiles: Int = 0,
    val isPaused: Boolean = false,
    val isSolved: Boolean = false,
    val isGameOver: Boolean = false,
    val isNewRecord: Boolean = false,
    val newAchievements: List<Achievement> = emptyList(),
    val newCard: CollectedCard? = null,
    val history: List<Board> = emptyList(),
    val settings: GameSettings = GameSettings(),
    val currentUser: User? = null,

    // Stats for the whole Endless run
    val totalEndlessMoves: Int = 0,
    val totalEndlessTime: Long = 0,

    // Stats for the whole Survival run
    val totalSurvivalMoves: Int = 0,
    val totalSurvivalTime: Long = 0,
    
    // Experiment mode stats
    val inversions: Int = 0,
    val optimalMoves: Int? = null,
    val averageMoveTime: Double = 0.0,
    
    // Competition
    val opponentDistance: Int? = null,
    val initialDistance: Int? = null,
    val opponentTime: Long? = null,
    val opponentMoves: Int? = null,
    val opponentProgress: Float = 0f,
    val opponentNickname: String? = null,
    val opponentAvatarUrl: String? = null,
    val teamPartnerProgress: Float = 0f,
    val isMatchStarted: Boolean = false,
    val matchCountdown: Int? = null,
    val ratingChange: Int? = null,
    val activePowerUps: Map<PowerUpType, Long> = emptyMap(),
    val availablePowerUps: List<PowerUpType> = emptyList(),
    val showGoalPreview: Boolean = false
)

// ---- Profile update result types ----
sealed class ProfileUpdateResult {
    object Success : ProfileUpdateResult()
    data class Error(val message: String) : ProfileUpdateResult()
}

enum class ForgotPasswordResult {
    EmailSent,
    InvalidEmail
}

// ---- QR friend-add result types ----
sealed class AddFriendResult {
    data class Added(val nickname: String) : AddFriendResult()
    data class AlreadyFriend(val nickname: String) : AddFriendResult()
    object UserNotFound : AddFriendResult()
    object IsSelf : AddFriendResult()
    object InvalidCode : AddFriendResult()
    object NotLoggedIn : AddFriendResult()
}

