package com.example.a1234567889.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.example.a1234567889.logic.CardRewardLogic
import com.example.a1234567889.logic.CardShareUtils
import com.example.a1234567889.logic.PictureProvider
import com.example.a1234567889.models.CardRank
import com.example.a1234567889.models.CollectedCard
import kotlinx.coroutines.launch

@Composable
fun CardRevealDialog(
    card: CollectedCard,
    onDismiss: () -> Unit
) {
    val rankColor = when (card.rank) {
        CardRank.BRONZE -> Color(0xFFCD7F32)
        CardRank.SILVER -> Color(0xFFC0C0C0)
        CardRank.GOLD   -> Color(0xFFFFD700)
    }

    // Flip animation (only scale on X = "flip" effect)
    var revealed by remember { mutableStateOf(false) }
    val flipProgress by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "flip"
    )
    val scaleX = if (flipProgress < 0.5f) (1f - flipProgress * 2f) else ((flipProgress - 0.5f) * 2f)
    val showFront = flipProgress >= 0.5f

    // Pop-in scale
    val popScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "pop"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        revealed = true
    }

    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(vertical = 24.dp)
            ) {
                // Title
                AnimatedVisibility(visible = showFront) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Вы получили карточку!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            CardRewardLogic.rankLabel(card.rank),
                            color = rankColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        if (card.isDuplicate) {
                            Text(
                                "ДУБЛИКАТ — эта карточка уже была в коллекции",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                // The card itself
                Box(
                    modifier = Modifier
                        .scale(popScale)
                        .graphicsLayerScaleX(scaleX)
                        .width(260.dp)
                        .aspectRatio(535f / 707f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, rankColor.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                ) {
                    if (showFront) {
                        CardFrontContent(card = card, context = context, rankColor = rankColor)
                    } else {
                        CardBackContent(rankColor = rankColor)
                    }
                }

                // Rank strip
                AnimatedVisibility(visible = showFront) {
                    RankStrip(rank = card.rank, rankColor = rankColor)
                }

                // Source image info
                AnimatedVisibility(visible = showFront) {
                    Text(
                        "За пазл: ${card.sourceTitle}  •  ${card.boardSize}×${card.boardSize}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                // Close button
                AnimatedVisibility(visible = showFront) {
                    val scope = rememberCoroutineScope()
                    var isSharing by remember { mutableStateOf(false) }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            enabled = !isSharing,
                            onClick = {
                                isSharing = true
                                scope.launch {
                                    CardShareUtils.shareCard(context, card)
                                    isSharing = false
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = rankColor)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Поделиться")
                        }
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = rankColor)
                        ) {
                            Text("В коллекцию!", fontWeight = FontWeight.Bold, color = Color.Black.copy(alpha = 0.8f))
                        }
                    }
                }
            }

            // Background glow
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            listOf(rankColor.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
            )
        }
    }
}
