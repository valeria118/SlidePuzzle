package com.example.a1234567889.logic

import androidx.compose.ui.graphics.Color
import com.example.a1234567889.models.CardRank
import kotlin.random.Random

/**
 * Decides which card rank (and which card art) a player receives for completing
 * a built-in "Картинка" puzzle. The bigger the board, the better the odds of a
 * higher rank — 3x3 always gives Bronze, while 8x8 mostly gives Gold.
 */
object CardRewardLogic {

    // Probability table per board size. Each list sums to 1.0.
    // 3x3 - Бронза 100%; 4x4 - бронза 70% / серебро 30%; 5x5 - бронза 50% / серебро 50%;
    // 6x6 - бронза 40% / серебро 60%; 7x7 - бронза 48% / серебро 48% / золото 4%;
    // 8x8 - бронза 40% / серебро 40% / золото 20%.
    private val distribution: Map<Int, List<Pair<CardRank, Float>>> = mapOf(
        3 to listOf(CardRank.BRONZE to 1.00f),
        4 to listOf(CardRank.BRONZE to 0.70f, CardRank.SILVER to 0.30f),
        5 to listOf(CardRank.BRONZE to 0.50f, CardRank.SILVER to 0.50f),
        6 to listOf(CardRank.BRONZE to 0.40f, CardRank.SILVER to 0.60f),
        7 to listOf(CardRank.BRONZE to 0.48f, CardRank.SILVER to 0.48f, CardRank.GOLD to 0.04f),
        8 to listOf(CardRank.BRONZE to 0.40f, CardRank.SILVER to 0.40f, CardRank.GOLD to 0.20f)
    )

    // Mapping of ranks to their specific card photos, bundled as flat drawable
    // resources (res/drawable-nodpi/<name>.jpg), sourced from kart/<rank>/.
    private val cardPool: Map<CardRank, List<String>> = mapOf(
        CardRank.BRONZE to listOf("a2", "b1", "b5", "b6", "c1", "img_1896"),
        CardRank.SILVER to listOf("c4", "c6", "c7", "c9", "d3", "d5"),
        CardRank.GOLD to listOf("a1", "a7", "b3", "b4", "d4", "img_1897")
    )

    fun oddsFor(boardSize: Int): List<Pair<CardRank, Float>> =
        distribution[boardSize] ?: distribution.entries.minBy { kotlin.math.abs(it.key - boardSize) }.value

    fun rollRank(boardSize: Int, random: Random = Random.Default): CardRank {
        val table = oddsFor(boardSize)
        val roll = random.nextFloat()
        var acc = 0f
        for ((rank, p) in table) {
            acc += p
            if (roll < acc) return rank
        }
        return table.last().first
    }

    fun randomCardPhoto(rank: CardRank, random: Random = Random.Default): String {
        val pool = cardPool[rank] ?: return "a2" // fallback
        val name = pool.random(random)
        return name
    }

    fun frameRes(rank: CardRank): String = when (rank) {
        CardRank.BRONZE -> "bronz"
        CardRank.SILVER -> "sereb"
        CardRank.GOLD -> "alm_zol"
    }

    fun rankLabel(rank: CardRank): String = when (rank) {
        CardRank.BRONZE -> "Бронзовая"
        CardRank.SILVER -> "Серебряная"
        CardRank.GOLD -> "Золотая"
    }

    fun rankColor(rank: CardRank): Color = when (rank) {
        CardRank.BRONZE -> Color(0xFFCD7F32)
        CardRank.SILVER -> Color(0xFFC0C0C0)
        CardRank.GOLD -> Color(0xFFFFD700)
    }
}
