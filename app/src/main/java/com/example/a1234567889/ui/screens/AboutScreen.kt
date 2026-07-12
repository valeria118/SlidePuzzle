package com.example.a1234567889.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.a1234567889.logic.IntentHelper

/**
 * Lab item 14 — "Вызов сторонних приложений: телефон, браузер, email, Google Play. Intent".
 *
 * One screen, four Intents — every row launches a different external app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("О приложении") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Slide Puzzle", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Классическая игра-пятнашки с режимами картинок, вызовов и соревнований.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Версия 1.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Связаться с нами", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            ContactRow(
                icon = Icons.Default.Call,
                title = "Позвонить в поддержку",
                subtitle = "+7 900 123-45-67",
                onClick = { IntentHelper.dialPhone(context, "+79001234567") }
            )
            ContactRow(
                icon = Icons.Default.Email,
                title = "Написать на почту",
                subtitle = "support@slidepuzzle.example",
                onClick = {
                    IntentHelper.sendEmail(
                        context,
                        to = "support@slidepuzzle.example",
                        subject = "Slide Puzzle — обращение в поддержку",
                        body = "Здравствуйте! "
                    )
                }
            )
            ContactRow(
                icon = Icons.Default.Language,
                title = "Открыть сайт разработчика",
                subtitle = "developer.example.com",
                onClick = { IntentHelper.openBrowser(context, "https://developer.android.com") }
            )
            ContactRow(
                icon = Icons.Default.Star,
                title = "Оценить в Google Play",
                subtitle = "Оставьте отзыв о приложении",
                onClick = { IntentHelper.openPlayStore(context) }
            )

            Spacer(Modifier.height(20.dp))
            Row {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Каждая кнопка выше вызывает соответствующее системное приложение через Intent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ContactRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
