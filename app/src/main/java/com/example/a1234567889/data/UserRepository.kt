package com.example.a1234567889.data

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.a1234567889.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repository for managing user data, authentication, and stats.
 * In a real app, this would interact with a backend (Firebase, REST API, etc.).
 */
class UserRepository(private val context: Context) {

    companion object {
        /** Prefix that marks a scanned QR payload as a "friend invite" code (vs. some other QR). */
        const val FRIEND_QR_PREFIX = "puzzleapp:friend:"

        /** Builds the QR payload string for a given userId (encode this string into a QR bitmap). */
        fun friendQrPayload(userId: String): String = "$FRIEND_QR_PREFIX$userId"

        private const val MIN_PASSWORD_LENGTH = 6
        private const val MIN_NICKNAME_LENGTH = 2
        private val ACCOUNTS_KEY = stringSetPreferencesKey("registered_accounts")
        private val SESSION_EMAIL_KEY = androidx.datastore.preferences.core.stringPreferencesKey("logged_in_email")

        fun isValidEmail(email: String): Boolean =
            email.contains('@') && email.contains('.') && email.indexOf('@') < email.lastIndexOf('.')

        fun isValidPassword(password: String): Boolean =
            password.length >= MIN_PASSWORD_LENGTH

        fun isValidNickname(nickname: String): Boolean =
            nickname.length >= MIN_NICKNAME_LENGTH
    }

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: Flow<User?> = _currentUser.asStateFlow()

    private val _userStats = MutableStateFlow(UserStats())
    val userStats: Flow<UserStats> = _userStats.asStateFlow()

    private val _achievements = MutableStateFlow<List<Achievement>>(defaultAchievements())
    val achievements: Flow<List<Achievement>> = _achievements.asStateFlow()

    private val _newAchievements = MutableStateFlow<Achievement?>(null)
    val newAchievements: Flow<Achievement?> = _newAchievements.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: Flow<String?> = _authError.asStateFlow()

    // Mock database: Email -> Triple(HashedPassword, Nickname, User)
    private val mockUserDb = mutableMapOf<String, Triple<String, String, User>>()

    // ---- Persistence of registered accounts across app restarts ----
    // Accounts are stored on-device (DataStore) so that every account created
    // (e.g. "aa", then later "bb") stays saved, can be logged into again,
    // and shows up in friend/nickname search — even after the app is closed
    // and reopened, not just within the current app session.
    private val accountsLoadMutex = Mutex()
    private var accountsLoaded = false

    private data class AccountRecord(
        val email: String,
        val password: String,
        val user: User
    )

    private fun userToJson(user: User): JSONObject {
        val json = JSONObject()
        json.put("id", user.id)
        json.put("nickname", user.nickname)
        json.put("email", user.email)
        
        // Stats
        val stats = JSONObject()
        stats.put("totalGames", user.stats.totalGames)
        stats.put("totalWins", user.stats.totalWins)
        stats.put("totalLosses", user.stats.totalLosses)
        stats.put("totalTime", user.stats.totalTime)
        stats.put("classicGamesSolved", user.stats.classicGamesSolved)
        stats.put("completedChallengesCount", user.stats.completedChallengesCount)
        
        val challengeIds = JSONArray()
        user.stats.completedChallengeIds.forEach { challengeIds.put(it) }
        stats.put("completedChallengeIds", challengeIds)
        
        val bestScoresJson = JSONObject()
        user.stats.bestScores.forEach { (key, score) ->
            val scoreJson = JSONObject()
            scoreJson.put("time", score.timeSeconds)
            scoreJson.put("moves", score.moves)
            bestScoresJson.put(key, scoreJson)
        }
        stats.put("bestScores", bestScoresJson)
        
        json.put("stats", stats)

        // Achievements
        val achievements = JSONArray()
        user.achievements.forEach { achievement ->
            val achJson = JSONObject()
            achJson.put("id", achievement.id)
            achJson.put("isUnlocked", achievement.isUnlocked)
            achJson.put("progress", achievement.progress)
            achJson.put("unlockedAt", achievement.unlockedAt ?: 0L)
            achievements.put(achJson)
        }
        json.put("achievements", achievements)

        // Gallery (collected/uploaded pictures)
        val gallery = JSONArray()
        user.gallery.forEach { item ->
            val itemJson = JSONObject()
            itemJson.put("id", item.id)
            itemJson.put("title", item.title)
            itemJson.put("uri", item.uri)
            itemJson.put("isCollected", item.isCollected)
            itemJson.put("bestTime", item.bestTime ?: -1L)
            itemJson.put("bestMoves", item.bestMoves ?: -1)
            itemJson.put("category", item.category)
            itemJson.put("dateCollected", item.dateCollected ?: -1L)
            itemJson.put("timesSolved", item.timesSolved)
            itemJson.put("isCustom", item.isCustom)
            itemJson.put("boardSize", item.boardSize)
            itemJson.put("bestCardRank", item.bestCardRank?.name ?: "")
            gallery.put(itemJson)
        }
        json.put("gallery", gallery)

        // Card collection
        val cards = JSONArray()
        user.cardCollection.forEach { card ->
            val cardJson = JSONObject()
            cardJson.put("id", card.id)
            cardJson.put("rank", card.rank.name)
            cardJson.put("photoRes", card.photoRes)
            cardJson.put("sourceImageId", card.sourceImageId)
            cardJson.put("sourceTitle", card.sourceTitle)
            cardJson.put("boardSize", card.boardSize)
            cardJson.put("obtainedAt", card.obtainedAt)
            cardJson.put("isDuplicate", card.isDuplicate)
            cards.put(cardJson)
        }
        json.put("cardCollection", cards)
        
        return json
    }

