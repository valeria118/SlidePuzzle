package com.example.a1234567889.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.a1234567889.logic.CardRewardLogic
import com.example.a1234567889.logic.PictureProvider
import com.example.a1234567889.models.CardRank
import com.example.a1234567889.models.CollectedCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardGalleryScreen(
    cards: List<CollectedCard>,
    onNavigateBack: () -> Unit
) {
    var selectedRank by remember { mutableStateOf<CardRank?>(null) }
    var selectedCard by remember { mutableStateOf<CollectedCard?>(null) }

    val filteredCards = if (selectedRank == null) {
        cards
    } else {
        cards.filter { it.rank == selectedRank }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Коллекция карточек") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Фильтр")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Все") },
                            onClick = { selectedRank = null; showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Золотые") },
                            onClick = { selectedRank = CardRank.GOLD; showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Серебряные") },
                            onClick = { selectedRank = CardRank.SILVER; showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Бронзовые") },
                            onClick = { selectedRank = CardRank.BRONZE; showMenu = false }
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (cards.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("У вас пока нет карточек.\nПобеждайте в режиме «Картинка», чтобы получить их!", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredCards) { card ->
                    CardItem(card = card, onClick = { selectedCard = card })
                }
            }
        }
    }

    if (selectedCard != null) {
        CardFullView(
            card = selectedCard!!,
            onDismiss = { selectedCard = null }
        )
    }
}

@Composable
fun CardItem(card: CollectedCard, onClick: () -> Unit) {
    val context = LocalContext.current
    val rankColor = CardRewardLogic.rankColor(card.rank)
    
    val photoResId = context.resources.getIdentifier(card.photoRes, "drawable", context.packageName)
    val photoUri = if (photoResId != 0) PictureProvider.resUri(card.photoRes) else null

    val frameResName = CardRewardLogic.frameRes(card.rank)
    val frameResId = context.resources.getIdentifier(frameResName, "drawable", context.packageName)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(535f / 707f)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, rankColor.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (photoUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(photoUri),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            if (frameResId != 0) {
                Image(
                    painter = painterResource(id = frameResId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            }
            
            // Rank indicator
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(rankColor.copy(alpha = 0.8f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = when(card.rank) {
                        CardRank.GOLD -> "G"
                        CardRank.SILVER -> "S"
                        CardRank.BRONZE -> "B"
                    },
                    color = Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (card.isDuplicate) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ДУБЛИКАТ",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
