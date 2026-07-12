package com.example.a1234567889.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.a1234567889.R
import com.example.a1234567889.logic.CardRewardLogic
import com.example.a1234567889.logic.PictureProvider
import com.example.a1234567889.models.*

// ─────────────────────────────────────────────────────────────────────────────
// Main Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PictureSelectionScreen(
    user: User?,
    currentBoardSize: Int = 0, // If > 0, size is fixed
    selectedImageUri: String? = null,
    onImageSelected: (ImageGalleryItem, Boolean) -> Unit, // Boolean: true to start game immediately
    onUploadOwn: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    // If currentBoardSize is provided (from ModeSettingsDialog), we lock it.
    // Otherwise (if 0), we default to 3 but allow changing (if ever used that way).
    val isSizeFixed = currentBoardSize > 0
    var selectedSize by remember { mutableIntStateOf(if (isSizeFixed) currentBoardSize else 3) }
    var locallySelectedUri by remember { mutableStateOf(selectedImageUri) }

    val userGallery = user?.gallery ?: emptyList()

    // Built-in images for the selected board size, merged with user progress
    val builtInForSize = PictureProvider.imagesForSize(selectedSize).map { base ->
        userGallery.find { it.id == base.id } ?: base
    }
    
    // User-uploaded/custom images
    val customImages = userGallery.filter { it.isCustom }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Выбор картинки", style = MaterialTheme.typography.titleLarge)
                        if (isSizeFixed) {
                            Text("Для поля $selectedSize×$selectedSize", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        bottomBar = {
            if (locallySelectedUri != null) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val item = (builtInForSize + customImages).find { it.uri == locallySelectedUri }
                                if (item != null) {
                                    onImageSelected(item, false)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Выбрать")
                        }
                        Button(
                            onClick = {
                                val item = (builtInForSize + customImages).find { it.uri == locallySelectedUri }
                                if (item != null) {
                                    onImageSelected(item, true)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(8.dp))
                            Text("В игру")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Show size picker only if size isn't locked by the previous screen
            if (!isSizeFixed) {
                BoardSizePicker(
                    selectedSize = selectedSize,
                ) { selectedSize = it }
            }

            // Card odds info for selected size
            CardOddsInfo(boardSize = selectedSize)

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // First, the upload card
                item {
                    UploadNewCard(onClick = onUploadOwn)
                }
                
                // Then user's custom images
                items(customImages) { item ->
                    val isSelected = item.uri == locallySelectedUri
                    UserPictureCard(
                        item = item,
                        isSelected = isSelected,
                        onClick = { locallySelectedUri = item.uri }
                    )
                }

                // Finally built-in images
                items(builtInForSize) { item ->
                    val isSelected = item.uri == locallySelectedUri
                    BuiltInPictureCard(
                        item = item,
                        isSelected = isSelected,
                        onClick = { locallySelectedUri = item.uri }
                    )
                }
            }
        }
    }
}



// ─────────────────────────────────────────────────────────────────────────────
// Board-size selector (3×3 … 8×8)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BoardSizePicker(
    selectedSize: Int,
    onSizeSelected: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text("Размер поля", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items((3..8).toList()) { size ->
                val selected = size == selectedSize
                Surface(
                    modifier = Modifier.clickable { onSizeSelected(size) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = if (selected) 0.dp else 1.dp,
                    shadowElevation = if (selected) 4.dp else 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$size×$size",
                            fontWeight = FontWeight.Bold,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Card-odds hint strip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CardOddsInfo(boardSize: Int) {
    val odds = CardRewardLogic.oddsFor(boardSize)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Default.CardGiftcard, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Карточки:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        odds.forEach { (rank, prob) ->
            val (color, label) = when (rank) {
                CardRank.BRONZE -> Color(0xFFCD7F32) to "Бронза"
                CardRank.SILVER -> Color(0xFFC0C0C0) to "Серебро"
                CardRank.GOLD   -> Color(0xFFFFD700) to "Золото"
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = color.copy(alpha = 0.18f),
                border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "$label ${(prob * 100).toInt()}%",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Built-in puzzle card (from pazly/…)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BuiltInPictureCard(item: ImageGalleryItem, isSelected: Boolean = false, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.78f)
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                else Modifier
            ),
        elevation = CardDefaults.cardElevation(if (isSelected) 8.dp else 4.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Box {
            Image(
                painter = rememberAsyncImagePainter(item.uri),
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Selected checkmark overlay
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp),
                        shadowElevation = 4.dp
                    ) {
                        Icon(
                            Icons.Default.Check, 
                            null, 
                            tint = MaterialTheme.colorScheme.onPrimary, 
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            // Dark gradient at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.42f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                        )
                    )
            )

            // Best card rank badge (top-start)
            item.bestCardRank?.let { rank ->
                CardRankBadge(rank = rank, modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
            }

            // "New" chip if never solved
            if (!item.isCollected && (item.bestCardRank == null)) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                ) {
                    Text("NEW", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // Solved checkmark
            if (item.isCollected) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }

            // Bottom info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                if (item.bestTime != null) {
                    Text(
                        "Рекорд: ${formatTime(item.bestTime)}",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// User-uploaded picture card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun UserPictureCard(item: ImageGalleryItem, isSelected: Boolean = false, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.78f)
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                else Modifier
            ),
        elevation = CardDefaults.cardElevation(if (isSelected) 8.dp else 4.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Box {
            Image(
                painter = rememberAsyncImagePainter(item.uri),
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Selected checkmark overlay
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp),
                        shadowElevation = 4.dp
                    ) {
                        Icon(
                            Icons.Default.Check, 
                            null, 
                            tint = MaterialTheme.colorScheme.onPrimary, 
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.38f)
                    .background(
                        Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.76f)))
                    )
            )
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Моя", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            if (item.bestTime != null) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                if (item.bestTime != null) {
                    Text("Рекорд: ${formatTime(item.bestTime)}", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
                } else {
                    Text("Ещё не решена", color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Upload-new placeholder card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun UploadNewCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.78f)
            .clickable { onClick() }
            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Загрузить свою", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                Text("из галереи", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun EmptyUploadsPlaceholder(onUploadOwn: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
            Text("Здесь пока пусто", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Загрузите своё изображение из галереи\n и пройдите его как пазл", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Button(onClick = onUploadOwn) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Загрузить картинку")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Rank badge  (shows best card rank earned for a picture)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CardRankBadge(rank: CardRank, modifier: Modifier = Modifier) {
    val (bgColor, label) = when (rank) {
        CardRank.GOLD   -> Color(0xFFFFD700) to "🏆 Золото"
        CardRank.SILVER -> Color(0xFFC0C0C0) to "⬡ Серебро"
        CardRank.BRONZE -> Color(0xFFCD7F32) to "◆ Бронза"
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        shadowElevation = 3.dp
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black.copy(alpha = 0.75f)
        )
    }
}

private fun formatTime(seconds: Long): String {
    val m = seconds / 60; val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
