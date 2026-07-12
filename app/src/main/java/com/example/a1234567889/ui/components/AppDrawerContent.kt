package com.example.a1234567889.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.a1234567889.R

private data class DrawerEntry(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

/**
 * Lab item 11 — "Меню: меню-шторка (Navigation Drawer)".
 *
 * The main app-wide drawer, opened from the hamburger icon on the home screen's
 * top bar. Navigation entries map to NavHost routes in MainActivity, except
 * "chat" which launches the classic [com.example.a1234567889.chat.ChatActivity]
 * (lab items 11/12 — Fragments + RecyclerView adapter).
 */
@Composable
fun AppDrawerContent(isLoggedIn: Boolean, onItemSelected: (route: String) -> Unit) {
    val entries = listOf(
        DrawerEntry("mode_selection", "Главная", Icons.Default.Home),
        DrawerEntry("profile", if (isLoggedIn) "Профиль" else "Войти", Icons.Default.AccountCircle),
        DrawerEntry("gallery", "Галерея изображений", Icons.Default.Photo),
        DrawerEntry("achievements", "Достижения", Icons.Default.EmojiEvents),
        DrawerEntry("friends", "Мои друзья", Icons.Default.People),
        DrawerEntry("leaderboard", "Рейтинг", Icons.Default.BarChart),
        DrawerEntry("game_history", "История партий", Icons.Default.BarChart),
        DrawerEntry("score_calculator", "Калькулятор результата", Icons.Default.Calculate),
        DrawerEntry("settings", "Настройки", Icons.Default.Settings),
        DrawerEntry("about", "О приложении", Icons.Default.Info)
    )

    ModalDrawerSheet {
        Column(modifier = Modifier.padding(20.dp)) {
            Image(
                painter = painterResource(R.drawable.ic_app_logo),
                contentDescription = null,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text("Slide Puzzle", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Меню навигации", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Divider()
        Spacer(Modifier.height(8.dp))
        entries.forEach { entry ->
            NavigationDrawerItem(
                icon = { Icon(entry.icon, contentDescription = null) },
                label = { Text(entry.label) },
                selected = false,
                onClick = { onItemSelected(entry.route) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}
