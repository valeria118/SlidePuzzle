package com.example.a1234567889.logic

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.a1234567889.MainActivity
import com.example.a1234567889.R

/**
 * Lab item 5 — "Уведомления Notifications. Иконки, приоритеты, звук, вибрация".
 *
 * Two channels are used on purpose to demonstrate different priorities:
 *  - [CHANNEL_ACHIEVEMENTS] — IMPORTANCE_HIGH, with sound + vibration (pops up / heads-up).
 *  - [CHANNEL_GENERAL]      — IMPORTANCE_DEFAULT, for ordinary game-result notifications.
 */
object NotificationHelper {

    const val CHANNEL_GENERAL = "general_channel"
    const val CHANNEL_ACHIEVEMENTS = "achievements_channel"
    const val CHANNEL_CHAT = "chat_channel"

    private const val ID_GAME_RESULT = 1001
    private const val ID_ACHIEVEMENT = 1002
    private const val ID_CHAT = 1003

    @Volatile private var channelsCreated = false

    private fun ensureChannels(context: Context) {
        if (channelsCreated || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            channelsCreated = true
            return
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val general = NotificationChannel(
            CHANNEL_GENERAL,
            "Игровые результаты",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Победы и поражения в партиях"
        }

        val achievements = NotificationChannel(
            CHANNEL_ACHIEVEMENTS,
            "Достижения",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Новые открытые достижения"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 80, 60, 80, 60, 160)
            enableLights(true)
        }

        val chat = NotificationChannel(
            CHANNEL_CHAT,
            "Сообщения чата",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Новые сообщения в чате"
            enableVibration(true)
        }

        manager.createNotificationChannels(listOf(general, achievements, chat))
        channelsCreated = true
    }

    private fun contentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    private fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Shows a notification when a match finishes (win or loss) — IMPORTANCE_DEFAULT channel. */
    fun showGameResultNotification(context: Context, won: Boolean, modeName: String, moves: Int, timeSeconds: Long) {
        ensureChannels(context)
        if (!canNotify(context)) return

        val minutes = timeSeconds / 60
        val seconds = timeSeconds % 60
        val timeText = "%d:%02d".format(minutes, seconds)

        val title = if (won) "Победа! 🎉" else "Игра окончена"
        val text = if (won)
            "Режим «$modeName» пройден за $timeText, ходов: $moves"
        else
            "Режим «$modeName» — попробуйте ещё раз"

        val notification = NotificationCompat.Builder(context, CHANNEL_GENERAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(ID_GAME_RESULT, notification)
        } catch (_: SecurityException) {
            // Permission was revoked between the check and the call — ignore safely.
        }
    }

    /** Shows a high-priority, sound+vibration notification for a freshly unlocked achievement. */
    fun showAchievementNotification(context: Context, title: String, description: String) {
        ensureChannels(context)
        if (!canNotify(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ACHIEVEMENTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Достижение получено: $title")
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(ID_ACHIEVEMENT, notification)
        } catch (_: SecurityException) {
        }
    }

    /** Notification for a new chat message received in [com.example.a1234567889.chat.ChatActivity]. */
    fun showChatNotification(context: Context, sender: String, message: String) {
        ensureChannels(context)
        if (!canNotify(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_CHAT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(sender)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(ID_CHAT, notification)
        } catch (_: SecurityException) {
        }
    }
}