    private fun jsonToUser(json: JSONObject): User {
        val statsJson = json.optJSONObject("stats")
        val stats = if (statsJson != null) {
            val challengeIds = mutableSetOf<String>()
            val idsArray = statsJson.optJSONArray("completedChallengeIds")
            if (idsArray != null) {
                for (i in 0 until idsArray.length()) {
                    challengeIds.add(idsArray.getString(i))
                }
            }
            
            val bestScores = mutableMapOf<String, Score>()
            val bestJson = statsJson.optJSONObject("bestScores")
            if (bestJson != null) {
                bestJson.keys().forEach { key ->
                    val s = bestJson.getJSONObject(key)
                    bestScores[key] = Score(s.getLong("time"), s.getInt("moves"))
                }
            }
            
            UserStats(
                totalGames = statsJson.optInt("totalGames", 0),
                totalWins = statsJson.optInt("totalWins", 0),
                totalLosses = statsJson.optInt("totalLosses", 0),
                totalTime = statsJson.optLong("totalTime", 0),
                classicGamesSolved = statsJson.optInt("classicGamesSolved", 0),
                completedChallengesCount = statsJson.optInt("completedChallengesCount", 0),
                completedChallengeIds = challengeIds,
                bestScores = bestScores
            )
        } else UserStats()

        val achievementsList = mutableListOf<Achievement>()
        val achJsonArray = json.optJSONArray("achievements")
        val defaultAchs = defaultAchievements()
        
        if (achJsonArray != null) {
            for (i in 0 until achJsonArray.length()) {
                val achJson = achJsonArray.getJSONObject(i)
                val id = achJson.getString("id")
                val isUnlocked = achJson.getBoolean("isUnlocked")
                val progress = achJson.getInt("progress")
                val unlockedAt = achJson.optLong("unlockedAt", 0L)
                
                defaultAchs.find { it.id == id }?.let { base ->
                    achievementsList.add(base.copy(
                        isUnlocked = isUnlocked,
                        progress = progress,
                        unlockedAt = if (unlockedAt > 0) unlockedAt else null
                    ))
                }
            }
        }
        
        // Ensure all achievements are present
        defaultAchs.forEach { base ->
            if (achievementsList.none { it.id == base.id }) {
                achievementsList.add(base)
            }
        }

        val galleryList = mutableListOf<ImageGalleryItem>()
        val galleryJsonArray = json.optJSONArray("gallery")
        if (galleryJsonArray != null) {
            for (i in 0 until galleryJsonArray.length()) {
                val itemJson = galleryJsonArray.getJSONObject(i)
                val bestTime = itemJson.optLong("bestTime", -1L).let { if (it < 0) null else it }
                val bestMoves = itemJson.optInt("bestMoves", -1).let { if (it < 0) null else it }
                val dateCollected = itemJson.optLong("dateCollected", -1L).let { if (it < 0) null else it }
                val rankName = itemJson.optString("bestCardRank", "")
                galleryList.add(
                    ImageGalleryItem(
                        id = itemJson.getString("id"),
                        title = itemJson.optString("title", ""),
                        uri = itemJson.getString("uri"),
                        isCollected = itemJson.optBoolean("isCollected", false),
                        bestTime = bestTime,
                        bestMoves = bestMoves,
                        category = itemJson.optString("category", "General"),
                        dateCollected = dateCollected,
                        timesSolved = itemJson.optInt("timesSolved", 0),
                        isCustom = itemJson.optBoolean("isCustom", false),
                        boardSize = itemJson.optInt("boardSize", 0),
                        bestCardRank = if (rankName.isBlank()) null else runCatching { CardRank.valueOf(rankName) }.getOrNull()
                    )
                )
            }
        }

        val cardList = mutableListOf<CollectedCard>()
        val cardJsonArray = json.optJSONArray("cardCollection")
        if (cardJsonArray != null) {
            for (i in 0 until cardJsonArray.length()) {
                val cardJson = cardJsonArray.getJSONObject(i)
                val rankName = cardJson.optString("rank", CardRank.BRONZE.name)
                cardList.add(
                    CollectedCard(
                        id = cardJson.optString("id", java.util.UUID.randomUUID().toString()),
                        rank = runCatching { CardRank.valueOf(rankName) }.getOrDefault(CardRank.BRONZE),
                        photoRes = cardJson.getString("photoRes"),
                        sourceImageId = cardJson.optString("sourceImageId", ""),
                        sourceTitle = cardJson.optString("sourceTitle", ""),
                        boardSize = cardJson.optInt("boardSize", 0),
                        obtainedAt = cardJson.optLong("obtainedAt", System.currentTimeMillis()),
                        isDuplicate = cardJson.optBoolean("isDuplicate", false)
                    )
                )
            }
        }

        return User(
            id = json.getString("id"),
            nickname = json.getString("nickname"),
            email = json.optString("email"),
            stats = stats,
            achievements = achievementsList,
            gallery = galleryList,
            cardCollection = cardList
        )
    }

