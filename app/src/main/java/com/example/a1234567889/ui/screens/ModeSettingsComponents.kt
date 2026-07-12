package com.example.a1234567889.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.a1234567889.models.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SizeSelector(
    selectedSize: Int,
    onSizeSelected: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Размер поля:", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (3..5).forEach { size ->
                FilterChip(
                    selected = selectedSize == size,
                    onClick = { onSizeSelected(size) },
                    label = { Text("${size}×${size}") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (6..8).forEach { size ->
                FilterChip(
                    selected = selectedSize == size,
                    onClick = { onSizeSelected(size) },
                    label = { Text("${size}×${size}") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DifficultySelector(
    selectedDifficulty: ChallengeDifficulty,
    onDifficultySelected: (ChallengeDifficulty) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Сложность:", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChallengeDifficulty.entries.forEach { difficulty ->
                FilterChip(
                    selected = selectedDifficulty == difficulty,
                    onClick = { onDifficultySelected(difficulty) },
                    label = {
                        Text(when(difficulty) {
                            ChallengeDifficulty.EASY -> "Легко"
                            ChallengeDifficulty.MEDIUM -> "Средне"
                            ChallengeDifficulty.HARD -> "Сложно"
                            ChallengeDifficulty.EXPERT -> "Эксперт"
                        })
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ImageSelector(
    gallery: List<ImageGalleryItem>,
    selectedUri: String?,
    onImageSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Выбор картинки:", style = MaterialTheme.typography.labelLarge)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(gallery) { item ->
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = if (selectedUri == item.uri) 3.dp else 0.dp,
                            color = if (selectedUri == item.uri) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onImageSelected(item.uri) }
                ) {
                    AsyncImage(
                        model = item.uri,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun PaletteSelector(
    palettes: List<ColorPalette>,
    selectedPaletteId: String?,
    onPaletteSelected: (ColorPalette) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Выбор палитры:", style = MaterialTheme.typography.labelLarge)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(palettes) { palette ->
                Column(
                    modifier = Modifier
                        .width(100.dp)
                        .clickable { onPaletteSelected(palette) }
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(
                                width = if (selectedPaletteId == palette.id) 2.dp else 0.dp,
                                color = if (selectedPaletteId == palette.id) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            )
                    ) {
                        palette.colors.take(4).forEach { color ->
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(color))
                        }
                    }
                    Text(palette.name, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }
        }
    }
}
