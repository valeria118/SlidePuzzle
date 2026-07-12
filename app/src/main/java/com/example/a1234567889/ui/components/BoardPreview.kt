package com.example.a1234567889.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.a1234567889.logic.GameEngine
import com.example.a1234567889.models.*

@Composable
fun BoardPreview(
    settings: GameSettings,
    modifier: Modifier = Modifier
) {
    val actualMode = settings.visualMode
    
    val targetBoard = remember(settings) {
        GameEngine.createTargetBoard(
            settings.size, 
            settings.challenge, 
            actualMode,
            settings.paletteType,
            settings.colorPattern,
            settings.customPaletteColors
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        val tileSize = maxWidth / settings.size
        
        Column {
            for (r in 0 until settings.size) {
                Row {
                    for (c in 0 until settings.size) {
                        val tile = targetBoard.tiles[r][c]
                        
                        // Apply preview-time transformations
                        val transformedTile = remember(tile, settings) {
                            var t = tile
                            val currentColor = t.color
                            if (!t.isEmpty && currentColor != null && actualMode == GameMode.COLORS) {
                                var workingColor = currentColor
                                
                                // 1. Color Blind Mode (Simulated)
                                if (settings.isColorBlindMode) {
                                    workingColor = workingColor.toColorBlind()
                                }
                                
                                // 2. Chromatic Adaptation (Simulated lighting shift)
                                if (settings.isChromaticAdaptation) {
                                    workingColor = workingColor.applySimulatedAdaptation(settings.adaptationIntensity)
                                }
                                
                                t = t.copy(color = workingColor)
                            }
                            t
                        }

                        PreviewTile(
                            tile = transformedTile,
                            mode = actualMode,
                            imageUri = settings.imageUri,
                            size = tileSize,
                            gridSize = settings.size,
                            row = r,
                            col = c,
                            showLabels = actualMode == GameMode.COLORS && settings.showColorLabels
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewTile(
    tile: Tile,
    mode: GameMode,
    imageUri: String?,
    size: androidx.compose.ui.unit.Dp,
    gridSize: Int,
    row: Int,
    col: Int,
    showLabels: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(size)
            .padding(1.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(
                if (tile.isEmpty) Color.Transparent 
                else tile.color ?: MaterialTheme.colorScheme.primaryContainer
            )
            .then(
                if (!tile.isEmpty && mode != GameMode.IMAGE) 
                    Modifier.border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!tile.isEmpty) {
            when {
                mode == GameMode.NUMBERS || mode == GameMode.CUSTOM || mode == GameMode.CHALLENGE || mode == GameMode.TIME_ATTACK || mode == GameMode.ENDLESS || showLabels -> {
                    Text(
                        text = tile.number.toString(),
                        fontSize = (size.value * 0.4f).sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showLabels) {
                            // High contrast label for color mode
                            if ((tile.color?.luminance() ?: 0f) > 0.5f) Color.Black else Color.White
                        } else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                mode == GameMode.IMAGE -> {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            alignment = BiasAlignment(
                                horizontalBias = -1f + (col.toFloat() / (gridSize - 1)) * 2f,
                                verticalBias = -1f + (row.toFloat() / (gridSize - 1)) * 2f
                            )
                        )
                    }
                }
                else -> {}
            }
        }
    }
}

// Helper functions for preview transformations

private fun Color.toColorBlind(): Color {
    // Simple desaturation/shift to simulate accessibility view
    val gray = red * 0.299f + green * 0.587f + blue * 0.114f
    return Color(
        red = (red + gray) / 2f,
        green = (green + gray) / 2f,
        blue = gray, // Blue reduction is common in simulations
        alpha = alpha
    )
}

private fun Color.applySimulatedAdaptation(intensity: Float): Color {
    // Simulate a slight yellow/blue tint based on "lighting"
    return Color(
        red = (red + intensity * 0.1f).coerceIn(0f, 1f),
        green = green,
        blue = (blue - intensity * 0.1f).coerceIn(0f, 1f),
        alpha = alpha
    )
}

private fun Color.luminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}
