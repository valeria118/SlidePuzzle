package com.example.a1234567889.ui.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.a1234567889.logic.CardRewardLogic
import com.example.a1234567889.logic.PictureProvider
import com.example.a1234567889.models.CardRank
import com.example.a1234567889.models.CollectedCard

@Composable
fun CardFrontContent(card: CollectedCard, context: Context, rankColor: Color) {
    val photoResId = context.resources.getIdentifier(card.photoRes, "drawable", context.packageName)
    val photoUri = if (photoResId != 0) PictureProvider.resUri(card.photoRes) else null

    val frameResName = CardRewardLogic.frameRes(card.rank)
    val frameResId = context.resources.getIdentifier(frameResName, "drawable", context.packageName)

    Box(modifier = Modifier.fillMaxSize()) {
        if (photoUri != null) {
            Image(
                painter = rememberAsyncImagePainter(photoUri),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E)))
        }

        if (frameResId != 0) {
            Image(
                painter = painterResource(id = frameResId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }
    }
}

@Composable
fun CardBackContent(rankColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A1A2E), rankColor.copy(alpha = 0.4f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text("?", fontSize = 80.sp, color = rankColor.copy(alpha = 0.5f), fontWeight = FontWeight.Black)
    }
}

@Composable
fun RankStrip(rank: CardRank, rankColor: Color, fontSize: androidx.compose.ui.unit.TextUnit = 16.sp) {
    val label = when (rank) {
        CardRank.BRONZE -> "◆ БРОНЗОВАЯ"
        CardRank.SILVER -> "⬡ СЕРЕБРЯНАЯ"
        CardRank.GOLD   -> "🏆 ЗОЛОТАЯ"
    }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = rankColor.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, rankColor.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            color = rankColor,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize,
            letterSpacing = 2.sp
        )
    }
}

fun Modifier.graphicsLayerScaleX(sx: Float): Modifier =
    this.graphicsLayer { scaleX = sx.coerceAtLeast(0.001f) }
