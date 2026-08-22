package com.velocity.launcher.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.velocity.launcher.data.BackgroundType
import com.velocity.launcher.data.ColorTone
import com.velocity.launcher.data.ThemeMode

val AppLabelFontSize = 16.sp

val SoftTextShadow = Shadow(
    color = Color.Black.copy(alpha = 0.60f),
    offset = Offset(0f, 2f),
    blurRadius = 8f
)

val ClockTextShadow = Shadow(
    color = Color.Black.copy(alpha = 0.60f),
    offset = Offset(0f, 3f),
    blurRadius = 12f
)

fun calculateLuminance(color: Color): Float {
    return (0.299f * color.red + 0.587f * color.green + 0.114f * color.blue)
}

fun calculateLuminance(colorInt: Int): Float {
    val r = ((colorInt shr 16) and 0xFF) / 255f
    val g = ((colorInt shr 8) and 0xFF) / 255f
    val b = (colorInt and 0xFF) / 255f
    return 0.299f * r + 0.587f * g + 0.114f * b
}

fun isDarkUi(
    backgroundType: BackgroundType = BackgroundType.WALLPAPER,
    colorTone: ColorTone = ColorTone.ADAPTIVE,
    isSystemInDarkTheme: Boolean = false,
    solidBackgroundColor: Int = 0xFF000000.toInt()
): Boolean {
    return when (colorTone) {
        ColorTone.DARK -> true
        ColorTone.LIGHT -> false
        ColorTone.SYSTEM -> isSystemInDarkTheme
        ColorTone.ADAPTIVE -> {
            if (backgroundType == BackgroundType.SOLID) {
                calculateLuminance(solidBackgroundColor) < 0.5f
            } else {
                true
            }
        }
    }
}

fun isDarkUi(
    themeMode: ThemeMode,
    isSystemInDarkTheme: Boolean = false,
    solidBackgroundColor: Int = 0xFF000000.toInt()
): Boolean {
    return when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme
        ThemeMode.SOLID_COLOR -> calculateLuminance(solidBackgroundColor) < 0.5f
        ThemeMode.ADAPTIVE -> true
    }
}

private val DefaultDarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    background = Color(0xFF121212),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF4B5563)
)

private val DefaultLightColorScheme = lightColorScheme(
    primary = Color(0xFF6366F1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = Color(0xFF312E81),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1)
)

fun buildAdaptiveColorScheme(adaptiveAccentColor: Int?): ColorScheme {
    val accent = adaptiveAccentColor?.let { Color(it) } ?: Color(0xFF818CF8)
    val isDarkAccent = calculateLuminance(accent) < 0.5f

    return darkColorScheme(
        primary = accent,
        onPrimary = if (isDarkAccent) Color.White else Color(0xFF0F172A),
        primaryContainer = accent.copy(alpha = 0.35f),
        onPrimaryContainer = Color(0xFFE0E7FF),
        background = Color.Transparent,
        onBackground = Color.White,
        surface = Color.Transparent,
        onSurface = Color.White,
        surfaceVariant = Color(0xFF1E1E2E).copy(alpha = 0.92f),
        onSurfaceVariant = Color(0xFFE2E8F0),
        outline = accent.copy(alpha = 0.5f)
    )
}

fun buildSolidColorScheme(solidBackgroundColor: Int, solidAccentColor: Int): ColorScheme {
    val bg = Color(solidBackgroundColor)
    val accent = Color(solidAccentColor)
    val isDarkBg = calculateLuminance(solidBackgroundColor) < 0.5f
    val isDarkAccent = calculateLuminance(solidAccentColor) < 0.5f

    return if (isDarkBg) {
        darkColorScheme(
            primary = accent,
            onPrimary = if (isDarkAccent) Color.White else Color.Black,
            primaryContainer = accent.copy(alpha = 0.30f),
            onPrimaryContainer = Color(0xFFE0E7FF),
            background = bg,
            onBackground = Color.White,
            surface = bg,
            onSurface = Color(0xFFF3F4F6),
            surfaceVariant = if (solidBackgroundColor == 0xFF000000.toInt() || bg == Color.Black) {
                Color(0xFF141414)
            } else {
                Color(0xFF22262B)
            },
            onSurfaceVariant = Color(0xFF9CA3AF),
            outline = Color(0xFF374151)
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = if (isDarkAccent) Color.White else Color.Black,
            primaryContainer = accent.copy(alpha = 0.15f),
            onPrimaryContainer = Color(0xFF312E81),
            background = bg,
            onBackground = Color(0xFF0F172A),
            surface = bg,
            onSurface = Color(0xFF1E293B),
            surfaceVariant = Color(0xFFE2E8F0),
            onSurfaceVariant = Color(0xFF64748B),
            outline = Color(0xFFCBD5E1)
        )
    }
}

@Composable
fun NirantaraTheme(
    backgroundType: BackgroundType = BackgroundType.WALLPAPER,
    colorTone: ColorTone = ColorTone.ADAPTIVE,
    themeMode: ThemeMode = when (backgroundType) {
        BackgroundType.SOLID -> ThemeMode.SOLID_COLOR
        BackgroundType.WALLPAPER -> when (colorTone) {
            ColorTone.ADAPTIVE -> ThemeMode.ADAPTIVE
            ColorTone.DARK -> ThemeMode.DARK
            ColorTone.LIGHT -> ThemeMode.LIGHT
            ColorTone.SYSTEM -> ThemeMode.SYSTEM
        }
    },
    adaptiveAccentColor: Int? = null,
    solidBackgroundColor: Int = 0xFF000000.toInt(),
    solidAccentColor: Int = 0xFF818CF8.toInt(),
    isSystemInDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (backgroundType) {
        BackgroundType.SOLID -> {
            when (colorTone) {
                ColorTone.ADAPTIVE -> buildSolidColorScheme(solidBackgroundColor, adaptiveAccentColor ?: solidAccentColor)
                ColorTone.DARK -> buildSolidColorScheme(solidBackgroundColor, solidAccentColor)
                ColorTone.LIGHT -> buildSolidColorScheme(solidBackgroundColor, solidAccentColor)
                ColorTone.SYSTEM -> if (isSystemInDarkTheme) buildSolidColorScheme(solidBackgroundColor, solidAccentColor) else buildSolidColorScheme(solidBackgroundColor, solidAccentColor)
            }
        }
        BackgroundType.WALLPAPER -> {
            when (colorTone) {
                ColorTone.ADAPTIVE -> buildAdaptiveColorScheme(adaptiveAccentColor)
                ColorTone.DARK -> DefaultDarkColorScheme
                ColorTone.LIGHT -> DefaultLightColorScheme
                ColorTone.SYSTEM -> if (isSystemInDarkTheme) DefaultDarkColorScheme else DefaultLightColorScheme
            }
        }
    }

    val isDark = isDarkUi(
        backgroundType = backgroundType,
        colorTone = colorTone,
        isSystemInDarkTheme = isSystemInDarkTheme,
        solidBackgroundColor = solidBackgroundColor
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.show(WindowInsetsCompat.Type.statusBars())
                insetsController.show(WindowInsetsCompat.Type.navigationBars())
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
