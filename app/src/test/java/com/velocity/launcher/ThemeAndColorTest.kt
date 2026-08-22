package com.velocity.launcher

import com.velocity.launcher.data.BackgroundType
import com.velocity.launcher.data.ColorTone
import com.velocity.launcher.data.ExtractedColors
import com.velocity.launcher.data.SolidColorPreset
import com.velocity.launcher.data.ThemeMode
import com.velocity.launcher.ui.theme.buildAdaptiveColorScheme
import com.velocity.launcher.ui.theme.buildSolidColorScheme
import com.velocity.launcher.ui.theme.calculateLuminance
import com.velocity.launcher.ui.theme.isDarkUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeAndColorTest {

    @Test
    fun testBackgroundTypeEnum() {
        val types = BackgroundType.entries
        assertEquals(2, types.size)
        assertTrue(types.contains(BackgroundType.WALLPAPER))
        assertTrue(types.contains(BackgroundType.SOLID))
    }

    @Test
    fun testColorToneEnum() {
        val tones = ColorTone.entries
        assertEquals(4, tones.size)
        assertTrue(tones.contains(ColorTone.ADAPTIVE))
        assertTrue(tones.contains(ColorTone.DARK))
        assertTrue(tones.contains(ColorTone.LIGHT))
        assertTrue(tones.contains(ColorTone.SYSTEM))
    }

    @Test
    fun testThemeModeEnum() {
        val modes = ThemeMode.entries
        assertEquals(5, modes.size)
        assertTrue(modes.contains(ThemeMode.ADAPTIVE))
        assertTrue(modes.contains(ThemeMode.DARK))
        assertTrue(modes.contains(ThemeMode.LIGHT))
        assertTrue(modes.contains(ThemeMode.SYSTEM))
        assertTrue(modes.contains(ThemeMode.SOLID_COLOR))
    }

    @Test
    fun testSolidColorPresetEnum() {
        val presets = SolidColorPreset.entries
        assertEquals(5, presets.size)
        assertTrue(presets.contains(SolidColorPreset.AMOLED_BLACK))
        assertTrue(presets.contains(SolidColorPreset.DEEP_CHARCOAL))
        assertTrue(presets.contains(SolidColorPreset.MIDNIGHT_SLATE))
        assertTrue(presets.contains(SolidColorPreset.CLEAN_LIGHT))
        assertTrue(presets.contains(SolidColorPreset.CUSTOM))

        assertEquals(0xFF000000.toInt(), SolidColorPreset.AMOLED_BLACK.defaultBgColor)
        assertEquals(0xFF818CF8.toInt(), SolidColorPreset.AMOLED_BLACK.defaultAccentColor)

        assertEquals(0xFF121212.toInt(), SolidColorPreset.DEEP_CHARCOAL.defaultBgColor)
        assertEquals(0xFF60A5FA.toInt(), SolidColorPreset.DEEP_CHARCOAL.defaultAccentColor)

        assertEquals(0xFF1E1E2E.toInt(), SolidColorPreset.MIDNIGHT_SLATE.defaultBgColor)
        assertEquals(0xFF38BDF8.toInt(), SolidColorPreset.MIDNIGHT_SLATE.defaultAccentColor)

        assertEquals(0xFFF8F9FA.toInt(), SolidColorPreset.CLEAN_LIGHT.defaultBgColor)
        assertEquals(0xFF4F46E5.toInt(), SolidColorPreset.CLEAN_LIGHT.defaultAccentColor)
    }

    @Test
    fun testLuminanceCalculation() {
        // Black has 0 luminance
        val blackLum = calculateLuminance(0xFF000000.toInt())
        assertEquals(0.0f, blackLum, 0.01f)

        // White has 1 luminance
        val whiteLum = calculateLuminance(0xFFFFFFFF.toInt())
        assertEquals(1.0f, whiteLum, 0.01f)

        // Clean light preset background should have high luminance (> 0.5)
        val cleanLightLum = calculateLuminance(SolidColorPreset.CLEAN_LIGHT.defaultBgColor)
        assertTrue(cleanLightLum > 0.5f)

        // Amoled Black preset background should have low luminance (< 0.5)
        val amoledLum = calculateLuminance(SolidColorPreset.AMOLED_BLACK.defaultBgColor)
        assertTrue(amoledLum < 0.5f)

        // Deep Charcoal background should have low luminance (< 0.5)
        val charcoalLum = calculateLuminance(SolidColorPreset.DEEP_CHARCOAL.defaultBgColor)
        assertTrue(charcoalLum < 0.5f)

        // Midnight Slate background should have low luminance (< 0.5)
        val slateLum = calculateLuminance(SolidColorPreset.MIDNIGHT_SLATE.defaultBgColor)
        assertTrue(slateLum < 0.5f)
    }

    @Test
    fun testExtractedColorsPrimaryAccentFallback() {
        val colorsWithVibrant = ExtractedColors(
            vibrantColor = 0xFF6366F1.toInt(),
            dominantColor = 0xFF1E1E2E.toInt()
        )
        assertEquals(0xFF6366F1.toInt(), colorsWithVibrant.primaryAccent)

        val colorsWithDominantOnly = ExtractedColors(
            dominantColor = 0xFF38BDF8.toInt()
        )
        assertEquals(0xFF38BDF8.toInt(), colorsWithDominantOnly.primaryAccent)

        val colorsEmpty = ExtractedColors()
        assertEquals(0xFF818CF8.toInt(), colorsEmpty.primaryAccent)
    }

    @Test
    fun testAdaptiveColorSchemeBuilder() {
        val scheme = buildAdaptiveColorScheme(0xFF6366F1.toInt())
        assertNotNull(scheme)
    }

    @Test
    fun testSolidColorSchemeBuilder() {
        val darkSolid = buildSolidColorScheme(0xFF000000.toInt(), 0xFF818CF8.toInt())
        assertNotNull(darkSolid)

        val lightSolid = buildSolidColorScheme(0xFFFFFFFF.toInt(), 0xFF6366F1.toInt())
        assertNotNull(lightSolid)
    }

    @Test
    fun testIsDarkUiWithBackgroundTypeAndColorTone() {
        // 1. ColorTone.DARK -> true
        assertTrue(isDarkUi(BackgroundType.WALLPAPER, ColorTone.DARK))
        assertTrue(isDarkUi(BackgroundType.SOLID, ColorTone.DARK))

        // 2. ColorTone.LIGHT -> false
        assertFalse(isDarkUi(BackgroundType.WALLPAPER, ColorTone.LIGHT))
        assertFalse(isDarkUi(BackgroundType.SOLID, ColorTone.LIGHT))

        // 3. ColorTone.SYSTEM -> follows isSystemInDarkTheme
        assertTrue(isDarkUi(BackgroundType.WALLPAPER, ColorTone.SYSTEM, isSystemInDarkTheme = true))
        assertFalse(isDarkUi(BackgroundType.WALLPAPER, ColorTone.SYSTEM, isSystemInDarkTheme = false))
        assertTrue(isDarkUi(BackgroundType.SOLID, ColorTone.SYSTEM, isSystemInDarkTheme = true))
        assertFalse(isDarkUi(BackgroundType.SOLID, ColorTone.SYSTEM, isSystemInDarkTheme = false))

        // 4. ColorTone.ADAPTIVE with WALLPAPER -> true (white status bar icons on dark scrim)
        assertTrue(isDarkUi(BackgroundType.WALLPAPER, ColorTone.ADAPTIVE))

        // 5. ColorTone.ADAPTIVE with SOLID -> checks solidBackgroundColor luminance
        // AMOLED Black -> dark UI (true)
        assertTrue(isDarkUi(BackgroundType.SOLID, ColorTone.ADAPTIVE, solidBackgroundColor = SolidColorPreset.AMOLED_BLACK.defaultBgColor))
        // Deep Charcoal -> dark UI (true)
        assertTrue(isDarkUi(BackgroundType.SOLID, ColorTone.ADAPTIVE, solidBackgroundColor = SolidColorPreset.DEEP_CHARCOAL.defaultBgColor))
        // Midnight Slate -> dark UI (true)
        assertTrue(isDarkUi(BackgroundType.SOLID, ColorTone.ADAPTIVE, solidBackgroundColor = SolidColorPreset.MIDNIGHT_SLATE.defaultBgColor))
        // Clean Light -> light UI (false)
        assertFalse(isDarkUi(BackgroundType.SOLID, ColorTone.ADAPTIVE, solidBackgroundColor = SolidColorPreset.CLEAN_LIGHT.defaultBgColor))
        // Pure White -> light UI (false)
        assertFalse(isDarkUi(BackgroundType.SOLID, ColorTone.ADAPTIVE, solidBackgroundColor = 0xFFFFFFFF.toInt()))
    }

    @Test
    fun testIsDarkUiLegacyThemeModeOverload() {
        assertTrue(isDarkUi(ThemeMode.DARK))
        assertFalse(isDarkUi(ThemeMode.LIGHT))
        assertTrue(isDarkUi(ThemeMode.SYSTEM, isSystemInDarkTheme = true))
        assertFalse(isDarkUi(ThemeMode.SYSTEM, isSystemInDarkTheme = false))
        assertTrue(isDarkUi(ThemeMode.ADAPTIVE))
        assertTrue(isDarkUi(ThemeMode.SOLID_COLOR, solidBackgroundColor = SolidColorPreset.AMOLED_BLACK.defaultBgColor))
        assertFalse(isDarkUi(ThemeMode.SOLID_COLOR, solidBackgroundColor = SolidColorPreset.CLEAN_LIGHT.defaultBgColor))
    }
}
