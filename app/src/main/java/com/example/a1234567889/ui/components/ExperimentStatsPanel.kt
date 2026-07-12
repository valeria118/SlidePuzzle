package com.example.a1234567889.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExperimentStatsPanel(
    inversions: Int,
    optimalMoves: Int,
    avgTime: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Analytics, "Experiment Stats", tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(8.dp))
                Text("Аналитика", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ExperimentStatItem("Инверсии", inversions.toString())
                ExperimentStatItem("Мин. ходов", optimalMoves.toString())
                ExperimentStatItem("Ср. время", "%.2fs".format(avgTime))
            }
        }
    }
}

@Composable
private fun ExperimentStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
        Text(value, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 14.sp)
    }
}
