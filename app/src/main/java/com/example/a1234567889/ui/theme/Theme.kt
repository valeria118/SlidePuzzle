package com.example.a1234567889.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun _1234567889Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    colorSchemeName: String = "Classic",
    content: @Composable () -> Unit
) {
    val baseColorScheme = when (colorSchemeName) {
        "Ocean" -> if (darkTheme) darkColorScheme(primary = OceanPrimary, secondary = OceanSecondary, tertiary = OceanTertiary) 
                   else lightColorScheme(primary = OceanPrimary, secondary = OceanSecondary, tertiary = OceanTertiary)
        "Forest" -> if (darkTheme) darkColorScheme(primary = ForestPrimary, secondary = ForestSecondary, tertiary = ForestTertiary)
                    else lightColorScheme(primary = ForestPrimary, secondary = ForestSecondary, tertiary = ForestTertiary)
        "Space" -> if (darkTheme) darkColorScheme(primary = SpacePrimary, secondary = SpaceSecondary, tertiary = SpaceTertiary)
                    else lightColorScheme(primary = SpacePrimary, secondary = SpaceSecondary, tertiary = SpaceTertiary)
        "Pastel" -> if (darkTheme) darkColorScheme(primary = PastelPrimary, secondary = PastelSecondary, tertiary = PastelTertiary)
                    else lightColorScheme(primary = PastelPrimary, secondary = PastelSecondary, tertiary = PastelTertiary)
        else -> if (darkTheme) DarkColorScheme else LightColorScheme
    }

    val colorScheme = when {
        dynamicColor && colorSchemeName == "Classic" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> baseColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}