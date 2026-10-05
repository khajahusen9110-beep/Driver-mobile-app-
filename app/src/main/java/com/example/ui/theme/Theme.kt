package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DriverPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = DriverSecondary,
    onSecondary = Color.White,
    background = DriverDarkBackground,
    onBackground = TextPrimaryDark,
    surface = DriverDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DriverDarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    error = DriverOfflineRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = DriverPrimaryVariant,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF065F46),
    secondary = DriverSecondary,
    onSecondary = Color.White,
    background = DriverLightBackground,
    onBackground = TextPrimaryLight,
    surface = DriverLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = DriverLightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    error = DriverOfflineRed,
    onError = Color.White
)

@Composable
fun RiderDriverTheme(
    darkTheme: Boolean = true, // Default to sleek night driver mode
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
