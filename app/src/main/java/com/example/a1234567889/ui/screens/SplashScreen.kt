package com.example.a1234567889.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.a1234567889.R
import kotlinx.coroutines.delay

/**
 * Lab item 9 — "Загрузочный экран (Splash Screen). Реализация с задержкой. Анимация элементов".
 *
 * A first NavHost destination that fades & scales the app logo in, holds briefly,
 * then calls [onFinished] so MainActivity can navigate to "mode_selection" and pop
 * this screen off the back stack.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.6f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing))
        scale.animateTo(1f, animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing))
        textAlpha.animateTo(1f, animationSpec = tween(durationMillis = 450))
        delay(900) // hold the splash on screen
        onFinished()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF3E2BAF)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(R.drawable.ic_app_logo),
                    contentDescription = "Логотип",
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale.value)
                        .alpha(alpha.value)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Slide Puzzle",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.alpha(textAlpha.value)
                )
                Text(
                    text = "Собери картинку до последней детали",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .alpha(textAlpha.value)
                )
            }
        }
    }
}
