package com.example.weighttracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = OceanPrimary,
    onPrimary = OceanOnPrimary,
    secondary = OceanSecondary,
    background = OceanSurface,
    surface = OceanSurface,
    onSurface = ColorPalette.Gray900
)

private val DarkColorScheme = darkColorScheme(
    primary = OceanSecondary,
    onPrimary = OceanOnPrimary,
    secondary = OceanPrimary,
    background = ColorPalette.Gray900,
    surface = ColorPalette.Gray900,
    onSurface = OceanOnPrimary
)

@Composable
fun WeightTrackerTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

private object ColorPalette {
    val Gray900 = androidx.compose.ui.graphics.Color(0xFF101418)
}
