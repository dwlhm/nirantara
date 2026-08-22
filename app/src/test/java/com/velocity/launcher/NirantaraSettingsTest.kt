package com.velocity.launcher

import com.velocity.launcher.data.ScrollbarPosition
import com.velocity.launcher.data.ScrollbarVerticalAlignment
import com.velocity.launcher.data.ThemeMode
import com.velocity.launcher.data.TopSpacingMode
import com.velocity.launcher.ui.compose.components.SETTINGS_GLYPHS
import com.velocity.launcher.ui.compose.components.SettingsFilterCategory
import com.velocity.launcher.ui.compose.components.SettingsPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NirantaraSettingsTest {

    @Test
    fun testSettingsPageEnum_containsAllExpectedPages() {
        val pages = SettingsPage.entries
        assertEquals(6, pages.size)
        assertTrue(pages.contains(SettingsPage.MAIN_HUB))
        assertTrue(pages.contains(SettingsPage.LOOK_AND_FEEL))
        assertTrue(pages.contains(SettingsPage.REACHABILITY))
        assertTrue(pages.contains(SettingsPage.SCROLLBAR))
        assertTrue(pages.contains(SettingsPage.APPS_SEARCH))
        assertTrue(pages.contains(SettingsPage.SYSTEM))
    }

    @Test
    fun testSettingsFilterCategory_hasExpectedCategories() {
        val categories = SettingsFilterCategory.entries
        assertEquals(6, categories.size)
        assertEquals("Semua", SettingsFilterCategory.ALL.label)
        assertEquals("🎨 Tampilan", SettingsFilterCategory.LOOK_AND_FEEL.label)
        assertEquals("📱 Ergonomi", SettingsFilterCategory.REACHABILITY.label)
        assertEquals("🔤 Scrollbar", SettingsFilterCategory.SCROLLBAR.label)
        assertEquals("📦 Aplikasi", SettingsFilterCategory.APPS_SEARCH.label)
        assertEquals("⚙️ Sistem", SettingsFilterCategory.SYSTEM.label)
    }

    @Test
    fun testSettingsGlyphs_hasExpectedCategoriesAndOrder() {
        assertEquals(5, SETTINGS_GLYPHS.size)
        assertEquals("🎨", SETTINGS_GLYPHS[0])
        assertEquals("📱", SETTINGS_GLYPHS[1])
        assertEquals("🔤", SETTINGS_GLYPHS[2])
        assertEquals("📦", SETTINGS_GLYPHS[3])
        assertEquals("⚙️", SETTINGS_GLYPHS[4])
    }

    @Test
    fun testBackgroundTypeEnum_containsExpectedTypes() {
        val types = com.velocity.launcher.data.BackgroundType.entries
        assertEquals(2, types.size)
        assertTrue(types.contains(com.velocity.launcher.data.BackgroundType.WALLPAPER))
        assertTrue(types.contains(com.velocity.launcher.data.BackgroundType.SOLID))
    }

    @Test
    fun testColorToneEnum_containsExpectedTones() {
        val tones = com.velocity.launcher.data.ColorTone.entries
        assertEquals(4, tones.size)
        assertTrue(tones.contains(com.velocity.launcher.data.ColorTone.ADAPTIVE))
        assertTrue(tones.contains(com.velocity.launcher.data.ColorTone.DARK))
        assertTrue(tones.contains(com.velocity.launcher.data.ColorTone.LIGHT))
        assertTrue(tones.contains(com.velocity.launcher.data.ColorTone.SYSTEM))
    }

    @Test
    fun testThemeModeEnum_containsExpectedModes() {
        val modes = ThemeMode.entries
        assertEquals(5, modes.size)
        assertTrue(modes.contains(ThemeMode.ADAPTIVE))
        assertTrue(modes.contains(ThemeMode.DARK))
        assertTrue(modes.contains(ThemeMode.LIGHT))
        assertTrue(modes.contains(ThemeMode.SYSTEM))
        assertTrue(modes.contains(ThemeMode.SOLID_COLOR))
    }

    @Test
    fun testTopSpacingModeEnum_containsExpectedModes() {
        val modes = TopSpacingMode.entries
        assertEquals(4, modes.size)
        assertTrue(modes.contains(TopSpacingMode.NORMAL))
        assertTrue(modes.contains(TopSpacingMode.LARGE))
        assertTrue(modes.contains(TopSpacingMode.COMPACT))
        assertTrue(modes.contains(TopSpacingMode.NONE))
    }

    @Test
    fun testScrollbarPositionAndAlignment_containExpectedValues() {
        assertEquals(3, ScrollbarPosition.entries.size)
        assertTrue(ScrollbarPosition.entries.contains(ScrollbarPosition.LEFT))
        assertTrue(ScrollbarPosition.entries.contains(ScrollbarPosition.RIGHT))
        assertTrue(ScrollbarPosition.entries.contains(ScrollbarPosition.BOTH))

        assertEquals(3, ScrollbarVerticalAlignment.entries.size)
        assertTrue(ScrollbarVerticalAlignment.entries.contains(ScrollbarVerticalAlignment.TOP))
        assertTrue(ScrollbarVerticalAlignment.entries.contains(ScrollbarVerticalAlignment.CENTER))
        assertTrue(ScrollbarVerticalAlignment.entries.contains(ScrollbarVerticalAlignment.BOTTOM))
    }
}
