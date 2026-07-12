package com.example.a1234567889.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.example.a1234567889.R
import com.example.a1234567889.logic.CardRewardLogic
import com.example.a1234567889.logic.CardShareUtils
import com.example.a1234567889.logic.PictureProvider
import com.example.a1234567889.models.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    gallery: List<ImageGalleryItem>,
    cards: List<CollectedCard>,
    onPlayItem: (ImageGalleryItem) -> Unit,
    onUploadImage: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Puzzles, 1: Cards
    var searchQuery by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<ImageGalleryItem?>(null) }
    var selectedCard by remember { mutableStateOf<CollectedCard?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var sortOption by remember { mutableIntStateOf(0) } // 0=название, 1=дата, 2=лучшее время
    var selectedCategories by remember { mutableStateOf(setOf<String>()) } // empty = all categories

    val allCategories = remember(gallery) { gallery.map { it.category }.distinct().sorted() }

    val filteredGallery = gallery.filter { 
        (it.title.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true)) &&
            (selectedCategories.isEmpty() || it.category in selectedCategories)
    }.let { list ->
        when (sortOption) {
            1 -> list.sortedByDescending { it.dateCollected ?: 0L }
            2 -> list.sortedBy { it.bestTime ?: Long.MAX_VALUE }
            else -> list.sortedBy { it.title }
        }
    }
    
    val filteredCards = cards.filter {
        it.sourceTitle.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.gallery)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (selectedTab == 0) {
                        IconButton(onClick = { showFilterDialog = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Фильтр и сортировка")
                        }
                        IconButton(onClick = onUploadImage) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = stringResource(R.string.upload_image))
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Пазлы") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Карточки") }
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text(if (selectedTab == 0) stringResource(R.string.search) else "Поиск по названию...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )

            if (selectedTab == 0) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredGallery) { item ->
                        GalleryCard(item, onClick = { selectedItem = item })
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredCards) { card ->
                        CollectionCardTile(card = card, onClick = { selectedCard = card })
                    }
                }
            }
        }
    }

    selectedItem?.let { item ->
        GalleryDetailDialog(
            item = item,
            onDismiss = { selectedItem = null },
            onPlay = { 
                onPlayItem(item)
                selectedItem = null
            }
        )
    }

    selectedCard?.let { card ->
        CardFullView(card = card, onDismiss = { selectedCard = null })
    }

    // Lab item 4 — "Диалоговые окна AlertDialog. Кнопки, списки, множественный выбор".
    if (showFilterDialog) {
        var draftSort by remember { mutableIntStateOf(sortOption) }
        var draftCategories by remember { mutableStateOf(selectedCategories) }
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Фильтр и сортировка") },
            text = {
                Column {
                    Text("Сортировать по:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                    // Single-choice list (radio buttons)
                    listOf("Названию", "Дате добавления", "Лучшему времени").forEachIndexed { index, label ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { draftSort = index }
                        ) {
                            RadioButton(selected = draftSort == index, onClick = { draftSort = index })
                            Text(label)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("Категории:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                    // Multiple-choice list (checkboxes)
                    allCategories.forEach { category ->
                        val checked = category in draftCategories
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    draftCategories = if (checked) draftCategories - category else draftCategories + category
                                }
                        ) {
                            Checkbox(checked = checked, onCheckedChange = {
                                draftCategories = if (checked) draftCategories - category else draftCategories + category
                            })
                            Text(category)
                        }
                    }
                    if (allCategories.isEmpty()) {
                        Text("Нет категорий", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    sortOption = draftSort
                    selectedCategories = draftCategories
                    showFilterDialog = false
                }) { Text("Применить") }
            },
            dismissButton = {
                TextButton(onClick = {
                    draftSort = 0
                    draftCategories = emptySet()
                    sortOption = 0
                    selectedCategories = emptySet()
                    showFilterDialog = false
                }) { Text("Сбросить") }
            }
        )
    }
}