    private fun encodeAccount(record: AccountRecord): String {
        val json = JSONObject()
        json.put("email", record.email)
        json.put("password", record.password)
        json.put("user", userToJson(record.user))
        return Base64.encodeToString(json.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    private fun decodeAccount(raw: String): AccountRecord? {
        return try {
            val json = JSONObject(String(Base64.decode(raw, Base64.NO_WRAP), Charsets.UTF_8))
            AccountRecord(
                email = json.getString("email"),
                password = json.getString("password"),
                user = jsonToUser(json.getJSONObject("user"))
            )
        } catch (e: Exception) {
            null
        }
    }

    /** Loads previously saved accounts from disk into memory (once per process). */
    private suspend fun ensureAccountsLoaded() {
        if (accountsLoaded) return
        accountsLoadMutex.withLock {
            if (accountsLoaded) return
            val saved = context.dataStore.data
                .map { it[ACCOUNTS_KEY] ?: emptySet() }
                .first()
            for (raw in saved) {
                val record = decodeAccount(raw) ?: continue
                val emailKey = record.email.lowercase()
                if (!mockUserDb.containsKey(emailKey)) {
                    mockUserDb[emailKey] = Triple(record.password, record.user.nickname, record.user)
                    registeredUserNicknames[record.user.id] = record.user.nickname
                    
                    // Reload their best scores into memory leaderboard
                    val overallBest = record.user.stats.bestScores["overall"]
                    if (overallBest != null) {
                        recordLeaderboardResultForUser(record.user.id, record.user.nickname, overallBest.timeSeconds, overallBest.moves)
                    }
                }
            }
            accountsLoaded = true
        }
    }

    /** Saves the current state of the logged-in user to disk. */
    private suspend fun saveCurrentUser() {
        val user = _currentUser.value ?: return
        val email = user.email?.takeIf { it.isNotBlank() } ?: return
        val existing = mockUserDb[email] ?: return
        mockUserDb[email] = Triple(existing.first, existing.second, user)
        persistAccounts()
    }

    /** Persists the full set of registered accounts to disk. */
    private suspend fun persistAccounts() = withContext(Dispatchers.IO) {
        val encoded = mockUserDb.entries.map { (email, triple) ->
            encodeAccount(AccountRecord(email = email, password = triple.first, user = triple.third))
        }.toSet()
        context.dataStore.edit { prefs ->
            prefs[ACCOUNTS_KEY] = encoded
        }
    }


    suspend fun register(email: String, nickname: String, password: String): Boolean {
        ensureAccountsLoaded()

        val trimmedEmail = email.trim()
        val trimmedNickname = nickname.trim()

        if (trimmedEmail.isBlank() || trimmedNickname.isBlank() || password.isBlank()) {
            _authError.value = "Заполните все поля"
            return false
        }
        if (!isValidEmail(trimmedEmail)) {
            _authError.value = "Введите корректный email (должен содержать @)"
            return false
        }
        if (!isValidNickname(trimmedNickname)) {
            _authError.value = "Никнейм должен содержать не менее $MIN_NICKNAME_LENGTH символов"
            return false
        }
        if (!isValidPassword(password)) {
            _authError.value = "Пароль должен содержать не менее $MIN_PASSWORD_LENGTH символов"
            return false
        }
        if (mockUserDb.containsKey(trimmedEmail.lowercase())) {
            _authError.value = "Пользователь с таким email уже существует"
            return false
        }

        val user = User(
            id = java.util.UUID.randomUUID().toString(),
            nickname = trimmedNickname,
            email = trimmedEmail,
            achievements = defaultAchievements()
        )
        mockUserDb[trimmedEmail.lowercase()] = Triple(password, trimmedNickname, user)
        _currentUser.value = user
        _userStats.value = user.stats
        _achievements.value = user.achievements
        registeredUserNicknames[user.id] = trimmedNickname
        _authError.value = null
        
        // Save session
        context.dataStore.edit { it[SESSION_EMAIL_KEY] = trimmedEmail.lowercase() }
        
        persistAccounts()
        syncWithCloud()
        return true
    }

    suspend fun login(email: String, password: String): Boolean {
        ensureAccountsLoaded()

        val trimmedEmail = email.trim()

        if (trimmedEmail.isBlank() || password.isBlank()) {
            _authError.value = "Заполните все поля"
            return false
        }
        if (!isValidEmail(trimmedEmail)) {
            _authError.value = "Введите корректный email (должен содержать @)"
            return false
        }

        val entry = mockUserDb[trimmedEmail.lowercase()]
        if (entry != null && entry.first == password) {
            val user = entry.third
            _currentUser.value = user
            _userStats.value = user.stats
            _achievements.value = user.achievements
            registeredUserNicknames[user.id] = user.nickname
            _authError.value = null

            // Save session
            context.dataStore.edit { it[SESSION_EMAIL_KEY] = trimmedEmail.lowercase() }

            syncWithCloud()
            return true
        }
        _authError.value = "Неверный email или пароль"
        return false
    }

    fun clearError() {
        _authError.value = null
    }

    // Record a real game result for the current user (used to populate leaderboard)
    suspend fun recordLeaderboardResult(timeSeconds: Long, moves: Int) {
        val user = _currentUser.value ?: return
        
        // Update user's overall best score
        val currentBest = user.stats.bestScores["overall"]
        val isBetter = currentBest == null || timeSeconds < currentBest.timeSeconds || (timeSeconds == currentBest.timeSeconds && moves < currentBest.moves)
        
        if (isBetter) {
            val newBestScores = user.stats.bestScores.toMutableMap()
            newBestScores["overall"] = Score(timeSeconds, moves)
            val newStats = user.stats.copy(bestScores = newBestScores)
            _currentUser.update { it?.copy(stats = newStats) }
            _userStats.value = newStats
            syncUserWithDb()
            persistAccounts()
        }

        recordLeaderboardResultForUser(user.id, user.nickname, timeSeconds, moves)
    }

    // Mode-specific collections
    suspend fun addToGallery(uri: String, time: Long, moves: Int, boardSize: Int = 0) {
        _currentUser.update { user ->
            user?.let {
                val currentGallery = it.gallery.toMutableList()
                val index = currentGallery.indexOfFirst { item -> item.uri == uri }
                if (index != -1) {
                    val item = currentGallery[index]
                    currentGallery[index] = item.copy(
                        isCollected = true,
                        bestTime = minOf(item.bestTime ?: Long.MAX_VALUE, time),
                        bestMoves = minOf(item.bestMoves ?: Int.MAX_VALUE, moves),
                        timesSolved = item.timesSolved + 1,
                        dateCollected = item.dateCollected ?: System.currentTimeMillis()
                    )
                } else {
                    currentGallery.add(
                        ImageGalleryItem(
                            id = java.util.UUID.randomUUID().toString(),
                            title = "Custom Puzzle",
                            uri = uri,
                            isCollected = true,
                            bestTime = time,
                            bestMoves = moves,
                            timesSolved = 1,
                            dateCollected = System.currentTimeMillis(),
                            isCustom = true,
                            boardSize = boardSize
                        )
                    )
                }
                it.copy(gallery = currentGallery)
            }
        }
    }

    /** Awards a card, updates gallery bestCardRank and saves. Returns the CollectedCard added
     *  (with [com.example.a1234567889.models.CollectedCard.isDuplicate] resolved against the
     *  collection the player already owned). */
    suspend fun addCollectedCard(card: com.example.a1234567889.models.CollectedCard): com.example.a1234567889.models.CollectedCard {
        var savedCard = card
        _currentUser.update { user ->
            user?.let {
                val alreadyOwned = it.cardCollection.any { existing -> existing.photoRes == card.photoRes }
                savedCard = card.copy(isDuplicate = alreadyOwned)
                val cards = it.cardCollection.toMutableList()
                cards.add(savedCard)
                // Also bump bestCardRank on the matching gallery item
                val gallery = it.gallery.toMutableList()
                val gIdx = gallery.indexOfFirst { item -> item.id == card.sourceImageId }
                if (gIdx != -1) {
                    val item = gallery[gIdx]
                    val better = when {
                        item.bestCardRank == null -> true
                        card.rank == com.example.a1234567889.models.CardRank.GOLD -> true
                        card.rank == com.example.a1234567889.models.CardRank.SILVER && item.bestCardRank == com.example.a1234567889.models.CardRank.BRONZE -> true
                        else -> false
                    }
                    if (better) gallery[gIdx] = item.copy(bestCardRank = card.rank)
                }
                it.copy(cardCollection = cards, gallery = gallery)
            }
        }
        saveCurrentUser()
        return savedCard
    }

    suspend fun uploadToGallery(uri: String, title: String, category: String) {
        _currentUser.update { user ->
            user?.let {
                val currentGallery = it.gallery.toMutableList()
                currentGallery.add(
                    ImageGalleryItem(
                        id = java.util.UUID.randomUUID().toString(),
                        title = title,
                        uri = uri,
                        category = category,
                        isCustom = true
                    )
                )
                it.copy(gallery = currentGallery)
            }
        }
    }

    suspend fun updateWinStreak(won: Boolean) {
        _currentUser.update { it?.copy(winStreak = if (won) it.winStreak + 1 else 0) }
    }

    suspend fun recordChallenge(mode: ChallengeMode, time: Long) {
        _currentUser.update { user ->
            user?.let {
                val currentChallenges = it.challenges.toMutableList()
                val index = currentChallenges.indexOfFirst { record -> record.type == mode }
                
                // Track unique completions for progression
                val newCompletedChallengeIds = it.stats.completedChallengeIds.toMutableSet()
                val isFirstTime = newCompletedChallengeIds.add(mode.name)
                
                val newStats = it.stats.copy(
                    completedChallengesCount = if (isFirstTime) it.stats.completedChallengesCount + 1 else it.stats.completedChallengesCount,
                    completedChallengeIds = newCompletedChallengeIds
                )

                if (index != -1) {
                    val record = currentChallenges[index]
                    currentChallenges[index] = record.copy(
                        count = record.count + 1,
                        bestTime = minOf(record.bestTime ?: Long.MAX_VALUE, time)
                    )
                } else {
                    currentChallenges.add(ChallengeRecord(mode, 1, time))
                }
                
                // Update local flow immediately
                _userStats.value = newStats

                it.copy(
                    challenges = currentChallenges,
                    stats = newStats
                )
            }
        }
        syncUserWithDb()
        persistAccounts()
    }

    suspend fun logout() {
        _currentUser.value = null
        _userStats.value = UserStats()
        _achievements.value = defaultAchievements()
        // Clear session
        context.dataStore.edit { it.remove(SESSION_EMAIL_KEY) }
    }

    suspend fun updateNickname(newNickname: String) {
        _currentUser.update { it?.copy(nickname = newNickname) }
    }

    suspend fun incrementClassicWins() {
        val newStats = _userStats.value.copy(classicGamesSolved = _userStats.value.classicGamesSolved + 1)
        _userStats.value = newStats
        updateCurrentUserStats(newStats)
    }

    suspend fun savePreset(name: String, settings: GameSettings, presetId: String? = null, isPublic: Boolean = false) {
        _currentUser.update { user ->
            user?.let {
                val currentPresets = it.presets.toMutableList()
                if (presetId != null) {
                    val index = currentPresets.indexOfFirst { p -> p.id == presetId }
                    if (index != -1) {
                        currentPresets[index] = currentPresets[index].copy(
                            name = name,
                            settings = settings,
                            isPublic = isPublic,
                            updatedAt = System.currentTimeMillis()
                        )
                    } else {
                        currentPresets.add(CustomPreset(id = presetId, name = name, settings = settings, isPublic = isPublic))
                    }
                } else {
                    currentPresets.add(CustomPreset(name = name, settings = settings, isPublic = isPublic))
                }
                it.copy(presets = currentPresets)
            }
        }
    }

    suspend fun deletePreset(presetId: String) {
        _currentUser.update { user ->
            user?.let {
                it.copy(presets = it.presets.filter { p -> p.id != presetId })
            }
        }
    }

    suspend fun createChallenge(title: String, description: String?, settings: GameSettings): String {
        val code = com.example.a1234567889.logic.SettingSharingUtils.generateCode(settings)
        val instance = ChallengeInstance(
            code = code,
            title = title,
            description = description,
            settings = settings,
            creatorId = _currentUser.value?.id ?: "guest"
        )
        _currentUser.update { user ->
            user?.let {
                val currentChallenges = it.myChallenges.toMutableList()
                currentChallenges.add(instance)
                it.copy(myChallenges = currentChallenges)
            }
        }
        return code
    }

    suspend fun recordGame(time: Long) {
        val newStats = _userStats.value.copy(
            totalGames = _userStats.value.totalGames + 1,
            totalWins = _userStats.value.totalWins + 1,
            totalTime = _userStats.value.totalTime + time
        )
        _userStats.value = newStats
        updateCurrentUserStats(newStats)
    }

    suspend fun recordLoss() {
        val newStats = _userStats.value.copy(
            totalGames = _userStats.value.totalGames + 1,
            totalLosses = _userStats.value.totalLosses + 1
        )
        _userStats.value = newStats
        updateCurrentUserStats(newStats)
    }

    private suspend fun updateCurrentUserStats(stats: UserStats) {
        _currentUser.update { it?.copy(stats = stats) }
        syncUserWithDb()
        persistAccounts()
    }

    private fun syncUserWithDb() {
        val user = _currentUser.value ?: return
        val emailKey = user.email?.lowercase() ?: return
        val entry = mockUserDb[emailKey]
        if (entry != null) {
            mockUserDb[emailKey] = entry.copy(third = user)
        }
    }

    suspend fun updateStats(newStats: UserStats) {
        _userStats.value = newStats
    }

    fun clearNewAchievement() {
        _newAchievements.value = null
    }

    suspend fun updateAchievementProgress(achievementId: String, increment: Int = 1) {
        _achievements.update { current ->
            current.map {
                if (it.id == achievementId && !it.isUnlocked) {
                    val newProgress = it.progress + increment
                    if (newProgress >= it.maxProgress) {
                        val unlocked = it.copy(
                            isUnlocked = true,
                            unlockedAt = System.currentTimeMillis(),
                            progress = it.maxProgress
                        )
                        _newAchievements.value = unlocked
                        unlocked
                    } else {
                        it.copy(progress = newProgress)
                    }
                } else it
            }
        }
        updateCurrentUserAchievements()
    }

    suspend fun unlockAchievement(achievementId: String) {
        _achievements.update { current ->
            current.map {
                if (it.id == achievementId && !it.isUnlocked) {
                    val unlocked = it.copy(
                        isUnlocked = true,
                        unlockedAt = System.currentTimeMillis(),
                        progress = it.maxProgress,
                        isNew = true
                    )
                    _newAchievements.value = unlocked
                    unlocked
                } else it
            }
        }
        updateCurrentUserAchievements()
    }

    private suspend fun updateCurrentUserAchievements() {
        val achs = _achievements.value
        _currentUser.update { it?.copy(achievements = achs) }
        syncUserWithDb()
        persistAccounts()
    }

    suspend fun resetAchievements() {
        _achievements.value = defaultAchievements()
    }

    // All registered users who have ever played a game (userId -> LeaderboardEntry)
    // This is populated for EVERY registered user when they complete a game
    private val allUserLeaderboardScores = mutableMapOf<String, LeaderboardEntry>()

    // Registered user nicknames lookup: userId -> nickname (for friend search)
    private val registeredUserNicknames = mutableMapOf<String, String>()

    // Tournaments created by users: tournamentId -> Tournament
    private val _tournaments = MutableStateFlow<List<Tournament>>(emptyList())
    val tournaments: kotlinx.coroutines.flow.Flow<List<Tournament>> = _tournaments.asStateFlow()

    suspend fun getLeaderboard(
        mode: GameMode,
        size: Int,
        sortBy: LeaderboardSortBy,
        onlyFriends: Boolean,
        period: LeaderboardPeriod,
        searchQuery: String = ""
    ): List<LeaderboardEntry> {
        val currentUserId = _currentUser.value?.id ?: ""
        val currentUserNickname = _currentUser.value?.nickname ?: ""

        val entries = mutableListOf<LeaderboardEntry>()

        // Add the current user's entry (real score or placeholder)
        if (currentUserId.isNotEmpty()) {
            val myScore = allUserLeaderboardScores[currentUserId]
            entries.add(
                myScore?.copy(nickname = currentUserNickname, isCurrentUser = true)
                    ?: LeaderboardEntry(
                        userId = currentUserId,
                        nickname = currentUserNickname,
                        timeSeconds = 0L,
                        moves = 0,
                        isCurrentUser = true,
                        rankChange = 0
                    )
            )
        }

        // Add all OTHER registered users who have scores (no fake/hardcoded entries)
        for ((userId, entry) in allUserLeaderboardScores) {
            if (userId == currentUserId) continue
            val nickname = registeredUserNicknames[userId] ?: entry.nickname
            entries.add(entry.copy(nickname = nickname, isCurrentUser = false))
        }

        var filtered = entries.filter {
            searchQuery.isBlank() || it.nickname.contains(searchQuery, ignoreCase = true)
        }

        if (onlyFriends) {
            val friends = _currentUser.value?.friends ?: emptyList()
            filtered = filtered.filter { it.userId in friends || it.isCurrentUser }
        }

        // Only show users with actual results (except current user who gets a placeholder)
        val withResults = filtered.filter { it.isCurrentUser || it.timeSeconds > 0 }

        val sorted = when (sortBy) {
            LeaderboardSortBy.TIME -> withResults.sortedWith(
                compareByDescending<LeaderboardEntry> { it.isCurrentUser && it.timeSeconds == 0L }
                    .thenBy { if (it.timeSeconds == 0L) Long.MAX_VALUE else it.timeSeconds }
            )
            LeaderboardSortBy.MOVES -> withResults.sortedWith(
                compareByDescending<LeaderboardEntry> { it.isCurrentUser && it.moves == 0 }
                    .thenBy { if (it.moves == 0) Int.MAX_VALUE else it.moves }
            )
        }

        return sorted.mapIndexed { index, entry ->
            entry.copy(rank = index + 1)
        }
    }

    // Called when ANY registered user finishes a game — records their score globally
    fun recordLeaderboardResultForUser(userId: String, nickname: String, timeSeconds: Long, moves: Int) {
        val existing = allUserLeaderboardScores[userId]
        val bestTime = if (existing != null) minOf(existing.timeSeconds, timeSeconds) else timeSeconds
        val bestMoves = if (existing != null) minOf(existing.moves, moves) else moves
        allUserLeaderboardScores[userId] = LeaderboardEntry(
            userId = userId,
            nickname = nickname,
            timeSeconds = bestTime,
            moves = bestMoves,
            isCurrentUser = false
        )
        registeredUserNicknames[userId] = nickname
    }

    /** Search registered users by nickname prefix (for friend/duel invites). */
    suspend fun searchRegisteredUsers(query: String): List<Pair<String, String>> {
        ensureAccountsLoaded()
        if (query.isBlank()) return emptyList()
        val currentId = _currentUser.value?.id ?: ""
        return registeredUserNicknames
            .filter { (id, nick) -> id != currentId && nick.contains(query, ignoreCase = true) }
            .map { (id, nick) -> id to nick }
            .sortedBy { it.second }
    }

    /** Get the list of registered users who are "friends" of the current user. */
    fun getFriends(): List<Pair<String, String>> {
        val user = _currentUser.value ?: return emptyList()
        return user.friends.mapNotNull { friendId ->
            registeredUserNicknames[friendId]?.let { friendId to it }
        }
    }

    /** Add a friend by userId. */
    suspend fun addFriend(friendUserId: String): Boolean {
        val user = _currentUser.value ?: return false
        if (friendUserId == user.id) return false
        if (user.friends.contains(friendUserId)) return false
        if (!registeredUserNicknames.containsKey(friendUserId)) return false
        _currentUser.update { it?.copy(friends = it.friends + friendUserId) }
        return true
    }

    /** Nickname for a given userId, if known to this device (used to label "added a friend"). */
    fun nicknameForUserId(userId: String): String? = registeredUserNicknames[userId]

    /** The current user's id and QR payload to show on screen so a friend can scan it. */
    fun myFriendQrPayload(): String? = _currentUser.value?.id?.let { friendQrPayload(it) }

    /**
     * Add a friend from a scanned QR payload (see [com.example.a1234567889.logic.QrUtils]).
     * Returns a result describing what happened so the UI can show a clear message.
     */
    suspend fun addFriendFromQrPayload(payload: String): AddFriendResult {
        val user = _currentUser.value ?: return AddFriendResult.NotLoggedIn
        if (!payload.startsWith(FRIEND_QR_PREFIX)) return AddFriendResult.InvalidCode
        val friendUserId = payload.removePrefix(FRIEND_QR_PREFIX).trim()
        if (friendUserId.isBlank()) return AddFriendResult.InvalidCode
        if (friendUserId == user.id) return AddFriendResult.IsSelf
        if (user.friends.contains(friendUserId)) {
            return AddFriendResult.AlreadyFriend(registeredUserNicknames[friendUserId] ?: friendUserId)
        }
        val nickname = registeredUserNicknames[friendUserId]
            ?: return AddFriendResult.UserNotFound
        _currentUser.update { it?.copy(friends = it.friends + friendUserId) }
        persistAccounts()
        return AddFriendResult.Added(nickname)
    }

    /** Remove a friend by userId. */
    suspend fun removeFriend(friendUserId: String) {
        _currentUser.update { it?.copy(friends = it.friends.filter { id -> id != friendUserId }) }
    }

    /** Create a new tournament and return its id. */
    fun createTournament(name: String, config: GameSettings): Tournament {
        val user = _currentUser.value ?: throw IllegalStateException("Not logged in")
        val tournament = Tournament(
            name = name,
            organizerId = user.id,
            participants = listOf(user.id),
            matches = emptyList(),
            config = config
        )
        _tournaments.update { current -> current + tournament }
        _currentUser.update { it?.copy(joinedTournaments = it.joinedTournaments + tournament.id) }
        return tournament
    }

    /** Join an existing tournament by id. */
    fun joinTournament(tournamentId: String): Boolean {
        val user = _currentUser.value ?: return false
        val tournament = _tournaments.value.find { it.id == tournamentId } ?: return false
        if (tournament.isStarted || tournament.participants.contains(user.id)) return false
        _tournaments.update { list ->
            list.map { t ->
                if (t.id == tournamentId) t.copy(participants = t.participants + user.id) else t
            }
        }
        _currentUser.update { it?.copy(joinedTournaments = it.joinedTournaments + tournamentId) }
        return true
    }

    /** Get tournaments the current user is participating in. */
    fun getMyTournaments(): List<Tournament> {
        val user = _currentUser.value ?: return emptyList()
        return _tournaments.value.filter { it.participants.contains(user.id) }
    }

    /** Get all open (not yet started) tournaments any user can join. */
    fun getOpenTournaments(): List<Tournament> {
        return _tournaments.value.filter { !it.isStarted && !it.isFinished }
    }

    private suspend fun syncWithCloud() {
        // Placeholder for cloud synchronization logic
    }

    private fun defaultAchievements(): List<Achievement> = listOf(
        Achievement("welcome", "Добро пожаловать!", "Открыть игру в первый раз.", AchievementCategory.GENERAL),
        Achievement("first_win", "Первая победа", "Собрать любую головоломку (независимо от режима).", AchievementCategory.GENERAL),
        Achievement("novice", "Новичок", "Собрать 10 партий в любых режимах.", AchievementCategory.GENERAL, maxProgress = 10),
        Achievement("experienced", "Опытный игрок", "Собрать 100 партий.", AchievementCategory.GENERAL, AchievementRarity.BRONZE, maxProgress = 100),
        Achievement("veteran", "Ветеран", "Собрать 500 партий.", AchievementCategory.GENERAL, AchievementRarity.SILVER, maxProgress = 500),
        Achievement("legend", "Легенда", "Собрать 1000 партий.", AchievementCategory.GENERAL, AchievementRarity.GOLD, maxProgress = 1000),
        Achievement("social", "Социальный игрок", "Добавить первого друга в игре.", AchievementCategory.GENERAL),
        Achievement("popular", "Популярный", "Получить 5 вызовов от друзей.", AchievementCategory.GENERAL, maxProgress = 5),
        Achievement("leader", "Лидер", "Попасть в топ-10 мирового рейтинга по любому размеру/режиму.", AchievementCategory.GENERAL, AchievementRarity.GOLD, isHidden = true),
        Achievement("no_pause", "Без паузы", "Собрать партию, не используя паузу (время не останавливалось).", AchievementCategory.GENERAL, AchievementRarity.SILVER),
        Achievement("mathematician", "Математик", "Собрать 4×4 за минимально возможное число ходов (для 4×4 — 16 ходов).", AchievementCategory.CLASSIC, AchievementRarity.SILVER),
        Achievement("counter", "Счётчик", "Сделать суммарно 1000 ходов во всех партиях в режиме «Цифры».", AchievementCategory.CLASSIC, maxProgress = 1000),
        Achievement("follower", "Последователь", "Собрать 3×3, 4×4 и 5×5 (по одному разу) без ошибок (без перезапусков).", AchievementCategory.CLASSIC, AchievementRarity.SILVER, maxProgress = 3),
        Achievement("no_returns", "Без возвратов", "Собрать партию, ни разу не переместив плитку обратно на предыдущее место.", AchievementCategory.CLASSIC, AchievementRarity.GOLD, isHidden = true),
        Achievement("classic_marathon", "Марафон цифр", "Собрать 50 партий в режиме «Цифры».", AchievementCategory.CLASSIC, maxProgress = 50),
        Achievement("fast_30", "Быстрее времени", "Собрать 4×4 за 30 секунд.", AchievementCategory.CLASSIC, AchievementRarity.BRONZE),
        Achievement("perfect_3x3", "Идеальный порядок", "Собрать поле 3×3 за 4 хода (минимально возможное).", AchievementCategory.CLASSIC, AchievementRarity.GOLD),
        Achievement("collector", "Коллекционер", "Собрать 10 разных картинок.", AchievementCategory.IMAGE, maxProgress = 10),
        Achievement("artist", "Художник", "Собрать 25 разных картинок.", AchievementCategory.IMAGE, AchievementRarity.SILVER, maxProgress = 25),
        Achievement("photographer", "Фотограф", "Загрузить свою картинку и собрать её.", AchievementCategory.IMAGE),
        Achievement("daily_7", "Ежедневник", "Собрать «Пазл дня» 7 дней подряд.", AchievementCategory.IMAGE, AchievementRarity.SILVER, maxProgress = 7),
        Achievement("puzzle_master", "Мастер пазлов", "Собрать 50 картинок из встроенной библиотеки.", AchievementCategory.IMAGE, AchievementRarity.GOLD, maxProgress = 50),
        Achievement("legendary_find", "Редкая находка", "Собрать картинку категории «Легендарная».", AchievementCategory.IMAGE, AchievementRarity.GOLD, isHidden = true),
        Achievement("gallerist", "Галерист", "Заполнить все слоты в галерее.", AchievementCategory.IMAGE, AchievementRarity.PLATINUM),
        Achievement("rainbow", "Радуга", "Собрать поле в порядке радуги.", AchievementCategory.COLORS),
        Achievement("monochrome", "Монохромный мастер", "Собрать градиент из 6 оттенков одного цвета.", AchievementCategory.COLORS, AchievementRarity.SILVER),
        Achievement("designer", "Дизайнер", "Создать и сохранить свою цветовую палитру, затем использовать её в 5 играх.", AchievementCategory.COLORS, maxProgress = 5),
        Achievement("chromatic", "Хроматик", "Собрать 50 партий в цветовом режиме.", AchievementCategory.COLORS, maxProgress = 50),
        Achievement("color_genius", "Цветовой гений", "Собрать 10 разных цветовых схем.", AchievementCategory.COLORS, AchievementRarity.SILVER, maxProgress = 10),
        Achievement("symbolist", "Символист", "Собрать 20 партий с символами.", AchievementCategory.COLORS, maxProgress = 20),
        Achievement("palette_today", "Палитра дня", "Использовать палитру, созданную в тот же день.", AchievementCategory.COLORS),
        Achievement("inventor", "Изобретатель", "Выполнить 10 разных типов вызовов.", AchievementCategory.CHALLENGE, AchievementRarity.SILVER, maxProgress = 10),
        Achievement("reverse_pro", "Обратный ход", "Выиграть в вызове «Обратный порядок» за меньшее число ходов, чем ваш рекорд.", AchievementCategory.CHALLENGE, AchievementRarity.GOLD),
        Achievement("spiral_pro", "Спиральный гений", "Собрать спираль за минимальное время для размера 4×4.", AchievementCategory.CHALLENGE, AchievementRarity.SILVER),
        Achievement("universal", "Универсал", "Выполнить по одному вызову каждого типа.", AchievementCategory.CHALLENGE, maxProgress = 5),
        Achievement("random_hero", "Случайный герой", "Выиграть партию в вызове, сгенерированном случайно.", AchievementCategory.CHALLENGE),
        Achievement("novice_challenges", "Новичок вызовов", "Выполнить 5 разных вызовов.", AchievementCategory.CHALLENGE, maxProgress = 5),
        Achievement("experienced_challenges", "Опытный игрок", "Выполнить 15 разных вызовов.", AchievementCategory.CHALLENGE, AchievementRarity.BRONZE, maxProgress = 15),
        Achievement("master_challenges", "Мастер вызовов", "Выполнить 30 разных вызовов.", AchievementCategory.CHALLENGE, AchievementRarity.SILVER, maxProgress = 30),
        Achievement("duelist", "Дуэлянт", "Провести 10 соревновательных партий (с друзьями).", AchievementCategory.COMPETITION, maxProgress = 10),
        Achievement("champion", "Чемпион", "Одержать 10 побед над друзьями.", AchievementCategory.COMPETITION, AchievementRarity.SILVER, maxProgress = 10),
        Achievement("prizewinner", "Призёр", "Занять 2-е место или выше в любом соревновании.", AchievementCategory.COMPETITION),
        Achievement("unbeatable", "Беспроигрышный", "Выиграть 5 соревнований подряд.", AchievementCategory.COMPETITION, AchievementRarity.GOLD, maxProgress = 5),
        Achievement("friendly_challenges", "Дружелюбный", "Принять 20 вызовов от друзей.", AchievementCategory.COMPETITION, maxProgress = 20),
        Achievement("duel_legend", "Легенда дуэлей", "Одержать 50 побед.", AchievementCategory.COMPETITION, AchievementRarity.GOLD, maxProgress = 50),
        Achievement("sprinter", "Спринтер", "Собрать 4×4 за 45 секунд с учётом временного лимита.", AchievementCategory.TIME_ATTACK, AchievementRarity.SILVER),
        Achievement("time_master", "Мастер времени", "Собрать 5 пазлов подряд в режиме Time Attack, не нарушив лимит.", AchievementCategory.TIME_ATTACK, maxProgress = 5),
        Achievement("survivor", "Выживший", "В режиме «На выживание» продержаться 10 уровней.", AchievementCategory.TIME_ATTACK, AchievementRarity.GOLD, maxProgress = 10),
        Achievement("wind_speed", "Быстрее ветра", "Установить личный рекорд времени в Time Attack для размера 5×5.", AchievementCategory.TIME_ATTACK, AchievementRarity.SILVER),
        Achievement("time_manager", "Тайм-менеджер", "В общей сложности провести в Time Attack 60 минут игрового времени.", AchievementCategory.TIME_ATTACK, maxProgress = 60),
        Achievement("survival_first", "Первый шаг", "Пройти 1 уровень в режиме выживания.", AchievementCategory.TIME_ATTACK),
        Achievement("survival_sprinter", "Спринтер (Выживание)", "Пройти 5 уровней подряд.", AchievementCategory.TIME_ATTACK, maxProgress = 5),
        Achievement("survival_master", "Мастер времени (Выживание)", "Пройти 10 уровней.", AchievementCategory.TIME_ATTACK, AchievementRarity.SILVER, maxProgress = 10),
        Achievement("survival_marathoner", "Марафонец (Выживание)", "Пройти 20 уровней.", AchievementCategory.TIME_ATTACK, AchievementRarity.GOLD, maxProgress = 20),
        Achievement("survival_endless", "Бесконечный бег", "Пройти 50 уровней.", AchievementCategory.TIME_ATTACK, AchievementRarity.PLATINUM, maxProgress = 50),
        Achievement("survival_lightning", "Быстрее молнии", "Пройти уровень за время, меньшее половины лимита.", AchievementCategory.TIME_ATTACK, AchievementRarity.GOLD),
        Achievement("survival_iron_nerve", "Железный нерв", "Пройти уровень, когда на таймере оставалось менее 3 секунд.", AchievementCategory.TIME_ATTACK, AchievementRarity.SILVER),
        Achievement("marathoner", "Марафонец", "Пройти 3 полных цикла (15 уровней).", AchievementCategory.ENDLESS, maxProgress = 15),
        Achievement("enduring", "Выносливый", "Пройти 25 уровней.", AchievementCategory.ENDLESS, AchievementRarity.BRONZE, maxProgress = 25),
        Achievement("iron_player", "Железный игрок", "Пройти 5 полных циклов.", AchievementCategory.ENDLESS, AchievementRarity.SILVER, maxProgress = 25),
        Achievement("endless_wanderer", "Бесконечный странник", "Пройти 10 полных циклов.", AchievementCategory.ENDLESS, AchievementRarity.GOLD, maxProgress = 50),
        Achievement("adaptation_master", "Мастер адаптации", "В бесконечном режиме собрать пазл на максимальной сложности (8x8).", AchievementCategory.ENDLESS, AchievementRarity.GOLD, isHidden = true),
        Achievement("first_steps", "Первые шаги", "Пройти первый уровень (4x4) в бесконечном режиме.", AchievementCategory.ENDLESS),
        Achievement("size_conqueror", "Покоритель размеров", "Пройти все размеры от 4x4 до 8x8 (один цикл).", AchievementCategory.ENDLESS),
        Achievement("no_losses", "Без потерь", "Пройти первый цикл, не потеряв ни одной жизни.", AchievementCategory.ENDLESS, AchievementRarity.SILVER),
        Achievement("speed_run", "Скоростной забег", "Пройти 5x5 за 30 секунд в бесконечном режиме.", AchievementCategory.ENDLESS, AchievementRarity.BRONZE),
        Achievement("difficulty_master", "Мастер сложности", "Пройти уровень 8x8 со сложностью «Сложная».", AchievementCategory.ENDLESS, AchievementRarity.SILVER),
        Achievement("record_breaker", "Рекордсмен", "Установить рекорд по количеству пройденных уровней среди друзей.", AchievementCategory.ENDLESS, AchievementRarity.SILVER),
        Achievement("preset_collector", "Коллекционер пресетов", "Сохранить 10 пресетов в режиме «Свои настройки».", AchievementCategory.GENERAL, maxProgress = 10),
        Achievement("chef", "Шеф-повар", "Создать 5 челленджей для друзей.", AchievementCategory.GENERAL, AchievementRarity.SILVER, maxProgress = 5),
        Achievement("popular_challenger", "Популярный челленджер", "Ваш челлендж приняли 10 разных игроков.", AchievementCategory.GENERAL, AchievementRarity.GOLD, maxProgress = 10),
        Achievement("experimenter", "Экспериментатор", "Сыграть 20 партий в режиме «Эксперимент».", AchievementCategory.GENERAL, maxProgress = 20),
        Achievement("daily_custom", "Ежедневный игрок", "Пройти «Пресет дня» 7 дней подряд.", AchievementCategory.GENERAL, AchievementRarity.SILVER, maxProgress = 7),
        Achievement("random_hero_custom", "Случайный герой", "Пройти 10 партий со «Случайным пресетом».", AchievementCategory.GENERAL, maxProgress = 10),
        Achievement("tournament_fighter", "Турнирный боец", "Участвовать в 10 турнирах.", AchievementCategory.COMPETITION, maxProgress = 10),
        Achievement("tournament_champ", "Чемпион турнира", "Выиграть турнир.", AchievementCategory.COMPETITION, AchievementRarity.GOLD),
        Achievement("ranked_serial", "Серийный победитель", "Выиграть 10 рейтинговых матчей подряд.", AchievementCategory.COMPETITION, AchievementRarity.SILVER, maxProgress = 10),
        Achievement("master_league", "Лига мастеров", "Достичь ранга «Алмаз» в рейтинговом режиме.", AchievementCategory.COMPETITION, AchievementRarity.GOLD),
        Achievement("team_spirit", "Командный дух", "Выиграть 50 командных матчей.", AchievementCategory.COMPETITION, AchievementRarity.SILVER, maxProgress = 50),
        Achievement("spectator_100", "Наблюдатель", "Посмотреть 100 дуэлей в режиме наблюдателя.", AchievementCategory.COMPETITION, maxProgress = 100),
        Achievement("bonus_king", "Бонусный король", "Активировать 200 бонусов.", AchievementCategory.COMPETITION, maxProgress = 200),
        Achievement("comp_first_duel", "Первый бой", "Провести первую дуэль (с другом или ИИ).", AchievementCategory.COMPETITION),
        Achievement("comp_ai_easy", "Победитель ИИ (лёгкий)", "Одержать первую победу над ИИ на лёгком уровне.", AchievementCategory.COMPETITION),
        Achievement("comp_ai_medium", "Победитель ИИ (средний)", "Одержать первую победу над ИИ на среднем уровне.", AchievementCategory.COMPETITION, AchievementRarity.BRONZE),
        Achievement("comp_ai_hard", "Победитель ИИ (сложный)", "Одержать первую победу над ИИ на сложном уровне.", AchievementCategory.COMPETITION, AchievementRarity.SILVER),
        Achievement("comp_ai_expert", "Победитель ИИ (эксперт)", "Одержать первую победу над ИИ на экспертном уровне.", AchievementCategory.COMPETITION, AchievementRarity.GOLD),
        Achievement("comp_faster", "Быстрее противника", "Победить в дуэли, затратив меньше времени.", AchievementCategory.COMPETITION),
        Achievement("comp_efficient", "Эффективнее противника", "Победить в дуэли, затратив меньше ходов.", AchievementCategory.COMPETITION)
    )

    // ---- Profile editing & password reset ----

    suspend fun updateProfile(newNickname: String, newEmail: String): ProfileUpdateResult {
        val trimmedNick = newNickname.trim()
        val trimmedEmail = newEmail.trim()

        if (trimmedNick.isBlank() || trimmedEmail.isBlank())
            return ProfileUpdateResult.Error("Заполните все поля")
        if (!isValidNickname(trimmedNick))
            return ProfileUpdateResult.Error("Никнейм должен содержать не менее 2 символов")
        if (!isValidEmail(trimmedEmail))
            return ProfileUpdateResult.Error("Введите корректный email (должен содержать @)")

        val current = _currentUser.value ?: return ProfileUpdateResult.Error("Не авторизован")

        // If email changed, make sure it's not taken by someone else
        if (trimmedEmail.lowercase() != current.email?.lowercase()) {
            if (mockUserDb.containsKey(trimmedEmail.lowercase()))
                return ProfileUpdateResult.Error("Этот email уже занят")
            // Re-register under new email key
            val entry = mockUserDb.remove(current.email?.lowercase() ?: "")
            if (entry != null) {
                mockUserDb[trimmedEmail.lowercase()] = Triple(
                    entry.first,
                    trimmedNick,
                    current.copy(nickname = trimmedNick, email = trimmedEmail)
                )
            }
        } else if (trimmedNick != current.nickname) {
            // Only nickname changed – update db entry
            val entry = mockUserDb[trimmedEmail.lowercase()]
            if (entry != null) {
                mockUserDb[trimmedEmail.lowercase()] = Triple(
                    entry.first,
                    trimmedNick,
                    entry.third.copy(nickname = trimmedNick)
                )
            }
        }

        _currentUser.value = current.copy(nickname = trimmedNick, email = trimmedEmail)
        persistAccounts()
        return ProfileUpdateResult.Success
    }

    /**
     * Updates the current user's avatar (profile photo). The [uri] can come either
     * from the system photo picker, the camera (item 17 — "Работа с камерой"), or any
     * gallery URI. Persists immediately so the new photo survives app restarts.
     */
    suspend fun updateAvatar(uri: String): ProfileUpdateResult {
        val current = _currentUser.value ?: return ProfileUpdateResult.Error("Не авторизован")
        _currentUser.value = current.copy(avatarUrl = uri)
        val emailKey = current.email?.lowercase()
        if (emailKey != null) {
            val entry = mockUserDb[emailKey]
            if (entry != null) {
                mockUserDb[emailKey] = Triple(entry.first, entry.second, entry.third.copy(avatarUrl = uri))
            }
        }
        persistAccounts()
        return ProfileUpdateResult.Success
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): ProfileUpdateResult {
        if (currentPassword.isBlank() || newPassword.isBlank())
            return ProfileUpdateResult.Error("Заполните все поля")
        if (!isValidPassword(newPassword))
            return ProfileUpdateResult.Error("Новый пароль должен содержать не менее 6 символов")

        val current = _currentUser.value ?: return ProfileUpdateResult.Error("Не авторизован")
        val emailKey = current.email?.lowercase() ?: return ProfileUpdateResult.Error("Нет email")
        val entry = mockUserDb[emailKey] ?: return ProfileUpdateResult.Error("Аккаунт не найден")

        if (entry.first != currentPassword)
            return ProfileUpdateResult.Error("Неверный текущий пароль")
        if (currentPassword == newPassword)
            return ProfileUpdateResult.Error("Новый пароль должен отличаться от текущего")

        mockUserDb[emailKey] = Triple(newPassword, entry.second, entry.third)
        persistAccounts()
        return ProfileUpdateResult.Success
    }

    /** Simulates "forgot password" – in production would send a reset email. */
    fun requestPasswordReset(email: String): ForgotPasswordResult {
        val trimmed = email.trim()
        if (!isValidEmail(trimmed))
            return ForgotPasswordResult.InvalidEmail
        // We deliberately don't reveal whether the account exists (security best practice)
        return ForgotPasswordResult.EmailSent
    }

    suspend fun autoLogin() {
        ensureAccountsLoaded()
        val savedEmail = context.dataStore.data.map { it[SESSION_EMAIL_KEY] }.first()
        if (savedEmail != null) {
            val entry = mockUserDb[savedEmail]
            if (entry != null) {
                val user = entry.third
                _currentUser.value = user
                _userStats.value = user.stats
                _achievements.value = user.achievements
                registeredUserNicknames[user.id] = user.nickname
            }
        }
    }
}
