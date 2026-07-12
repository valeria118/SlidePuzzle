package com.example.a1234567889.logic

import android.util.Base64
import com.example.a1234567889.models.*
import java.nio.charset.Charset

object SettingSharingUtils {

    fun generateCode(settings: GameSettings): String {
        // Format: size;mode;challenge;timeLimit;survival;experiment;imageUri
        val parts = listOf(
            settings.size.toString(),
            settings.mode.ordinal.toString(),
            settings.challenge.ordinal.toString(),
            settings.timeAttackLimit.toString(),
            if (settings.isSurvivalMode) "1" else "0",
            if (settings.isExperimentMode) "1" else "0",
            settings.imageUri ?: ""
        )
        val raw = parts.joinToString(";")
        return Base64.encodeToString(raw.toByteArray(Charset.forName("UTF-8")), Base64.NO_WRAP or Base64.URL_SAFE)
    }

    fun decodeCode(code: String): GameSettings? {
        return try {
            val decoded = String(Base64.decode(code, Base64.NO_WRAP or Base64.URL_SAFE), Charset.forName("UTF-8"))
            val parts = decoded.split(";")
            if (parts.size < 6) return null
            
            GameSettings(
                size = parts[0].toInt(),
                mode = GameMode.entries[parts[1].toInt()],
                challenge = ChallengeMode.entries[parts[2].toInt()],
                timeAttackLimit = parts[3].toInt(),
                isSurvivalMode = parts[4] == "1",
                isExperimentMode = parts[5] == "1",
                imageUri = if (parts.size > 6 && parts[6].isNotEmpty()) parts[6] else null
            )
        } catch (e: Exception) {
            null
        }
    }
}