@Composable
fun CollectionCardTile(card: CollectedCard, onClick: () -> Unit) {
    val context = LocalContext.current
    val rankC = rankColor(card.rank)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(535f / 707f)
            .clickable { onClick() }
            .border(1.5.dp, rankC.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Photo
            val photoUri = PictureProvider.resUri(card.photoRes)
            Image(
                painter = rememberAsyncImagePainter(photoUri),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Frame overlay
            val frameResName = CardRewardLogic.frameRes(card.rank)
            val frameResId = context.resources.getIdentifier(frameResName, "drawable", context.packageName)
            if (frameResId != 0) {
                Image(
                    painter = painterResource(id = frameResId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            }

            // Rank glow strip at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, rankC.copy(alpha = 0.35f))))
            )

            if (card.isDuplicate) {
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Text(
                        "ДУБЛИКАТ",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CardFullView(card: CollectedCard, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val rankC = rankColor(card.rank)
    val scope = rememberCoroutineScope()
    var isSharing by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                // Rank badge
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = rankC.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, rankC.copy(alpha = 0.5f))
                    ) {
                        Text(
                            rankLabel(card.rank).uppercase(),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            color = rankC, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 2.sp
                        )
                    }
                    if (card.isDuplicate) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                        ) {
                            Text(
                                "ДУБЛИКАТ",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp
                            )
                        }
                    }
                }

                // Card visual — tap is consumed here so it doesn't dismiss the dialog (lets the
                // person view it large without accidentally closing).
                Box(
                    modifier = Modifier
                        .width(280.dp)
                        .aspectRatio(535f / 707f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, rankC, RoundedCornerShape(16.dp))
                        .clickable(enabled = false) {}
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(PictureProvider.resUri(card.photoRes)),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    val frameResName = CardRewardLogic.frameRes(card.rank)
                    val frameResId = context.resources.getIdentifier(frameResName, "drawable", context.packageName)
                    if (frameResId != 0) {
                        Image(
                            painter = painterResource(id = frameResId),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )
                    }
                }

                // Meta info
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(card.sourceTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Поле ${card.boardSize}×${card.boardSize}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Text(
                        "Получена ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(card.obtainedAt))}",
                        color = rankC.copy(alpha = 0.8f), fontSize = 11.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        enabled = !isSharing,
                        onClick = {
                            isSharing = true
                            scope.launch {
                                CardShareUtils.shareCard(context, card)
                                isSharing = false
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Поделиться")
                    }
                }

                // Close
                TextButton(onClick = onDismiss) {
                    Text("Закрыть", color = Color.White.copy(alpha = 0.6f))
                }
            }
        }
    }
}

private fun rankColor(rank: CardRank): Color = when (rank) {
    CardRank.BRONZE -> Color(0xFFCD7F32)
    CardRank.SILVER -> Color(0xFFC0C0C0)
    CardRank.GOLD   -> Color(0xFFFFD700)
}
private fun rankLabel(rank: CardRank): String = when (rank) {
    CardRank.BRONZE -> "Бронзовая"
    CardRank.SILVER -> "Серебряная"
    CardRank.GOLD   -> "Золотая"
}

@Composable
fun GalleryCard(item: ImageGalleryItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box {
            Image(
                painter = rememberAsyncImagePainter(item.uri),
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            if (item.isCollected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.Green,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(24.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp)
            ) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun GalleryDetailDialog(
    item: ImageGalleryItem,
    onDismiss: () -> Unit,
    onPlay: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Image(
                    painter = rememberAsyncImagePainter(item.uri),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = item.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = item.category, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                DetailRow(Icons.Default.History, stringResource(R.string.best_time), formatTime(item.bestTime ?: 0))
                DetailRow(Icons.Default.TrendingUp, stringResource(R.string.best_moves), item.bestMoves?.toString() ?: "-")
                if (item.boardSize > 0) {
                    DetailRow(Icons.Default.GridOn, "Размер поля", "${item.boardSize}×${item.boardSize}")
                }
                DetailRow(Icons.Default.Extension, stringResource(R.string.complexity), "%.1f".format(item.complexity))
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onPlay,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.play_again), maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "$label:", fontSize = 14.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
