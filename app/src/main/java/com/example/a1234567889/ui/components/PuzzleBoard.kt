package com.example.a1234567889.ui.components

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.a1234567889.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PuzzleBoard(
    board: Board,
    onTileClick: (Position) -> Unit,
    settings: GameSettings,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    val boardBgColor = when (settings.mode) {
        GameMode.CHALLENGE -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        GameMode.TIME_ATTACK -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        GameMode.ENDLESS -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
        GameMode.COMPETITION -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    // Load the full bitmap once at board level for IMAGE mode
    val context = LocalContext.current
    var fullBitmap by remember(settings.imageUri) { mutableStateOf<ImageBitmap?>(null) }

    if (settings.visualMode == GameMode.IMAGE && settings.imageUri != null) {
        LaunchedEffect(settings.imageUri) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = ImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(settings.imageUri)
                        .allowHardware(false)
                        .build()
                    val result = loader.execute(request)
                    if (result is SuccessResult) {
                        val bmp = (result.drawable as? BitmapDrawable)?.bitmap
                        if (bmp != null) fullBitmap = bmp.asImageBitmap()
                    }
                } catch (_: Exception) { }
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .padding(16.dp)
            .background(boardBgColor, RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        val size = board.size
        val tileSize = maxWidth / size

        for (r in 0 until size) {
            for (c in 0 until size) {
                val tile = board.tiles[r][c]
                if (!tile.isEmpty) {
                    TileView(
                        tile = tile,
                        row = r,
                        col = c,
                        gridSize = size,
                        tileSize = tileSize,
                        settings = settings,
                        isPaused = isPaused,
                        fullBitmap = fullBitmap,
                        onClick = { onTileClick(Position(r, c)) }
                    )
                } else if (settings.visualMode == GameMode.CHALLENGE) {
                    EmptyTileView(
                        row = r,
                        col = c,
                        tileSize = tileSize
                    )
                }
            }
        }
        
        if (isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PAUSED",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TileView(
    tile: Tile,
    row: Int,
    col: Int,
    gridSize: Int,
    tileSize: androidx.compose.ui.unit.Dp,
    settings: GameSettings,
    isPaused: Boolean,
    fullBitmap: ImageBitmap?,
    onClick: () -> Unit
) {
    val animatedX by animateDpAsState(
        targetValue = tileSize * col,
        animationSpec = tween(durationMillis = 150),
        label = "tileX"
    )
    val animatedY by animateDpAsState(
        targetValue = tileSize * row,
        animationSpec = tween(durationMillis = 150),
        label = "tileY"
    )

    val visualMode = settings.visualMode

    val tileColor = if (visualMode == GameMode.COLORS && tile.color != null) {
        tile.color
    } else {
        when (settings.mode) {
            GameMode.CHALLENGE -> MaterialTheme.colorScheme.primary
            GameMode.TIME_ATTACK -> Color(0xFFE53935)
            GameMode.ENDLESS -> Color(0xFF00897B)
            GameMode.COMPETITION -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.primary
        }
    }

    val cornerRadius = when (settings.mode) {
        GameMode.CHALLENGE -> 16.dp
        GameMode.ENDLESS -> 4.dp
        else -> 8.dp
    }

    val isChallenge = settings.mode == GameMode.CHALLENGE
    val labelText = if (isChallenge) toRoman(tile.number) else tile.number.toString()
    
    // Scale font based on string length to ensure fit
    val lengthFactor = if (labelText.length > 2) {
        if (labelText.length > 4) 0.6f else 0.8f
    } else 1.0f
    
    val baseFontSize = (tileSize.value * 0.35f * lengthFactor).sp
    val textStyle = MaterialTheme.typography.labelLarge.copy(
        fontWeight = FontWeight.Bold,
        fontSize = baseFontSize
    )

    Box(
        modifier = Modifier
            .size(tileSize)
            .offset(x = animatedX, y = animatedY)
            .padding(4.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(tileColor)
            .then(
                if (visualMode == GameMode.IMAGE && fullBitmap != null) {
                    val originalIdx = tile.id - 1
                    val originalRow = originalIdx / gridSize
                    val originalCol = originalIdx % gridSize
                    Modifier.drawBehind {
                        val sliceW = fullBitmap.width.toFloat() / gridSize
                        val sliceH = fullBitmap.height.toFloat() / gridSize
                        val srcLeft = (originalCol * sliceW).toInt()
                        val srcTop  = (originalRow * sliceH).toInt()
                        val srcRight  = (srcLeft + sliceW).toInt().coerceAtMost(fullBitmap.width)
                        val srcBottom = (srcTop  + sliceH).toInt().coerceAtMost(fullBitmap.height)
                        drawIntoCanvas { canvas ->
                            canvas.drawImageRect(
                                image     = fullBitmap,
                                srcOffset = IntOffset(srcLeft, srcTop),
                                srcSize   = IntSize(srcRight - srcLeft, srcBottom - srcTop),
                                dstOffset = IntOffset(0, 0),
                                dstSize   = IntSize(size.width.toInt(), size.height.toInt()),
                                paint     = Paint()
                            )
                        }
                    }
                } else if (visualMode == GameMode.COLORS && settings.isColorBlindMode) {
                    Modifier.drawBehind {
                        val color = Color.White.copy(alpha = 0.6f)
                        val strokeWidth = 2.dp.toPx()
                        
                        // Use ID to determine texture, ensuring variety
                        when (tile.id % 8) {
                            0 -> { // Vertical lines
                                for (i in 1..3) drawLine(color, Offset(i * size.width / 4, 0f), Offset(i * size.width / 4, size.height), strokeWidth)
                            }
                            1 -> { // Horizontal lines
                                for (i in 1..3) drawLine(color, Offset(0f, i * size.height / 4), Offset(size.width, i * size.height / 4), strokeWidth)
                            }
                            2 -> { // Diagonal \
                                drawLine(color, Offset(0f, 0f), Offset(size.width, size.height), strokeWidth)
                                drawLine(color, Offset(size.width/2f, 0f), Offset(size.width, size.height/2f), strokeWidth)
                                drawLine(color, Offset(0f, size.height/2f), Offset(size.width/2f, size.height), strokeWidth)
                            }
                            3 -> { // Diagonal /
                                drawLine(color, Offset(size.width, 0f), Offset(0f, size.height), strokeWidth)
                                drawLine(color, Offset(size.width/2f, 0f), Offset(0f, size.height/2f), strokeWidth)
                                drawLine(color, Offset(size.width, size.height/2f), Offset(size.width/2f, size.height), strokeWidth)
                            }
                            4 -> { // Cross
                                drawLine(color, Offset(size.width/2f, 0f), Offset(size.width/2f, size.height), strokeWidth)
                                drawLine(color, Offset(0f, size.height/2f), Offset(size.width, size.height/2f), strokeWidth)
                            }
                            5 -> { // Circle
                                drawCircle(color, size.minDimension / 4f, style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth))
                            }
                            6 -> { // Dots grid
                                val step = size.width / 4
                                for (i in 1..3) for (j in 1..3) drawCircle(color, strokeWidth * 1.5f, Offset(i * step, j * step))
                            }
                            7 -> { // Square
                                drawRect(color, Offset(size.width/4, size.height/4), androidx.compose.ui.geometry.Size(size.width/2, size.height/2), style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth))
                            }
                        }
                    }
                } else Modifier
            )
            .clickable(enabled = !isPaused) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (visualMode == GameMode.NUMBERS || (visualMode == GameMode.COLORS && settings.showColorLabels) || isChallenge || (visualMode == GameMode.IMAGE && settings.showHints)) {
            val textColor = if (visualMode == GameMode.IMAGE) {
                Color.White.copy(alpha = 0.8f)
            } else if (visualMode == GameMode.COLORS) {
                Color.White
            } else {
                MaterialTheme.colorScheme.onPrimary
            }
            
            val shadowColor = if (visualMode == GameMode.IMAGE) Color.Black else Color.Transparent

            Text(
                text = labelText,
                color = textColor,
                style = textStyle.copy(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = shadowColor,
                        offset = Offset(2f, 2f),
                        blurRadius = 4f
                    )
                ),
                softWrap = false,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

private fun toRoman(number: Int): String {
    if (number <= 0) return ""
    val map = linkedMapOf(
        100 to "C", 90 to "XC", 50 to "L", 40 to "XL",
        10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I"
    )
    var num = number
    val res = StringBuilder()
    for ((value, roman) in map) {
        while (num >= value) {
            res.append(roman)
            num -= value
        }
    }
    return res.toString()
}

@Composable
fun EmptyTileView(
    row: Int,
    col: Int,
    tileSize: androidx.compose.ui.unit.Dp
) {
    val animatedX by animateDpAsState(
        targetValue = tileSize * col,
        animationSpec = tween(durationMillis = 150),
        label = "emptyTileX"
    )
    val animatedY by animateDpAsState(
        targetValue = tileSize * row,
        animationSpec = tween(durationMillis = 150),
        label = "emptyTileY"
    )

    Box(
        modifier = Modifier
            .size(tileSize)
            .offset(x = animatedX, y = animatedY)
            .padding(4.dp)
            .drawBehind {
                val dotRadius = 2.dp.toPx()
                val spacing = size.width / 4
                val color = Color.Gray.copy(alpha = 0.5f)
                
                for (i in 1..3) {
                    for (j in 1..3) {
                        drawCircle(
                            color = color,
                            radius = dotRadius,
                            center = Offset(i * spacing, j * spacing)
                        )
                    }
                }
            }
    )
}
