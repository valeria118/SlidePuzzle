package com.example.a1234567889.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private data class CalcResult(
    val efficiencyPercent: Int,
    val timeBonus: Int,
    val totalScore: Int,
    val stars: Int,
    val isNewRecord: Boolean,
    val breakdown: String
)

/**
 * Lab item 2 — "Использование арифметических или логических операций".
 *
 * A standalone "match-rating calculator": given a grid size, a move count and a
 * finish time, it derives an efficiency percentage, a time bonus, a total score and
 * a 1–3 star rating — explicitly using arithmetic ( * / + - % ) and logical
 * ( && || ! comparisons ) operators, all shown to the user as a worked formula.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreCalculatorScreen(onNavigateBack: () -> Unit) {
    var sizeText by remember { mutableStateOf("4") }
    var movesText by remember { mutableStateOf("60") }
    var timeText by remember { mutableStateOf("90") }
    var bestScoreText by remember { mutableStateOf("0") }
    var result by remember { mutableStateOf<CalcResult?>(null) }

    fun calculate() {
        val size = sizeText.toIntOrNull()?.coerceIn(2, 8) ?: 4
        val moves = movesText.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val timeSeconds = timeText.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val bestScore = bestScoreText.toIntOrNull() ?: 0

        // ---- Arithmetic operations: *, /, -, %, + ----
        val optimalMoves = size * size * size                      // multiplication
        val efficiencyRaw = (optimalMoves * 100) / moves            // multiplication + integer division
        val efficiencyPercent = efficiencyRaw.coerceAtMost(100)
        val timeBonus = ((300 - timeSeconds) / 10).coerceAtLeast(0) // subtraction + division
        val movesParity = moves % 2                                 // modulo, used for a small bonus rule below
        val parityBonus = if (movesParity == 0) 2 else 0
        val totalScore = efficiencyPercent + timeBonus + parityBonus // addition

        // ---- Logical operations: &&, ||, !, comparisons ----
        val isFast = timeSeconds <= 90
        val isEfficient = efficiencyPercent >= 80
        val threeStars = isEfficient && isFast                       // AND
        val twoStars = !threeStars && (isEfficient || isFast)        // NOT, OR, AND
        val oneStarOrMore = totalScore > 0
        val stars = when {
            threeStars -> 3
            twoStars -> 2
            oneStarOrMore -> 1
            else -> 0
        }
        val isNewRecord = totalScore > bestScore && bestScore >= 0   // comparison + AND

        result = CalcResult(
            efficiencyPercent = efficiencyPercent,
            timeBonus = timeBonus,
            totalScore = totalScore,
            stars = stars,
            isNewRecord = isNewRecord,
            breakdown = "Эффективность = min(100, $size³×100 / $moves) = $efficiencyPercent%\n" +
                "Бонус за время = max(0, (300 − $timeSeconds) / 10) = $timeBonus\n" +
                "Бонус за чётность ходов ($moves % 2 == 0) = $parityBonus\n" +
                "Итог = $efficiencyPercent + $timeBonus + $parityBonus = $totalScore"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Калькулятор результата") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Рассчитайте свою оценку за партию: введите параметры и нажмите «Рассчитать».",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = sizeText,
                onValueChange = { sizeText = it.filter { c -> c.isDigit() } },
                label = { Text("Размер поля (2–8)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = movesText,
                onValueChange = { movesText = it.filter { c -> c.isDigit() } },
                label = { Text("Количество ходов") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = timeText,
                onValueChange = { timeText = it.filter { c -> c.isDigit() } },
                label = { Text("Время (секунд)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = bestScoreText,
                onValueChange = { bestScoreText = it.filter { c -> c.isDigit() } },
                label = { Text("Ваш предыдущий лучший результат") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Button(onClick = { calculate() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Calculate, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Рассчитать")
            }

            Spacer(Modifier.height(20.dp))

            result?.let { r ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(3) { i ->
                                Icon(
                                    if (i < r.stars) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = if (i < r.stars) Color(0xFFFFC107) else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text("Итог: ${r.totalScore}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        if (r.isNewRecord) {
                            Spacer(Modifier.height(8.dp))
                            Text("🎉 Новый личный рекорд!", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(r.breakdown, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
