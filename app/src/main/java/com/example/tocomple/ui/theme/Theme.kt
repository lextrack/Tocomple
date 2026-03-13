package com.example.tocomple.ui.theme

import android.app.Activity
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
    primary = TomatoSoft,
    onPrimary = BreadDark,
    primaryContainer = TomatoRed,
    onPrimaryContainer = BreadCream,
    secondary = AvocadoSoft,
    onSecondary = BreadDark,
    secondaryContainer = AvocadoGreen,
    onSecondaryContainer = BreadCream,
    tertiary = MustardSoft,
    onTertiary = BreadDark,
    tertiaryContainer = MustardGold,
    onTertiaryContainer = BreadDark,
    background = SurfaceWarmDark,
    onBackground = BreadCream,
    surface = SurfaceMutedDark,
    onSurface = BreadCream,
    surfaceVariant = SurfaceMutedDark,
    onSurfaceVariant = Color(0xFFE2D6CA),
    outlineVariant = Color(0xFF6E625A),
    errorContainer = Color(0xFF8F3A31)
)

private val LightColorScheme = lightColorScheme(
    primary = TomatoRed,
    onPrimary = BreadCream,
    primaryContainer = TomatoSoft,
    onPrimaryContainer = BreadDark,
    secondary = AvocadoGreen,
    onSecondary = BreadCream,
    secondaryContainer = AvocadoSoft,
    onSecondaryContainer = BreadDark,
    tertiary = MustardGold,
    onTertiary = BreadDark,
    tertiaryContainer = MustardSoft,
    onTertiaryContainer = BreadDark,
    background = BreadCream,
    onBackground = BreadDark,
    surface = SurfaceWarm,
    onSurface = BreadDark,
    surfaceVariant = Color(0xFFF8EDE2),
    onSurfaceVariant = Color(0xFF63564D),
    outlineVariant = OutlineWarm,
    errorContainer = Color(0xFFFFDAD4)
)

@Composable
fun TocompleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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
